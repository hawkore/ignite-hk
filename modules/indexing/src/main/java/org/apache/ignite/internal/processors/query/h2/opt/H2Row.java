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

package org.apache.ignite.internal.processors.query.h2.opt;

import org.apache.ignite.internal.processors.cache.mvcc.MvccVersionAware;
import org.apache.ignite.internal.processors.cache.mvcc.txlog.TxState;
import org.apache.ignite.internal.processors.query.h2.database.H2Tree;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.store.Data;
import org.h2.value.Value;

import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_COUNTER_NA;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_CRD_COUNTER_NA;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_OP_COUNTER_NA;

/**
 * Dummy H2 search row adadpter.
 *
 * HK-PATCHED: allow add row to org.h2.table.RegularTable - register H2 spatial functions
 */
public abstract class H2Row implements Row, MvccVersionAware {

    private long k;
    private int version;
    private boolean deleted;
    private int sessionId;

    /** {@inheritDoc} */
    @Override
    public int getVersion() {
        return this.version;
    }
    /** {@inheritDoc} */
    @Override
    public void setVersion(int paramInt) {
        this.version = paramInt;
    }
    /** {@inheritDoc} */
    @Override
    public long getKey() {
        return this.k;
    }
    /** {@inheritDoc} */
    @Override
    public void setKey(long paramLong) {
        this.k = paramLong;
    }
    /** {@inheritDoc} */
    @Override
    public void setDeleted(boolean paramBoolean) {
        this.deleted = paramBoolean;
    }
    /** {@inheritDoc} */
    @Override
    public void setSessionId(int paramInt) {
        this.sessionId = paramInt;
    }
    /** {@inheritDoc} */
    @Override
    public int getSessionId() {
        return this.sessionId;
    }
    /** {@inheritDoc} */
    @Override
    public void commit() {
        this.sessionId = 0;
    }
    /** {@inheritDoc} */
    @Override
    public boolean isDeleted() {
        return this.deleted;
    }

    /** {@inheritDoc} */
    @Override public void setKeyAndVersion(SearchRow old) {
        throw new UnsupportedOperationException();
    }


    /** {@inheritDoc} */
    @Override public int getMemory() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public Row getCopy() {
        throw new UnsupportedOperationException();
    }


    /** {@inheritDoc} */
    @Override public int getByteCount(Data dummy) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public Value[] getValueList() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public long mvccCoordinatorVersion() {
        return MVCC_CRD_COUNTER_NA;
    }

    /** {@inheritDoc} */
    @Override public long mvccCounter() {
        return MVCC_COUNTER_NA;
    }

    /** {@inheritDoc} */
    @Override public int mvccOperationCounter() {
        return MVCC_OP_COUNTER_NA;
    }

    /** {@inheritDoc} */
    @Override public byte mvccTxState() {
        return TxState.NA;
    }

    /**
     * @return Expire time.
     */
    public long expireTime() {
        return 0;
    }

    /**
     * @return {@code True} for rows used for index search (as opposed to rows stored in {@link H2Tree}.
     */
    public abstract boolean indexSearchRow();
}
