package bloody_mind.ethriaiaaddon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager für separate Modul-Konfigurationsdateien
 */
public class ConfigManager {

    private final EthriaIAAddon plugin;
    private final Map<String, FileConfiguration> moduleConfigs;
    private final Map<String, File> moduleConfigFiles;

    public ConfigManager(EthriaIAAddon plugin) {
        this.plugin = plugin;
        this.moduleConfigs = new HashMap<>();
        this.moduleConfigFiles = new HashMap<>();

        // Initialisiere bekannte Module
        initializeModuleConfigs();
    }

    /**
     * Initialisiert alle Modul-Konfigurationen
     */
    private void initializeModuleConfigs() {
        // CustomLight Modul
        createModuleConfig("customlight");

        // Hier können weitere Module hinzugefügt werden:
        // createModuleConfig("customanvil");
    }

    /**
     * Erstellt oder lädt eine Modul-Konfiguration
     */
    private void createModuleConfig(String moduleName) {
        String fileName = moduleName.toLowerCase() + ".yml";
        File configFile = new File(plugin.getDataFolder(), fileName);

        // Erstelle die Datei aus den Ressourcen falls sie nicht existiert
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        // Lade die Konfiguration
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Lade Defaults aus der JAR-Datei
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaultConfig);
        }

        // Speichere in den Maps
        moduleConfigs.put(moduleName, config);
        moduleConfigFiles.put(moduleName, configFile);

        plugin.getLogger().info("Modul-Konfiguration geladen: " + fileName);
    }

    /**
     * Gibt die Konfiguration für ein bestimmtes Modul zurück
     */
    public FileConfiguration getModuleConfig(String moduleName) {
        return moduleConfigs.get(moduleName.toLowerCase());
    }

    /**
     * Speichert eine Modul-Konfiguration
     */
    public void saveModuleConfig(String moduleName) {
        try {
            FileConfiguration config = moduleConfigs.get(moduleName.toLowerCase());
            File configFile = moduleConfigFiles.get(moduleName.toLowerCase());

            if (config != null && configFile != null) {
                config.save(configFile);
                plugin.getLogger().info("Modul-Konfiguration gespeichert: " + moduleName + ".yml");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Modul-Konfiguration " + moduleName + ": " + e.getMessage());
        }
    }

    /**
     * Lädt eine Modul-Konfiguration neu
     */
    public void reloadModuleConfig(String moduleName) {
        String moduleKey = moduleName.toLowerCase();
        File configFile = moduleConfigFiles.get(moduleKey);

        if (configFile != null && configFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Lade Defaults aus der JAR-Datei
            InputStream defaultStream = plugin.getResource(moduleKey + ".yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defaultConfig);
            }

            moduleConfigs.put(moduleKey, config);
            plugin.getLogger().info("Modul-Konfiguration neu geladen: " + moduleName + ".yml");
        }
    }

    /**
     * Lädt alle Modul-Konfigurationen neu
     */
    public void reloadAllModuleConfigs() {
        for (String moduleName : moduleConfigs.keySet()) {
            reloadModuleConfig(moduleName);
        }
        plugin.getLogger().info("Alle Modul-Konfigurationen neu geladen");
    }

    /**
     * Prüft ob ein Modul in der Hauptkonfiguration aktiviert ist
     */
    public boolean isModuleEnabled(String moduleName) {
        return plugin.getConfig().getBoolean("modules." + moduleName.toLowerCase() + ".enabled", false);
    }

    /**
     * Gibt alle verfügbaren Modul-Konfigurationen zurück
     */
    public Map<String, FileConfiguration> getAllModuleConfigs() {
        return new HashMap<>(moduleConfigs);
    }
}
