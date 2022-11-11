package io.jans.as.model.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.jans.as.model.BaseTest;
import io.jans.as.model.common.Display;
import io.jans.as.model.common.SubjectType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.testng.Assert.*;

public class UtilTest extends BaseTest {

    @Test
    public void putArray_whenListIsNull_shouldNotFail() {
        JSONObject json = new JSONObject();
        Util.putArray(json, null, "key");

        assertNull(json.optJSONArray("key"));
    }

    @Test
    public void putArray_whenListIsNotEmpty_shouldAddArray() {
        JSONObject json = new JSONObject();
        Util.putArray(json, Lists.newArrayList("a"), "key");

        final JSONArray jsonArray = json.optJSONArray("key");
        assertNotNull(jsonArray);
        assertEquals(jsonArray.get(0), "a");
    }

    @Test
    public void putArray_whenListIsEmpty_shouldNotAddArray() {
        JSONObject json = new JSONObject();
        Util.putArray(json, Lists.newArrayList(), "key");

        assertNull(json.optJSONArray("key"));
    }

    @Test
    public void putNotBlank_keyNull_nothing() {
        showTitle("putNotBlank_keyNull_nothing");
        Map<String, String> map = new HashMap<>();
        Util.putNotBlank(map, null, "value0");
        assertEquals(map.size(), 0);
    }

    @Test
    public void putNotBlank_valueStringEmpty_nothing() {
        showTitle("putNotBlank_valueStringEmpty_nothing");
        Map<String, String> map = new HashMap<>();
        Util.putNotBlank(map, "key0", "");
        assertEquals(map.size(), 0);
    }

    @Test
    public void putNotBlank_validParams_mapUpdated() {
        showTitle("putNotBlank_validParams_mapUpdated");
        Map<String, String> map = new HashMap<>();
        Util.putNotBlank(map, "key0", "value0");
        assertEquals(map.size(), 1);
        assertTrue(map.containsKey("key0"));
        assertEquals(String.valueOf(map.get("key0")), "value0");
    }

    @Test
    public void escapeLog_paramNull_returnEmpty() {
        showTitle("escapeLog_paramNull_returnEmpty");
        String param = null;
        String result = Util.escapeLog(param);
        assertEquals(result, "", "result is no empty");
    }

    @Test
    public void escapeLog_validParagraph_string() {
        showTitle("escapeLog_validParagraph_string");
        String param = "Hello world\n" + "this is a paragraph\n" + "to test some  functionality.";
        String result = Util.escapeLog(param);
        assertEquals(result, "Hello world_this is a paragraph_to test some  functionality.");
    }

    @Test
    public void createJsonMapper_methodCall_objectMapperNotNull() {
        showTitle("createJsonMapper_methodCall_objectMapperNotNull");
        ObjectMapper objectMapper = Util.createJsonMapper();
        assertNotNull(objectMapper);
    }

    @Test
    public void asJsonSilently_null_stringJsonFromObject() {
        showTitle("asJsonSilently_null_stringJsonFromObject");
        String stringJson = Util.asJsonSilently(null);
        assertNotNull(stringJson);
        assertNotEquals(stringJson, "");
    }

    @Test
    public void asJsonSilently_validObject_stringJsonFromObject() {
        showTitle("asJsonSilently_validObject_stringJsonFromObject");
        Date dateObject = new Date();
        String stringJson = Util.asJsonSilently(dateObject);
        assertNotNull(stringJson);
        assertEquals(stringJson, String.valueOf(dateObject.getTime()));
    }

    @Test
    public void asPrettyJson_validObject_prettyStringJsonFromObject() throws IOException {
        showTitle("asPrettyJson_validObject_prettyStringJsonFromObject");
        List<String> list = Arrays.asList("1", "2");
        String stringJson = Util.asPrettyJson(list);
        assertNotNull(stringJson);
        assertEquals(stringJson, "[ \"1\", \"2\" ]");
    }

    @Test
    public void getBytes_validString_byteArray() throws IOException {
        showTitle("getBytes_validString_byteArray");
        String input = "Hello world";
        byte[] byteArray = Util.getBytes(input);
        assertNotNull(byteArray);
        assertEquals(byteArray, new byte[]{72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100});
    }

    @Test
    public void asList_jsonArray_listString() throws IOException {
        showTitle("asList_jsonArray_listString");
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("value1");
        jsonArray.put("value2");
        jsonArray.put("value3");
        List<String> result = Util.asList(jsonArray);
        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.get(0), "value1");
        assertEquals(result.get(1), "value2");
        assertEquals(result.get(2), "value3");
    }

    @Test
    public void asEnumList_jsonArrayAndClassType_listEnums() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        showTitle("asEnumList_jsonArrayAndClassType_listEnums");
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(SubjectType.PAIRWISE.getValue());
        jsonArray.put(SubjectType.PUBLIC.getValue());
        List<SubjectType> result = Util.asEnumList(jsonArray, SubjectType.class);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertEquals(result.get(0).getValue(), SubjectType.PAIRWISE.getValue());
        assertEquals(result.get(1).getValue(), SubjectType.PUBLIC.getValue());
    }

    @Test
    public void addToListIfHas_jsonObjectWithKey_listWithNewValues() throws JSONException {
        showTitle("addToListIfHas_jsonObjectWithKey_listWithNewValues");
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("value0");
        jsonArray.put("value1");
        jsonObject.putOpt("array", jsonArray);
        List<String> listString = new ArrayList<>();
        Util.addToListIfHas(listString, jsonObject, "123");
        assertTrue(listString.isEmpty());
        Util.addToListIfHas(listString, jsonObject, "array");
        assertEquals(listString.size(), 2);
        assertEquals(listString.get(0), "value0");
        assertEquals(listString.get(1), "value1");
    }


    @Test
    public void addToJSONObjectIfNotNull_jsonObjectAndKeyAndValue_jsonObjectWithNewValue() throws JSONException {
        showTitle("addToJSONObjectIfNotNull_jsonObjectAndKeyAndValue_jsonObjectWithNewValue");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key0", "value0");
        Util.addToJSONObjectIfNotNull(jsonObject, "key1", "value1");
        assertTrue(jsonObject.has("key1"));
        assertEquals(jsonObject.getString("key1"), "value1");
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
        Util.addToJSONObjectIfNotNull(jsonObject, "key0", "value00");
        assertTrue(jsonObject.has("key0"));
        assertEquals(jsonObject.getString("key0"), "value00");
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
    }

    @Test
    public void addToJSONObjectIfNotNull_jsonObjectAndKeyAndObjectValue_jsonObjectWithNewValue() {
        showTitle("addToJSONObjectIfNotNull_jsonObjectAndKeyAndObjectValue_jsonObjectWithNewValue");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key0", new Date());
        Util.addToJSONObjectIfNotNull(jsonObject, "key1", Double.valueOf(100D));
        assertTrue(jsonObject.has("key1"));
        assertEquals(jsonObject.get("key1"), Double.valueOf(100D));
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
        Util.addToJSONObjectIfNotNull(jsonObject, "key0", Long.valueOf(100L));
        assertTrue(jsonObject.has("key0"));
        assertEquals(jsonObject.get("key0"), Long.valueOf(100L));
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
    }

    @Test
    public void addToJSONObjectIfNotNull_jsonObjectAndKeyAndEnumValue_jsonObjectWithNewValue() {
        showTitle("addToJSONObjectIfNotNull_jsonObjectAndKeyAndEnumValue_jsonObjectWithNewValue");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key0", "value0");
        Util.addToJSONObjectIfNotNull(jsonObject, "key1", SubjectType.PAIRWISE);
        assertTrue(jsonObject.has("key1"));
        assertEquals(jsonObject.getString("key1"), SubjectType.PAIRWISE.getValue());
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
        Util.addToJSONObjectIfNotNull(jsonObject, "key0", SubjectType.PUBLIC);
        assertTrue(jsonObject.has("key0"));
        assertEquals(jsonObject.getString("key0"), SubjectType.PUBLIC.getValue());
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
    }

    @Test
    public void addToJSONObjectIfNotNull_jsonObjectAndKeyAndStringArray_jsonObjectWithNewValue() {
        showTitle("addToJSONObjectIfNotNull_jsonObjectAndKeyAndStringArray_jsonObjectWithNewValue");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key0", "value0");
        Util.addToJSONObjectIfNotNull(jsonObject, "key1", new String[]{"value1", "value2"});
        assertTrue(jsonObject.has("key1"));
        assertEquals(jsonObject.length(), 2, "Length is unexpected");
        JSONArray jsonArray = jsonObject.getJSONArray("key1");
        assertEquals(jsonArray.length(), 2);
        assertEquals(jsonArray.getString(0), "value1");
        assertEquals(jsonArray.getString(1), "value2");
    }

    @Test
    public void asString_listHashParamName_stringResultSpaceSeparated() {
        showTitle("asString_listHashParamName_stringResultSpaceSeparated");
        List<Display> listHashParams = new ArrayList<>();
        listHashParams.add(Display.PAGE);
        listHashParams.add(Display.EMBEDDED);
        listHashParams.add(Display.POPUP);
        String result = Util.asString(listHashParams);
        assertNotNull(result);
        assertEquals(result, "page embedded popup");
    }

    @Test
    public void listAsString_listString_stringResultSpaceSeparated() {
        showTitle("listAsString_listString_stringResultSpaceSeparated");
        List<String> listString = new ArrayList<>();
        listString.add("value0");
        listString.add("value1");
        listString.add("value2");
        String result = Util.listAsString(listString);
        assertNotNull(result);
        assertEquals(result, "value0 value1 value2");
    }

    @Test
    public void mapAsString_mapOfStrings_stringResultJSONArrayFormat() {
        showTitle("mapAsString_mapOfStrings_stringResultSpaceSeparated");
        Map<String, String> map = new HashMap<>();
        map.put("key0", "value0");
        map.put("key1", "value1");
        map.put("key2", "value2");
        String result = Util.mapAsString(map);
        assertNotNull(result);
        assertEquals(result, "[{\"key1\":\"value1\"},{\"key2\":\"value2\"},{\"key0\":\"value0\"}]");
    }

    @Test
    public void allNotBlank_arrayOfStringsOneNull_false() {
        showTitle("allNotBlank_arrayOfStringsOneNull_false");
        String[] arrayString = {"value0", null, "value2"};
        assertFalse(Util.allNotBlank(arrayString));
    }

    @Test
    public void allNotBlank_arrayOfStringsOneEmpty_false() {
        showTitle("allNotBlank_arrayOfStringsOneEmpty_false");
        String[] arrayString = {"value0", "", "value2"};
        assertFalse(Util.allNotBlank(arrayString));
    }

    @Test
    public void allNotBlank_arrayOfValidStrings_true() {
        showTitle("allNotBlank_arrayOfValidStrings_true");
        String[] arrayString = {"value0", "value1", "value2"};
        assertTrue(Util.allNotBlank(arrayString));
    }

    @Test
    public void splittedStringAsList_stringInputAndStringSplitter_listString() {
        showTitle("splittedStringAsList_stringInputAndStringSplitter_listString");
        List<String> listString = Util.splittedStringAsList("Hello-World, -this-is-a-test", "-");
        assertNotNull(listString);
        assertEquals(listString.size(), 6);
        assertEquals(listString.get(0), "Hello");
        assertEquals(listString.get(5), "test");
    }

    @Test
    public void jsonArrayStringAsList_stringJsonInput_listString() {
        showTitle("jsonArrayStringAsList_stringJsonInput_listString");
        String stringJsonArray = "[\"Hello\",\"World\",\"this is a test\"]";
        List<String> listString = Util.jsonArrayStringAsList(stringJsonArray);
        assertNotNull(listString);
        assertEquals(listString.size(), 3);
        assertEquals(listString.get(0), "Hello");
        assertEquals(listString.get(1), "World");
        assertEquals(listString.get(2), "this is a test");
    }

    @Test
    public void listToJsonArray_null_jsonArrayEmpty() {
        showTitle("listToJsonArray_null_jsonArrayEmpty");
        JSONArray arrayResult = Util.listToJsonArray(null);
        assertNotNull(arrayResult);
        assertEquals(arrayResult.length(), 0);
    }

    @Test
    public void listToJsonArray_listString_jsonArray() {
        showTitle("listToJsonArray_listString_jsonArray");
        List<String> listString = Arrays.asList("value0", "value1", "value2");
        JSONArray arrayResult = Util.listToJsonArray(listString);
        assertNotNull(arrayResult);
        assertEquals(arrayResult.length(), 3);
        assertEquals(arrayResult.getString(0), "value0");
        assertEquals(arrayResult.getString(1), "value1");
        assertEquals(arrayResult.getString(2), "value2");
    }


    @Test
    public void jsonObjectArrayStringAsMap_stringJson_hashMap() {
        showTitle("jsonObjectArrayStringAsMap_stringJson_hashMap");
        String stringJsonArray = "[{\"header1\":\"valueHeader1\",\"header2\":\"valueHeader2\"},{\"header3\":\"valueHeader3\"}]";
        Map<String, String> hashMap = Util.jsonObjectArrayStringAsMap(stringJsonArray);
        assertNotNull(hashMap);
        assertEquals(hashMap.size(), 3);
        assertTrue(hashMap.containsKey("header1"));
        assertTrue(hashMap.containsKey("header2"));
        assertTrue(hashMap.containsKey("header3"));
        assertEquals(hashMap.get("header1"), "valueHeader1");
        assertEquals(hashMap.get("header2"), "valueHeader2");
        assertEquals(hashMap.get("header3"), "valueHeader3");
    }

    @Test
    public void firstItem_null_null() {
        showTitle("firstItem_null_null");
        String firstItem = Util.firstItem(null);
        assertNull(firstItem);
    }

    @Test
    public void firstItem_listEmpty_null() {
        showTitle("firstItem_listEmpty_null");
        String firstItem = Util.firstItem(new ArrayList<>());
        assertNull(firstItem);
    }

    @Test
    public void firstItem_listString_firstString() {
        showTitle("firstItem_listString_firstString");
        List<String> listString = Arrays.asList("value0", "value1", "value2");
        String firstItem = Util.firstItem(listString);
        assertNotNull(firstItem);
        assertEquals(firstItem, "value0");
    }

    @Test
    public void isNullOrEmpty_null_false() {
        showTitle("isNullOrEmpty_null_false");
        assertTrue(Util.isNullOrEmpty(null));
    }

    @Test
    public void isNullOrEmpty_emptyString_false() {
        showTitle("isNullOrEmpty_emptyString_false");
        assertTrue(Util.isNullOrEmpty(""));
    }

    @Test
    public void isNullOrEmpty_validString_true() {
        showTitle("isNullOrEmpty_validString_true");
        assertFalse(Util.isNullOrEmpty("text"));
    }

    @Test
    public void parseIntSilently_null_int1Negative() {
        showTitle("parseIntSilently_null_int1Negative");
        assertEquals(Util.parseIntSilently(null), -1);
    }

    @Test
    public void parseIntSilently_invalidString_int1Negative() {
        showTitle("parseIntSilently_invalidString_int1Negative");
        assertEquals(Util.parseIntSilently("onetwothree"), -1);
    }

    @Test
    public void parseIntSilently_validString_int() {
        showTitle("parseIntSilently_validString_int");
        assertEquals(Util.parseIntSilently("123"), 123);
    }

    @Test
    public void parseIntegerSilently_null_null() {
        showTitle("parseIntegerSilently_null_null");
        assertNull(Util.parseIntegerSilently(null));
    }

    @Test
    public void parseIntegerSilently_invalidString_null() {
        showTitle("parseIntegerSilently_invalidString_null");
        assertNull(Util.parseIntegerSilently("onetwothree"));
    }

    @Test
    public void parseIntegerSilently_validString_integerValue() {
        showTitle("parseIntegerSilently_validString_integerValue");
        assertEquals(Util.parseIntegerSilently("123"), Integer.valueOf(123));
    }

    @Test
    public void toSHA1HexString_validString_shaHexString() {
        showTitle("toSHA1HexString_validString_shaHexString");
        String result = Util.toSHA1HexString("Hello world");
        assertEquals(result, "7b502c3a1f48c8609ae212cdfb639dee39673f5e");
    }

    @Test
    public void byteArrayToHexString_byteArray_hexString() {
        showTitle("byteArrayToHexString_byteArray_hexString");
        String input = "Hello world";
        byte[] byteArray = Util.getBytes(input);
        String result = Util.byteArrayToHexString(byteArray);
        assertEquals(result, "48656c6c6f20776f726c64");
    }

    @Test
    public void createExpirationDate_nullLifeTime_IllegalArgumentException() {
        showTitle("createExpirationDate_nullLifeTime_IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> Util.createExpirationDate(null));
    }

    @Test
    public void createExpirationDate_zeroLifeTime_IllegalArgumentException() {
        showTitle("createExpirationDate_zeroLifeTime_IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> Util.createExpirationDate(0));
    }

    @Test
    public void createExpirationDate_validLifeTime_dateExpiration() {
        showTitle("createExpirationDate_validLifeTime_dateExpiration");
        Date date = Util.createExpirationDate(60);
        assertNotNull(date);
    }

    @Test
    public void isPar_emptyUri_false() {
        showTitle("isPar_emptyUri_false");
        assertFalse(Util.isPar(""));
    }

    @Test
    public void isPar_nullUri_false() {
        showTitle("isPar_nullUri_false");
        assertFalse(Util.isPar(null));
    }

    @Test
    public void isPar_noParUri_false() {
        showTitle("isPar_noParUri_false");
        assertFalse(Util.isPar("http://uriemaple.com"));
    }

    @Test
    public void isPar_validParUri_true() {
        showTitle("isPar_noParUri_false");
        assertTrue(Util.isPar("par:uriemaple/test"));
    }

    @Test
    public void toSerializableMap_nullMap_emptyHashMap() {
        showTitle("toSerializableMap_nullMap_emptyHashMap");
        Map<String, Serializable> hashMap = Util.toSerializableMap(null);
        assertNotNull(hashMap);
        assertEquals(hashMap.size(), 0);
    }

    @Test
    public void toSerializableMap_validObjectMap_serializableMap() {
        showTitle("toSerializableMap_validObjectMap_serializableMap");
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("key0", new UtilTest());
        inputMap.put("key1", Integer.valueOf(123));
        inputMap.put("key2", Double.valueOf(10));
        inputMap.put("key3", "text");
        Map<String, Serializable> resultMap = Util.toSerializableMap(inputMap);
        assertNotNull(resultMap);
        assertEquals(resultMap.size(), 3);
        assertEquals(resultMap.get("key1"), inputMap.get("key1"));
        assertEquals(resultMap.get("key2"), inputMap.get("key2"));
        assertEquals(resultMap.get("key3"), inputMap.get("key3"));
    }

}
