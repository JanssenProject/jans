/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

/**
 * Relevant constants for SCIM server and client
 */
public interface Constants {

    /**
     * Default media type used in SCIM service
     */
    String MEDIA_TYPE_SCIM_JSON = "application/scim+json";

    /**
     * URN used to identify the schema used to extend the User resource type
     */
    String USER_EXT_SCHEMA_ID = "urn:ietf:params:scim:schemas:extension:gluu:2.0:User";

    /**
     * Human-readable name of the schema used to extend the User resource type
     */
    String USER_EXT_SCHEMA_NAME = "GluuUserCustomExtension";

    /**
     * Description of the schema used to extend the User resource type
     */
    String USER_EXT_SCHEMA_DESCRIPTION = "Gluu User Custom Extension";

    /**
     * Schema URI utilized in bulk requests. See section 8.2 of RFC 7644
     */
    String BULK_REQUEST_SCHEMA_ID = "urn:ietf:params:scim:api:messages:2.0:BulkRequest";

    /**
     * Schema URI utilized in bulk responses. See section 8.2 of RFC 7644
     */
    String BULK_RESPONSE_SCHEMA_ID = "urn:ietf:params:scim:api:messages:2.0:BulkResponse";

    /**
     * Schema URI utilized in query responses. See section 8.2 of RFC 7644
     */
    String LIST_RESPONSE_SCHEMA_ID = "urn:ietf:params:scim:api:messages:2.0:ListResponse";

    /**
     * Schema URI utilized in query requests. See section 8.2 of RFC 7644
     */
    String SEARCH_REQUEST_SCHEMA_ID = "urn:ietf:params:scim:api:messages:2.0:SearchRequest";

    /**
     * Schema URI utilized in the patch operation. See section 8.2 of RFC 7644
     */
    String PATCH_REQUEST_SCHEMA_ID = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

    /**
     * Schema URI utilized for error responses. See section 8.2 of RFC 7644
     */
    String ERROR_RESPONSE_URI = "urn:ietf:params:scim:api:messages:2.0:Error";

    String UTF8_CHARSET_FRAGMENT="; charset=utf-8";

    /**
     * The HTTP query parameter used to provide a filter expression.
     */
    String QUERY_PARAM_FILTER = "filter";

    /**
     * An HTTP query parameter used to override the behavior for returning resource attributes.
     * See section 3.4.2.5 of RFC 7644
     */
    String QUERY_PARAM_ATTRIBUTES = "attributes";

    /**
     * An HTTP query parameter used to override the behavior for returning resource attributes.
     * See section 3.4.2.5 of RFC 7644
     */
    String QUERY_PARAM_EXCLUDED_ATTRS = "excludedAttributes";

    /**
     * The HTTP query parameter used to specify an attribute to sort search results.
     */
    String QUERY_PARAM_SORT_BY = "sortBy";

    /**
     * The HTTP query parameter used to specify a sort order (ascending/descending).
     */
    String QUERY_PARAM_SORT_ORDER = "sortOrder";

    /**
     * The HTTP query parameter used to specify the starting index for page of results.
     */
    String QUERY_PARAM_START_INDEX = "startIndex";

    /**
     * The HTTP query parameter used to specify the maximum number of results per result page.
     */
    String QUERY_PARAM_COUNT = "count";

    String GROUP_OVERHEAD_BYPASS_PARAM = "Group-Overhead-Bypass";

    int MAX_COUNT = 200;    //Do not remove till Gluu 4 release. This is used in SCIM-client project

    int MAX_BULK_OPERATIONS = 30;
    int MAX_BULK_PAYLOAD_SIZE = 3072000;  // 3 MB

}