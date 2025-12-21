package bloody_mind.ethriaiaaddon;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * Manager für dynamische Command-Registrierung basierend auf config.yml
 */
public class DynamicCommandManager {

    private final EthriaIAAddon plugin;
    private CommandMap commandMap;

    public DynamicCommandManager(EthriaIAAddon plugin) {
        this.plugin = plugin;
        initializeCommandMap();
    }

    /**
     * Initialisiert die CommandMap über Reflection
     */
    private void initializeCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Zugriff auf CommandMap: " + e.getMessage());
        }
    }

    /**
     * Registriert alle Aliases aus der config.yml als echte Commands
     */
    public void registerDynamicCommands() {
        if (commandMap == null) {
            plugin.getLogger().warning("CommandMap nicht verfügbar - Aliases werden nicht registriert");
            return;
        }

        // Hole alle Aliases aus der Konfiguration
        var aliasSection = plugin.getConfig().getConfigurationSection("general.command-aliases");
        if (aliasSection == null) {
            plugin.getLogger().warning("Keine command-aliases in der Konfiguration gefunden");
            return;
        }

        Set<String> aliases = aliasSection.getKeys(false);

        for (String alias : aliases) {
            registerCommand(alias);
        }

        plugin.getLogger().info("Dynamisch registrierte Commands: " + aliases.size());
    }

    /**
     * Registriert einen einzelnen Command
     */
    private void registerCommand(String name) {
        try {
            // Erstelle PluginCommand über Reflection
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            PluginCommand command = constructor.newInstance(name, plugin);
            command.setExecutor(plugin);
            command.setTabCompleter(plugin);
            command.setPermission("ethriaiaaddon.admin");

            // Registriere den Command
            commandMap.register(plugin.getName(), command);

            plugin.getLogger().info("Command-Alias registriert: /" + name);

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Registrieren von Command '" + name + "': " + e.getMessage());
        }
    }

    /**
     * Entfernt alle dynamisch registrierten Commands
     */
    public void unregisterDynamicCommands() {
        if (commandMap == null) return;

        var aliasSection = plugin.getConfig().getConfigurationSection("general.command-aliases");
        if (aliasSection == null) return;

        Set<String> aliases = aliasSection.getKeys(false);

        for (String alias : aliases) {
            Command command = commandMap.getCommand(alias);
            if (command instanceof PluginCommand && ((PluginCommand) command).getPlugin() == plugin) {
                command.unregister(commandMap);
                plugin.getLogger().info("Command-Alias entfernt: /" + alias);
            }
        }
    }
}
