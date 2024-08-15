package org.avarion.yaml;

import java.util.List;

@YamlFile(header = "Sample YAML File")
class YamlFileInterfaceTestImpl1 extends YamlFileInterface {
	@YamlKey("name")
	@YamlComment("The name of the object")
	private String name = "John Doe";

	@YamlKey("age")
	@YamlComment("The age of the object")
	private int age = 30;

	@YamlKey("isStudent")
	@YamlComment("Indicates if the object is a student")
	private boolean isStudent = true;

	@YamlKey("weight")
	@YamlComment("The weight of the object")
	private double weight = 70.5;

	@YamlKey("score")
	@YamlComment("The score of the object")
	private float score = 85.5f;

	@YamlKey("id")
	@YamlComment("The ID of the object")
	private long id = 123456789L;

	@YamlKey("isMale")
	@YamlComment("Indicates if the object is male")
	private boolean isMale = true;

	@YamlKey("address.city")
	@YamlComment("The city in the address")
	private String city = "New York";

	@YamlKey("address.street.number")
	@YamlComment("The street number in the address")
	private int streetNumber = 123;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isStudent() {
		return isStudent;
	}

	public void setStudent(boolean student) {
		isStudent = student;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isMale() {
		return isMale;
	}

	public void setMale(boolean male) {
		isMale = male;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public int getStreetNumber() {
		return streetNumber;
	}

	public void setStreetNumber(int streetNumber) {
		this.streetNumber = streetNumber;
	}
}


class YamlFileInterfaceTestImpl2 extends YamlFileInterface {
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
	public List<Material> materials = List.of(Material.A, Material.B);

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


class DoubleKeyUsage extends YamlFileInterface {
	@YamlKey("key1")
	public int key1 = 1;

	@YamlKey("key1")
	public int key2 = 1;
}


class EmptyKey extends YamlFileInterface {
	@YamlKey("")
	public int key = 1;
}

class FinalKeyword extends YamlFileInterface {
	@YamlKey("key")
	public final int key = 1;
}

@YamlFile(header = "")
class BlankHeader extends YamlFileInterface {
	@YamlKey("key")
	public int key = 1;
}

class NoDefaultConstructor extends YamlFileInterface {
	@YamlKey("key")
	public int key = 1;

	NoDefaultConstructor(int k) {
		key = k;
	}
}
