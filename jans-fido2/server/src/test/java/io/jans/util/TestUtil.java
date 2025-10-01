package io.jans.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {

    private static final TestUtil testUtil = new TestUtil();

    private final ObjectMapper mapper = new ObjectMapper();

    private static TestUtil instanceTestUtil() {
        return testUtil;
    }

    public static ObjectMapper instanceMapper() {
        return instanceTestUtil().getMapper();
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
