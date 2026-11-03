/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.jans.lock.model.trace.api.ProducerChainListResponse;
import io.jans.lock.model.trace.api.ProducerChainRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerChainResponse;
import io.jans.lock.model.trace.api.ProducerKeyListResponse;
import io.jans.lock.model.trace.api.ProducerKeyRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerKeyResponse;
import io.jans.lock.model.trace.api.ProducerKeyRevokeRequest;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.api.TraceBulkIngestionResponse;
import io.jans.lock.model.trace.api.TraceBulkResultEntry;
import io.jans.lock.model.trace.api.TraceErrorResponse;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceFlagsResponse;
import io.jans.lock.model.trace.api.TraceIngestionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.model.trace.api.TraceRecordSearchResponse;
import io.jans.lock.model.trace.api.TraceVerificationResponse;
import io.jans.lock.model.trace.grpc.BulkAcceptance;
import io.jans.lock.model.trace.grpc.BulkResultEntry;
import io.jans.lock.model.trace.grpc.ExecutionResult;
import io.jans.lock.model.trace.grpc.ProducerChain;
import io.jans.lock.model.trace.grpc.ProducerChainList;
import io.jans.lock.model.trace.grpc.ProducerKey;
import io.jans.lock.model.trace.grpc.ProducerKeyList;
import io.jans.lock.model.trace.grpc.RecordAcceptance;
import io.jans.lock.model.trace.grpc.RecordEnvelope;
import io.jans.lock.model.trace.grpc.RecordIngestion;
import io.jans.lock.model.trace.grpc.RecordSearchResult;
import io.jans.lock.model.trace.grpc.RecordVerification;
import io.jans.lock.model.trace.grpc.RegisterProducerChainRequest;
import io.jans.lock.model.trace.grpc.RegisterProducerKeyRequest;
import io.jans.lock.model.trace.grpc.RevokeProducerKeyRequest;
import io.jans.lock.model.trace.grpc.TraceError;
import io.jans.lock.model.trace.grpc.TraceFlags;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper between TRACE gRPC proto messages and the TRACE REST DTOs: requests are converted to
 * DTOs, REST responses to proto messages. Unset proto optional fields map to {@code null}, the
 * value an absent REST parameter has.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcTraceMapper {

    public ProducerKeyRegistrationRequest toKeyRegistrationRequest(RegisterProducerKeyRequest grpcRequest) {
        ProducerKeyRegistrationRequest request = new ProducerKeyRegistrationRequest();
        request.setProducerId(grpcRequest.getProducerId());
        request.setKid(grpcRequest.getKid());
        request.setPublicKeyJwk(new LinkedHashMap<>(grpcRequest.getPublicKeyJwkMap()));
        request.setAuthorizedEventKinds(new ArrayList<>(grpcRequest.getAuthorizedEventKindsList()));
        request.setValidFrom(grpcRequest.getValidFrom());
        request.setValidUntil(grpcRequest.hasValidUntil() ? grpcRequest.getValidUntil() : null);
        return request;
    }

    public ProducerKeyRevokeRequest toKeyRevokeRequest(RevokeProducerKeyRequest grpcRequest) {
        ProducerKeyRevokeRequest request = new ProducerKeyRevokeRequest();
        request.setProducerId(grpcRequest.getProducerId());
        request.setKid(grpcRequest.getKid());
        return request;
    }

    public ProducerChainRegistrationRequest toChainRegistrationRequest(RegisterProducerChainRequest grpcRequest) {
        ProducerChainRegistrationRequest request = new ProducerChainRegistrationRequest();
        request.setProducerId(grpcRequest.getProducerId());
        request.setProducerInstanceId(grpcRequest.getProducerInstanceId());
        request.setProducerChainId(grpcRequest.getProducerChainId());
        return request;
    }

    /**
     * @return the value of an optional string field, or {@code null} when it is unset or blank
     */
    public String optionalString(boolean present, String value) {
        return present && StringUtils.isNotBlank(value) ? value : null;
    }

    public RecordAcceptance toRecordAcceptance(TraceAcceptanceResponse response) {
        return RecordAcceptance.newBuilder()
                .setAccepted(response.isAccepted())
                .setProducerId(nullToEmpty(response.getProducerId()))
                .setRecordId(nullToEmpty(response.getRecordId()))
                .setContentDigest(nullToEmpty(response.getContentDigest()))
                .setReceiptSequence(response.getReceiptSequence())
                .setReceivedAt(nullToEmpty(response.getReceivedAt()))
                .setIdempotentReplay(response.isIdempotentReplay())
                .setFlags(toFlags(response))
                .build();
    }

    public BulkAcceptance toBulkAcceptance(TraceBulkIngestionResponse response) {
        BulkAcceptance.Builder builder = BulkAcceptance.newBuilder();
        for (TraceBulkResultEntry entry : response.getResults()) {
            builder.addResults(toBulkResultEntry(entry));
        }
        return builder.build();
    }

    public RecordEnvelope toRecordEnvelope(TraceRecordResponse response) {
        RecordEnvelope.Builder builder = RecordEnvelope.newBuilder()
                .setProducerId(nullToEmpty(response.getProducerId()))
                .setRecordId(nullToEmpty(response.getRecordId()))
                .setAssertion(response.getAssertion() == null ? "" : response.getAssertion().toString())
                .setAssertionRaw(nullToEmpty(response.getAssertionRaw()))
                .setContentDigest(nullToEmpty(response.getContentDigest()));
        if (response.getVerification() != null) {
            builder.setVerification(toVerification(response.getVerification()));
        }
        if (response.getIngestion() != null) {
            builder.setIngestion(toIngestion(response.getIngestion()));
        }
        return builder.build();
    }

    public RecordSearchResult toRecordSearchResult(TraceRecordSearchResponse response) {
        RecordSearchResult.Builder builder = RecordSearchResult.newBuilder();
        for (TraceRecordResponse record : response.getRecords()) {
            builder.addRecords(toRecordEnvelope(record));
        }
        return builder.build();
    }

    public ExecutionResult toExecutionResult(TraceExecutionResponse response) {
        ExecutionResult.Builder builder = ExecutionResult.newBuilder()
                .setExecutionAuthority(nullToEmpty(response.getExecutionAuthority()))
                .setTraceExecutionId(nullToEmpty(response.getTraceExecutionId()))
                .setOrdering(nullToEmpty(response.getOrdering()))
                .setCompleteness(nullToEmpty(response.getCompleteness()));
        for (TraceRecordResponse record : response.getRecords()) {
            builder.addRecords(toRecordEnvelope(record));
        }
        return builder.build();
    }

    public ProducerKey toProducerKey(ProducerKeyResponse response) {
        ProducerKey.Builder builder = ProducerKey.newBuilder()
                .setEvidenceDomainId(nullToEmpty(response.getEvidenceDomainId()))
                .setProducerId(nullToEmpty(response.getProducerId()))
                .setKid(nullToEmpty(response.getKid()))
                .setValidFrom(nullToEmpty(response.getValidFrom()))
                .setRegisteredBy(nullToEmpty(response.getRegisteredBy()))
                .setCreatedAt(nullToEmpty(response.getCreatedAt()));
        Map<String, String> jwk = response.getPublicKeyJwk();
        if (jwk != null) {
            builder.putAllPublicKeyJwk(jwk);
        }
        if (response.getAuthorizedEventKinds() != null) {
            builder.addAllAuthorizedEventKinds(response.getAuthorizedEventKinds());
        }
        if (response.getValidUntil() != null) {
            builder.setValidUntil(response.getValidUntil());
        }
        if (response.getRevokedAt() != null) {
            builder.setRevokedAt(response.getRevokedAt());
        }
        return builder.build();
    }

    public ProducerKeyList toProducerKeyList(ProducerKeyListResponse response) {
        ProducerKeyList.Builder builder = ProducerKeyList.newBuilder();
        for (ProducerKeyResponse key : response.getKeys()) {
            builder.addKeys(toProducerKey(key));
        }
        return builder.build();
    }

    public ProducerChain toProducerChain(ProducerChainResponse response) {
        return ProducerChain.newBuilder()
                .setEvidenceDomainId(nullToEmpty(response.getEvidenceDomainId()))
                .setProducerId(nullToEmpty(response.getProducerId()))
                .setProducerInstanceId(nullToEmpty(response.getProducerInstanceId()))
                .setProducerChainId(nullToEmpty(response.getProducerChainId()))
                .setRegisteredBy(nullToEmpty(response.getRegisteredBy()))
                .setGenesisKind(nullToEmpty(response.getGenesisKind()))
                .setCreatedAt(nullToEmpty(response.getCreatedAt()))
                .build();
    }

    public ProducerChainList toProducerChainList(ProducerChainListResponse response) {
        ProducerChainList.Builder builder = ProducerChainList.newBuilder();
        for (ProducerChainResponse chain : response.getChains()) {
            builder.addChains(toProducerChain(chain));
        }
        return builder.build();
    }

    private BulkResultEntry toBulkResultEntry(TraceBulkResultEntry entry) {
        BulkResultEntry.Builder builder = BulkResultEntry.newBuilder()
                .setIndex(entry.getIndex())
                .setAccepted(entry.isAccepted())
                .setProducerId(nullToEmpty(entry.getProducerId()))
                .setRecordId(nullToEmpty(entry.getRecordId()))
                .setContentDigest(nullToEmpty(entry.getContentDigest()))
                .setReceivedAt(nullToEmpty(entry.getReceivedAt()));
        if (entry.getReceiptSequence() != null) {
            builder.setReceiptSequence(entry.getReceiptSequence());
        }
        if (entry.getIdempotentReplay() != null) {
            builder.setIdempotentReplay(entry.getIdempotentReplay());
        }
        if (entry.isAccepted()) {
            builder.setFlags(toFlags(entry));
        }
        if (entry.getError() != null) {
            builder.setError(toTraceError(entry.getError()));
        }
        return builder.build();
    }

    private TraceFlags toFlags(TraceFlagsResponse flags) {
        TraceFlags.Builder builder = TraceFlags.newBuilder()
                .setCoverageGapFlag(flags.isCoverageGapFlag())
                .setChainLinkFailureFlag(flags.isChainLinkFailureFlag())
                .setEquivocationFlag(flags.isEquivocationFlag())
                .setLateFlag(flags.isLateFlag())
                .setPolicyMismatchFlag(flags.isPolicyMismatchFlag())
                .setReservedMemberFlag(flags.isReservedMemberFlag());
        addReservedMembers(builder, flags.getReservedMembers());
        return builder.build();
    }

    private TraceFlags toFlags(TraceBulkResultEntry entry) {
        TraceFlags.Builder builder = TraceFlags.newBuilder()
                .setCoverageGapFlag(Boolean.TRUE.equals(entry.getCoverageGapFlag()))
                .setChainLinkFailureFlag(Boolean.TRUE.equals(entry.getChainLinkFailureFlag()))
                .setEquivocationFlag(Boolean.TRUE.equals(entry.getEquivocationFlag()))
                .setLateFlag(Boolean.TRUE.equals(entry.getLateFlag()))
                .setPolicyMismatchFlag(Boolean.TRUE.equals(entry.getPolicyMismatchFlag()))
                .setReservedMemberFlag(Boolean.TRUE.equals(entry.getReservedMemberFlag()));
        addReservedMembers(builder, entry.getReservedMembers());
        return builder.build();
    }

    private void addReservedMembers(TraceFlags.Builder builder, List<String> reservedMembers) {
        if (reservedMembers != null) {
            builder.addAllReservedMembers(reservedMembers);
        }
    }

    private TraceError toTraceError(TraceErrorResponse error) {
        return TraceError.newBuilder()
                .setError(nullToEmpty(error.getError()))
                .setErrorDescription(nullToEmpty(error.getErrorDescription()))
                .setReason(nullToEmpty(error.getReason()))
                .build();
    }

    private RecordVerification toVerification(TraceVerificationResponse verification) {
        RecordVerification.Builder builder = RecordVerification.newBuilder()
                .setSignatureValid(verification.isSignatureValid())
                .setKeyId(nullToEmpty(verification.getKeyId()))
                .setKeyThumbprint(nullToEmpty(verification.getKeyThumbprint()))
                .setVerifiedAt(nullToEmpty(verification.getVerifiedAt()))
                .setAlgorithm(nullToEmpty(verification.getAlgorithm()));
        if (verification.getKeyAuthorized() != null) {
            builder.setKeyAuthorized(verification.getKeyAuthorized());
        }
        return builder.build();
    }

    private RecordIngestion toIngestion(TraceIngestionResponse ingestion) {
        return RecordIngestion.newBuilder()
                .setReceiptSequence(ingestion.getReceiptSequence())
                .setReceivedAt(nullToEmpty(ingestion.getReceivedAt()))
                .setPrevReceiptHash(nullToEmpty(ingestion.getPrevReceiptHash()))
                .setReceiptHash(nullToEmpty(ingestion.getReceiptHash()))
                .setReceiptState(nullToEmpty(ingestion.getReceiptState()))
                .setFlags(toFlags(ingestion))
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
