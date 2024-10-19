package org.avarion.yaml.testClasses;

public class Boss {
    private String name;
    private String internalName;
    private String arena;

    public Boss(String name, String internalName, String arena) {
        this.name = name;
        this.internalName = internalName;
        this.arena = arena;
    }

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getArena() {
        return arena;
    }
}
