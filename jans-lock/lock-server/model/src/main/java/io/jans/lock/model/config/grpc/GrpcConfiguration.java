/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.config.grpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import io.jans.lock.model.config.GrpcServerMode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * gRPC server configuration
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrpcConfiguration {

    @DocProperty(description = "gRPC server mode")
    @Schema(description = "gRPC server mode")
    private GrpcServerMode serverMode = GrpcServerMode.BRIDGE;
    
	@DocProperty(description = "Specify grpc port", defaultValue = "50051")
	@Schema(description = "Specify grpc port")
    private int grpcPort = 50051; // Default gRPC port

    // TLS/ALPN support (Netty-based)
	@DocProperty(description = "Use TLS for gRPC communication", defaultValue = "false")
	@Schema(description = "Use TLS for gRPC communication")
    private boolean useTls = false;

	@DocProperty(description = "TLS Cert Chain File Path", defaultValue = "")
	@Schema(description = "TLS Cert Chain File Path")
	private String tlsCertChainFilePath; // PEM cert chain file

	@DocProperty(description = "TLS Private Key File Path", defaultValue = "")
	@Schema(description = "TLS Private Key File Path")
    private String tlsPrivateKeyFilePath; // PEM private key file

	public GrpcServerMode getServerMode() {
		return serverMode;
	}

	public void setServerMode(GrpcServerMode serverMode) {
		this.serverMode = serverMode;
	}

	public int getGrpcPort() {
		return grpcPort;
	}

	public void setGrpcPort(int grpcPort) {
		this.grpcPort = grpcPort;
	}

	public boolean isUseTls() {
		return useTls;
	}

	public void setUseTls(boolean useTls) {
		this.useTls = useTls;
	}

	public String getTlsCertChainFilePath() {
		return tlsCertChainFilePath;
	}

	public void setTlsCertChainFilePath(String tlsCertChainFilePath) {
		this.tlsCertChainFilePath = tlsCertChainFilePath;
	}

	public String getTlsPrivateKeyFilePath() {
		return tlsPrivateKeyFilePath;
	}

	public void setTlsPrivateKeyFilePath(String tlsPrivateKeyFilePath) {
		this.tlsPrivateKeyFilePath = tlsPrivateKeyFilePath;
	}

	@Override
	public String toString() {
		return "GrpcConfiguration [serverMode=" + serverMode + ", grpcPort=" + grpcPort + ", useTls=" + useTls
				+ ", tlsCertChainFilePath=" + tlsCertChainFilePath + ", tlsPrivateKeyFilePath=" + tlsPrivateKeyFilePath
				+ "]";
	}

}
