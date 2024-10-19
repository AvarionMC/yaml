package org.avarion.yaml.testClasses;

public class CustomStringAcceptingClass {
    public String s;

    public CustomStringAcceptingClass(final String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return s;
    }
}
