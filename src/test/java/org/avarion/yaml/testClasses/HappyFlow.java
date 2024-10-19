package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

@YamlFile(header = "Sample YAML File")
public class HappyFlow extends YamlFileInterface {
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
