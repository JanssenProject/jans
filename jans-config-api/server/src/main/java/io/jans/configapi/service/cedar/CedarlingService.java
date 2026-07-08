package io.jans.configapi.service.cedar;

import io.jans.configapi.util.AuthUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import io.jans.core.cedarling.service.CedarlingAuthorizationService;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import uniffi.cedarling_uniffi.*;

@ApplicationScoped
@Named
public class CedarlingService {

    @Inject
    Logger logger;

    @Inject 
    CedarlingAuthorizationService cedarlingAuthorizationService;
    

    public void authorizeCall() {
        cedarlingAuthorizationService.authorize(null, null, null, null);
    }
}
