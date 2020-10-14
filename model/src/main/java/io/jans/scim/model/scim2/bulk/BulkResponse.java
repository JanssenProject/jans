/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.bulk;

import static io.jans.scim.model.scim2.Constants.BULK_RESPONSE_SCHEMA_ID;

import java.util.Collections;

/**
 * Encapsulates the components of a SCIM BulkResponse (see section 3.7 of RFC 7644).
 * @author Rahat Ali Date: 05.08.2015
 */
/*
 * Updated by jgomer on 2017-11-21.
 */
public class BulkResponse extends BulkBase {

    /**
     * Creates an empty BulkResponse (initializing its {@link #getSchemas() schemas} properly).
     */
    public BulkResponse(){
        setSchemas(Collections.singletonList(BULK_RESPONSE_SCHEMA_ID));
    }

}
