package bloody_mind.ethriaiaaddon;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manager für das mehrsprachige Nachrichtensystem
 */
public class LanguageManager {

    private final EthriaIAAddon plugin;
    private FileConfiguration langConfig;
    private String currentLanguage;

    public LanguageManager(EthriaIAAddon plugin) {
        this.plugin = plugin;
        this.currentLanguage = plugin.getConfig().getString("general.language", "de");
        loadLanguage();
    }

    /**
     * Lädt die Sprachdatei basierend auf der aktuellen Einstellung
     */
    public void loadLanguage() {
        String language = plugin.getConfig().getString("general.language", "de");

        // Überprüfe ob die Sprache unterstützt wird
        if (!language.equals("de") && !language.equals("eng")) {
            plugin.getLogger().warning("Unsupported language '" + language + "', falling back to 'de'");
            language = "de";
        }

        this.currentLanguage = language;

        // Erstelle lang-Ordner falls er nicht existiert
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Erstelle Sprachdatei falls sie nicht existiert
        File langFile = new File(langDir, language + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }

        // Lade die Sprachdatei
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Lade Defaults aus der JAR-Datei
        InputStream defaultStream = plugin.getResource("lang/" + language + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultConfig);
        }

        plugin.getLogger().info("Sprache geladen: " + language);
    }

    /**
     * Gibt eine übersetzte Nachricht zurück
     */
    public String getMessage(String path) {
        return getMessage(path, "");
    }

    /**
     * Gibt eine übersetzte Nachricht mit Platzhaltern zurück
     */
    public String getMessage(String path, String... placeholders) {
        String message = langConfig.getString(path, "Missing message: " + path);

        // Ersetze Platzhalter
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Gibt eine Liste von übersetzten Nachrichten zurück
     */
    @SuppressWarnings("unchecked")
    public List<String> getMessageList(String path) {
        List<String> messages = (List<String>) langConfig.getList(path);
        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                messages.set(i, ChatColor.translateAlternateColorCodes('&', messages.get(i)));
            }
        }
        return messages;
    }

    /**
     * Sendet eine übersetzte Nachricht an einen Empfänger
     */
    public void sendMessage(org.bukkit.command.CommandSender sender, String path, String... placeholders) {
        sender.sendMessage(getMessage(path, placeholders));
    }

    /**
     * Gibt die aktuelle Sprache zurück
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Lädt das Sprachsystem neu
     */
    public void reload() {
        loadLanguage();
    }
}
