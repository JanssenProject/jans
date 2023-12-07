package io.jans.fido2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DataMapperServiceTest {

    @InjectMocks
    private DataMapperService dataMapperService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dataMapperService.init();
    }

    @Test
    public void writeValueAsString_withValidData_valid() throws JsonProcessingException {
        Map<String, String> value = new HashMap<>();
        value.put("field1", "value1");
        value.put("field2", "value2");

        String response = dataMapperService.writeValueAsString(value);
        assertNotNull(response);
        assertTrue(isValidJson(response));
        System.out.println(response);
    }

    @Test
    public void readValueString_withValidData_valid() throws JsonProcessingException {
        String content = "{\"field1\":\"value1\",\"field2\":\"value2\"}";
        TestJson response = dataMapperService.readValueString(content, TestJson.class);
        assertNotNull(response);
        assertNotNull(response.getField1());
        assertNotNull(response.getField2());
        assertEquals(response.getField1(), "value1");
        assertEquals(response.getField2(), "value2");
    }

    static class TestJson {
        private String field1;
        private String field2;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setField2(String field2) {
            this.field2 = field2;
        }
    }

    private boolean isValidJson(String json) {
        try {
            mapper.readTree(json);
        } catch (JsonProcessingException e) {
            return false;
        }
        return true;
    }
}
