/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.datastructures;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_ATOMIC_CACHE_QUEUE_RETRY_TIMEOUT;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.cache.processor.EntryProcessor;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

/**
 * {@link org.apache.ignite.IgniteQueue} implementation using atomic cache.
 */
public class GridAtomicCacheQueueImpl<T> extends GridCacheQueueAdapter<T> {
    /** */
    private static final long RETRY_TIMEOUT = Integer.getInteger(IGNITE_ATOMIC_CACHE_QUEUE_RETRY_TIMEOUT, 10000);

    /**
     * @param queueName Queue name.
     * @param hdr Queue header.
     * @param cctx Cache context.
     */
    public GridAtomicCacheQueueImpl(String queueName, GridCacheQueueHeader hdr, GridCacheContext<?, ?> cctx, CollectionConfiguration colConfig) {
        super(queueName, hdr, cctx, colConfig);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public boolean offer(T item) throws IgniteException {
        try {
            Long idx = transformHeader(new AddProcessor(id, 1));

            if (idx == null)
                return false;

            checkRemoved(idx);

            queueCache.put(itemKey(idx), item);

            return true;
        }
        catch (IgniteCheckedException e) {
            throw U.convertException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable @Override public T poll() throws IgniteException {

        GridCacheQueueHeader cHdr = getCurrentHdr(false);

        //avoid over cluster processor polling if queue has no more elements
        if (cHdr.size()==0){
            checkRemoved(cHdr);
            return null;
        }

        try {

            while (true) {

                Long idx = transformHeader(new PollProcessor(id));

                if (idx == null)
                    return null;

                checkRemoved(idx);

                QueueItemKey key = itemKey(idx);

                T data = (T)queueCache.getAndRemove(key);

                if (data != null)
                    return data;

                long stop = U.currentTimeMillis() + RETRY_TIMEOUT;

                while (U.currentTimeMillis() < stop) {
                    data = (T)queueCache.getAndRemove(key);

                    if (data != null)
                        return data;
                }

                U.warn(log, "Failed to get item due to poll timeout [queue=" + queueName + ", idx=" + idx + "]. " +
                    "Poll timeout can be redefined by 'IGNITE_ATOMIC_CACHE_QUEUE_RETRY_TIMEOUT' system property.");
            }
        }
        catch (IgniteCheckedException e) {
            throw U.convertException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public boolean addAll(Collection<? extends T> items) {
        A.notNull(items, "items");

        try {
            Long idx = transformHeader(new AddProcessor(id, items.size()));

            if (idx == null)
                return false;

            checkRemoved(idx);

            Map<QueueItemKey, T> putMap = new HashMap<>();

            for (T item : items) {
                putMap.put(itemKey(idx), item);

                idx++;
            }

            queueCache.putAll(putMap);

            return true;
        }
        catch (IgniteCheckedException e) {
            throw U.convertException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected void removeItem(long rmvIdx) throws IgniteCheckedException {

        GridCacheQueueHeader cHdr = getCurrentHdr(true);
        //avoid over cluster processor if queue has no more elements
        if (cHdr.size()==0){
            checkRemoved(cHdr);
            return;
        }

        Long idx = (Long)queueCache.invoke(queueKey, new RemoveProcessor(id, rmvIdx)).get();

        if (idx != null) {
            checkRemoved(idx);

            QueueItemKey key = itemKey(idx);

            if (queueCache.remove(key))
                return;

            long stop = U.currentTimeMillis() + RETRY_TIMEOUT;

            while (U.currentTimeMillis() < stop) {
                if (queueCache.remove(key))
                    return;
            }

            U.warn(log, "Failed to remove item, [queue=" + queueName + ", idx=" + idx + ']');
        }
    }

    /**
     * @param c EntryProcessor to be applied for queue header.
     * @return Value computed by the entry processor.
     * @throws IgniteCheckedException If failed.
     */
    @SuppressWarnings("unchecked")
    @Nullable private Long transformHeader(EntryProcessor<GridCacheQueueHeaderKey, GridCacheQueueHeader, Long> c)
        throws IgniteCheckedException {
        return (Long)queueCache.invoke(queueKey, c).get();
    }
}
