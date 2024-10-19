package org.avarion.yaml.testClasses;

public class CustomNonStringAcceptingClass {
    public int i;

    public CustomNonStringAcceptingClass(final int i) {
        this.i = i;
    }

    @Override
    public String toString() {
        return String.valueOf(i);
    }
}
