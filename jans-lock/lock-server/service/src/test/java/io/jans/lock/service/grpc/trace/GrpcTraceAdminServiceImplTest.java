/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import io.grpc.Status;
import io.jans.lock.model.trace.api.ProducerChainListResponse;
import io.jans.lock.model.trace.api.ProducerChainRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerChainResponse;
import io.jans.lock.model.trace.api.ProducerKeyListResponse;
import io.jans.lock.model.trace.api.ProducerKeyRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerKeyResponse;
import io.jans.lock.model.trace.api.ProducerKeyRevokeRequest;
import io.jans.lock.model.trace.grpc.ListProducerChainsRequest;
import io.jans.lock.model.trace.grpc.ListProducerKeysRequest;
import io.jans.lock.model.trace.grpc.ProducerChain;
import io.jans.lock.model.trace.grpc.ProducerChainList;
import io.jans.lock.model.trace.grpc.ProducerKey;
import io.jans.lock.model.trace.grpc.ProducerKeyList;
import io.jans.lock.model.trace.grpc.RegisterProducerChainRequest;
import io.jans.lock.model.trace.grpc.RegisterProducerKeyRequest;
import io.jans.lock.model.trace.grpc.RevokeProducerKeyRequest;
import io.jans.lock.service.ws.rs.trace.TraceAdminRestWebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

class GrpcTraceAdminServiceImplTest {

	private TraceAdminRestWebService rest;

	private GrpcTraceAdminServiceImpl service;

	@BeforeEach
	void setUp() {
		rest = mock(TraceAdminRestWebService.class);
		service = new GrpcTraceAdminServiceImpl(rest, new GrpcTraceMapper(),
				LoggerFactory.getLogger(GrpcTraceAdminServiceImpl.class));
	}

	private static ProducerKeyResponse keyResponse(boolean revoked) {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");

		ProducerKeyResponse dto = new ProducerKeyResponse();
		dto.setEvidenceDomainId("domain-1");
		dto.setProducerId("producer-1");
		dto.setKid("kid-1");
		dto.setPublicKeyJwk(jwk);
		dto.setAuthorizedEventKinds(List.of("AUTHORIZATION_DECISION"));
		dto.setValidFrom("2026-01-01T00:00:00Z");
		dto.setRevokedAt(revoked ? "2026-02-01T00:00:00Z" : null);
		dto.setRegisteredBy("client-1");
		dto.setCreatedAt("2026-01-01T00:00:00Z");
		return dto;
	}

	@Test
	void testRegisterProducerKey_MapsRequestAndResponse() {
		when(rest.registerProducerKey(any())).thenReturn(Response.status(Response.Status.CREATED).entity(keyResponse(false)).build());

		RecordingObserver<ProducerKey> observer = new RecordingObserver<>();
		service.registerProducerKey(RegisterProducerKeyRequest.newBuilder().setProducerId("producer-1").setKid("kid-1")
				.putPublicKeyJwk("kty", "OKP").putPublicKeyJwk("x", "abc")
				.addAuthorizedEventKinds("AUTHORIZATION_DECISION").setValidFrom("2026-01-01T00:00:00Z").build(), observer);

		ArgumentCaptor<ProducerKeyRegistrationRequest> captor = ArgumentCaptor.forClass(ProducerKeyRegistrationRequest.class);
		verify(rest).registerProducerKey(captor.capture());
		ProducerKeyRegistrationRequest request = captor.getValue();
		assertEquals("producer-1", request.getProducerId());
		assertEquals("kid-1", request.getKid());
		assertEquals("abc", request.getPublicKeyJwk().get("x"));
		assertEquals(List.of("AUTHORIZATION_DECISION"), request.getAuthorizedEventKinds());
		assertEquals("2026-01-01T00:00:00Z", request.getValidFrom());
		assertEquals(null, request.getValidUntil());

		ProducerKey result = observer.values.get(0);
		assertTrue(observer.completed);
		assertEquals("domain-1", result.getEvidenceDomainId());
		assertEquals("Ed25519", result.getPublicKeyJwkMap().get("crv"));
		assertFalse(result.hasValidUntil());
		assertFalse(result.hasRevokedAt());
	}

	@Test
	void testRegisterProducerKey_PassesValidUntilWhenSet() {
		when(rest.registerProducerKey(any())).thenReturn(Response.status(Response.Status.CREATED).entity(keyResponse(false)).build());

		service.registerProducerKey(RegisterProducerKeyRequest.newBuilder().setValidUntil("2027-01-01T00:00:00Z").build(),
				new RecordingObserver<>());

		ArgumentCaptor<ProducerKeyRegistrationRequest> captor = ArgumentCaptor.forClass(ProducerKeyRegistrationRequest.class);
		verify(rest).registerProducerKey(captor.capture());
		assertEquals("2027-01-01T00:00:00Z", captor.getValue().getValidUntil());
	}

	@Test
	void testListProducerKeys_FiltersByProducerOnlyWhenSet() {
		ProducerKeyListResponse dto = new ProducerKeyListResponse();
		dto.setKeys(List.of(keyResponse(false), keyResponse(true)));
		when(rest.listProducerKeys(any())).thenReturn(Response.ok(dto).build());

		RecordingObserver<ProducerKeyList> observer = new RecordingObserver<>();
		service.listProducerKeys(ListProducerKeysRequest.getDefaultInstance(), observer);
		verify(rest).listProducerKeys(null);
		assertEquals(2, observer.values.get(0).getKeysCount());
		assertTrue(observer.values.get(0).getKeys(1).hasRevokedAt());

		service.listProducerKeys(ListProducerKeysRequest.newBuilder().setProducerId("producer-1").build(), new RecordingObserver<>());
		verify(rest).listProducerKeys("producer-1");
	}

	@Test
	void testRevokeProducerKey_MapsRequestAndResponse() {
		when(rest.revokeProducerKey(any())).thenReturn(Response.ok(keyResponse(true)).build());

		RecordingObserver<ProducerKey> observer = new RecordingObserver<>();
		service.revokeProducerKey(RevokeProducerKeyRequest.newBuilder().setProducerId("producer-1").setKid("kid-1").build(), observer);

		ArgumentCaptor<ProducerKeyRevokeRequest> captor = ArgumentCaptor.forClass(ProducerKeyRevokeRequest.class);
		verify(rest).revokeProducerKey(captor.capture());
		assertEquals("producer-1", captor.getValue().getProducerId());
		assertEquals("kid-1", captor.getValue().getKid());
		assertEquals("2026-02-01T00:00:00Z", observer.values.get(0).getRevokedAt());
	}

	@Test
	void testRegisterAndListProducerChains() {
		ProducerChainResponse chain = new ProducerChainResponse();
		chain.setEvidenceDomainId("domain-1");
		chain.setProducerId("producer-1");
		chain.setProducerInstanceId("instance-1");
		chain.setProducerChainId("chain-1");
		chain.setRegisteredBy("client-1");
		chain.setGenesisKind("pre_registered");
		chain.setCreatedAt("2026-01-01T00:00:00Z");
		when(rest.registerProducerChain(any())).thenReturn(Response.status(Response.Status.CREATED).entity(chain).build());
		ProducerChainListResponse list = new ProducerChainListResponse();
		list.setChains(List.of(chain));
		when(rest.listProducerChains(any())).thenReturn(Response.ok(list).build());

		RecordingObserver<ProducerChain> registered = new RecordingObserver<>();
		service.registerProducerChain(RegisterProducerChainRequest.newBuilder().setProducerId("producer-1")
				.setProducerInstanceId("instance-1").setProducerChainId("chain-1").build(), registered);
		ArgumentCaptor<ProducerChainRegistrationRequest> captor = ArgumentCaptor.forClass(ProducerChainRegistrationRequest.class);
		verify(rest).registerProducerChain(captor.capture());
		assertEquals("instance-1", captor.getValue().getProducerInstanceId());
		assertEquals("chain-1", captor.getValue().getProducerChainId());
		assertEquals("pre_registered", registered.values.get(0).getGenesisKind());

		RecordingObserver<ProducerChainList> listed = new RecordingObserver<>();
		service.listProducerChains(ListProducerChainsRequest.getDefaultInstance(), listed);
		verify(rest).listProducerChains(null);
		assertEquals("chain-1", listed.values.get(0).getChains(0).getProducerChainId());
	}

	@Test
	void testRestError_IsTranslatedToGrpcStatus() {
		when(rest.revokeProducerKey(any())).thenThrow(new WebApplicationException(
				Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"record_not_found\"}").build()));

		RecordingObserver<ProducerKey> observer = new RecordingObserver<>();
		service.revokeProducerKey(RevokeProducerKeyRequest.getDefaultInstance(), observer);

		assertEquals(Status.Code.NOT_FOUND, observer.errorStatus().getCode());
		assertEquals("{\"error\":\"record_not_found\"}", observer.errorStatus().getDescription());
	}
}
