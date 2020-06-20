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

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRow;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.processors.query.GridQueryTypeDescriptor;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.processors.query.h2.H2Utils;
import org.apache.ignite.internal.util.typedef.internal.SB;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

/**
 * Table row implementation based on {@link GridQueryTypeDescriptor}.
 *
 * HK-PATCHED: add support to advanced lucene index
 */
public class H2CacheRow extends H2Row implements CacheDataRow {
    /** H2 row descriptor. */
    private final GridH2RowDescriptor desc;

    /** Cache row. */
    private final CacheDataRow row;

    /** */
    private Value lucene = ValueNull.INSTANCE;

    /** */
    private Value luceneScoreDoc = ValueNull.INSTANCE;

    /** */
    private Value[] valCache;

    public H2CacheRow(GridH2RowDescriptor desc, CacheDataRow row) {
            this(desc, row,null,null);
    }

    /**
     * Constructor.
     *
     * @param desc Row descriptor.
     * @param row Row.
     */
    public H2CacheRow(GridH2RowDescriptor desc, CacheDataRow row, String luceneExpression, Object luceneScoreDoc) {
        this.desc = desc;
        this.row = row;
        // luceneExpression is mandatory when filter by lucene expression to ensures H2 internal comparison select * from xxx where lucene=luceneExpression matches
        if (luceneExpression != null){
            this.lucene  = ValueString.get(luceneExpression);
        }
        // will be used to sort SQL query results collected from cluster nodes when filter by lucene expression
        if (luceneScoreDoc != null){
            this.luceneScoreDoc = wrap(luceneScoreDoc, Value.JAVA_OBJECT);
        }
    }

    /** {@inheritDoc} */
    @Override public int getColumnCount() {
        if (removedRow())
            return 1;

        return QueryUtils.DEFAULT_COLUMNS_COUNT + desc.fieldsCount();
    }

    /** {@inheritDoc} */
    @Override public Value getValue(int col) {
        if (removedRow()) {
            assert col == 0 : col;

            return keyWrapped();
        }

        switch (col) {
            case QueryUtils.KEY_COL:
                return keyWrapped();

            case QueryUtils.VAL_COL:
                return valueWrapped();

            case QueryUtils.LUCENE_COL:
                return lucene;

            case QueryUtils.LUCENE_SCORE_COL:
                return luceneScoreDoc;

            default:
                if (desc.isKeyAliasColumn(col))
                    return keyWrapped();
                else if (desc.isValueAliasColumn(col))
                    return valueWrapped();

                return getValue0(col - QueryUtils.DEFAULT_COLUMNS_COUNT);
        }
    }

    /**
     * Get real column value.
     *
     * @param col Adjusted column index (without default columns).
     * @return Value.
     */
    private Value getValue0(int col) {
        Value v = getCached(col);

        if (v != null)
            return v;

        Object res = desc.columnValue(row.key(), row.value(), col);

        v = res == null ? ValueNull.INSTANCE : wrap(res, desc.fieldType(col));

        setCached(col, v);

        return v;
    }

    /**
     * Prepare values cache.
     */
    public void prepareValuesCache() {
        valCache = new Value[desc.fieldsCount()];
    }

    /**
     * Clear values cache.
     */
    public void clearValuesCache() {
        valCache = null;
    }

    /**
     * Get cached value (if any).
     *
     * @param colIdx Column index.
     * @return Value.
     */
    private Value getCached(int colIdx) {
    	if (valCache != null && desc.fieldsCount() != valCache.length){
    		// desc has been updated (new columns?) -> reset val cached size
    		this.prepareValuesCache();
    	}
        return valCache != null ? valCache[colIdx] : null;
    }

    /**
     * Set cache value.
     *
     * @param colIdx Column index.
     * @param val Value.
     */
    private void setCached(int colIdx, Value val) {
        if (valCache != null)
            valCache[colIdx] = val;
    }

    /**
     * @return Wrapped key value.
     */
    private Value keyWrapped() {
        return wrap(row.key(), desc.keyType());
    }

    /**
     * @return Wrapped value value.
     */
    private Value valueWrapped() {
        return wrap(row.value(), desc.valueType());
    }

    /**
     * Wrap the given object into H2 value.
     *
     * @param val Value.
     * @param type Type.
     * @return wrapped value.
     */
    private Value wrap(Object val, int type) {
        try {
            return H2Utils.wrap(desc.indexing().objectContext(), val, type);
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException("Failed to wrap object into H2 Value.", e);
        }
    }

    /**
     * @return {@code True} if this is removed row (doesn't have value).
     */
    private boolean removedRow() {
        return row.value() == null;
    }

    /** {@inheritDoc} */
    @Override public KeyCacheObject key() {
        return row.key();
    }

    /** {@inheritDoc} */
    @Override public void key(KeyCacheObject key) {
        row.key(key);
    }

    /** {@inheritDoc} */
    @Override public CacheObject value() {
        return row.value();
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion version() {
        return row.version();
    }

    /** {@inheritDoc} */
    @Override public int partition() {
        return row.partition();
    }

    /** {@inheritDoc} */
    @Override public long expireTime() {
        return row.expireTime();
    }

    /** {@inheritDoc} */
    @Override public long link() {
        return row.link();
    }

    /** {@inheritDoc} */
    @Override public void link(long link) {
        row.link(link);
    }

    /** {@inheritDoc} */
    @Override public int hash() {
        return row.hash();
    }

    /** {@inheritDoc} */
    @Override public int cacheId() {
        return row.cacheId();
    }

    /** {@inheritDoc} */
    @Override public long mvccCoordinatorVersion() {
        return row.mvccCoordinatorVersion();
    }

    /** {@inheritDoc} */
    @Override public long mvccCounter() {
        return row.mvccCounter();
    }

    /** {@inheritDoc} */
    @Override public int mvccOperationCounter() {
        return row.mvccOperationCounter();
    }

    /** {@inheritDoc} */
    @Override public byte mvccTxState() {
        return row.mvccTxState();
    }

    /** {@inheritDoc} */
    @Override public long newMvccCoordinatorVersion() {
        return row.newMvccCoordinatorVersion();
    }

    /** {@inheritDoc} */
    @Override public long newMvccCounter() {
        return row.newMvccCounter();
    }

    /** {@inheritDoc} */
    @Override public int newMvccOperationCounter() {
        return row.newMvccOperationCounter();
    }

    /** {@inheritDoc} */
    @Override public byte newMvccTxState() {
        return row.newMvccTxState();
    }

    /** {@inheritDoc} */
    @Override public boolean indexSearchRow() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void setKey(long key) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public void setValue(int idx, Value v) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public int size() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public int headerSize() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        SB sb = new SB("Row@");

        sb.a(Integer.toHexString(System.identityHashCode(this)));

        Value v = keyWrapped();
        sb.a("[ key: ").a(v == null ? "nil" : v.getString());

        v = valueWrapped();
        sb.a(", val: ").a(v == null ? "nil" : v.getString());

        sb.a(" ][ ");

        if (v != null) {
            for (int i = QueryUtils.DEFAULT_COLUMNS_COUNT, cnt = getColumnCount(); i < cnt; i++) {
                v = getValue(i);

                if (i != QueryUtils.DEFAULT_COLUMNS_COUNT)
                    sb.a(", ");

                if (!desc.isInternalColumn(i))
                    sb.a(v == null ? "nil" : v.getString());
            }
        }

        sb.a(" ]");

        return sb.toString();
    }
}
