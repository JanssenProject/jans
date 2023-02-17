/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.fido2.service.sg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Arrays;

import org.jboss.weld.junit5.ExplicitParamInjection;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.junit5.auto.ExcludeBean;
import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.as.common.model.common.User;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.fido.u2f.protocol.RegisterResponse;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.processor.attestation.U2FSuperGluuAttestationProcessor;
import io.jans.fido2.service.sg.converter.AttestationSuperGluuController;
import io.jans.fido2.service.shared.UserService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.sg.SuperGluuMode;
import io.jans.junit.extension.CustomExtension;
import io.jans.junit.extension.Name;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 0.1, 17/02/2023
 */
@EnableAutoWeld
@ExtendWith(WeldJunit5AutoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
@AddBeanClasses(io.jans.service.util.Resources.class)
@AddBeanClasses(io.jans.service.net.NetworkService.class)
@ExplicitParamInjection
public class AttestationSignatureAndroidTest {

	private String issuer;
	private String challenge;

	// Static to store value between tests executions
	static Fido2RegistrationEntry registrationEntry;

	AutoCloseable closeable;

	@Inject
	AttestationSuperGluuController attestationSuperGluuController;

	@Inject
	U2FSuperGluuAttestationProcessor attestationProcessor;

	@Inject
	AttestationService attestationService;

	@Mock
	UserService userService = Mockito.mock(UserService.class);

	@InjectMocks
	RegistrationPersistenceService registrationPersistenceService = Mockito.mock(RegistrationPersistenceService.class);

	@BeforeAll
	public static void beforeAll() {
		SecurityProviderUtility.installBCProvider();
	}

	@BeforeEach
    void initService() {
        closeable = MockitoAnnotations.openMocks(this);
    }

	@AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

	@ApplicationScoped
	@Produces
	StaticConfiguration produceStaticConfiguration() {
		StaticConfiguration staticConfiguration = Mockito.mock(StaticConfiguration.class);

		BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
		Mockito.when(staticConfiguration.getBaseDn()).thenReturn(baseDnConfiguration);

		return staticConfiguration;
	}

	@ApplicationScoped
	@Produces
	AppConfiguration produceAppConfiguration() {
		AppConfiguration appConfiguration = Mockito.mock(AppConfiguration.class);

		Fido2Configuration fido2Configuration = new Fido2Configuration();
		Mockito.when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
		Mockito.when(appConfiguration.getIssuer()).thenReturn(issuer);

		return appConfiguration;
	}

	@ApplicationScoped
	@Produces
	@ExcludeBean
	RegistrationPersistenceService produceRegistrationPersistenceService() {
		Mockito.when(registrationPersistenceService.buildFido2RegistrationEntry(any(), anyBoolean())).thenCallRealMethod();
		Mockito.when(registrationPersistenceService.findByChallenge(anyString(), anyBoolean())).thenReturn(Arrays.asList(registrationEntry));

		Mockito.when(userService.getUser(anyString(), any())).thenReturn(new User());

		return registrationPersistenceService;
	}

	@ApplicationScoped
	@Produces
	@ExcludeBean
	ChallengeGenerator produceChallengeGenerator() {
		return Mockito.when(Mockito.mock(ChallengeGenerator.class).getChallenge())
				.thenReturn(challenge).getMock();
	}

	@ApplicationScoped
	@Produces
	@ExcludeBean
	UserSessionIdService produceUserSessionIdService() {
		return Mockito.when(Mockito.mock(UserSessionIdService.class).isValidSessionId(anyString(), anyString()))
				.thenReturn(true).getMock();
	}

	@Test
	@Order(1)
    @ExtendWith(CustomExtension.class)
	public void testStartAttestationSignature(@Name("attestation.android.issuer") String issuer, @Name("attestation.android.challenge") String challenge,
			@Name("attestation.android.userName") String userName, @Name("attestation.android.applicationId") String applicationId,
			@Name("attestation.android.sessionId") String sessionId, @Name("attestation.android.enrollmentCode") String enrollmentCode) {
		
		this.issuer = issuer;
		this.challenge = challenge;

		JsonNode request = attestationSuperGluuController.buildFido2AttestationStartResponse(userName, applicationId, sessionId);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_REQUEST).asBoolean(), true);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_MODE).asText(), SuperGluuMode.TWO_STEP.getMode());
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_APP_ID).asText(), applicationId);

		ObjectNode response = attestationService.options(request);
		
		// Get saved entry for finish attestation test
        ArgumentCaptor<Fido2RegistrationEntry> captor = ArgumentCaptor.forClass(Fido2RegistrationEntry.class);
        Mockito.verify(registrationPersistenceService).save(captor.capture());
        registrationEntry = captor.getValue();

        assertNotNull(registrationEntry);
        assertNotNull(response);
        assertEquals(response.get("challenge").asText(), challenge);
	}

	@Test
	@Order(2)
    @ExtendWith(CustomExtension.class)
	public void testFinishAttestationSignature(@Name("attestation.android.userName") String userName, @Name("attestation.android.finish.request") String registerFinishResponse) {
		// Parse register response
		RegisterResponse registerResponse = attestationSuperGluuController.parseRegisterResponse(registerFinishResponse);

		JsonNode request = attestationSuperGluuController.buildFido2AttestationVerifyResponse(userName, registerResponse);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_REQUEST).asBoolean(), true);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_MODE).asText(), SuperGluuMode.TWO_STEP.getMode());

		ObjectNode response = attestationService.verify(request);

		assertNotNull(response);
        assertEquals(response.get("status").asText(), "ok");
        assertEquals(response.get("createdCredentials").get("id").asText(), "lGWf7urVmKzN_4vklat2W8jqJoWCTIYfrjkLFDkef2Zmdl7k13FXCFHdMMw0G_YyluFAHwx5oDf-7bcbAlG0Wg");
	}

}
