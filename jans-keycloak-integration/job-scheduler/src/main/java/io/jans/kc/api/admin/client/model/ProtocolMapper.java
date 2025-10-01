package io.jans.kc.api.admin.client.model;

import java.util.HashMap;
import java.util.Map;

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

        return representation.getId();
    }

    public ProtocolMapperRepresentation representation() {

        return representation;
    }

    public String getName() {

        return representation.getName();
    }

    public static SamlUserAttributeMapperBuilder samlUserAttributeMapper(final String id) {

        return new SamlUserAttributeMapperBuilder(id);
    }

    public static SamlUserAttributeMapperBuilder samlUserAttributeMapper(final ProtocolMapper mapper) {

        return new SamlUserAttributeMapperBuilder(mapper);
    }

    public static class SamlUserAttributeMapperBuilder {

        private static final String NAMEFORMAT_OPT_BASIC = "Basic";
        private static final String NAMEFORMAT_OPT_URI_REFERENCE = "URI Reference";
        private static final String NAMEFORMAT_OPT_UNSPECIFIED = "Unspecified";

        private final ProtocolMapper mapper;
        private final Map<String,String> config;

        public SamlUserAttributeMapperBuilder(final String mapperid) {

            ProtocolMapperRepresentation pmr = new ProtocolMapperRepresentation();
            pmr.setProtocol(Protocol.SAML.value());
            pmr.setProtocolMapper(mapperid);
            this.mapper = new ProtocolMapper(pmr);
            this.config = new HashMap<>();
            pmr.setConfig(this.config);
        }

        private SamlUserAttributeMapperBuilder(final ProtocolMapper other) {

            this.mapper = new ProtocolMapper(other.representation);
            this.config = this.mapper.representation().getConfig();
        }

        public SamlUserAttributeMapperBuilder name(final String name) {

            mapper.representation.setName(name);
            return this;
        }

        public SamlUserAttributeMapperBuilder userAttribute(final String userattribute) {

            config.put("user.attribute",userattribute);
            return this;
        }

        public SamlUserAttributeMapperBuilder friendlyName(final String friendlyname)  {

            config.put("friendly.name",friendlyname);
            return this;
        }

        public SamlUserAttributeMapperBuilder attributeName(final String attributename) {

            config.put("attribute.name",attributename);
            return this;
        }

        public SamlUserAttributeMapperBuilder attributeNameFormatBasic() {

            config.put("attribute.nameformat",NAMEFORMAT_OPT_BASIC);
            return this;
        }

        public SamlUserAttributeMapperBuilder attributeNameFormatUriReference() {

            config.put("attribute.nameformat",NAMEFORMAT_OPT_URI_REFERENCE);
            return this;
        }

        public SamlUserAttributeMapperBuilder attributeNameFormatUnspecified() {

            config.put("attribute.nameformat",NAMEFORMAT_OPT_UNSPECIFIED);
            return this;
        }

        public SamlUserAttributeMapperBuilder jansAttributeName(final String attributename) {

            config.put("jans.attribute.name",attributename);
            return this;
        }

        public ProtocolMapper build() {

            return this.mapper;
        }
    }
}
