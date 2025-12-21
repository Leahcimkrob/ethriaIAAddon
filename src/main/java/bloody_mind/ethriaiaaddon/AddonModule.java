package bloody_mind.ethriaiaaddon;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Abstrakte Basisklasse für alle EthriaIAAddon Module
 */
public abstract class AddonModule {

    protected final EthriaIAAddon plugin;
    protected final String moduleName;

    public AddonModule(EthriaIAAddon plugin, String moduleName) {
        this.plugin = plugin;
        this.moduleName = moduleName;
    }

    /**
     * Wird aufgerufen, wenn das Modul aktiviert wird
     */
    public abstract void onEnable();

    /**
     * Wird aufgerufen, wenn das Modul deaktiviert wird
     */
    public abstract void onDisable();

    /**
     * Gibt den Namen des Moduls zurück
     */
    public String getName() {
        return moduleName;
    }

    /**
     * Gibt die Plugin-Instanz zurück
     */
    protected EthriaIAAddon getPlugin() {
        return plugin;
    }

    /**
     * Gibt die Konfiguration zurück
     */
    protected FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Prüft ob das Modul in der Konfiguration aktiviert ist
     */
    protected boolean isModuleEnabled() {
        return getConfig().getBoolean("modules." + moduleName.toLowerCase() + ".enabled", true);
    }
}
