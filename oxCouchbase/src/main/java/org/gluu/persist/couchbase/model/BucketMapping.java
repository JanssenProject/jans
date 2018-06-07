/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.persist.couchbase.model;

import com.couchbase.client.java.Bucket;

/**
 * Holds bucket reference associated with it's string representation
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class BucketMapping {

    private final String bucketName;
    private final Bucket bucket;

    public BucketMapping(final String bucketName, final Bucket bucket) {
        this.bucketName = bucketName;
        this.bucket = bucket;
    }

    public final String getBucketName() {
        return bucketName;
    }

    public final Bucket getBucket() {
        return bucket;
    }
}
