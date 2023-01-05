package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import io.jans.as.model.common.Prompt;
import org.json.JSONArray;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.Assert.*;

public class StringUtilsTest extends BaseTest {

    @Test
    public void nullToEmpty_nullString_emptyString() {
        showTitle("nullToEmpty_nullString_emptyString");
        String result = StringUtils.nullToEmpty(null);
        assertNotNull(result);
        assertEquals(result, StringUtils.EMPTY_STRING);
    }

    @Test
    public void equals_bothParamsNull_true() {
        showTitle("equals_bothParamsNull_true");
        assertTrue(StringUtils.equals(null, null));
    }

    @Test
    public void equals_oneParamNull_false() {
        showTitle("equals_oneParamNull_false");
        assertFalse(StringUtils.equals("text", null));
    }

    @Test
    public void equals_validParamsDifferents_false() {
        showTitle("equals_validParamsDifferents_false");
        assertFalse(StringUtils.equals("text", "other_text"));
    }

    @Test
    public void equals_validParamsEquals_true() {
        showTitle("equals_validParamsEquals_true");
        assertTrue(StringUtils.equals("text", "text"));
    }

    @Test
    public void equalsIgnoringSpaces_nullParams_false() {
        showTitle("equalsIgnoringSpaces_nullParams_false");
        assertFalse(StringUtils.equalsIgnoringSpaces(null, null));
    }

    @Test
    public void equalsIgnoringSpaces_validParamsEquals_true() {
        showTitle("equalsIgnoringSpaces_validParamsEquals_true");
        assertTrue(StringUtils.equalsIgnoringSpaces("text test 1", "TextTest1"));
    }

    @Test
    public void implode_inputArrayGlueString_true() {
        showTitle("implode_inputArrayGlueString_true");
        String[] inputArray = {"text", "123", "abc 123", " ", "new"};
        String glueString = "-";
        assertEquals(StringUtils.implode(inputArray, glueString), "text-123-abc 123- -new");
    }

    @Test
    public void implode_collectionGlueString_true() {
        showTitle("implode_collectionGlueString_true");
        String[] inputArray = {"text", "123", "abc 123", " ", "new"};
        List<String> collection = Arrays.asList(inputArray);
        String glueString = "-";
        assertEquals(StringUtils.implode(collection, glueString), "text-123-abc 123- -new");
    }

    @Test
    public void implodeEnum_listPromtGlueString_true() {
        showTitle("implodeEnum_listPromtGlueString_true");
        List<Prompt> lisEnumsPrompts = new ArrayList<>();
        lisEnumsPrompts.add(Prompt.LOGIN);
        lisEnumsPrompts.add(Prompt.SELECT_ACCOUNT);
        lisEnumsPrompts.add(Prompt.CONSENT);
        String glueString = "-";
        assertEquals(StringUtils.implodeEnum(lisEnumsPrompts, glueString), "login-select_account-consent");
    }

    @Test
    public void spaceSeparatedToList_stringWithSpaces_listSeparatedBySpace() {
        showTitle("spaceSeparatedToList_stringWithSpaces_listSeparatedBySpace");
        String spaceSeparatedString = "Hello World, this is a test";
        List<String> expectedResult = new ArrayList<>();
        expectedResult.add("Hello");
        expectedResult.add("World,");
        expectedResult.add("this");
        expectedResult.add("is");
        expectedResult.add("a");
        expectedResult.add("test");
        assertEquals(StringUtils.spaceSeparatedToList(spaceSeparatedString), expectedResult);
    }

    @Test
    public void toJSONArray_listString_jsonArrayOfString() {
        showTitle("toJSONArray_listString_jsonArrayOfString");
        List<String> inputList = new ArrayList<>();
        inputList.add("Hello");
        inputList.add("World");
        inputList.add("this");
        inputList.add("is");
        inputList.add("a");
        inputList.add("test");
        JSONArray arrayResult = StringUtils.toJSONArray(inputList);
        assertNotNull(arrayResult);
        assertTrue(arrayResult.length() == 6);
        assertEquals(arrayResult.getString(0), "Hello");
        assertEquals(arrayResult.getString(1), "World");
        assertEquals(arrayResult.getString(5), "test");
    }

    @Test
    public void toList_jsonArray_listOfString() {
        showTitle("toList_jsonArray_listOfString");
        JSONArray inputJsonArray = new JSONArray();
        inputJsonArray.put("Hello");
        inputJsonArray.put("World");
        inputJsonArray.put("this");
        inputJsonArray.put("is");
        inputJsonArray.put("a");
        inputJsonArray.put("test");
        List<String> listResult = StringUtils.toList(inputJsonArray);
        assertNotNull(listResult);
        assertTrue(listResult.size() == 6);
        assertEquals(listResult.get(0), "Hello");
        assertEquals(listResult.get(1), "World");
        assertEquals(listResult.get(5), "test");
    }

    @Test
    public void parseSilently_stringDate_dateParsed() {
        showTitle("parseSilently_stringDate_dateParsed");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        Date testDate = calendar.getTime();
        String inputStringDate = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy").format(testDate);
        Date resultDate = StringUtils.parseSilently(inputStringDate);
        assertNotNull(resultDate);
        assertEquals(resultDate, testDate);
    }

    @Test
    public void addQueryStringParam_validKeyAndValue_stringBuilderWithNewValues() throws UnsupportedEncodingException {
        showTitle("addQueryStringParam_validKeyAndValue_stringBuilderWithNewValues");
        StringBuilder queryStringBuilder = new StringBuilder();
        String key = "key0";
        String value = "value0";
        StringUtils.addQueryStringParam(queryStringBuilder, key, value);
        assertEquals(queryStringBuilder.length(), 11);
        assertEquals(queryStringBuilder.toString(), "key0=value0");
    }

    @Test
    public void addQueryStringParam_validKeyAndCollectionValues_stringBuilderWithNewValues() throws UnsupportedEncodingException {
        showTitle("addQueryStringParam_validKeyAndCollectionValues_stringBuilderWithNewValues");
        StringBuilder queryStringBuilder = new StringBuilder();
        String key = "key0";
        List<String> collectionValues = new ArrayList<>();
        collectionValues.add("value0");
        collectionValues.add("value1");
        collectionValues.add("value2");
        StringUtils.addQueryStringParam(queryStringBuilder, key, collectionValues);
        assertEquals(queryStringBuilder.length(), 37);
        assertEquals(queryStringBuilder.toString(), "key0=%5Bvalue0%2C+value1%2C+value2%5D");
    }

    @Test
    public void generateRandomReadableCode_length12_stringCodeSeparatedByDashes() {
        showTitle("generateRandomReadableCode_length12_stringCodeSeparatedByDashes");
        byte length = 12;
        String codeGenerated = StringUtils.generateRandomReadableCode(length);
        assertNotNull(codeGenerated);
        assertEquals(codeGenerated.replaceAll("-", "").length(), length);
    }

    @Test
    public void generateRandomCode_length8Seed_stringCode() {
        showTitle("generateRandomCode_length8Seed_stringCode");
        byte seedLength = 8;
        String codeGenerated = StringUtils.generateRandomCode(seedLength);
        assertNotNull(codeGenerated);
        assertEquals(codeGenerated.length(), seedLength * 2);
    }

    @Test
    public void base64urlencode_validStringToEncode_encodedString() {
        showTitle("base64urlencode_validStringToEncode_encodedString");
        String input = "Hello world, this is a test.";
        String encodedResult = StringUtils.base64urlencode(input);
        assertNotNull(encodedResult);
        assertEquals(encodedResult, "SGVsbG8gd29ybGQsIHRoaXMgaXMgYSB0ZXN0Lg");
    }

}
