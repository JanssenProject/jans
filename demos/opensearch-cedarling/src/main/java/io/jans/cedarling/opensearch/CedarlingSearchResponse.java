package io.jans.cedarling.opensearch;

import java.io.IOException;
import java.util.Map;

import org.opensearch.action.search.*;
import org.opensearch.core.xcontent.XContentBuilder;

public class CedarlingSearchResponse extends SearchResponse {
    
    private static final String EXT_SECTION_NAME = "ext";

    private Map<String, Object> params;

    public CedarlingSearchResponse(
        Map<String, Object> params,
        SearchResponseSections internalResponse,
        String scrollId,
        int totalShards,
        int successfulShards,
        int skippedShards,
        long tookInMillis,
        PhaseTook phaseTook,
        ShardSearchFailure[] shardFailures,
        Clusters clusters,
        String pointInTimeId) {

        super(internalResponse, scrollId, totalShards, successfulShards, skippedShards, 
                tookInMillis, phaseTook, shardFailures, clusters, pointInTimeId);
        this.params = params;

    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {

        builder.startObject();
        innerToXContent(builder, params);

        if (this.params != null) {
            builder.startObject(EXT_SECTION_NAME);
            builder.field(CedarlingSearchResponseProcessor.TYPE, this.params);
            builder.endObject();
        }
        builder.endObject();
        return builder;
        
    }
    
}
