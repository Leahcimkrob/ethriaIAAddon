package bloody_mind.ethriaiaaddon;

import bloody_mind.ethriaiaaddon.modules.CustomLightModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
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
public class EthriaIAAddon extends JavaPlugin {
    
    private List<AddonModule> modules;
    private static EthriaIAAddon instance;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("Initialisiere EthriaIAAddon Framework...");
        
        // Module initialisieren
        initializeModules();
        
        // Module starten
        enableModules();
        
        getLogger().info("EthriaIAAddon Framework erfolgreich geladen mit " + modules.size() + " Modulen!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Deaktiviere EthriaIAAddon Module...");
        
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
}
