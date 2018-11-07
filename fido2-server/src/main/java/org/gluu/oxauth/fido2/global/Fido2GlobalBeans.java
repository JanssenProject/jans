/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.global;

import java.security.Provider;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.mds.TOCEntryDigester;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

@ApplicationScoped
@Named
public class Fido2GlobalBeans {
    
    private ResteasyClientBuilder resteasyClientBuilder;

    @PostConstruct
    public void init() {
        this.resteasyClientBuilder = new ResteasyClientBuilder();
    }

    @Produces
    @RequestScoped
    @Named("resteasyClient")
    public ResteasyClient getMDSRestTemplate() {
        return resteasyClientBuilder.build();
    }

    @Produces
    @ApplicationScoped
    @Named("cborMapper")
    public ObjectMapper getCborMapper() {
        return new ObjectMapper(new CBORFactory());
    }

    @Produces
    @Singleton
    @Named("base64UrlEncoder")
    public Base64.Encoder getBase64UrlEncoder() {
        return Base64.getUrlEncoder().withoutPadding();
    }

    @Produces
    @Singleton
    @Named("base64Encoder")
    public Base64.Encoder getBase64Encoder() {
        return Base64.getEncoder().withoutPadding();
    }

    @Produces
    @Singleton
    @Named("base64UrlDecoder")
    public Base64.Decoder getBase64UrlDecoder() {
        return Base64.getUrlDecoder();
    }

    @Produces
    @Singleton
    @Named("base64Decoder")
    public Base64.Decoder getBase64Decoder() {
        return Base64.getDecoder();
    }

    @Produces
    @ApplicationScoped
    @Named("objectMapper")
    public ObjectMapper getJsonMapper() {
        return new ObjectMapper();
    }

    @Produces
    @ApplicationScoped
    @Named("bouncyCastleProvider")
    Provider getBouncyCastleProvider() {
        return new BouncyCastleProvider();
    }

    @Produces
    @ApplicationScoped
    @Named("supportedAttestationFormats")
    public List<String> getSupportedAttestationFormats() {
        return Arrays.stream(AttestationFormat.values()).map(f -> f.getFmt()).collect(Collectors.toList());
    }

    @Produces
    @ApplicationScoped
    @Named("authenticatorsMetadata")
    public Map<String, JsonNode> getMetadata() {
        return Collections.synchronizedMap(new HashMap());
    }

    @Produces
    @ApplicationScoped
    @Named("tocEntries")
    public Map<String, JsonNode> getTocEntries() {
        return Collections.synchronizedMap(new HashMap());
    }

    @Produces
    @ApplicationScoped
    @Named("tocEntryDigester")
    public TOCEntryDigester getTocEntryDigester() {
        return new TOCEntryDigester();
    }

}
