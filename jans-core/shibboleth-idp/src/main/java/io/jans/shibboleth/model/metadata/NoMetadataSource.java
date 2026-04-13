package io.jans.shibboleth.model.metadata;


public class NoMetadataSource implements MetadataSource {


    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.NONE;
    }
}