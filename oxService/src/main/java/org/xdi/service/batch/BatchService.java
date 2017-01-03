package org.xdi.service.batch;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * Created by eugeniuparvan on 12/29/16.
 */
public abstract class BatchService<T> {

    protected abstract List<T> getChunkOrNull(int offset, int chunkSize);

    protected abstract void performAction(List<T> objects);

    public void iterateAllByChunks(int chunkSize) {
        List<T> objects;
        int offset = 0;
        while (!CollectionUtils.isEmpty(objects = getChunkOrNull(offset, chunkSize))) {
            offset += objects.size();
            performAction(objects);
            if(objects.size() < chunkSize)
                break;
        }
    }
}
