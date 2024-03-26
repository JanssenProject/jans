package io.jans.kc.api.admin.client.model;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;

public class ProtocolMapper {

    public enum Protocol {
        OPENID("openid"),
        SAML("saml");
        private final String value;

        private Protocol(final String value) {
            this.value = value;
        }

        public String value() {

            return this.value;
        }
    }
    private final ProtocolMapperRepresentation representation;

    public ProtocolMapper() {

        this.representation = new ProtocolMapperRepresentation();
    }

    public ProtocolMapper(ProtocolMapperRepresentation representation) {

        this.representation = representation;
    }

    public String getId() {

        return this.representation.getId();
    }

    public ProtocolMapperRepresentation representation() {

        return this.representation;
    }
}
