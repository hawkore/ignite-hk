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

import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.store.Data;
import org.h2.value.Value;

/**
 * Dummy H2 search row adadpter.
 */
public abstract class GridH2SearchRowAdapter implements Row {
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
    
    //hack to allow add row to org.h2.table.RegularTable - register h2 spatial functions
	private long k;
	private int version;
	private boolean deleted;
	private int sessionId;
	public int getVersion() {
		return this.version;
	}
	public void setVersion(int paramInt) {
		this.version = paramInt;
	}
	public long getKey() {
		return this.k;
	}
	public void setKey(long paramLong) {
		this.k = paramLong;
	}
	public void setDeleted(boolean paramBoolean) {
		this.deleted = paramBoolean;
	}
	public void setSessionId(int paramInt) {
		this.sessionId = paramInt;
	}
	public int getSessionId() {
		return this.sessionId;
	}
	public void commit() {
		this.sessionId = 0;
	}
	public boolean isDeleted() {
		return this.deleted;
	}
}
