package io.jans.shibboleth.model.metadata;

import io.jans.shibboleth.model.core.EntityId;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.model.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.model.metadata.manual.CertificateInfo;
import io.jans.shibboleth.model.metadata.manual.NoCertificateInfo;
import io.jans.shibboleth.model.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.model.util.TrustResult;

public class ManualMetadataSource implements MetadataSource  {
    
    private final EntityId entityId;
    private final ValidityPeriod validUntil;
    private final AssertionConsumerService assertionConsumerService;
    private final CertificateInfo signingCertificate;

    private ManualMetadataSource(EntityId entityId, ValidityPeriod validUntil, 
        AssertionConsumerService assertionConsumerService, CertificateInfo signingCertificate) {

        this.entityId = entityId;
        this.validUntil = validUntil;
        this.assertionConsumerService = assertionConsumerService;
        this.signingCertificate = signingCertificate;
    }

    public EntityId getEntityId() {

        return entityId;
    }

    public ValidityPeriod getValidUntil() {

        return validUntil;
    }


    public AssertionConsumerService getAssertionConsumerService() {

        return assertionConsumerService;
    }

    public CertificateInfo getSigningCertificate() {

        return signingCertificate;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.MANUAL;
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(ManualMetadataSource source) {

        return new Builder(source);
    }

    public static Builder withNoSigningCertificate() {

        return new Builder(null).signingCertificate(new NoCertificateInfo());
    }

    public static class Builder {

        private EntityId entityId;
        private ValidityPeriod validUntil;
        private AssertionConsumerService assertionConsumerService;
        private CertificateInfo signingCertificate;
        
        public Builder(ManualMetadataSource source) {

            if (source != null) {

                this.entityId = source.entityId;
                this.validUntil = source.validUntil;
                this.assertionConsumerService = source.assertionConsumerService;
                this.signingCertificate = source.signingCertificate;
            }
        }

        public Builder entityId(EntityId entityId) {

            this.entityId = entityId;
            return this;
        }

        public Builder validUntil(ValidityPeriod validUntil) {

            this.validUntil = validUntil;
            return this;
        }

        public Builder assertionConsumerService(AssertionConsumerService assertionConsumerService) {

            this.assertionConsumerService = assertionConsumerService;
            return this;
        }

        public Builder signingCertificate(CertificateInfo signingCertificate) {

            this.signingCertificate = signingCertificate;
            return this;
        }

        public TrustResult<MetadataSource> build() {
            
            if (entityId == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("entityId"));
            }

            if (validUntil == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("validUntil"));
            }

            if (assertionConsumerService == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("assertionConsumerService"));
            }

            if (signingCertificate == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("signingCertificate"));
            }

            return TrustResult.success(new ManualMetadataSource(
                entityId, validUntil, 
                assertionConsumerService, signingCertificate
            ));
        }

    }
}