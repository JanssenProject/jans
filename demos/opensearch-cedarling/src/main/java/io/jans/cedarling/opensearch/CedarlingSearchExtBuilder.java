package io.jans.cedarling.opensearch;

import java.io.IOException;
import java.util.*;

import org.opensearch.core.common.io.stream.*;
import org.opensearch.core.xcontent.*;
import org.opensearch.search.SearchExtBuilder;

public class CedarlingSearchExtBuilder extends SearchExtBuilder {

    public final static String PARAM_FIELD_NAME = "tbac";
    
    protected Map<String, Object> params;

    public CedarlingSearchExtBuilder(Map<String, Object> params) {
        this.params = params;
    }
    
    public CedarlingSearchExtBuilder(StreamInput in) throws IOException {
        params = in.readMap();
    }
    
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public String getWriteableName() {
        return PARAM_FIELD_NAME;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(params);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        for (String key : this.params.keySet()) {
            builder.field(key, this.params.get(key));
        }
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.params);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CedarlingSearchExtBuilder) && params.equals(((CedarlingSearchExtBuilder) obj).params);
    }

    /**
     * Pick out the first CedarlingSearchExtBuilder from a list of SearchExtBuilders
     * @param builders list of SearchExtBuilders
     * @return the CedarlingSearchExtBuilder
     */
    public static CedarlingSearchExtBuilder fromExtBuilderList(List<SearchExtBuilder> builders) {
        Optional<SearchExtBuilder> b = builders.stream().filter(CedarlingSearchExtBuilder.class::isInstance).findFirst();
        if (b.isPresent()) {
            return (CedarlingSearchExtBuilder) b.get();
        } else {
            return null;
        }
    }

    /**
     * Parse XContent to CedarlingSearchExtBuilder
     * @param parser parser parsing this searchExt
     * @return CedarlingSearchExtBuilder represented by this searchExt
     * @throws IOException if problems parsing
     */
    public static CedarlingSearchExtBuilder parse(XContentParser parser) throws IOException {
        CedarlingSearchExtBuilder ans = new CedarlingSearchExtBuilder((Map<String, Object>) parser.map());
        return ans;
    }

}
