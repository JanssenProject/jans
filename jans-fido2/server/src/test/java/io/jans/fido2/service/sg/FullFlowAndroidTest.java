/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.fido2.service.sg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.Optional;

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
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.RegisterResponse;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.processor.assertion.U2FSuperGluuAssertionFormatProcessor;
import io.jans.fido2.service.processor.attestation.U2FSuperGluuAttestationProcessor;
import io.jans.fido2.service.sg.converter.AssertionSuperGluuController;
import io.jans.fido2.service.sg.converter.AttestationSuperGluuController;
import io.jans.fido2.service.shared.UserService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.sg.SuperGluuMode;
import io.jans.junit.extension.FileParameterExtension;
import io.jans.junit.extension.Name;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.u2f.service.persist.DeviceRegistrationService;
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
public class FullFlowAndroidTest {

	private String issuer;
	private String attestationChallenge;
	private String assertionChallenge;

	// Static to store value between tests executions
	static Fido2RegistrationEntry registrationEntry;
	static Fido2AuthenticationEntry authenticationEntry;

	private AutoCloseable closeable;

	@Inject
	AttestationSuperGluuController attestationSuperGluuController;

	@Inject
	AssertionSuperGluuController assertionSuperGluuController;

	@Inject
	U2FSuperGluuAttestationProcessor attestationProcessor;

	@Inject
	U2FSuperGluuAssertionFormatProcessor assertionFormatProcessor;

	@Inject
	AttestationService attestationService;

	@Inject
	AssertionService assertionService;

	@Mock
	@Produces
	@ExcludeBean
	UserService userService = Mockito.mock(UserService.class);

	@Mock
	@Produces
	@ExcludeBean
	PersistenceEntryManager persistenceEntryManager = Mockito.mock(PersistenceEntryManager.class);

	@Mock
	@Produces
	@ExcludeBean
	DeviceRegistrationService deviceRegistrationService = Mockito.mock(DeviceRegistrationService.class);

	@Mock
	ChallengeGenerator challengeGenerator = Mockito.mock(ChallengeGenerator.class);

	@InjectMocks
	RegistrationPersistenceService registrationPersistenceService = Mockito.mock(RegistrationPersistenceService.class);

	@InjectMocks
	AuthenticationPersistenceService authenticationPersistenceService = Mockito.mock(AuthenticationPersistenceService.class);

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
		Mockito.doCallRealMethod().when(registrationPersistenceService).update(any(Fido2RegistrationEntry.class));
		if (registrationEntry != null) {
			Mockito.when(registrationPersistenceService.findByChallenge(eq(registrationEntry.getChallange()), anyBoolean())).thenReturn(Arrays.asList(registrationEntry));
			Mockito.when(registrationPersistenceService.findByPublicKeyId(eq(registrationEntry.getPublicKeyId()), eq(registrationEntry.getRpId()))).thenReturn(Optional.of(registrationEntry));
			Mockito.when(registrationPersistenceService.findByPublicKeyId(anyString(), eq(registrationEntry.getPublicKeyId()), eq(registrationEntry.getRpId()))).thenReturn(Optional.of(registrationEntry));
		}

		Mockito.when(userService.getUser(anyString(), any())).thenReturn(new User());

		return registrationPersistenceService;
	}

	@ApplicationScoped
	@Produces
	@ExcludeBean
	AuthenticationPersistenceService produceAuthenticationPersistenceService() {
		Mockito.when(authenticationPersistenceService.buildFido2AuthenticationEntry(any(), anyBoolean())).thenCallRealMethod();
		Mockito.doCallRealMethod().when(authenticationPersistenceService).update(any(Fido2AuthenticationEntry.class));
		if (authenticationEntry != null) {
			Mockito.when(authenticationPersistenceService.findByChallenge(eq(authenticationEntry.getChallange()), anyBoolean())).thenReturn(Arrays.asList(authenticationEntry));
		}

		return authenticationPersistenceService;
	}

	@ApplicationScoped
	@Produces
	@ExcludeBean
	ChallengeGenerator produceChallengeGenerator() {
		Mockito.when(challengeGenerator.getAttestationChallenge()).thenReturn(attestationChallenge);
		Mockito.when(challengeGenerator.getAssertionChallenge()).thenReturn(assertionChallenge);

		return challengeGenerator;
	}

	@ApplicationScoped
	@Produces
	@ExcludeBean
	UserSessionIdService produceUserSessionIdService() {
		return Mockito.when(Mockito.mock(UserSessionIdService.class).isValidSessionId(anyString(), anyString()))
				.thenReturn(true).getMock();
	}

	public void testStartAttestationTwoStepAndroidImpl(String issuer, String challenge, String userName,
			String applicationId, String sessionId) {
		this.issuer = issuer;
		this.attestationChallenge = challenge;

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

        assertEquals(registrationEntry.getRegistrationStatus(), Fido2RegistrationStatus.pending);
	}

	public void testFinishAttestationTwoStepAndroidImpl(String userName, String registerFinishResponse, String registeredPublicKey) {
		// Parse register response
		RegisterResponse registerResponse = attestationSuperGluuController.parseRegisterResponse(registerFinishResponse);

		JsonNode request = attestationSuperGluuController.buildFido2AttestationVerifyResponse(userName, registerResponse);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_REQUEST).asBoolean(), true);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_MODE).asText(), SuperGluuMode.TWO_STEP.getMode());

		ObjectNode response = attestationService.verify(request);

		// Get updated entry for checks
		ArgumentCaptor<Fido2RegistrationEntry> captor = ArgumentCaptor.forClass(Fido2RegistrationEntry.class);
        Mockito.verify(registrationPersistenceService).update(captor.capture());
        registrationEntry = captor.getValue();

		assertNotNull(response);
        assertEquals(response.get("status").asText(), "ok");
        assertEquals(response.get("createdCredentials").get("id").asText(), registeredPublicKey);

        assertEquals(registrationEntry.getRegistrationStatus(), Fido2RegistrationStatus.registered);
	}

	public void testStartAssertionTwoStepAndroidImpl(String issuer, String challenge, String userName,
			String applicationId, String sessionId) {
		this.issuer = issuer;
		this.assertionChallenge = challenge;

		JsonNode request = assertionSuperGluuController.buildFido2AssertionStartResponse(userName, registrationEntry.getPublicKeyId(), applicationId, sessionId);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_REQUEST).asBoolean(), true);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_MODE).asText(), SuperGluuMode.TWO_STEP.getMode());
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_KEY_HANDLE).asText(), registrationEntry.getPublicKeyId());
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_APP_ID).asText(), applicationId);

		ObjectNode response = assertionService.options(request);
		
		// Get saved entry for finish authentication test
        ArgumentCaptor<Fido2AuthenticationEntry> captor = ArgumentCaptor.forClass(Fido2AuthenticationEntry.class);
        Mockito.verify(authenticationPersistenceService).save(captor.capture());
        authenticationEntry = captor.getValue();

        assertNotNull(authenticationEntry);
        assertNotNull(response);
        assertTrue(response.get("allowCredentials").size() > 0);
        assertEquals(response.get("allowCredentials").get(0).get("id").asText(), registrationEntry.getPublicKeyId());

        assertEquals(authenticationEntry.getAuthenticationStatus(), Fido2AuthenticationStatus.pending);
	}

	public void testFinishAssertionTwoStepAndroidImpl(String userName, String authenticateFinishResponse) {
		// Parse register response
		AuthenticateResponse authenticateResponse = assertionSuperGluuController.parseAuthenticateResponse(authenticateFinishResponse);

		JsonNode request = assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(userName, authenticateFinishResponse, authenticateResponse);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_REQUEST).asBoolean(), true);
        assertEquals(request.get(CommonVerifiers.SUPER_GLUU_MODE).asText(), SuperGluuMode.TWO_STEP.getMode());

		ObjectNode response = assertionService.verify(request);

		// Get updated entry for checks
        ArgumentCaptor<Fido2AuthenticationEntry> captorAssertion = ArgumentCaptor.forClass(Fido2AuthenticationEntry.class);
        Mockito.verify(authenticationPersistenceService).update(captorAssertion.capture());
        authenticationEntry = captorAssertion.getValue();

		ArgumentCaptor<Fido2RegistrationEntry> captorAttestation = ArgumentCaptor.forClass(Fido2RegistrationEntry.class);
        Mockito.verify(registrationPersistenceService).update(captorAttestation.capture());
        registrationEntry = captorAttestation.getValue();

		assertNotNull(response);
        assertEquals(response.get("status").asText(), "ok");
        assertEquals(response.get("authenticatedCredentials").get("id").asText(), registrationEntry.getPublicKeyId());

        assertEquals(authenticationEntry.getAuthenticationStatus(), Fido2AuthenticationStatus.authenticated);
	}

	@Test
	@Order(1)
    @ExtendWith(FileParameterExtension.class)
	public void testStartAttestationTwoStepAndroid(@Name("attestation.android.two-step.issuer") String issuer, @Name("attestation.android.two-step.challenge") String challenge,
			@Name("attestation.android.two-step.userName") String userName, @Name("attestation.android.two-step.applicationId") String applicationId,
			@Name("attestation.android.two-step.sessionId") String sessionId, @Name("attestation.android.two-step.enrollmentCode") String enrollmentCode) {
		testStartAttestationTwoStepAndroidImpl(issuer, challenge, userName, applicationId, sessionId);
	}

	@Test
	@Order(2)
    @ExtendWith(FileParameterExtension.class)
	public void testFinishAttestationTwoStepAndroid(@Name("attestation.android.two-step.userName") String userName,
			@Name("attestation.android.two-step.finish.request") String registerFinishResponse, @Name("attestation.android.two-step.finish.publicKeyId") String publicKeyId) {
		testFinishAttestationTwoStepAndroidImpl(userName, registerFinishResponse, publicKeyId);
	}

	@Test
	@Order(3)
    @ExtendWith(FileParameterExtension.class)
	public void testStartAssertionTwoStepAndroid(@Name("attestation.android.two-step.issuer") String issuer, @Name("assertion.android.two-step.challenge") String challenge,
			@Name("attestation.android.two-step.userName") String userName, @Name("attestation.android.two-step.applicationId") String applicationId,
			@Name("attestation.android.two-step.sessionId") String sessionId) {

		testStartAssertionTwoStepAndroidImpl(issuer, challenge, userName, applicationId, sessionId);
	}

	@Test
	@Order(4)
    @ExtendWith(FileParameterExtension.class)
	public void testFinishAssertionTwoStepAndroid(@Name("attestation.android.two-step.userName") String userName,
			@Name("assertion.android.two-step.finish.request") String authenticateFinishResponse) {
		testFinishAssertionTwoStepAndroidImpl(userName, authenticateFinishResponse);
        assertTrue(registrationEntry.getCounter() == 1);
	}

	@Test
	@Order(5)
    @ExtendWith(FileParameterExtension.class)
	public void testSecondStartAssertionTwoStepAndroid(@Name("attestation.android.two-step.issuer") String issuer, @Name("assertion.android.two-step.challenge2") String challenge,
			@Name("attestation.android.two-step.userName") String userName, @Name("attestation.android.two-step.applicationId") String applicationId,
			@Name("attestation.android.two-step.sessionId") String sessionId) {
		testStartAssertionTwoStepAndroidImpl(issuer, challenge, userName, applicationId, sessionId);
	}

	@Test
	@Order(6)
    @ExtendWith(FileParameterExtension.class)
	public void testSecondFinishAssertionTwoStepAndroid(@Name("attestation.android.two-step.userName") String userName,
			@Name("assertion.android.two-step.finish.request2") String authenticateFinishResponse) {
		testFinishAssertionTwoStepAndroidImpl(userName, authenticateFinishResponse);
        assertTrue(registrationEntry.getCounter() == 2);
	}

	@Test
	@Order(7)
    @ExtendWith(FileParameterExtension.class)
	public void testSecondReplyFinishAssertionTwoStepAndroid(@Name("attestation.android.two-step.userName") String userName,
			@Name("assertion.android.two-step.finish.request2") String authenticateFinishResponse) {
		try {
			testFinishAssertionTwoStepAndroidImpl(userName, authenticateFinishResponse);
		} catch (Fido2RuntimeException ex) {
			if (!(ex.getCause() instanceof Fido2CompromisedDevice)) {
				throw ex;
			}
		}
	}

}
