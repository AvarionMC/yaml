package org.avarion.yaml.testClasses;

public class StaticMat {
    public String name;

    public StaticMat(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + "-test";
    }
}
