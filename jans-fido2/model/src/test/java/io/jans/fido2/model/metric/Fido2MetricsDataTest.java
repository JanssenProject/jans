/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Fido2MetricsDataTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testDefaultConstructorStampsUtcTimestamp() {
        LocalDateTime before = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
        Fido2MetricsData data = new Fido2MetricsData();
        LocalDateTime after = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();

        assertNotNull(data.getTimestamp(), "Default constructor must stamp a timestamp");
        assertFalse(data.getTimestamp().isBefore(before),
                "Timestamp must not be before the UTC instant captured prior to construction");
        assertFalse(data.getTimestamp().isAfter(after),
                "Timestamp must not be after the UTC instant captured after construction (guards against system-default timezone regression)");
    }

    @Test
    void testGetterSetterRoundTripCoversEachValueType() {
        Fido2MetricsData data = new Fido2MetricsData();

        data.setOperationType("REGISTRATION");
        data.setOperationStatus("SUCCESS");
        assertEquals("REGISTRATION", data.getOperationType());
        assertEquals("SUCCESS", data.getOperationStatus());

        data.setDurationMs(1234L);
        data.setMemoryUsageMb(512L);
        assertEquals(1234L, data.getDurationMs());
        assertEquals(512L, data.getMemoryUsageMb());

        data.setRetryCount(3);
        data.setConcurrentOperations(7);
        assertEquals(3, data.getRetryCount());
        assertEquals(7, data.getConcurrentOperations());

        data.setCpuUsagePercent(42.5);
        assertEquals(42.5, data.getCpuUsagePercent());

        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 15, 10, 30, 5);
        data.setStartTime(start);
        data.setEndTime(end);
        assertEquals(start, data.getStartTime());
        assertEquals(end, data.getEndTime());

        Map<String, Object> additional = new HashMap<>();
        additional.put("key", "value");
        data.setAdditionalData(additional);
        assertEquals(additional, data.getAdditionalData());

        Fido2MetricsData.DeviceInfo device = new Fido2MetricsData.DeviceInfo();
        device.setBrowser("Firefox");
        data.setDeviceInfo(device);
        assertEquals(device, data.getDeviceInfo());
    }

    @Test
    void testJsonSerializationUsesSnakeCaseKeys() throws Exception {
        Fido2MetricsData data = buildSampleData();

        String json = objectMapper.writeValueAsString(data);

        assertTrue(json.contains("\"operation_type\""), "missing operation_type key");
        assertTrue(json.contains("\"operation_status\""), "missing operation_status key");
        assertTrue(json.contains("\"user_id\""), "missing user_id key");
        assertTrue(json.contains("\"device_info\""), "missing device_info key");
        assertTrue(json.contains("\"authenticator_type\""), "missing authenticator_type key");
        assertTrue(json.contains("\"duration_ms\""), "missing duration_ms key");
        assertTrue(json.contains("\"start_time\""), "missing start_time key");
        assertTrue(json.contains("\"end_time\""), "missing end_time key");
        assertTrue(json.contains("\"ip_address\""), "missing ip_address key");
        assertTrue(json.contains("\"user_agent\""), "missing user_agent key");
        assertTrue(json.contains("\"cpu_usage_percent\""), "missing cpu_usage_percent key");
        assertTrue(json.contains("\"memory_usage_mb\""), "missing memory_usage_mb key");
    }

    @Test
    void testJsonSerializationDeserializationRoundTrip() throws Exception {
        Fido2MetricsData original = buildSampleData();

        String json = objectMapper.writeValueAsString(original);
        Fido2MetricsData roundtripped = objectMapper.readValue(json, Fido2MetricsData.class);

        assertEquals(original.getOperationType(), roundtripped.getOperationType());
        assertEquals(original.getOperationStatus(), roundtripped.getOperationStatus());
        assertEquals(original.getUserId(), roundtripped.getUserId());
        assertEquals(original.getAuthenticatorType(), roundtripped.getAuthenticatorType());
        assertEquals(original.getDurationMs(), roundtripped.getDurationMs());
        assertEquals(original.getStartTime(), roundtripped.getStartTime());
        assertEquals(original.getEndTime(), roundtripped.getEndTime());
        assertEquals(original.getIpAddress(), roundtripped.getIpAddress());
        assertEquals(original.getUserAgent(), roundtripped.getUserAgent());
        assertEquals(original.getCpuUsagePercent(), roundtripped.getCpuUsagePercent());
        assertEquals(original.getMemoryUsageMb(), roundtripped.getMemoryUsageMb());

        assertNotNull(roundtripped.getDeviceInfo());
        assertEquals(original.getDeviceInfo().getBrowser(), roundtripped.getDeviceInfo().getBrowser());
        assertEquals(original.getDeviceInfo().getUserAgent(), roundtripped.getDeviceInfo().getUserAgent());
    }

    @Test
    void testJsonDeserializationIgnoresUnknownFields() {
        String json = "{"
                + "\"operation_type\":\"AUTHENTICATION\","
                + "\"operation_status\":\"SUCCESS\","
                + "\"user_id\":\"alice\","
                + "\"unknown_future_field\":\"x\","
                + "\"another_unknown\":42"
                + "}";

        Fido2MetricsData data = assertDoesNotThrow(
                () -> objectMapper.readValue(json, Fido2MetricsData.class),
                "@JsonIgnoreProperties(ignoreUnknown=true) must allow extra fields");

        assertEquals("AUTHENTICATION", data.getOperationType());
        assertEquals("SUCCESS", data.getOperationStatus());
        assertEquals("alice", data.getUserId());
    }

    @Test
    void testDeviceInfoGetterSetterRoundTrip() {
        Fido2MetricsData.DeviceInfo device = new Fido2MetricsData.DeviceInfo();
        device.setBrowser("Chrome");
        device.setBrowserVersion("120.0");
        device.setOperatingSystem("Linux");
        device.setOsVersion("6.5");
        device.setDeviceType("DESKTOP");
        device.setUserAgent("Mozilla/5.0");

        assertEquals("Chrome", device.getBrowser());
        assertEquals("120.0", device.getBrowserVersion());
        assertEquals("Linux", device.getOperatingSystem());
        assertEquals("6.5", device.getOsVersion());
        assertEquals("DESKTOP", device.getDeviceType());
        assertEquals("Mozilla/5.0", device.getUserAgent());
    }

    @Test
    void testDeviceInfoJsonUsesSnakeCaseKeys() throws Exception {
        Fido2MetricsData.DeviceInfo device = new Fido2MetricsData.DeviceInfo();
        device.setBrowser("Chrome");
        device.setBrowserVersion("120.0");
        device.setOperatingSystem("Linux");
        device.setOsVersion("6.5");
        device.setDeviceType("DESKTOP");
        device.setUserAgent("Mozilla/5.0");

        String json = objectMapper.writeValueAsString(device);

        assertTrue(json.contains("\"browser\""), "missing browser key");
        assertTrue(json.contains("\"browser_version\""), "missing browser_version key");
        assertTrue(json.contains("\"operating_system\""), "missing operating_system key");
        assertTrue(json.contains("\"os_version\""), "missing os_version key");
        assertTrue(json.contains("\"device_type\""), "missing device_type key");
        assertTrue(json.contains("\"user_agent\""), "missing user_agent key");
    }

    private Fido2MetricsData buildSampleData() {
        Fido2MetricsData data = new Fido2MetricsData();
        data.setOperationType("REGISTRATION");
        data.setOperationStatus("SUCCESS");
        data.setUserId("user-123");
        data.setAuthenticatorType("PLATFORM");
        data.setDurationMs(987L);
        data.setStartTime(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        data.setEndTime(LocalDateTime.of(2024, 1, 15, 10, 30, 5));
        data.setIpAddress("10.0.0.1");
        data.setUserAgent("Mozilla/5.0");
        data.setCpuUsagePercent(55.5);
        data.setMemoryUsageMb(256L);

        Fido2MetricsData.DeviceInfo device = new Fido2MetricsData.DeviceInfo();
        device.setBrowser("Chrome");
        device.setUserAgent("Mozilla/5.0");
        data.setDeviceInfo(device);

        return data;
    }
}
