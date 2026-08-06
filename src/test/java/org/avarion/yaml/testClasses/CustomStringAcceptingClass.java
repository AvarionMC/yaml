package org.avarion.yaml.testClasses;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CustomStringAcceptingClass {
    public String s;

    @Override
    public String toString() {
        return s;
    }
}
