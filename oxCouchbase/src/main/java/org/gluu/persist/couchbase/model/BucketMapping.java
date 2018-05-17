package org.gluu.persist.couchbase.model;

import com.couchbase.client.java.Bucket;

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
