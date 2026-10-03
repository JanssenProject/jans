package io.jans.shibboleth.trust.config.error;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;

public class IncompatibleMetadataSourceForNature extends TrustError {
    
    private final TrustNature nature;

    private IncompatibleMetadataSourceForNature(MetadataSource source,TrustNature nature) {

        super(String.format("Metadatasource of type <%s> is not supported for <%s> TrustRelationships.",source.getType(),nature));
        this.nature = nature;
    }

    public TrustNature getNature() {

        return nature;
    }

    public static IncompatibleMetadataSourceForNature of(MetadataSource source, TrustNature nature) {
        
        return new IncompatibleMetadataSourceForNature(source, nature);
    }
}
