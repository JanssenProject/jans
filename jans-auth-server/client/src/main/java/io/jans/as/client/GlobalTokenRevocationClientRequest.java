package io.jans.as.client;

import io.jans.as.model.util.QueryBuilder;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Yuriy Z
 */
public class GlobalTokenRevocationClientRequest extends ClientAuthnRequest {

    private String format;
    private String id;

    public GlobalTokenRevocationClientRequest() {
        this(null, null);
    }

    public GlobalTokenRevocationClientRequest(String format, String id) {
        this.format = format;
        this.id = id;

        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
    }

    @Override
    public String getQueryString() {
        QueryBuilder builder = QueryBuilder.instance();

        builder.append("format", format);
        builder.append("id", id);
        appendClientAuthnToQuery(builder);

        return builder.toString();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "GlobalTokenRevocationRequest{" +
                "format='" + format + '\'' +
                ", id='" + id + '\'' +
                "} " + super.toString();
    }
}
