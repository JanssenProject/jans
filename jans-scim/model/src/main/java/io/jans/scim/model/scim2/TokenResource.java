package io.jans.scim.model.scim2;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;

import java.util.List;

@Schema(id="urn:ietf:params:scim:schemas:core:2.0:Token", name="Token", description="User token")
public class TokenResource extends BaseScimResource {

    @Attribute(description = "Internal token identifier",
            returned = AttributeDefinition.Returned.NEVER,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String iti;

    @Attribute(description = "An opaque token hash",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String hash;

    @Attribute(description = "Type of token",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            canonicalValues = { "access_token", "refresh_token", "id_token" })
    private String type;
    
    @Attribute(description = "Issued at timestamp (iat)",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private long issuedAt;
    
    @Attribute(description = "Expires at timestamp (exp)",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private long expiresAt;

    @Attribute(description = "Application associated to this token",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String appName;

    @Attribute(description = "OAuth client ID associated to this token",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String clientId;
    
    @Attribute(description = "Scopes associated to this token",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            multiValueClass = String.class)
    private List<String> scopes;
    
    public String getIti() {
        return iti;
    }

    public void setIti(String iti) {
        this.iti = iti;
    }
    
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public long getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

}
