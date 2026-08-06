package org.avarion.yaml.testClasses;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CustomNonStringAcceptingClass {
    public int i;

    @Override
    public String toString() {
        return String.valueOf(i);
    }
}
