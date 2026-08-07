package org.avarion.yaml;

import org.avarion.yaml.testClasses.Address;
import org.avarion.yaml.testClasses.Material;
import org.avarion.yaml.testClasses.Sounds;
import org.avarion.yaml.testClasses.ValidatingRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct tests for TypeConverter to increase branch and line coverage.
 */
class TypeConverterDirectTest {

    /** Stateless apart from naming and leniency, so one instance per mode serves every test. */
    private static final TypeConverter strict = new TypeConverter(Naming.SNAKE_CASE, false);
    private static final TypeConverter lenient = new TypeConverter(Naming.SNAKE_CASE, true);

    // ==================== getConvertedValue tests ====================

    @Test
    void testObjectTypeWithMapValueReturnsAsIs() throws IOException {
        Map<String, Object> mapValue = Map.of("key", "value");
        Object result = strict.getConvertedValue(null, Object.class, mapValue);
        assertSame(mapValue, result);
    }

    @Test
    void testObjectTypeWithCollectionValueReturnsAsIs() throws IOException {
        List<String> listValue = List.of("a", "b");
        Object result = strict.getConvertedValue(null, Object.class, listValue);
        assertSame(listValue, result);
    }

    @Test
    void testObjectTypeWithScalarValueDoesNotReturnAsIs() throws IOException {
        // Object.class + non-collection/non-map → should fall through to isInstance check
        Object result = strict.getConvertedValue(null, Object.class, "hello");
        assertEquals("hello", result);
    }

    @Test
    void testNullValueForPrimitiveWithoutFieldThrowsWithoutFieldName() {
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, int.class, null));
        assertEquals("Cannot assign null to primitive type int", thrown.getMessage());
        assertFalse(thrown.getMessage().contains("field:"));
    }

    @Test
    void testNullValueForNonPrimitiveReturnsNull() throws IOException {
        Object result = strict.getConvertedValue(null, String.class, null);
        assertNull(result);
    }

    @Test
    void testEnumTypeWithNonStringValueFallsThrough() {
        // Enum type but value is Integer → should fall through enum check, hit Number check, then throw
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, Material.class, 42));
        assertTrue(thrown.getMessage().contains("Cannot convert Integer to Material"));
    }

    // ==================== what the failure message says ====================

    @Test
    void aFailureNamesTheValueAndGuessesWhatWasMeant() {
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, Sounds.class, "bumba"));

        assertEquals("Cannot read Sounds from 'bumba': no constructor taking a String, and no constant with that name. "
                + "Did you mean YOUR_SOUND_ROCKS_TOO, MY_SOUND_ROCKS?", thrown.getMessage());
    }

    @Test
    void testMapValueWithNonMapExpectedType() {
        // value is Map but expectedType is not Map and not Record → falls through to constructor/field attempts
        Map<String, Object> mapValue = Map.of("key", "value");
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, Integer.class, mapValue));
        assertTrue(thrown.getMessage().contains("Cannot read Integer from '{key=value}'"));
    }

    @Test
    void testStringValueWithNonUuidExpectedType() throws IOException {
        // String value with non-UUID expectedType → should not enter UUID branch
        // String for a String field → isInstance returns true
        Object result = strict.getConvertedValue(null, String.class, "hello");
        assertEquals("hello", result);
    }

    @Test
    void testNonStringValueWithUuidExpectedType() {
        // Non-string value for UUID type → skip UUID branch, skip boolean, hit Number, throw
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, UUID.class, 42));
        assertTrue(thrown.getMessage().contains("Cannot convert"));
    }

    @Test
    void testBooleanConversionFromBooleanObject() throws IOException {
        // Direct Boolean value → convertToBoolean returns as-is
        Object result = strict.getConvertedValue(null, Boolean.class, Boolean.TRUE);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testBooleanConversionFromStringNo() throws IOException {
        Object result = strict.getConvertedValue(null, boolean.class, "no");
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testCollectionWithLenientFalseDoesNotConvertSingleValue() {
        // Single String value, Collection expectedType, but NOT lenient → should fall through
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, List.class, "single"));
        assertTrue(thrown.getMessage().contains("Cannot read List from 'single'"));
    }

    @Test
    void testCollectionWithLenientTrueConvertsSingleValue() throws IOException {
        // Single String value, Collection expectedType, lenient → wraps in List
        Object result = lenient.getConvertedValue(null, List.class, "single");
        assertInstanceOf(List.class, result);
        assertEquals(List.of("single"), result);
    }

    // ==================== convertWithType tests ====================

    @Test
    void testConvertWithTypeNullForPrimitiveThrows() {
        IOException thrown = assertThrows(IOException.class, () ->
                strict.convertWithType(int.class, null));
        assertEquals("Cannot assign null to primitive type int", thrown.getMessage());
    }

    @Test
    void testConvertWithTypeNullForNonPrimitiveReturnsNull() throws IOException {
        Object result = strict.convertWithType(String.class, null);
        assertNull(result);
    }

    @Test
    void testConvertWithTypeMapWithRawClass() throws IOException {
        // Map value with raw Map.class type (no parameterized type info)
        Map<String, Object> mapValue = new LinkedHashMap<>();
        mapValue.put("key", "value");
        Object result = strict.convertWithType(Map.class, mapValue);
        assertInstanceOf(Map.class, result);
    }

    @Test
    void testConvertWithTypeCollectionWithRawClass() throws IOException {
        // Collection value with raw List.class type (no parameterized type info)
        List<String> listValue = List.of("a", "b");
        Object result = strict.convertWithType(List.class, listValue);
        assertInstanceOf(List.class, result);
    }

    @Test
    void testConvertWithTypeScalar() throws IOException {
        Object result = strict.convertWithType(String.class, "hello");
        assertEquals("hello", result);
    }

    // ==================== Raw (non-parameterized) field handling ====================

    @SuppressWarnings("rawtypes")
    static class RawFieldFixture {
        public List rawList;
        public Map rawMap;
    }

    @Test
    void testHandleCollectionValueWithRawListField() throws Exception {
        // Raw List field (no parameterized type) — element type falls through to Object.
        java.lang.reflect.Field rawList = RawFieldFixture.class.getField("rawList");
        Object result = strict.getConvertedValue(rawList, List.class, List.of("a", "b"));
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void testHandleMapValueWithRawMapField() throws Exception {
        // Raw Map field (no parameterized type) — key/value types fall through to Object.
        java.lang.reflect.Field rawMap = RawFieldFixture.class.getField("rawMap");
        Map<String, String> input = new LinkedHashMap<>();
        input.put("k", "v");
        Object result = strict.getConvertedValue(rawMap, Map.class, input);
        assertEquals(input, result);
    }

    // ==================== createCollectionInstance tests ====================

    @Test
    void testCreateSetInstance() throws IOException {
        Collection<Object> result = TypeConverter.createCollectionInstance(Set.class);
        assertInstanceOf(LinkedHashSet.class, result);
    }

    @Test
    void testCreateListInstance() throws IOException {
        Collection<Object> result = TypeConverter.createCollectionInstance(List.class);
        assertInstanceOf(ArrayList.class, result);
    }

    @Test
    void testCreateQueueInstance() throws IOException {
        Collection<Object> result = TypeConverter.createCollectionInstance(Queue.class);
        assertInstanceOf(ArrayDeque.class, result);
    }

    @Test
    void testCreateUnsupportedCollectionTypeThrows() {
        IOException thrown = assertThrows(IOException.class, () ->
                TypeConverter.createCollectionInstance(String.class));
        assertEquals("Unsupported collection type: String", thrown.getMessage());
    }

    // ==================== getRawClass tests ====================

    @Test
    void testGetRawClassWithPlainClass() {
        assertEquals(String.class, TypeConverter.getRawClass(String.class));
    }

    @Test
    void testGetRawClassWithUnknownTypeReturnObject() {
        // Create a Type that is neither Class nor ParameterizedType
        Type wildcardType = new Type() {
            @Override
            public String getTypeName() {
                return "unknown";
            }
        };
        assertEquals(Object.class, TypeConverter.getRawClass(wildcardType));
    }

    @Test
    void testGetRawClassWithParameterizedType() {
        // Use a real ParameterizedType
        ParameterizedType mapType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class, Integer.class};
            }

            @Override
            public Type getRawType() {
                return Map.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        assertEquals(Map.class, TypeConverter.getRawClass(mapType));
    }

    @Test
    void testGetRawClassWithParameterizedTypeNonClassRawType() {
        // ParameterizedType whose getRawType() returns a non-Class Type → fallback to Object.class
        ParameterizedType weirdType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[0];
            }

            @Override
            public Type getRawType() {
                return new Type() {
                    @Override
                    public String getTypeName() {
                        return "weird";
                    }
                };
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        assertEquals(Object.class, TypeConverter.getRawClass(weirdType));
    }

    // ==================== Record conversion edge cases ====================

    @Test
    void testRecordWithNullPrimitiveComponentThrows() {
        // Trying to assign null to a primitive record component should throw
        Map<String, Object> recordMap = new LinkedHashMap<>();
        recordMap.put("street", "123 Main St");
        recordMap.put("city", "Springfield");
        recordMap.put("zip_code", null); // int cannot be null

        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, Address.class, recordMap));
        assertTrue(thrown.getMessage().contains("Cannot assign null to primitive record component 'zipCode'"));
    }

    @Test
    void testRecordWithMapComponent() throws IOException {
        // Record with map component inside it
        record RecordWithMap(String name, Map<String, Integer> scores) {}

        Map<String, Object> innerMap = new LinkedHashMap<>();
        innerMap.put("math", 90);
        innerMap.put("science", 85);

        Map<String, Object> recordMap = new LinkedHashMap<>();
        recordMap.put("name", "Student");
        recordMap.put("scores", innerMap);

        Object result = strict.getConvertedValue(null, RecordWithMap.class, recordMap);
        assertInstanceOf(RecordWithMap.class, result);
        RecordWithMap converted = (RecordWithMap) result;
        assertEquals("Student", converted.name());
        assertEquals(Map.of("math", 90, "science", 85), converted.scores());
    }

    @Test
    void testRecordWithCollectionComponent() throws IOException {
        record RecordWithList(String name, List<String> tags) {}

        List<String> tags = List.of("tag1", "tag2");
        Map<String, Object> recordMap = new LinkedHashMap<>();
        recordMap.put("name", "Item");
        recordMap.put("tags", tags);

        Object result = strict.getConvertedValue(null, RecordWithList.class, recordMap);
        assertInstanceOf(RecordWithList.class, result);
        RecordWithList converted = (RecordWithList) result;
        assertEquals("Item", converted.name());
        assertEquals(List.of("tag1", "tag2"), converted.tags());
    }

    // ==================== Number conversion edge cases ====================

    @Test
    void testNumberToUnsupportedTypeThrows() {
        // Number value but expectedType is not a numeric type
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, UUID.class, 42));
        assertTrue(thrown.getMessage().contains("Cannot convert Integer to UUID"));
    }

    @Test
    void testFloatConversionLenientAllowsPrecisionLoss() throws IOException {
        // Double that can't be exactly represented as float, but lenient mode allows it
        Object result = lenient.getConvertedValue(null, float.class, 1.234567890123);
        assertInstanceOf(Float.class, result);
    }

    @Test
    void testFloatConversionStrictRejectsPrecisionLoss() {
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, float.class, 1.234567890123));
        assertTrue(thrown.getMessage().contains("cannot be precisely represented as a float"));
    }

    @Test
    void testCharacterConversionLenientTakesFirstChar() throws IOException {
        Object result = lenient.getConvertedValue(null, char.class, "abc");
        assertEquals('a', result);
    }

    @Test
    void testCharacterConversionStrictRejectsMultiChar() {
        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, char.class, "abc"));
        assertTrue(thrown.getMessage().contains("Cannot convert String of length 3 to Character"));
    }

    // ==================== Number-to-Integer conversion (through convertToNumber) ====================

    @Test
    void testDoubleToIntegerConversion() throws IOException {
        // Double value being converted to Integer.class → hits the Integer.class branch in convertToNumber
        Object result = strict.getConvertedValue(null, Integer.class, 42.0);
        assertEquals(42, result);
    }

    @Test
    void testDoubleToLongConversion() throws IOException {
        Object result = strict.getConvertedValue(null, Long.class, 42.0);
        assertEquals(42L, result);
    }

    @Test
    void testDoubleToShortConversion() throws IOException {
        Object result = strict.getConvertedValue(null, Short.class, 42.0);
        assertEquals((short) 42, result);
    }

    @Test
    void testDoubleToByteConversion() throws IOException {
        Object result = strict.getConvertedValue(null, Byte.class, 42.0);
        assertEquals((byte) 42, result);
    }

    @Test
    void testIntToDoubleConversion() throws IOException {
        Object result = strict.getConvertedValue(null, Double.class, 42);
        assertEquals(42.0, result);
    }

    @Test
    void testIntToFloatConversion() throws IOException {
        Object result = strict.getConvertedValue(null, Float.class, 42);
        assertEquals(42.0f, result);
    }

    // ==================== handleMapValue without field (null field) ====================

    @Test
    void testConvertWithTypeMapWithParameterizedType() throws IOException {
        // Create a ParameterizedType for Map<String, Integer>
        java.lang.reflect.ParameterizedType mapType = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[]{String.class, Integer.class};
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return Map.class;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }
        };

        Map<String, Object> mapValue = new LinkedHashMap<>();
        mapValue.put("key1", 10);
        mapValue.put("key2", 20);

        Object result = strict.convertWithType(mapType, mapValue);
        assertInstanceOf(Map.class, result);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertEquals(10, resultMap.get("key1"));
    }

    @Test
    void testConvertWithTypeCollectionWithParameterizedType() throws IOException {
        // Create a ParameterizedType for List<String>
        java.lang.reflect.ParameterizedType listType = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[]{String.class};
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return List.class;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }
        };

        List<String> listValue = List.of("a", "b");
        Object result = strict.convertWithType(listType, listValue);
        assertInstanceOf(List.class, result);
        assertEquals(2, ((List<?>) result).size());
    }

    // ==================== Map with no type parameters ====================

    @Test
    void testConvertWithTypeMapWithEmptyTypeArgs() throws IOException {
        // ParameterizedType with empty type args (unusual but possible)
        java.lang.reflect.ParameterizedType rawMapType = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[0]; // Empty type args
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return Map.class;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }
        };

        Map<String, Object> mapValue = new LinkedHashMap<>();
        mapValue.put("key", "value");

        Object result = strict.convertWithType(rawMapType, mapValue);
        assertInstanceOf(Map.class, result);
    }

    @Test
    void testConvertWithTypeCollectionWithEmptyTypeArgs() throws IOException {
        java.lang.reflect.ParameterizedType rawListType = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[0];
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return List.class;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }
        };

        List<String> listValue = List.of("a", "b");
        Object result = strict.convertWithType(rawListType, listValue);
        assertInstanceOf(List.class, result);
    }

    @Test
    void testConvertWithTypeMapWithSingleTypeArg() throws IOException {
        // ParameterizedType with only 1 type arg (edge case)
        java.lang.reflect.ParameterizedType singleArgMapType = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[]{String.class}; // Only key type, no value type
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return Map.class;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }
        };

        Map<String, Object> mapValue = new LinkedHashMap<>();
        mapValue.put("key", "value");

        Object result = strict.convertWithType(singleArgMapType, mapValue);
        assertInstanceOf(Map.class, result);
    }

    // ==================== Record constructor failure (L286-287) ====================

    @Test
    void testRecordConstructorThrowsCausesIOException() {
        // ValidatingRecord throws IllegalArgumentException for negative values
        Map<String, Object> recordMap = new LinkedHashMap<>();
        recordMap.put("name", "Test");
        recordMap.put("value", -1);

        IOException thrown = assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, ValidatingRecord.class, recordMap));
        assertTrue(thrown.getMessage().contains("Failed to instantiate record ValidatingRecord"));
    }

    // ==================== Record component with Map value but non-Map type (L263 partial) ====================

    @Test
    void testRecordComponentGetsMapValueButExpectsString() throws IOException {
        // Address has String street, String city, int zipCode (keyed zip_code in YAML)
        // Provide a Map where a String is expected → falls through else-if chain to else block
        Map<String, Object> recordMap = new LinkedHashMap<>();
        recordMap.put("street", Map.of("nested", "value")); // Map for a String component
        recordMap.put("city", "Springfield");
        recordMap.put("zip_code", 12345);

        // String has a String(String) constructor, so Map.toString() will be used
        Object result = strict.getConvertedValue(null, Address.class, recordMap);
        assertInstanceOf(Address.class, result);
        // The map's toString becomes the street value
        Address addr = (Address) result;
        assertNotNull(addr.street());
    }

    // ==================== Record component with Collection value but non-Collection type (L267 partial) ====================

    @Test
    void testRecordComponentGetsListValueButExpectsString() {
        // List value for a String component: L267 else-if evaluates to false
        // (value IS Collection but componentType String is NOT Collection-assignable)
        Map<String, Object> recordMap = new LinkedHashMap<>();
        recordMap.put("street", List.of("a", "b")); // List for a String component
        recordMap.put("city", "Springfield");
        recordMap.put("zip_code", 12345);

        // Falls through to else block, then getConvertedValue tries to create String collection → fails
        assertThrows(IOException.class, () ->
                strict.getConvertedValue(null, Address.class, recordMap));
    }
}
