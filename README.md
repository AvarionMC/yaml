# YamlAnnotations

This library is intended to work with very simple configuration files (yaml).

I built this for MineCraft config.yml files, however it's not limited to that alone.

## Code

### Small sample:

```java
import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

@YamlFile(header = """
        This is some text that will appear at the top of the file.
        """,
        lenient = Leniency.LENIENT // default is STRICT
)
public class Settings extends YamlFileInterface {
    @YamlComment("Some comment describing `param1`")
    @YamlKey("param1")
    public long param1 = 1;

    @YamlComment("Some comment describing `param2`, demonstration that the attribute name doesn't need to match the key")
    @YamlKey("param2")
    public long par_am_2 = 2;

    @YamlComment("Demonstration of sub-sections -- short")
    @YamlKey("sub.short")
    public short subShort = 0;

    @YamlComment("Demonstration of sub-sections -- integer")
    @YamlKey("sub.int")
    public int subInteger = 0;

    @YamlComment("Demonstration of sub-sections -- long")
    @YamlKey("sub.long")
    public long subLong = 0;

    @YamlComment("Demonstration of sub-sections -- String")
    @YamlKey("sub.string")
    public String subString = "abc";

    @YamlComment("Demonstration of sub-sections -- float")
    @YamlKey(
            value="sub.float",
            lenient = Leniency.LENIENT  // default is inherited or STRICT
    )
    public float subFloat = 0.0;

    @YamlComment("Demonstration of sub-sections -- double")
    @YamlKey("sub.double")
    public double subDouble = 0.0;

    @YamlComment("Demonstration of collections -- List of strings")
    @YamlKey("sub.list")
    public List<String> subList = List.of("item1", "item2");

    @YamlComment("Demonstration of collections -- Set of integers")
    @YamlKey("sub.set")
    public Set<Integer> subSet = Set.of(1, 2, 3);
}
```

### You load it like:

```java
Settings settings = new Settings().load("config.yml");
```

Loading a non-existing file will save a default template immediately.

### This will result in this kind of yaml file:

```yaml
# This is some text that will appear at the top of the file.

# Some comment describing `param1`
param1: 1

# Some comment describing `param2`, demonstration that the attribute name doesn't need to match the key
param2: 2

sub:
    # Demonstration of sub-sections -- short
    short: 0

    # Demonstration of sub-sections -- integer
    int: 0

    # Demonstration of sub-sections -- long
    long: 0

    # Demonstration of sub-sections -- String
    string: "abc"

    # Demonstration of sub-sections -- float
    float: 0.0

    # Demonstration of sub-sections -- double
    double: 0.0

    # Demonstration of collections -- List of strings
    list:
        - item1
        - item2

    # Demonstration of collections -- Set of integers
    set:
        - 1
        - 2
        - 3
```

## Advanced Usage

### YamlMap annotation

For more complex nested structures, you can use `@YamlMap` with a custom processor. The `value` specifies the YAML key where the map will be stored:

```java
@YamlMap(value = "players", processor = PlayerProcessor.class)
public Map<String, Player> players = new HashMap<>();

public static class PlayerProcessor implements YamlMap.YamlMapProcessor<Settings> {
    @Override
    public void read(Settings obj, String key, Map<String, Object> value) {
        // Custom logic to read player data from YAML
        obj.players.put(key, new Player(value));
    }

    @Override
    public Map<String, Object> write(Settings obj, String key, Object value) {
        // Custom logic to write player data to YAML
        return ((Player) value).toMap();
    }
}
```

### Supported Types

The library supports various data types including:
- Primitives: `int`, `long`, `short`, `byte`, `float`, `double`, `boolean`, `char`
- Wrapper classes: `Integer`, `Long`, `Short`, `Byte`, `Float`, `Double`, `Boolean`, `Character`
- `String`, `UUID`
- Collections: `List`, `Set`, `Queue` (with generic type support)
- Enums
- Any class with a String constructor
- Static field constants (e.g., `Sound.ENTITY_PLAYER_HURT` from PaperMC API)

## Import

### Gradle

```gradle
plugins {
    id 'com.gradleup.shadow'
}

repositories {
    maven { url 'https://repo.codemc.io/repository/maven-public/' }
}

dependencies {
    implementation 'org.avarion:yaml:VERSION'
}

jar {
    dependsOn shadowJar
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>codemc-repo</id>
        <url>https://repo.codemc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.avarion</groupId>
        <artifactId>yaml</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.4.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Replace `VERSION` with the latest version available at [codemc.io](https://repo.codemc.io/#browse/browse:browse:maven-public:org%2Favarion%2Fyaml).

## Leniency
Leniency can be one of `UNDEFINED`, `STRICT` or `LENIENT`

- The default for the `YamlFile` is `STRICT`.
- The default for `YamlKey` is inherited from `YamlFile`.

What does this mean?
- If you define a field as char, and a string is given. In `STRICT` mode, it will give an error. In `LENIENT` mode it just takes the first character, discarding the rest.
- If you define a field as a float, and pass in for example 0.51, this is actually 0.5099999904632568 \[[Wikipedia](https://en.wikipedia.org/wiki/Floating-point_arithmetic)]. So not exactly the same. In `LENIENT` mode no error will be given, but in `STRICT` mode it will throw an exception.
- If you define a field as `List<String>` and provide a single `String` value in the YAML, `LENIENT` mode will automatically wrap it in a list with one entry. In `STRICT` mode, this would cause an error.