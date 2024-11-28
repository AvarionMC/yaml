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
}
```

### You load it like:

```java
Settings settings = new Settings().load("config.yml");
```

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
```

## Import

[Follow the instructions here](https://jitpack.io/#AvarionMC/yaml)

## Leniency
Leniency can be one of `UNDEFINED`, `STRICT` or `LENIENT`

- The default for the `YamlFile` is `STRICT`.
- The default for `YamlKey` is inherited from `YamlFile`.

What does this mean?
- If you define a field as char, and a string is given. In `STRICT` mode, it will give an error. In `LENIENT` mode it just takes the first character, discarding the rest.
- If you define a field as a float, and pass in for example 0.51, this is actually 0.5099999904632568 \[[Wikipedia](https://en.wikipedia.org/wiki/Floating-point_arithmetic)]. So not exactly the same. In `LENIENT` mode no error will be given, but in `STRICT` mode it will throw an exception.

