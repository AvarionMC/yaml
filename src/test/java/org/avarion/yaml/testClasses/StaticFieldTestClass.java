package org.avarion.yaml.testClasses;

/**
 * Test class with various static field configurations for testing getStaticFieldName
 */
public class StaticFieldTestClass {
    // Public static field - should be found
    public static final StaticFieldTestClass PUBLIC_INSTANCE = new StaticFieldTestClass("public");

    // Private static field - should NOT be found (not public)
    private static final StaticFieldTestClass PRIVATE_INSTANCE = new StaticFieldTestClass("private");

    // Non-static field - should NOT be found
    public final StaticFieldTestClass NON_STATIC_INSTANCE = new StaticFieldTestClass("non-static");

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
