package bloody_mind.ethriaiaaddon.modules;

import bloody_mind.ethriaiaaddon.AddonModule;
import bloody_mind.ethriaiaaddon.EthriaIAAddon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * CustomLight Modul - Erzeugt dynamisches Licht basierend auf der ModelID des Helms
 */
public class CustomLightModule extends AddonModule implements Listener {

    private final Map<Integer, Integer> modelIdToLightLevel = new HashMap<>();
    private final Map<UUID, Integer> lastModelId = new HashMap<>();
    private final Map<UUID, String> lastLocationKey = new HashMap<>();
    private final Map<UUID, Set<Location>> lightBlockLocations = new HashMap<>();
    private List<String> commandAliases = new ArrayList<>();
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
        if (!getConfig().getBoolean("modules.customlight.enabled", true)) {
            plugin.getLogger().info("CustomLight Modul ist deaktiviert");
            return;
        }

        plugin.getLogger().info("Lade CustomLight Modul...");

        // Konfiguration laden
        loadConfigValues();


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

    /**
     * Behandelt Commands für das CustomLight Modul
     */
    public boolean handleCommand(CommandSender sender, String[] args) {
        // Prüfe CustomLight-spezifische Permission ODER Admin-Permission
        if (!sender.hasPermission("ethriaiaaddon.customlight.use") && !sender.hasPermission("ethriaiaaddon.admin")) {
            plugin.getLanguageManager().sendMessage(sender, "customlight.no-permission");
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            List<String> helpMessages = plugin.getLanguageManager().getMessageList("customlight.help");
            if (helpMessages != null) {
                for (String helpEntry : helpMessages) {
                    sender.sendMessage(helpEntry);
                }
            }
            return true;
        }

        // Modul-spezifischer Reload (nur customlight.yml)
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            // Für Reload benötigt man Admin-Permission (entweder Modul-Admin oder Global-Admin)
            if (sender.hasPermission("ethriaiaaddon.customlight.admin") || sender.hasPermission("ethriaiaaddon.admin")) {
                // Nur CustomLight-Konfiguration neu laden
                plugin.getConfigManager().reloadModuleConfig("customlight");
                loadConfigValues();

                plugin.getLanguageManager().sendMessage(sender, "customlight.config-reloaded");
                plugin.getLogger().info("CustomLight-Konfiguration neu geladen von: " + sender.getName());
            } else {
                plugin.getLanguageManager().sendMessage(sender, "customlight.no-permission");
            }
            return true;
        }

        plugin.getLanguageManager().sendMessage(sender, "customlight.unknown-command");
        return true;
    }

    /**
     * Behandelt Tab-Completion für das CustomLight Modul
     */
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("help");
            if (sender.hasPermission("ethriaiaaddon.customlight.admin") || sender.hasPermission("ethriaiaaddon.admin")) {
                completions.add("reload");
            }
            return completions;
        }
        return Collections.emptyList();
    }

    private void loadConfigValues() {
        // Lade die separate CustomLight-Konfiguration
        FileConfiguration config = plugin.getConfigManager().getModuleConfig("customlight");
        if (config == null) {
            plugin.getLogger().warning("CustomLight-Konfiguration konnte nicht geladen werden!");
            return;
        }
        
        modelIdToLightLevel.clear();

        // Lade CustomLight-spezifische Konfiguration aus customlight.yml
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
                    plugin.getLogger().warning("Ungültige ModelID in CustomLight-Konfiguration: " + key);
                }
            }
        }

        this.removalRadius = config.getInt("radius", 10);
        this.updateInterval = config.getInt("update-interval", 1);
        this.removeAllOnHelmetOff = config.getBoolean("remove-all-on-helmet-off", true);
        this.maxLightBlocksPerPlayer = config.getInt("max-light-blocks-per-player", 3);

        commandAliases = config.getStringList("command-aliases");
        
        plugin.getLogger().info("CustomLight-Konfiguration geladen: " + modelIdToLightLevel.size() + " leuchtende Items");
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Nur bei Player-Inventar reagieren
        if (event.getInventory().getType() != InventoryType.PLAYER) return;

        boolean shouldCheck = false;

        // Prüfe ob es sich um den Helmet-Slot handelt (Slot 39 im Player-Inventar)
        if (event.getSlot() == 39) {
            shouldCheck = true;
        }

        // Prüfe auch Shift-Click auf Items die potentiell Helme sein könnten
        else if (event.isShiftClick() && event.getCurrentItem() != null) {
            ItemStack item = event.getCurrentItem();
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                int modelId = item.getItemMeta().getCustomModelData();
                if (modelIdToLightLevel.containsKey(modelId)) {
                    shouldCheck = true;
                }
            }
        }

        if (shouldCheck) {
            // Verzögere die Überprüfung um 2 Ticks für vollständige Inventory-Update
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    handleHelmetChange(player);
                }
            }, 2L);
        }
    }

    /**
     * Event Handler: Inventar-Drag - Überprüfe Helmet-Slot Änderungen
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Nur bei Player-Inventar reagieren
        if (event.getInventory().getType() != InventoryType.PLAYER) return;

        // Prüfe ob der Helmet-Slot betroffen ist (Slot 39)
        if (event.getRawSlots().contains(39)) {
            // Verzögere die Überprüfung um 2 Ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    handleHelmetChange(player);
                }
            }, 2L);
        }
    }

    /**
     * Event Handler: Item Drop - Überprüfe ob ein Light-Helmet gedroppt wurde
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelId = item.getItemMeta().getCustomModelData();
            if (modelIdToLightLevel.containsKey(modelId)) {
                // Verzögere die Überprüfung um 1 Tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        handleHelmetChange(player);
                    }
                }, 1L);
            }
        }
    }

    /**
     * Event Handler: Item Break - Überprüfe ob ein Light-Helmet kaputt gegangen ist
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getBrokenItem();

        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelId = item.getItemMeta().getCustomModelData();
            if (modelIdToLightLevel.containsKey(modelId)) {
                // Verzögere die Überprüfung um 1 Tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        handleHelmetChange(player);
                    }
                }, 1L);
            }
        }
    }

    /**
     * Behandelt Helmet-Änderungen und aktualisiert entsprechend die Lichtblöcke
     */
    private void handleHelmetChange(Player player) {
        try {
            if (!player.isOnline()) return;

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
                        plugin.getLogger().fine("Alle Lichtblöcke für " + player.getName() + " entfernt (Helmet abgenommen)");
                    }
                } else {
                    plugin.getLogger().fine("Light-Helmet für " + player.getName() + " erkannt: ModelID " + newModelId);
                }
                // Ansonsten wird das neue Licht durch den normalen Task-Loop gesetzt
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Behandeln von Helmet-Änderung für " + player.getName() + ": " + e.getMessage());
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
}
