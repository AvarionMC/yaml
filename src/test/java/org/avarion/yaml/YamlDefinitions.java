package org.avarion.yaml;

import java.util.Arrays;
import java.util.List;

@YamlFile(header = "Sample YAML File")
class HappyFlow extends YamlFileInterface {
	@YamlKey("name")
	@YamlComment("The name of the object")
	public String name = "John Doe";

	@YamlKey("age")
	@YamlComment("The age of the object")
	public int age = 30;

	@YamlKey("isStudent")
	@YamlComment("Indicates if the object is a student")
	public boolean isStudent = true;

	@YamlKey("weight")
	@YamlComment("The weight of the object")
	public double weight = 70.5;

	@YamlKey("score")
	@YamlComment("The score of the object")
	public float score = 85.5f;

	@YamlKey("id")
	@YamlComment("The ID of the object")
	public long id = 123456789L;

	@YamlKey("isMale")
	@YamlComment("Indicates if the object is male")
	public boolean isMale = true;

	@YamlKey("address.city")
	@YamlComment("The city in the address")
	public String city = "New York";

	@YamlKey("address.street.number")
	@YamlComment("The street number in the address")
	public int streetNumber = 123;
}


class NullOrEmptyKey extends YamlFileInterface {
	@YamlKey("")
	public String name1 = "A";

	public String name2 = "B";

	@YamlKey("save")
	public String name3 = "C";
}


enum Material {
	A, B, C,
}


class ListMaterial extends YamlFileInterface {
	@YamlKey("materials")
	public List<Material> materials = Arrays.asList(Material.A, Material.B);

	@YamlKey("enum")
	public Material material = Material.C;
}


class Primitive extends YamlFileInterface {
	@YamlKey("byte")
	public byte bt = 1;
	@YamlKey("char")
	public char chr = 'a';
	@YamlKey("short")
	public short shrt = 1;
	@YamlKey("integer")
	public int intgr = 1;
	@YamlKey("long")
	public long lng = 1L;

	@YamlKey("float")
	public float flt = 1f;
	@YamlKey("double")
	public double dbl = 1d;

	@YamlKey("boolean")
	public boolean bln = true;
}


class NonPrimitive extends YamlFileInterface {
	@YamlKey("byte")
	public Byte bt = 1;
	@YamlKey("char")
	public Character chr = 'a';
	@YamlKey("short")
	public Short shrt = 1;
	@YamlKey("integer")
	public Integer intgr = 1;
	@YamlKey("long")
	public Long lng = 1L;

	@YamlKey("float")
	public Float flt = 1f;
	@YamlKey("double")
	public Double dbl = 1d;

	@YamlKey("boolean")
	public Boolean bln = true;
}


@SuppressWarnings("unused")
class DoubleKeyUsage extends YamlFileInterface {
	@YamlKey("key1")
	public int key1 = 1;

	@YamlKey("key1")
	public int key2 = 1;
}


@SuppressWarnings("unused")
class FinalKeyword extends YamlFileInterface {
	@YamlKey("key")
	public final int key = 1;
}


@YamlFile(header = "")
@SuppressWarnings("unused")
class BlankHeader extends YamlFileInterface {
	@YamlKey("key")
	public int key = 1;
}

class ListYml extends YamlFileInterface {
	@YamlKey("key")
	public List<String> key = Arrays.asList("a", "b");
}
