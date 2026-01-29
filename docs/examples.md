# Examples

Real-world examples demonstrating YamlAnnotations features.

## Minecraft Plugin Configuration

A complete Minecraft plugin configuration example:

```java
@YamlFile(header = """
    ===========================================
    MyPlugin Configuration
    ===========================================

    Documentation: https://example.com/docs
    Support: https://discord.gg/example
    """,
    fileName = "config.yml",
    lenient = Leniency.LENIENT
)
public class PluginConfig extends YamlFileInterface {

    // ===== General Settings =====

    @YamlComment("Enable debug logging")
    @YamlKey("general.debug")
    public boolean debug = false;

    @YamlComment("Language code (en, de, fr, es)")
    @YamlKey("general.language")
    public String language = "en";

    @YamlComment("Check for updates on startup")
    @YamlKey("general.check-updates")
    public boolean checkUpdates = true;

    // ===== Database Settings =====

    @YamlComment("Database type: sqlite, mysql, postgresql")
    @YamlKey("database.type")
    public String dbType = "sqlite";

    @YamlKey("database.host")
    public String dbHost = "localhost";

    @YamlKey("database.port")
    public int dbPort = 3306;

    @YamlKey("database.name")
    public String dbName = "myplugin";

    @YamlKey("database.username")
    public String dbUsername = "root";

    @YamlKey("database.password")
    public String dbPassword = "";

    // ===== Feature Toggles =====

    @YamlComment("Enable the economy integration")
    @YamlKey("features.economy")
    public boolean economyEnabled = true;

    @YamlComment("Enable PlaceholderAPI support")
    @YamlKey("features.placeholders")
    public boolean placeholdersEnabled = true;

    // ===== Limits =====

    @YamlComment("Maximum homes per player (-1 for unlimited)")
    @YamlKey("limits.max-homes")
    public int maxHomes = 3;

    @YamlComment("Teleport cooldown in seconds")
    @YamlKey("limits.teleport-cooldown")
    public int teleportCooldown = 5;

    // ===== Messages =====

    @YamlKey("messages.prefix")
    public String messagePrefix = "&8[&6MyPlugin&8] &r";

    @YamlKey("messages.no-permission")
    public String noPermissionMsg = "&cYou don't have permission!";

    @YamlKey("messages.player-only")
    public String playerOnlyMsg = "&cThis command is for players only!";
}
```

**Usage in plugin:**

```java
public class MyPlugin extends JavaPlugin {
    private PluginConfig config;

    @Override
    public void onEnable() {
        try {
            config = new PluginConfig().load(this);

            if (config.debug) {
                getLogger().info("Debug mode enabled");
            }
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public PluginConfig getPluginConfig() {
        return config;
    }
}
```

---

## Game Settings with Records

Using records for structured game configuration:

```java
// Define records for clean data structures
public record WorldSettings(
    String name,
    String generator,
    long seed,
    boolean allowPvP,
    List<String> allowedGamemodes
) {}

public record SpawnPoint(String world, double x, double y, double z, float yaw, float pitch) {}

public record EconomySettings(
    String currency,
    double startingBalance,
    double maxBalance,
    Map<String, Double> prices
) {}

// Main configuration
@YamlFile(header = "Game Server Configuration")
public class GameConfig extends YamlFileInterface {

    @YamlComment("Main world settings")
    @YamlKey("worlds.main")
    public WorldSettings mainWorld = new WorldSettings(
        "world",
        "default",
        12345L,
        true,
        List.of("SURVIVAL", "ADVENTURE")
    );

    @YamlComment("Creative world settings")
    @YamlKey("worlds.creative")
    public WorldSettings creativeWorld = new WorldSettings(
        "creative",
        "flat",
        0L,
        false,
        List.of("CREATIVE")
    );

    @YamlComment("Server spawn location")
    @YamlKey("spawn")
    public SpawnPoint spawn = new SpawnPoint("world", 0, 64, 0, 0f, 0f);

    @YamlComment("Economy configuration")
    @YamlKey("economy")
    public EconomySettings economy = new EconomySettings(
        "coins",
        100.0,
        1000000.0,
        Map.of(
            "diamond", 100.0,
            "emerald", 50.0,
            "gold_ingot", 10.0
        )
    );
}
```

**Generated YAML:**

```yaml
# Game Server Configuration

# Main world settings
worlds:
  main:
    name: world
    generator: default
    seed: 12345
    allowPvP: true
    allowedGamemodes:
      - SURVIVAL
      - ADVENTURE

  # Creative world settings
  creative:
    name: creative
    generator: flat
    seed: 0
    allowPvP: false
    allowedGamemodes:
      - CREATIVE

# Server spawn location
spawn:
  world: world
  x: 0.0
  y: 64.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

# Economy configuration
economy:
  currency: coins
  startingBalance: 100.0
  maxBalance: 1000000.0
  prices:
    diamond: 100.0
    emerald: 50.0
    gold_ingot: 10.0
```

---

## User Data Storage

Storing player/user data with maps:

```java
public record PlayerData(
    UUID uuid,
    String lastKnownName,
    long firstJoin,
    long lastSeen,
    int playtimeMinutes,
    List<String> achievements
) {}

public class PlayerDatabase extends YamlFileInterface {

    @YamlKey("players")
    public Map<String, PlayerData> players = new LinkedHashMap<>();

    public void savePlayer(PlayerData data) {
        players.put(data.uuid().toString(), data);
    }

    public PlayerData getPlayer(UUID uuid) {
        return players.get(uuid.toString());
    }
}
```

---

## Multi-Language Support

Configuration for multi-language messages:

```java
public record MessageSet(
    String welcome,
    String goodbye,
    String levelUp,
    String noPermission,
    Map<String, String> custom
) {}

public class MessagesConfig extends YamlFileInterface {

    @YamlKey("default-language")
    public String defaultLanguage = "en";

    @YamlKey("languages.en")
    public MessageSet english = new MessageSet(
        "Welcome to the server, {player}!",
        "Goodbye, {player}! See you soon!",
        "Congratulations! You reached level {level}!",
        "You don't have permission to do that.",
        Map.of(
            "daily-reward", "You claimed your daily reward!",
            "vote-thanks", "Thanks for voting!"
        )
    );

    @YamlKey("languages.de")
    public MessageSet german = new MessageSet(
        "Willkommen auf dem Server, {player}!",
        "Auf Wiedersehen, {player}!",
        "Glückwunsch! Du hast Level {level} erreicht!",
        "Du hast keine Berechtigung dafür.",
        Map.of(
            "daily-reward", "Du hast deine tägliche Belohnung erhalten!",
            "vote-thanks", "Danke fürs Abstimmen!"
        )
    );

    public MessageSet getMessages(String language) {
        return switch (language) {
            case "de" -> german;
            default -> english;
        };
    }
}
```

---

## Shop Configuration

A shop system with items and prices:

```java
public record ShopItem(
    String material,
    String displayName,
    List<String> lore,
    double buyPrice,
    double sellPrice,
    int maxStack
) {}

public record ShopCategory(
    String name,
    String icon,
    List<ShopItem> items
) {}

@YamlFile(fileName = "shop.yml")
public class ShopConfig extends YamlFileInterface {

    @YamlComment("Shop GUI title")
    @YamlKey("title")
    public String title = "&6&lServer Shop";

    @YamlComment("Shop categories")
    @YamlKey("categories")
    public Map<String, ShopCategory> categories = new LinkedHashMap<>();

    public ShopConfig() {
        categories.put("blocks", new ShopCategory(
            "Building Blocks",
            "BRICKS",
            List.of(
                new ShopItem("STONE", "&7Stone", List.of("&8Basic building block"), 1.0, 0.5, 64),
                new ShopItem("OAK_PLANKS", "&6Oak Planks", List.of("&8Wooden planks"), 2.0, 1.0, 64),
                new ShopItem("GLASS", "&fGlass", List.of("&8Transparent block"), 5.0, 2.5, 64)
            )
        ));

        categories.put("tools", new ShopCategory(
            "Tools",
            "DIAMOND_PICKAXE",
            List.of(
                new ShopItem("WOODEN_PICKAXE", "&6Wooden Pickaxe", List.of("&8Basic pickaxe"), 10.0, 5.0, 1),
                new ShopItem("STONE_PICKAXE", "&7Stone Pickaxe", List.of("&8Better pickaxe"), 25.0, 12.5, 1),
                new ShopItem("IRON_PICKAXE", "&fIron Pickaxe", List.of("&8Good pickaxe"), 100.0, 50.0, 1)
            )
        ));
    }
}
```

---

## Application Configuration (Non-Minecraft)

YamlAnnotations works for any Java application:

```java
public record HttpConfig(String host, int port, boolean ssl, int timeout) {}
public record LogConfig(String level, String file, int maxSize, int maxBackups) {}
public record RetryConfig(int maxAttempts, int delayMs, double backoffMultiplier) {}

@YamlFile(header = """
    Application Configuration
    Environment: ${ENV:development}
    """)
public class AppConfig extends YamlFileInterface {

    @YamlComment("HTTP Server settings")
    @YamlKey("http")
    public HttpConfig http = new HttpConfig("0.0.0.0", 8080, false, 30000);

    @YamlComment("Logging configuration")
    @YamlKey("logging")
    public LogConfig logging = new LogConfig("INFO", "app.log", 10485760, 5);

    @YamlComment("External API settings")
    @YamlKey("api.endpoint")
    public String apiEndpoint = "https://api.example.com/v1";

    @YamlKey("api.key")
    public String apiKey = "";

    @YamlKey("api.retry")
    public RetryConfig apiRetry = new RetryConfig(3, 1000, 2.0);

    @YamlComment("Feature flags")
    @YamlKey("features")
    public Map<String, Boolean> features = Map.of(
        "new-dashboard", false,
        "beta-api", false,
        "analytics", true
    );
}
```

```java
// Usage
public class Application {
    public static void main(String[] args) throws IOException {
        AppConfig config = new AppConfig().load("application.yml");

        System.out.println("Starting server on " + config.http.host() + ":" + config.http.port());

        if (config.features.getOrDefault("analytics", false)) {
            System.out.println("Analytics enabled");
        }
    }
}
```
