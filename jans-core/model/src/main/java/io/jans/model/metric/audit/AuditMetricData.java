/**
 * 
 */
package io.jans.model.metric.audit;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.model.metric.MetricData;
import io.jans.orm.annotation.AttributeName;

/**
 * @author Sergey Manoylo
 * @version July 9, 2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditMetricData extends MetricData {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4153060103561752273L;
	
	///----------------------------------------------------------	
	
    @AttributeName(name = "type")
	private String type;
    
    @AttributeName(name = "dn")    
	private String dn;

    @AttributeName(name = "userDn")    
	private String userDn;
    
    @AttributeName(name = "id")    
	private String id;
    
    @AttributeName(name = "outsideSid")
	private String outsideSid;
    
    @AttributeName(name = "lastUsedAt")    
	private Date lastUsedAt;
    
    @AttributeName(name = "authenticationTime")    
	private Date authenticationTime;
    
    @AttributeName(name = "authState")    
    private String authState;
    
    @AttributeName(name = "expirationDate")
	private Date expirationDate;

    @AttributeName(name = "sessionState")    
	private String sessionState;
    
    @AttributeName(name = "permissionGranted")    
	private String permissionGranted;
    
    @AttributeName(name = "permissionGrantedMap")    
    private Map<String, Boolean> permissionGrantedMap;
    
    @AttributeName(name = "deviceSecrets")    
    private List<String> deviceSecrets;
    
    ///----------------------------------------------------------
    
    @AttributeName(name = "auth_external_attributes")
    private String authExternalAttributes;
    
    @AttributeName(name = "opbs")
    private String opbs;
    
    @AttributeName(name = "response_type")    
    private String responseType;
    
    @AttributeName(name = "client_id")    
    private String clientId;
    
    @AttributeName(name = "auth_step")    
    private String authStep;
    
    @AttributeName(name = "acr")    
    private String acr;
    
    @AttributeName(name = "casa_logoUrl")    
    private String casaLogoUrl;
    
    @AttributeName(name = "remote_ip")    
    private String remoteIp;
    
    @AttributeName(name = "scope")    
    private String scope;
    
    @AttributeName(name = "acr_values")    
    private String acrValues;
    
    @AttributeName(name = "casa_faviconUrl")    
    private String casaFaviconUrl;
    
    @AttributeName(name = "redirect_uri")    
    private String redirectUri;
    
    @AttributeName(name = "state")    
    private String state;
    
    @AttributeName(name = "casa_prefix")    
    private String casaPrefix;
    
    @AttributeName(name = "casa_contextPath")    
    private String casaContextPath;
    
    @AttributeName(name = "casa_extraCss")    
    private String casaExtraCss;
    
    ///----------------------------------------------------------    
	
    public void setType(String type) {
    	this.type = type;
    }

    public String getType() {
    	return this.type;
    }

    public void setDn(String dn) {
    	this.dn = dn;
    }

    public String getDn() {
    	return this.dn;
    }
    
    public void setUserDn(String userDn) {
    	this.userDn = userDn;
    }

    public String getUserDn() {
    	return this.userDn;
    }
    
    public void setId(String id) {
    	this.id = id;
    }

    public String getId() {
    	return this.id;
    }
    
    public void setOutsideSid(String outsideSid) {
    	this.outsideSid = outsideSid;
    }
    
    public String getOutsideSid() {
    	return this.outsideSid;
    }

    
    public void setLastUsedAt(Date lastUsedAt) {
    	this.lastUsedAt = lastUsedAt;
    }
    
    public Date getLastUsedAt() {
    	return this.lastUsedAt;
    }

    public void setAuthenticationTime(Date authenticationTime) {
    	this.authenticationTime = authenticationTime;
    }
    
    public Date getAuthenticationTime() {
    	return this.authenticationTime;
    }
    
    public void setAuthState(String authState) {
    	this.authState = authState;
    }
    
    public String getAuthState() {
    	return this.authState;
    }
    
    public void setExpirationDate(Date expirationDate) {
    	this.expirationDate = expirationDate;
    }
    
    public Date getExpirationDate() {
    	return this.expirationDate;
    }
    
    
    public void setSessionState(String sessionState) {
    	this.sessionState = sessionState;
    }

    public String getSessionState() {
    	return this.sessionState;
    }

    public void setPermissionGranted(String permissionGranted) {
    	this.permissionGranted = permissionGranted;
    }

    public String getPermissionGranted() {
    	return this.permissionGranted;
    }

    
    public void setPermissionGrantedMap(Map<String, Boolean> permissionGrantedMap) {
    	this.permissionGrantedMap = permissionGrantedMap;
    }

    public Map<String, Boolean> getPermissionGrantedMap() {
    	return this.permissionGrantedMap;
    }

    public void setDeviceSecrets(List<String> deviceSecrets) {
    	this.deviceSecrets = deviceSecrets;
    }

    public List<String> getDeviceSecrets() {
    	return this.deviceSecrets;
    }

    
    public void setAuthExternalAttributes(String authExternalAttributes) {
    	this.authExternalAttributes = authExternalAttributes;
    }

    public String getAuthExternalAttributes() {
    	return this.authExternalAttributes;
    }
    

    public void setOpbs(String opbs) {
    	this.opbs = opbs;
    }

    public String getOpbs() {
    	return this.opbs;
    }

    public void setResponseType(String responseType) {
    	this.responseType = responseType;
    }

    public String getResponseType() {
    	return this.responseType;
    }

    public void setClientId(String clientId) {
    	this.clientId = clientId;
    }

    public String getClientId() {
    	return this.clientId;
    }

    public void setAuthStep(String authStep) {
    	this.authStep = authStep;
    }

    public String getAuthStep() {
    	return this.authStep;
    }

    public void setAcr(String acr) {
    	this.acr = acr;
    }

    public String getAcr() {
    	return this.acr;
    }

    public void setCasaLogoUrl(String casaLogoUrl) {
    	this.casaLogoUrl = casaLogoUrl;
    }

    public String getCasaLogoUrl() {
    	return this.casaLogoUrl;
    }

    public void setRemoteIp(String remoteIp) {
    	this.remoteIp = remoteIp;
    }

    public String getRemoteIp() {
    	return this.remoteIp;
    }

    public void setScope(String scope) {
    	this.scope = scope;
    }

    public String getScope() {
    	return this.scope;
    }

    public void setAcrValues(String acrValues) {
    	this.acrValues = acrValues;
    }

    public String getAcrValues() {
    	return this.acrValues;
    }
    
    public void setCasaFaviconUrl(String casaFaviconUrl) {
    	this.casaFaviconUrl = casaFaviconUrl;
    }

    public String getCasaFaviconUrl() {
    	return this.casaFaviconUrl;
    }
    
    public void setRedirectUri(String redirectUri) {
    	this.redirectUri = redirectUri;
    }

    public String getRedirectUri() {
    	return this.redirectUri;
    }    

    public void setState(String state) {
    	this.state = state;    	
    }

    public String getState() {
    	return this.state;    	
    }

    public void setCasaPrefix(String casaPrefix) {
    	this.casaPrefix = casaPrefix;    	
    }
    
    public String getCasaPrefix() {
    	return this.casaPrefix;    	
    }

    public void setCasaContextPath(String casaContextPath) {
    	this.casaContextPath = casaContextPath;    	
    }
    
    public String getCasaContextPath() {
    	return this.casaContextPath;    	
    }

    public void setCasaExtraCss(String casaExtraCss) {
    	this.casaExtraCss = casaExtraCss;    	
    }
    
    public String getCasaExtraCss() {
    	return this.casaExtraCss;    	
    }
	
}
