/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for JsonService
 * 
 * @author Janssen Project
 */
public class JsonServiceTest {

    private JsonService jsonService;

    @BeforeClass
    public void setUp() {
        jsonService = new JsonService();
        jsonService.init();
    }

    @Test
    public void testJsonToObject_withValidJson_shouldDeserialize() throws IOException {
        String json = "{\"name\":\"John\",\"age\":30}";
        TestPojo result = jsonService.jsonToObject(json, TestPojo.class);
        
        assertNotNull(result);
        assertEquals(result.getName(), "John");
        assertEquals(result.getAge(), 30);
    }

    @Test(expectedExceptions = JsonParseException.class)
    public void testJsonToObject_withInvalidJson_shouldThrowException() throws IOException {
        String invalidJson = "{invalid json}";
        jsonService.jsonToObject(invalidJson, TestPojo.class);
    }

    @Test
    public void testJsonToObject_withNullValues_shouldHandleGracefully() throws IOException {
        String json = "{\"name\":null,\"age\":null}";
        TestPojo result = jsonService.jsonToObject(json, TestPojo.class);
        
        assertNotNull(result);
        assertNull(result.getName());
        assertEquals(result.getAge(), 0);
    }

    @Test
    public void testJsonToObject_withEmptyJson_shouldCreateEmptyObject() throws IOException {
        String json = "{}";
        TestPojo result = jsonService.jsonToObject(json, TestPojo.class);
        
        assertNotNull(result);
        assertNull(result.getName());
    }

    @Test
    public void testJsonToObject_withJavaType_shouldDeserializeList() throws IOException {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        TypeFactory typeFactory = jsonService.getTypeFactory();
        JavaType listType = typeFactory.constructCollectionType(List.class, TestPojo.class);
        
        List<TestPojo> result = jsonService.jsonToObject(json, listType);
        
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertEquals(result.get(0).getName(), "John");
        assertEquals(result.get(1).getName(), "Jane");
    }

    @Test
    public void testJsonToObject_withJavaType_shouldDeserializeMap() throws IOException {
        String json = "{\"user1\":{\"name\":\"John\",\"age\":30},\"user2\":{\"name\":\"Jane\",\"age\":25}}";
        TypeFactory typeFactory = jsonService.getTypeFactory();
        JavaType mapType = typeFactory.constructMapType(Map.class, String.class, TestPojo.class);
        
        Map<String, TestPojo> result = jsonService.jsonToObject(json, mapType);
        
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertTrue(result.containsKey("user1"));
        assertTrue(result.containsKey("user2"));
        assertEquals(result.get("user1").getName(), "John");
    }

    @Test
    public void testJsonToObject_withNestedObjects_shouldDeserialize() throws IOException {
        String json = "{\"name\":\"Parent\",\"child\":{\"name\":\"Child\",\"age\":5}}";
        NestedTestPojo result = jsonService.jsonToObject(json, NestedTestPojo.class);
        
        assertNotNull(result);
        assertEquals(result.getName(), "Parent");
        assertNotNull(result.getChild());
        assertEquals(result.getChild().getName(), "Child");
        assertEquals(result.getChild().getAge(), 5);
    }

    @Test
    public void testObjectToJson_withSimpleObject_shouldSerialize() throws IOException {
        TestPojo pojo = new TestPojo();
        pojo.setName("John");
        pojo.setAge(30);
        
        String json = jsonService.objectToJson(pojo);
        
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"age\":30"));
    }

    @Test
    public void testObjectToJson_withNullObject_shouldSerializeNull() throws IOException {
        TestPojo pojo = new TestPojo();
        pojo.setName(null);
        pojo.setAge(0);
        
        String json = jsonService.objectToJson(pojo);
        
        assertNotNull(json);
        assertTrue(json.contains("\"age\":0"));
    }

    @Test
    public void testObjectToJson_withList_shouldSerializeArray() throws IOException {
        List<TestPojo> list = new ArrayList<>();
        TestPojo pojo1 = new TestPojo();
        pojo1.setName("John");
        pojo1.setAge(30);
        list.add(pojo1);
        
        TestPojo pojo2 = new TestPojo();
        pojo2.setName("Jane");
        pojo2.setAge(25);
        list.add(pojo2);
        
        String json = jsonService.objectToJson(list);
        
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"name\":\"Jane\""));
    }

    @Test
    public void testObjectToJson_withMap_shouldSerializeObject() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 42);
        
        String json = jsonService.objectToJson(map);
        
        assertNotNull(json);
        assertTrue(json.contains("\"key1\":\"value1\""));
        assertTrue(json.contains("\"key2\":42"));
    }

    @Test
    public void testobjectToPrettyJson_shouldProducePrettyPrint() throws IOException {
        TestPojo pojo = new TestPojo();
        pojo.setName("John");
        pojo.setAge(30);
        
        String prettyJson = jsonService.objectToPrettyJson(pojo);
        
        assertNotNull(prettyJson);
        assertTrue(prettyJson.contains("\n"));
        assertTrue(prettyJson.contains("\"name\" : \"John\""));
        assertTrue(prettyJson.contains("\"age\" : 30"));
    }

    @Test
    public void testobjectToPrettyJson_withNestedObject_shouldFormatNicely() throws IOException {
        NestedTestPojo pojo = new NestedTestPojo();
        pojo.setName("Parent");
        TestPojo child = new TestPojo();
        child.setName("Child");
        child.setAge(5);
        pojo.setChild(child);
        
        String prettyJson = jsonService.objectToPrettyJson(pojo);
        
        assertNotNull(prettyJson);
        assertTrue(prettyJson.contains("\n"));
        assertTrue(prettyJson.contains("\"name\" : \"Parent\""));
        assertTrue(prettyJson.contains("\"child\""));
    }

    @Test
    public void testGetTypeFactory_shouldReturnNonNull() {
        TypeFactory typeFactory = jsonService.getTypeFactory();
        
        assertNotNull(typeFactory);
    }

    @Test
    public void testGetTypeFactory_shouldReturnSameInstance() {
        TypeFactory typeFactory1 = jsonService.getTypeFactory();
        TypeFactory typeFactory2 = jsonService.getTypeFactory();
        
        assertSame(typeFactory1, typeFactory2);
    }

    @Test
    public void testJsonToObject_withSpecialCharacters_shouldHandleCorrectly() throws IOException {
        String json = "{\"name\":\"John \\\"Doe\\\"\",\"age\":30}";
        TestPojo result = jsonService.jsonToObject(json, TestPojo.class);
        
        assertNotNull(result);
        assertEquals(result.getName(), "John \"Doe\"");
    }

    @Test
    public void testObjectToJson_withSpecialCharacters_shouldEscapeCorrectly() throws IOException {
        TestPojo pojo = new TestPojo();
        pojo.setName("John \"Doe\"");
        pojo.setAge(30);
        
        String json = jsonService.objectToJson(pojo);
        
        assertNotNull(json);
        assertTrue(json.contains("John \\\"Doe\\\""));
    }

    @Test
    public void testRoundTrip_shouldPreserveData() throws IOException {
        TestPojo original = new TestPojo();
        original.setName("John Doe");
        original.setAge(42);
        
        String json = jsonService.objectToJson(original);
        TestPojo deserialized = jsonService.jsonToObject(json, TestPojo.class);
        
        assertEquals(deserialized.getName(), original.getName());
        assertEquals(deserialized.getAge(), original.getAge());
    }

    @Test
    public void testRoundTrip_withComplexObject_shouldPreserveData() throws IOException {
        NestedTestPojo original = new NestedTestPojo();
        original.setName("Parent");
        TestPojo child = new TestPojo();
        child.setName("Child");
        child.setAge(10);
        original.setChild(child);
        
        String json = jsonService.objectToJson(original);
        NestedTestPojo deserialized = jsonService.jsonToObject(json, NestedTestPojo.class);
        
        assertEquals(deserialized.getName(), original.getName());
        assertEquals(deserialized.getChild().getName(), original.getChild().getName());
        assertEquals(deserialized.getChild().getAge(), original.getChild().getAge());
    }

    // Test POJOs
    public static class TestPojo {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class NestedTestPojo {
        private String name;
        private TestPojo child;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TestPojo getChild() {
            return child;
        }

        public void setChild(TestPojo child) {
            this.child = child;
        }
    }
}
