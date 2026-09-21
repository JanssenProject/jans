/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.ObjectClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reflective checks that TRACE entities expose exactly the mutable attributes the TRACE MVP
 * design allows (design decision D-7) and are tagged with the design's object class names.
 *
 * <p>Written against JUnit 5 rather than the repo's TestNG convention: this module's Surefire
 * setup (see {@code lock-server/pom.xml} surefire pluginManagement) pins the JUnit-Platform
 * provider and does not execute TestNG classes, so a TestNG version of this test would never run
 * (same precedent as {@code TraceConfigurationTest}, task 02).
 *
 * @author Janssen Project
 */
public class TraceEntityAnnotationsTest {

    private static Set<String> mutableAttributeNames(Class<?> entityClass) {
        Set<String> mutable = new HashSet<>();
        for (Field field : entityClass.getDeclaredFields()) {
            AttributeName attributeName = field.getAnnotation(AttributeName.class);
            if (attributeName == null) {
                continue;
            }
            if (!attributeName.ignoreDuringUpdate()) {
                mutable.add(attributeName.name());
            }
        }
        return mutable;
    }

    @Test
    public void testRecordEntry_onlyFlagsAreUpdatable() {
        Set<String> expected = new HashSet<>(
                Arrays.asList("jansTraceCoverageGap", "jansTraceChainLinkFailure", "jansTraceEquivocation"));

        assertEquals(expected, mutableAttributeNames(TraceRecordEntry.class));
    }

    @Test
    public void testReceiptEntry_onlyStateIsUpdatable() {
        assertEquals(new HashSet<>(Arrays.asList("jansTraceReceiptState")),
                mutableAttributeNames(TraceReceiptEntry.class));
    }

    @Test
    public void testProducerKeyEntry_onlyRevokedAtIsUpdatable() {
        assertEquals(new HashSet<>(Arrays.asList("jansTraceRevokedAt")),
                mutableAttributeNames(TraceProducerKeyEntry.class));
    }

    @Test
    public void testChainEntry_nothingUpdatable() {
        assertTrue(mutableAttributeNames(TraceChainEntry.class).isEmpty());
    }

    @Test
    public void testObjectClassNames_matchDesign() {
        assertEquals("jansTraceRecord", TraceRecordEntry.class.getAnnotation(ObjectClass.class).value());
        assertEquals("jansTraceReceipt", TraceReceiptEntry.class.getAnnotation(ObjectClass.class).value());
        assertEquals("jansTraceChain", TraceChainEntry.class.getAnnotation(ObjectClass.class).value());
        assertEquals("jansTraceProducerKey", TraceProducerKeyEntry.class.getAnnotation(ObjectClass.class).value());
    }

    @Test
    public void testVerification_jacksonPropertyNames_matchSnakeCase() throws Exception {
        TraceVerification verification = new TraceVerification();
        verification.setSignatureValid(true);
        verification.setKeyId("kid-1");
        verification.setVerifiedAtMs(1_700_000_000_000L);
        verification.setAlgorithm("Ed25519");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.valueToTree(verification);

        Set<String> fieldNames = new HashSet<>();
        node.fieldNames().forEachRemaining(fieldNames::add);

        assertEquals(new HashSet<>(Arrays.asList("signature_valid", "key_id", "verified_at", "algorithm")),
                fieldNames);
    }

}
