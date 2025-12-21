# EthriaIAAddon Framework

## [ENG]
A modular plugin framework for Paper servers with ItemsAdder integration.

### Features
- **Modular Architecture**: Easy to extend with new modules
- **Config-based Aliases**: All command aliases managed in config.yml
- **Multilingual Support**: German and English language files
- **Hot-Reload System**: Two-tier reload system (global and module-specific)
- **Hierarchical Permissions**: Admin permissions override module-specific permissions

### Current Modules

#### CustomLight Module
Items with custom model IDs get dynamic light effects.
- Model ID and light level configured in customlight.yml
- Air block required above player's head
- Real-time light block placement/removal

### Commands

#### Main Commands:
```
/ethriaiaaddon              # Show help
/ethriaiaaddon reload       # Reload ALL configurations and language files
/ethriaiaaddon customlight  # CustomLight module help
/ethriaiaaddon customlight reload  # Reload only CustomLight configuration
```

#### Command Aliases (configurable in config.yml):
```
/eia                        # Main command alias
/iaaddon                    # Alternative main command
/plotiaaddon               # Plot-specific alias
/clight                     # Direct CustomLight access
/cl                         # Short CustomLight alias
/reload                     # Direct global reload
```

### Permissions
```
ethriaiaaddon.admin                    # Access to ALL commands and modules (supersedes others)
ethriaiaaddon.customlight.use         # Basic CustomLight module access
ethriaiaaddon.customlight.admin       # CustomLight admin functions (reload, etc.)
```

### Configuration Structure
```
plugins/ethriaiaaddon/
├── config.yml              # Main config (language, aliases, module status)
├── customlight.yml          # CustomLight module configuration
├── lang/
│   ├── de.yml              # German messages
│   └── eng.yml             # English messages
```

---

## [DE]
Ein modulares Plugin-Framework für Paper-Server mit ItemsAdder-Integration.

### Features
- **Modulare Architektur**: Einfach erweiterbar mit neuen Modulen
- **Config-basierte Aliases**: Alle Command-Aliases in config.yml verwaltet
- **Mehrsprachigkeit**: Deutsche und englische Sprachdateien
- **Hot-Reload-System**: Zwei-stufiges Reload-System (global und modul-spezifisch)
- **Hierarchische Permissions**: Admin-Permissions übersteuern modul-spezifische Permissions

### Aktuelle Module

#### CustomLight Modul
Items mit Custom-Model-IDs erhalten dynamische Lichteffekte.
- Modell-ID und Lichtstärke in customlight.yml konfiguriert
- Luftblock über dem Spielerkopf erforderlich
- Echtzeit-Lichtblock-Platzierung/-Entfernung

### Befehle

#### Hauptbefehle:
```
/ethriaiaaddon              # Hilfe anzeigen
/ethriaiaaddon reload       # ALLE Konfigurationen und Sprachdateien neu laden
/ethriaiaaddon customlight  # CustomLight-Modul-Hilfe
/ethriaiaaddon customlight reload  # Nur CustomLight-Konfiguration neu laden
```

#### Command-Aliases (konfigurierbar in config.yml):
```
/eia                        # Hauptbefehl-Alias
/iaaddon                    # Alternativer Hauptbefehl
/plotiaaddon               # Plot-spezifischer Alias
/clight                     # Direkter CustomLight-Zugriff
/cl                         # Kurzer CustomLight-Alias
/reload                     # Direkter Global-Reload
```

### Permissions
```
ethriaiaaddon.admin                    # Zugriff auf ALLE Befehle und Module (übersteuert andere)
ethriaiaaddon.customlight.use         # Basis-Zugriff auf CustomLight-Modul
ethriaiaaddon.customlight.admin       # CustomLight-Admin-Funktionen (Reload, etc.)
```

### Konfigurationsstruktur
```
plugins/ethriaiaaddon/
├── config.yml              # Hauptkonfiguration (Sprache, Aliases, Modul-Status)
├── customlight.yml          # CustomLight-Modul-Konfiguration
├── lang/
│   ├── de.yml              # Deutsche Nachrichten
│   └── eng.yml             # Englische Nachrichten
```

