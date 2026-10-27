/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.telemetry;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional client-supplied context a native app/SDK may attach to an attestation or assertion
 * start/finish call, carrying detail no {@code User-Agent} string can (Play Services version, OEM
 * Credential Manager behavior, the last client-side error code, ...). See issue #14607.
 * <p>
 * Every field is optional and its absence must never change ceremony behavior — this class is pure
 * data, never validated against a fixed set of values. {@code platform}/{@code nativeApi}/
 * {@code flowContext} are documented as a closed set (android|ios, credential-manager|
 * asauthorization|legacy-fido2|webview, native|webview|system-browser) but are plain strings here:
 * an unrecognized value must still deserialize and persist, not reject the ceremony request over a
 * telemetry field the client got wrong.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NativeClientTelemetry implements Serializable {

	private static final long serialVersionUID = 1L;

	@JsonProperty("client_correlation_id")
	private String clientCorrelationId;

	@JsonProperty("platform")
	private String platform;

	@JsonProperty("native_api")
	private String nativeApi;

	@JsonProperty("os_version")
	private String osVersion;

	@JsonProperty("play_services_version")
	private String playServicesVersion;

	@JsonProperty("device_manufacturer")
	private String deviceManufacturer;

	@JsonProperty("device_model")
	private String deviceModel;

	@JsonProperty("credential_provider")
	private String credentialProvider;

	@JsonProperty("is_device_secure")
	private Boolean deviceSecure;

	@JsonProperty("flow_context")
	private String flowContext;

	@JsonProperty("app_version")
	private String appVersion;

	@JsonProperty("distribution_channel")
	private String distributionChannel;

	@JsonProperty("last_client_error_code")
	private String lastClientErrorCode;

	public String getClientCorrelationId() {
		return clientCorrelationId;
	}

	public void setClientCorrelationId(String clientCorrelationId) {
		this.clientCorrelationId = clientCorrelationId;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getNativeApi() {
		return nativeApi;
	}

	public void setNativeApi(String nativeApi) {
		this.nativeApi = nativeApi;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getPlayServicesVersion() {
		return playServicesVersion;
	}

	public void setPlayServicesVersion(String playServicesVersion) {
		this.playServicesVersion = playServicesVersion;
	}

	public String getDeviceManufacturer() {
		return deviceManufacturer;
	}

	public void setDeviceManufacturer(String deviceManufacturer) {
		this.deviceManufacturer = deviceManufacturer;
	}

	public String getDeviceModel() {
		return deviceModel;
	}

	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}

	public String getCredentialProvider() {
		return credentialProvider;
	}

	public void setCredentialProvider(String credentialProvider) {
		this.credentialProvider = credentialProvider;
	}

	public Boolean getDeviceSecure() {
		return deviceSecure;
	}

	public void setDeviceSecure(Boolean deviceSecure) {
		this.deviceSecure = deviceSecure;
	}

	public String getFlowContext() {
		return flowContext;
	}

	public void setFlowContext(String flowContext) {
		this.flowContext = flowContext;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getDistributionChannel() {
		return distributionChannel;
	}

	public void setDistributionChannel(String distributionChannel) {
		this.distributionChannel = distributionChannel;
	}

	public String getLastClientErrorCode() {
		return lastClientErrorCode;
	}

	public void setLastClientErrorCode(String lastClientErrorCode) {
		this.lastClientErrorCode = lastClientErrorCode;
	}

	@Override
	public String toString() {
		return "NativeClientTelemetry{" +
				"clientCorrelationId='" + clientCorrelationId + '\'' +
				", platform='" + platform + '\'' +
				", nativeApi='" + nativeApi + '\'' +
				", osVersion='" + osVersion + '\'' +
				", playServicesVersion='" + playServicesVersion + '\'' +
				", deviceManufacturer='" + deviceManufacturer + '\'' +
				", deviceModel='" + deviceModel + '\'' +
				", credentialProvider='" + credentialProvider + '\'' +
				", deviceSecure=" + deviceSecure +
				", flowContext='" + flowContext + '\'' +
				", appVersion='" + appVersion + '\'' +
				", distributionChannel='" + distributionChannel + '\'' +
				", lastClientErrorCode='" + lastClientErrorCode + '\'' +
				'}';
	}
}
