package bloody_mind.ethriaiaaddon.modules;

import bloody_mind.ethriaiaaddon.AddonModule;
import bloody_mind.ethriaiaaddon.EthriaIAAddon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * CustomLight Modul - Erzeugt dynamisches Licht basierend auf der ModelID des Helms
 */
public class CustomLightModule extends AddonModule implements CommandExecutor, TabCompleter, Listener {

    private final Map<Integer, Integer> modelIdToLightLevel = new HashMap<>();
    private String reloadMessage;
    private String nopermission;
    private String nocommand;
    private final Map<UUID, Integer> lastModelId = new HashMap<>();
    private final Map<UUID, String> lastLocationKey = new HashMap<>();
    private final Map<UUID, Set<Location>> lightBlockLocations = new HashMap<>();
    private List<String> commandAliases = new ArrayList<>();
    private List<String> helpMessages = new ArrayList<>();
    private int removalRadius = 1;
    private int updateInterval = 10;
    private boolean removeAllOnHelmetOff = true;
    private int maxLightBlocksPerPlayer = 3;

    private BukkitTask lightTask;

    public CustomLightModule(EthriaIAAddon plugin) {
        super(plugin, "CustomLight");
    }

    @Override
    public void onEnable() {
        if (!isModuleEnabled()) {
            plugin.getLogger().info("CustomLight Modul ist deaktiviert");
            return;
        }

        plugin.getLogger().info("Lade CustomLight Modul...");

        // Konfiguration laden
        loadConfigValues();

        // Commands registrieren
        registerCommands();

        // Events registrieren
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Licht-Task starten
        startLightTask();

        plugin.getLogger().info("CustomLight Modul aktiviert!");
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Deaktiviere CustomLight Modul...");

        // Task stoppen
        if (lightTask != null) {
            lightTask.cancel();
            lightTask = null;
        }

        // Alle Lichtblöcke entfernen
        removeAllLightBlocks();

        plugin.getLogger().info("CustomLight Modul deaktiviert!");
    }

    private void startLightTask() {
        if (lightTask != null) {
            lightTask.cancel();
        }

        lightTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int modelId = -1;
                    ItemStack helmet = player.getInventory().getHelmet();
                    if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasCustomModelData()) {
                        modelId = helmet.getItemMeta().getCustomModelData();
                    }
                    String locationKey = player.getWorld().getName() + ":"
                            + player.getLocation().getBlockX() + ":"
                            + player.getLocation().getBlockY() + ":"
                            + player.getLocation().getBlockZ();

                    Location lightLocation = player.getLocation().add(0, 2, 0).getBlock().getLocation();

                    if (modelIdToLightLevel.containsKey(modelId)) {
                        placeAndTrackLightBlock(player, modelId, lightLocation);
                        removeDistantLightBlocks(player, lightLocation);
                        trimPlayerLightBlocks(player);
                    } else {
                        if (removeAllOnHelmetOff) {
                            removeAllLightBlocks(player);
                        }
                    }

                    if (lastModelId.getOrDefault(player.getUniqueId(), -2) != modelId
                            || !locationKey.equals(lastLocationKey.get(player.getUniqueId()))) {
                        lastModelId.put(player.getUniqueId(), modelId);
                        lastLocationKey.put(player.getUniqueId(), locationKey);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, updateInterval);
    }

    private void registerCommands() {
        PluginCommand cmd = plugin.getCommand("ethriaiaaddon");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        modelIdToLightLevel.clear();

        if (config.isConfigurationSection("glowing_items")) {
            Set<String> keys = config.getConfigurationSection("glowing_items").getKeys(false);
            for (String key : keys) {
                try {
                    int id = Integer.parseInt(key);
                    int level = config.getInt("glowing_items." + key);
                    if (level >= 0 && level <= 15) {
                        modelIdToLightLevel.put(id, level);
                    }
                } catch (NumberFormatException e) {
                    // ignore invalid config entry
                }
            }
        }

        this.removalRadius = config.getInt("radius", 10);
        this.updateInterval = config.getInt("update-interval", 1);
        this.removeAllOnHelmetOff = config.getBoolean("remove-all-on-helmet-off", true);
        this.maxLightBlocksPerPlayer = config.getInt("max-light-blocks-per-player", 3);

        reloadMessage = config.getString("reload-message", "§7[§6Ethria-Light§7] §3Konfiguration neu geladen.");
        nopermission = config.getString("nopermission", "§7[§6Ethria-Light§7] §3Du hast keine Berechtigung für diesen Befehl.");
        nocommand = config.getString("nocommand", "§7[§6Ethria-Light§7] §4Unbekannter Befehl. Benutze /ethriaiaaddon help");
        commandAliases = config.getStringList("command-aliases");
        helpMessages = config.getStringList("help");

        if (helpMessages.isEmpty()) {
            helpMessages = Arrays.asList(
                    "§7[§6Ethria-Light§7] §3Hilfe:",
                    "§3/ethriaiaaddon reload - Konfiguration neu laden",
                    "§3/ethriaiaaddon help - Zeigt diese Hilfe an"
            );
        }
    }

    private void placeAndTrackLightBlock(Player player, int modelId, Location lightLocation) {
        int level = modelIdToLightLevel.get(modelId);
        Block lightBlock = lightLocation.getBlock();
        if (lightBlock.getType() == Material.AIR || lightBlock.getType() == Material.LIGHT) {
            BlockData data = Bukkit.createBlockData(Material.LIGHT);
            if (data instanceof Levelled) {
                ((Levelled) data).setLevel(level);
            }
            lightBlock.setBlockData(data, false);
        }
        Set<Location> locations = lightBlockLocations.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashSet<>());
        locations.add(lightLocation);
    }

    private void removeDistantLightBlocks(Player player, Location currentLightLoc) {
        Set<Location> locations = lightBlockLocations.get(player.getUniqueId());
        if (locations == null) return;
        Iterator<Location> it = locations.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            // Prüfe ob die Locations in derselben Welt sind
            if (!loc.getWorld().equals(currentLightLoc.getWorld())) {
                // Lichtblock in anderer Welt entfernen
                Block block = loc.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR);
                }
                it.remove();
                continue;
            }

            // Normale Distanz-Prüfung
            if (loc.distance(currentLightLoc) > removalRadius) {
                Block block = loc.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR);
                }
                it.remove();
            }
        }
    }

    private void trimPlayerLightBlocks(Player player) {
        Set<Location> locations = lightBlockLocations.get(player.getUniqueId());
        if (locations == null) return;
        while (locations.size() > maxLightBlocksPerPlayer) {
            Location oldest = locations.iterator().next();
            Block block = oldest.getBlock();
            if (block.getType() == Material.LIGHT) {
                block.setType(Material.AIR);
            }
            locations.remove(oldest);
        }
    }

    private void removeAllLightBlocks(Player player) {
        Set<Location> locations = lightBlockLocations.remove(player.getUniqueId());
        if (locations != null) {
            for (Location loc : locations) {
                Block block = loc.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private void removeAllLightBlocks() {
        for (UUID playerId : lightBlockLocations.keySet()) {
            Set<Location> locations = lightBlockLocations.get(playerId);
            if (locations != null) {
                for (Location loc : locations) {
                    Block block = loc.getBlock();
                    if (block.getType() == Material.LIGHT) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
        lightBlockLocations.clear();
        lastModelId.clear();
        lastLocationKey.clear();
    }

    /**
     * Event Handler: Inventar-Klick - Überprüfe Helmet-Slot Änderungen
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Prüfe ob es sich um den Helmet-Slot handelt
        if (event.getSlot() == 39 && event.getInventory().getType() == InventoryType.PLAYER) {
            // Verzögere die Überprüfung um 1 Tick, damit das Item korrekt gesetzt ist
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                handleHelmetChange(player);
            }, 1L);
        }

        // Prüfe auch Shift-Click auf Items die potentiell Helme sein könnten
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            ItemStack item = event.getCurrentItem();
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                int modelId = item.getItemMeta().getCustomModelData();
                if (modelIdToLightLevel.containsKey(modelId)) {
                    // Verzögere die Überprüfung um 1 Tick
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        handleHelmetChange(player);
                    }, 1L);
                }
            }
        }
    }

    /**
     * Event Handler: Inventar-Drag - Überprüfe Helmet-Slot Änderungen
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Prüfe ob der Helmet-Slot betroffen ist
        if (event.getRawSlots().contains(39)) {
            // Verzögere die Überprüfung um 1 Tick
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                handleHelmetChange(player);
            }, 1L);
        }
    }

    /**
     * Behandelt Helmet-Änderungen und aktualisiert entsprechend die Lichtblöcke
     */
    private void handleHelmetChange(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        int newModelId = -1;

        if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasCustomModelData()) {
            newModelId = helmet.getItemMeta().getCustomModelData();
        }

        int oldModelId = lastModelId.getOrDefault(player.getUniqueId(), -2);

        // Wenn sich das Helmet geändert hat
        if (oldModelId != newModelId) {
            lastModelId.put(player.getUniqueId(), newModelId);

            // Wenn das neue Helmet kein Licht erzeugt oder kein Helmet vorhanden
            if (!modelIdToLightLevel.containsKey(newModelId)) {
                if (removeAllOnHelmetOff) {
                    removeAllLightBlocks(player);
                }
            }
            // Ansonsten wird das neue Licht durch den normalen Task-Loop gesetzt
        }
    }

    /**
     * Event Handler: Weltenwechsel - Entfernt alle Lichtblöcke des Spielers
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("Spieler " + player.getName() + " hat die Welt gewechselt. Entferne alle Lichtblöcke.");
        removeAllLightBlocks(player);
    }

    /**
     * Event Handler: Spieler verlässt den Server - Entfernt alle Lichtblöcke des Spielers
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeAllLightBlocks(player);
        // Cleanup der Maps
        lastModelId.remove(player.getUniqueId());
        lastLocationKey.remove(player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("ethriaiaaddon") || commandAliases.contains(label.toLowerCase())) {
            if (!sender.hasPermission("ethriaiaaddon.use")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', nopermission));
                return true;
            }

            if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
                for (String helpEntry : helpMessages) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', helpEntry));
                }
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ethriaiaaddon.reload")) {
                    plugin.reloadModules();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMessage));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', nopermission));
                }
                return true;
            }

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', nocommand));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ((command.getName().equalsIgnoreCase("ethriaiaaddon") || commandAliases.contains(alias.toLowerCase())) && args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            completions.add("help");
            return completions;
        }
        return Collections.emptyList();
    }
}
