package org.gluu.site.ldap.persistence;

import com.unboundid.asn1.ASN1OctetString;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * Created by eugeniuparvan on 12/29/16.
 */
public abstract class BatchOperation<T> {

    private ASN1OctetString cookie = null;

    private boolean moreResultsToReturn;

    protected abstract List<T> getChunkOrNull(int batchSize);

    protected abstract void performAction(List<T> objects);

    public void iterateAllByChunks(int batchSize) {
        List<T> objects;
        while (!CollectionUtils.isEmpty(objects = getChunkOrNull(batchSize))) {
            performAction(objects);
            if (objects.size() < batchSize || !moreResultsToReturn)
                break;
        }
    }

    public ASN1OctetString getCookie() {
        return cookie;
    }

    public void setCookie(ASN1OctetString cookie) {
        this.cookie = cookie;
    }

    public void setMoreResultsToReturn(boolean moreResultsToReturn) {
        this.moreResultsToReturn = moreResultsToReturn;
    }
}
