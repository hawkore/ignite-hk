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
import java.util.UUID;

import org.apache.ignite.internal.processors.cache.GridCacheInternal;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Queue header key.
 *
 * HK-PATCHED: improve performance, faster UUID comparation instead of by name on CacheDataStructuresManager
 */
public class GridCacheQueueHeaderKey implements Externalizable, GridCacheInternal {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private UUID queueNameUuid;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheQueueHeaderKey() {
        // No-op.
    }

    /**
     * @param name Queue name.
     */
    public GridCacheQueueHeaderKey(String name) {
    	this.queueNameUuid = UUID.nameUUIDFromBytes(name.getBytes());
    }

    /**
     * @param name Queue name.
     */
    public GridCacheQueueHeaderKey(UUID queueNameUuid) {
    	this.queueNameUuid = queueNameUuid;
    }

	/**
	 * @return the queueNameUuid
	 */
	public UUID getQueueNameUuid() {
		return queueNameUuid;
	}

	/** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
    	U.writeUuid(out, queueNameUuid);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    	queueNameUuid = U.readUuid(in);
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridCacheQueueHeaderKey queueKey = (GridCacheQueueHeaderKey)o;

        return queueNameUuid.equals(queueKey.queueNameUuid);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return queueNameUuid.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheQueueHeaderKey.class, this);
    }
}
