package io.jans.shibboleth.trust.config.metadata;

import java.util.Objects;

import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.config.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.NoCertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.trust.shared.Result;

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

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if( o == null || getClass() != o.getClass() ) return false;

        ManualMetadataSource other = (ManualMetadataSource) o;

        return Objects.equals(entityId,other.entityId)
            && Objects.equals(validUntil,other.validUntil)
            && Objects.equals(assertionConsumerService,other.assertionConsumerService)
            && Objects.equals(signingCertificate,other.signingCertificate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(entityId,validUntil,assertionConsumerService,signingCertificate);
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

        public Result<MetadataSource> build() {
            
            if (entityId == null) {

                return Result.failure(RequiredValueMissing.forField("entityId"));
            }

            if (validUntil == null) {

                return Result.failure(RequiredValueMissing.forField("validUntil"));
            }

            if (assertionConsumerService == null) {

                return Result.failure(RequiredValueMissing.forField("assertionConsumerService"));
            }

            if (signingCertificate == null) {

                return Result.failure(RequiredValueMissing.forField("signingCertificate"));
            }

            return Result.success(new ManualMetadataSource(
                entityId, validUntil, 
                assertionConsumerService, signingCertificate
            ));
        }

    }
}