package org.avarion.yaml.testClasses;

/**
 * Test class with various static field configurations for testing getStaticFieldName
 */
public class StaticFieldTestClass {
    // Public static field - should be found
    public static final StaticFieldTestClass PUBLIC_INSTANCE = new StaticFieldTestClass("public");

    // Private static field - should NOT be found (not public). Read only by reflection, in
    // YamlWriter#getStaticFieldName: it is the sole case exercising the canAccess(null) == false
    // branch, so deleting it drops that method from 8/8 to 7/8 branches covered.
    @SuppressWarnings("unused")
    private static final StaticFieldTestClass PRIVATE_INSTANCE = new StaticFieldTestClass("private");

    // Public static but different value - should NOT match
    public static final StaticFieldTestClass OTHER_INSTANCE = new StaticFieldTestClass("other");

    private final String name;

    public StaticFieldTestClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        // Return generic toString format to match the pattern in formatValue
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public String getName() {
        return name;
    }
}
