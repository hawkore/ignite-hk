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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteUuid;

/**
 * Queue item key.
 *
 * HK-PATCHED: improve performance, queue name is not used at all
 */
class GridCacheQueueItemKey implements Externalizable, QueueItemKey {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private IgniteUuid queueId;

    /** */
    private long idx;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheQueueItemKey() {
        // No-op.
    }

    /**
     * @param queueId Queue unique ID.
     * @param queueName Queue name.
     * @param idx Item index.
     */
    GridCacheQueueItemKey(IgniteUuid queueId, int queueNameHash, long idx) {
        this.queueId = queueId;
        this.idx = idx;
    }

    /**
     * @return Item index.
     */
    public Long index() {
        return idx;
    }

    /**
     * @return Queue UUID.
     */
    public IgniteUuid queueId() {
        return queueId;
    }


    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeGridUuid(out, queueId);
        out.writeLong(idx);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        queueId = U.readGridUuid(in);
        idx = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridCacheQueueItemKey itemKey = (GridCacheQueueItemKey)o;

        return idx == itemKey.idx && queueId.equals(itemKey.queueId);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = queueId.hashCode();

        res = 31 * res + (int)(idx ^ (idx >>> 32));

        return res;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheQueueItemKey.class, this);
    }
}
