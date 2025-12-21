package bloody_mind.ethriaiaaddon;

import bloody_mind.ethriaiaaddon.modules.CustomLightModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Hauptklasse für EthriaIAAddon - Ein modulares Plugin-Framework für ItemsAdder-basierte Features
 * 
 * Geplante Module:
 * - CustomLight: Dynamisches Licht basierend auf Helm-ModelID
 * - CustomAnvil: Änderung von Item-ModelIDs über Custom-Amboss
 * - Weitere Module...
 */
public class EthriaIAAddon extends JavaPlugin implements TabCompleter {

    private List<AddonModule> modules;
    private static EthriaIAAddon instance;
    private LanguageManager languageManager;
    private ConfigManager configManager;
    private DynamicCommandManager dynamicCommandManager;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("Initialisiere EthriaIAAddon Framework...");
        
        // Erstelle Standard-Konfiguration falls sie nicht existiert
        saveDefaultConfig();

        // Initialisiere Sprachsystem
        languageManager = new LanguageManager(this);

        // Initialisiere Konfigurations-Manager
        configManager = new ConfigManager(this);

        // Initialisiere dynamischen Command-Manager
        dynamicCommandManager = new DynamicCommandManager(this);

        // Module initialisieren
        initializeModules();

        // Module starten
        enableModules();

        // Dynamische Commands registrieren (nach LanguageManager-Initialisierung)
        dynamicCommandManager.registerDynamicCommands();

        getLogger().info("EthriaIAAddon Framework erfolgreich geladen mit " + modules.size() + " Modulen!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Deaktiviere EthriaIAAddon Module...");

        // Dynamische Commands entfernen
        if (dynamicCommandManager != null) {
            dynamicCommandManager.unregisterDynamicCommands();
        }

        // Module sauber deaktivieren
        disableModules();

        getLogger().info("EthriaIAAddon Framework deaktiviert!");
        instance = null;
    }
    
    /**
     * Initialisiert alle verfügbaren Module
     */
    private void initializeModules() {
        modules = new ArrayList<>();
        
        // CustomLight Modul hinzufügen
        modules.add(new CustomLightModule(this));
        
        // Hier können weitere Module hinzugefügt werden:
        // modules.add(new CustomAnvilModule(this));
        // modules.add(new AnotherModule(this));
    }
    
    /**
     * Startet alle Module
     */
    private void enableModules() {
        for (AddonModule module : modules) {
            try {
                module.onEnable();
                getLogger().info("Modul '" + module.getName() + "' erfolgreich aktiviert");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Fehler beim Aktivieren von Modul '" + module.getName() + "':", e);
            }
        }
    }
    
    /**
     * Deaktiviert alle Module
     */
    private void disableModules() {
        if (modules != null) {
            for (AddonModule module : modules) {
                try {
                    module.onDisable();
                    getLogger().info("Modul '" + module.getName() + "' deaktiviert");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Fehler beim Deaktivieren von Modul '" + module.getName() + "':", e);
                }
            }
        }
    }
    
    /**
     * Lädt alle Module neu
     */
    public void reloadModules() {
        getLogger().info("Lade alle Module neu...");
        
        // Alle Module deaktivieren
        disableModules();
        
        // Konfigurationen neu laden
        reloadConfig();
        
        // Sprachsystem neu laden
        languageManager.reload();

        // Modul-Konfigurationen neu laden
        configManager.reloadAllModuleConfigs();

        // Dynamische Commands neu registrieren
        dynamicCommandManager.unregisterDynamicCommands();
        dynamicCommandManager.registerDynamicCommands();

        // Module neu initialisieren und starten
        initializeModules();
        enableModules();
        
        getLogger().info("Alle Module wurden neu geladen!");
    }
    
    /**
     * Gibt die Plugin-Instanz zurück
     */
    public static EthriaIAAddon getInstance() {
        return instance;
    }
    
    /**
     * Gibt den LanguageManager zurück
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Gibt den ConfigManager zurück
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gibt alle geladenen Module zurück
     */
    public List<AddonModule> getModules() {
        return new ArrayList<>(modules);
    }
    
    /**
     * Sucht ein spezifisches Modul nach Klasse
     */
    @SuppressWarnings("unchecked")
    public <T extends AddonModule> T getModule(Class<T> moduleClass) {
        for (AddonModule module : modules) {
            if (moduleClass.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Einheitliche Permission-Prüfung für alle Commands (Hauptbefehl und Aliases)
        if (!sender.hasPermission("ethriaiaaddon.admin")) {
            languageManager.sendMessage(sender, "general.no-permission");
            return true;
        }

        String subCommand = null;
        String[] effectiveArgs = args;
        
        // Prüfe ob es ein Alias aus der config.yml ist
        if (isConfigAlias(command.getName())) {
            subCommand = resolveConfigAlias(command.getName());
            effectiveArgs = args; // Bei direktem Alias sind args bereits die Module-Args
        } else if (command.getName().equalsIgnoreCase("ethriaiaaddon")) {
            // Hauptbefehl - erwarten Subcommand als erstes Argument
            if (args.length > 0) {
                subCommand = args[0].toLowerCase();
                effectiveArgs = Arrays.copyOfRange(args, 1, args.length);
            }
        }
        
        // Keine Subcommand -> Hilfe anzeigen
        if (subCommand == null) {
            showMainHelp(sender);
            return true;
        }
        
        // Behandle verschiedene Subcommands
        switch (subCommand.toLowerCase()) {
            case "help":
                showMainHelp(sender);
                return true;
                
            case "reload":
                reloadConfig();
                reloadModules();
                languageManager.sendMessage(sender, "general.config-reloaded");
                getLogger().info("GLOBAL RELOAD ausgeführt von: " + sender.getName() + " (alle Configs und Sprachdateien)");
                return true;
                
            case "customlight":
            case "clight":
            case "cl":
                return handleCustomLightCommand(sender, effectiveArgs);

            default:
                languageManager.sendMessage(sender, "general.unknown-command", "command", subCommand);
                languageManager.sendMessage(sender, "general.available-commands", "commands", "help, reload, customlight");
                break;
        }
        return true;
    }

    /**
     * Zeigt die Haupthilfe an
     */
    private void showMainHelp(CommandSender sender) {
        languageManager.sendMessage(sender, "main.help-header");
        languageManager.sendMessage(sender, "main.help-reload");
        languageManager.sendMessage(sender, "main.help-customlight");
        languageManager.sendMessage(sender, "main.help-usage");
    }

    /**
     * Behandelt CustomLight Commands mit Permission-Prüfung
     */
    private boolean handleCustomLightCommand(CommandSender sender, String[] args) {
        // Prüfe CustomLight-spezifische Permission ODER Admin-Permission
        if (!sender.hasPermission("ethriaiaaddon.customlight.use") && !sender.hasPermission("ethriaiaaddon.admin")) {
            languageManager.sendMessage(sender, "customlight.no-permission");
            return true;
        }

        CustomLightModule customLight = getModule(CustomLightModule.class);
        if (customLight != null) {
            return customLight.handleCommand(sender, args);
        } else {
            languageManager.sendMessage(sender, "customlight.module-not-loaded");
            return true;
        }
    }

    /**
     * Prüft ob ein Command ein Alias aus der config.yml ist
     */
    private boolean isConfigAlias(String commandName) {
        var aliasSection = getConfig().getConfigurationSection("general.command-aliases");
        return aliasSection != null && aliasSection.contains(commandName.toLowerCase());
    }

    /**
     * Löst einen Config-Alias zum entsprechenden Subcommand auf
     */
    private String resolveConfigAlias(String alias) {
        if (getConfig().getConfigurationSection("general.command-aliases") != null) {
            return getConfig().getString("general.command-aliases." + alias.toLowerCase(), alias);
        }
        return alias;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Permission-Check für Tab-Completion
        if (!sender.hasPermission("ethriaiaaddon.admin")) {
            return getTabCompletionFromLanguage("tabcomplete.no-permission-tab");
        }

        // Behandle direkte Alias-Commands
        if (isConfigAlias(command.getName())) {
            String resolvedCommand = resolveConfigAlias(command.getName());

            switch (resolvedCommand.toLowerCase()) {
                case "customlight":
                    if (sender.hasPermission("ethriaiaaddon.customlight.use") || sender.hasPermission("ethriaiaaddon.admin")) {
                        CustomLightModule customLight = getModule(CustomLightModule.class);
                        if (customLight != null) {
                            return customLight.handleTabComplete(sender, args);
                        }
                    }
                    break;
                case "help":
                case "reload":
                    // Diese Befehle haben keine weiteren Completions
                    return Collections.emptyList();
            }
        } else if (command.getName().equalsIgnoreCase("ethriaiaaddon")) {

            if (args.length == 1) {
                // Haupt-Befehle und Module vorschlagen
                List<String> completions = new ArrayList<>();
                completions.add("help");
                completions.add("reload");
                if (sender.hasPermission("ethriaiaaddon.customlight.use") || sender.hasPermission("ethriaiaaddon.admin")) {
                    completions.add("customlight");
                }
                return completions;
            } else if (args.length >= 2) {
                String subCommand = args[0].toLowerCase();
                String[] moduleArgs = Arrays.copyOfRange(args, 1, args.length);
                
                switch (subCommand) {
                    case "customlight":
                        if (sender.hasPermission("ethriaiaaddon.customlight.use") || sender.hasPermission("ethriaiaaddon.admin")) {
                            CustomLightModule customLight = getModule(CustomLightModule.class);
                            if (customLight != null) {
                                return customLight.handleTabComplete(sender, moduleArgs);
                            }
                        }
                        break;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Hilfsmethode um Tab-Completion aus Sprachdateien zu laden
     */
    private List<String> getTabCompletionFromLanguage(String path) {
        String completionString = languageManager.getMessage(path);
        if (completionString.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(completionString.split(" "));
    }
}
