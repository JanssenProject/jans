package io.jans.shibboleth.trust.persistence.config.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Stored JSON representation of a trust relationship's metadata source (the {@code jansMetadataSrc}
 * {@code @JsonObject} column). A flat, dedicated persistence type — not the API wire shape (TP3). The
 * {@code type} discriminator (a {@code MetadataSourceType} name) selects which fields are populated:
 *
 * <ul>
 *   <li>{@code NONE} — none</li>
 *   <li>{@code FILE} — {@code filePath}</li>
 *   <li>{@code URI} — {@code uri}</li>
 *   <li>{@code MDQ} — {@code baseUrl}</li>
 *   <li>{@code UPSTREAM} — {@code parentId}, {@code entityId}</li>
 *   <li>{@code MANUAL} — {@code entityId}, {@code validUntil}, {@code acs}, {@code signingCert}</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataSourcePayload {

    private String type;
    private String filePath;
    private String uri;
    private String baseUrl;
    private String parentId;
    private String entityId;
    private String validUntil;
    private Acs acs;
    private Cert signingCert;

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getFilePath() {

        return filePath;
    }

    public void setFilePath(String filePath) {

        this.filePath = filePath;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {

        this.uri = uri;
    }

    public String getBaseUrl() {

        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {

        this.baseUrl = baseUrl;
    }

    public String getParentId() {

        return parentId;
    }

    public void setParentId(String parentId) {

        this.parentId = parentId;
    }

    public String getEntityId() {

        return entityId;
    }

    public void setEntityId(String entityId) {

        this.entityId = entityId;
    }

    public String getValidUntil() {

        return validUntil;
    }

    public void setValidUntil(String validUntil) {

        this.validUntil = validUntil;
    }

    public Acs getAcs() {

        return acs;
    }

    public void setAcs(Acs acs) {

        this.acs = acs;
    }

    public Cert getSigningCert() {

        return signingCert;
    }

    public void setSigningCert(Cert signingCert) {

        this.signingCert = signingCert;
    }

    /** The assertion consumer service of a MANUAL metadata source. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Acs {

        private String location;
        private String binding;
        private int index;
        private boolean defaultEndpoint;

        public String getLocation() {

            return location;
        }

        public void setLocation(String location) {

            this.location = location;
        }

        public String getBinding() {

            return binding;
        }

        public void setBinding(String binding) {

            this.binding = binding;
        }

        public int getIndex() {

            return index;
        }

        public void setIndex(int index) {

            this.index = index;
        }

        public boolean isDefaultEndpoint() {

            return defaultEndpoint;
        }

        public void setDefaultEndpoint(boolean defaultEndpoint) {

            this.defaultEndpoint = defaultEndpoint;
        }
    }

    /** The signing certificate of a MANUAL metadata source: {@code type} is {@code X509} or {@code NONE}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cert {

        private String type;
        private String data;

        public String getType() {

            return type;
        }

        public void setType(String type) {

            this.type = type;
        }

        public String getData() {

            return data;
        }

        public void setData(String data) {

            this.data = data;
        }
    }
}
