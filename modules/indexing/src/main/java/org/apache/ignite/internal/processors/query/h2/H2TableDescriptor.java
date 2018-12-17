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

package org.apache.ignite.internal.processors.query.h2;

import static org.apache.ignite.internal.processors.query.h2.opt.GridH2KeyValueRowOnheap.KEY_COL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.query.GridQueryIndexDescriptor;
import org.apache.ignite.internal.processors.query.GridQueryTypeDescriptor;
import org.apache.ignite.internal.processors.query.h2.database.H2PkHashIndex;
import org.apache.ignite.internal.processors.query.h2.database.H2RowFactory;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2IndexBase;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2RowDescriptor;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2SystemIndexFactory;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Table;
import org.apache.ignite.internal.processors.query.h2.opt.GridLuceneIndex;
import org.apache.ignite.internal.processors.query.h2.opt.GridLuceneIndexLegacyImpl;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.h2.index.Index;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;

/**
 * Information about table in database.
 */
public class H2TableDescriptor implements GridH2SystemIndexFactory {
    /** Indexing. */
    private final IgniteH2Indexing idx;

    /** */
    private final String fullTblName;

    /** */
    private GridQueryTypeDescriptor type;

    /** */
    private final H2Schema schema;

    /** Cache context. */
    private final GridCacheContext cctx;

    /** */
    private GridH2Table tbl;

    /** */
    private GridLuceneIndex luceneIdx;

    /** */
    private H2PkHashIndex pkHashIdx;

    private Object mux = new Object();
    
    /**
     * Constructor.
     *
     * @param idx Indexing.
     * @param schema Schema.
     * @param type Type descriptor.
     * @param cctx Cache context.
     */
    public H2TableDescriptor(IgniteH2Indexing idx, H2Schema schema, GridQueryTypeDescriptor type,
        GridCacheContext cctx) {
        this.idx = idx;
        this.type = type;
        this.schema = schema;
        this.cctx = cctx;

        fullTblName = H2Utils.withQuotes(schema.schemaName()) + "." + H2Utils.withQuotes(type.tableName());
    }

    /**
     * @return Table.
     */
    public GridH2Table table() {
        return tbl;
    }

    /**
     * @param tbl Table.
     */
    public void table(GridH2Table tbl) {
        this.tbl = tbl;
    }

    /**
     * @return Schema.
     */
    public H2Schema schema() {
        return schema;
    }

    /**
     * @return Schema name.
     */
    public String schemaName() {
        return schema.schemaName();
    }

    /**
     * @return Table name.
     */
    public String tableName() {
        return type.tableName();
    }

    /**
     * @return Database full table name.
     */
    public String fullTableName() {
        return fullTblName;
    }

    /**
     * @return type name.
     */
    public String typeName() {
        return type.name();
    }

    /**
     * @return Cache context.
     */
    public GridCacheContext cache() {
        return cctx;
    }

    /**
     * @return Type.
     */
    public GridQueryTypeDescriptor type() {
        return type;
    }

    /**
     * @return Lucene index.
     */
    public GridLuceneIndex luceneIndex() {
        return luceneIdx;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(H2TableDescriptor.class, this);
    }

    /**
     * Create H2 row factory.
     *
     * @param rowDesc Row descriptor.
     * @return H2 row factory.
     */
    public H2RowFactory rowFactory(GridH2RowDescriptor rowDesc) {
        if (cctx.affinityNode())
            return new H2RowFactory(rowDesc, cctx);

        return null;
    }

    /** {@inheritDoc} */
    @Override public ArrayList<Index> createSystemIndexes(GridH2Table tbl) {
        ArrayList<Index> idxs = new ArrayList<>();

        IndexColumn keyCol = tbl.indexColumn(KEY_COL, SortOrder.ASCENDING);
        IndexColumn affCol = tbl.getAffinityKeyColumn();

        if (affCol != null && H2Utils.equals(affCol, keyCol))
            affCol = null;

        GridH2RowDescriptor desc = tbl.rowDescriptor();

        Index hashIdx = createHashIndex(
            tbl,
            "_key_PK_hash",
            H2Utils.treeIndexColumns(desc, new ArrayList<IndexColumn>(2), keyCol, affCol)
        );

        if (hashIdx != null)
            idxs.add(hashIdx);

        // Add primary key index.
        Index pkIdx = idx.createSortedIndex(
            "_key_PK",
            tbl,
            true,
            H2Utils.treeIndexColumns(desc, new ArrayList<IndexColumn>(2), keyCol, affCol),
            -1
        );

        idxs.add(pkIdx);

        if (luceneIdx == null){
            createOrUpdateLuceneIndex(null, false, false);
            //add Grid H2 Lucene index to H2 table
            if (luceneIdx != null && H2Utils.inClassPath(H2Utils.ADVANCED_LUCENE_IDX_CLS)){
                idxs.add(luceneIdx);
            }
        }


        boolean affIdxFound = false;

        // Locate index where affinity column is first (if any).
        if (affCol != null) {
            for (GridQueryIndexDescriptor idxDesc : type.indexes().values()) {
                if (idxDesc.type() != QueryIndexType.SORTED)
                    continue;

                String firstField = idxDesc.fields().iterator().next();

                Column col = tbl.getColumn(firstField);

                IndexColumn idxCol = tbl.indexColumn(col.getColumnId(),
                    idxDesc.descending(firstField) ? SortOrder.DESCENDING : SortOrder.ASCENDING);

                affIdxFound |= H2Utils.equals(idxCol, affCol);
            }
        }

        // Add explicit affinity key index if nothing alike was found.
        if (affCol != null && !affIdxFound) {
            idxs.add(idx.createSortedIndex("AFFINITY_KEY", tbl, false,
                H2Utils.treeIndexColumns(desc, new ArrayList<IndexColumn>(2), affCol, keyCol), -1));
        }

        return idxs;
    }

    /**
     * Get collection of user indexes.
     *
     * @return User indexes.
     */
    public Collection<GridH2IndexBase> createUserIndexes() {
        assert tbl != null;

        ArrayList<GridH2IndexBase> res = new ArrayList<>();

        for (GridQueryIndexDescriptor idxDesc : type.indexes().values()) {
            GridH2IndexBase idx = createUserIndex(idxDesc);
            if (idx != null){
                res.add(idx);
            }
         }
        //try globally

        if (luceneIdx == null){
            createOrUpdateLuceneIndex(null, false, false);
            //add Grid H2 Lucene index to H2 table
            if (luceneIdx != null && H2Utils.inClassPath(H2Utils.ADVANCED_LUCENE_IDX_CLS)){
                res.add(luceneIdx);
            }
        }

        return res;
    }

    /**
     * Create user index.
     *
     * @param idxDesc Index descriptor.
     * @return Index.
     */
    public GridH2IndexBase createUserIndex(GridQueryIndexDescriptor idxDesc) {
        IndexColumn keyCol = tbl.indexColumn(KEY_COL, SortOrder.ASCENDING);
        IndexColumn affCol = tbl.getAffinityKeyColumn();

        List<IndexColumn> cols = new ArrayList<>(idxDesc.fields().size() + 2);

        for (String field : idxDesc.fields()) {
            Column col = tbl.getColumn(field);

            cols.add(tbl.indexColumn(col.getColumnId(),
                idxDesc.descending(field) ? SortOrder.DESCENDING : SortOrder.ASCENDING));
        }

        GridH2RowDescriptor desc = tbl.rowDescriptor();

        if (idxDesc.type() == QueryIndexType.SORTED) {
            cols = H2Utils.treeIndexColumns(desc, cols, keyCol, affCol);

            return idx.createSortedIndex(idxDesc.name(), tbl, false, cols, idxDesc.inlineSize());
        }
        else if (idxDesc.type() == QueryIndexType.GEOSPATIAL){
            return H2Utils.createSpatialIndex(tbl, idxDesc.name(), cols.toArray(new IndexColumn[cols.size()]));

        } else if (idxDesc.type() == QueryIndexType.FULLTEXT && H2Utils.inClassPath(H2Utils.ADVANCED_LUCENE_IDX_CLS)){           
            if (luceneIdx == null){
                luceneIdx = (GridLuceneIndex) H2Utils.createAdvancedLuceneIndex(tbl, idxDesc.luceneIndexOptions());
                return luceneIdx;
            }
            return null;
        }
        
        throw new IllegalStateException("Index type: " + idxDesc.type());
    }

    /**
     * Create hash index.
     *
     * @param tbl Table.
     * @param idxName Index name.
     * @param cols Columns.
     * @return Index.
     */
    private Index createHashIndex(GridH2Table tbl, String idxName, List<IndexColumn> cols) {
        if (cctx.affinityNode()) {
            assert pkHashIdx == null : pkHashIdx;

            pkHashIdx = new H2PkHashIndex(cctx, tbl, idxName, cols);

            return pkHashIdx;
        }

        return null;
    }

    /**
     * Handle drop.
     */
    void onDrop() {
        idx.removeDataTable(tbl);

        tbl.destroy();

        try {
            dropLuceneIndex();
        } catch (IgniteCheckedException e) {
            //NOP
        }
	}

    /**
     * 
     * @param newType
     */
    public void updateType(GridQueryTypeDescriptor newType) {
        this.tbl.rowDescriptor().refreshMetadataFromTypeDescriptor(newType);
        this.type = newType;
    }
    
    /**
     * Drop Advanced Lucene Index 
     * 
     * Used from Advanced Lucence Index implementation
     * 
     * @param remove
     * @throws IgniteCheckedException 
     */
    public void dropLuceneIndex() throws IgniteCheckedException{
        if (luceneIdx != null){
           String sql = H2Utils.indexDropSql(this.schemaName(), luceneIdx.getName(), true);
           idx.executeSql(this.schemaName(), sql);
           luceneIdx.destroy(true);
        }
        luceneIdx = null;
    }
    
	/**
	 * Creates or updates (add new indexed fields) to Advanced Lucene Index
     * 
     * @param luceneIndexOptions
     * @param updateConfig
     * @param forceMutateQueryEntity
     * 
     * @return GridH2IndexBase lucene index
     */
	public GridH2IndexBase createOrUpdateLuceneIndex(String luceneIndexOptions, boolean updateConfig, boolean forceMutateQueryEntity) {

		if (this.tbl == null){
			return null;
		}
		
		if (this.type.luceneIndexOptions() != null || this.type.valueClass() == String.class || luceneIndexOptions != null ) {
		    synchronized (mux) {
     			if (luceneIdx == null){
    				try {
    				    //preferable Advanced Lucene Index if present
    	                if (H2Utils.inClassPath(H2Utils.ADVANCED_LUCENE_IDX_CLS)){
    	                    luceneIdx = (GridLuceneIndex) H2Utils.createAdvancedLuceneIndex(tbl, luceneIndexOptions == null ? this.type.luceneIndexOptions(): luceneIndexOptions);
    	                    return luceneIdx;
    	                }else{
    	                    luceneIdx = new GridLuceneIndexLegacyImpl(idx.kernalContext(), tbl.cacheName(), type);
    	                }
    				}catch (Exception e1) {
    					throw new IgniteException(e1);
    				}
    			}else{
    			    if (H2Utils.inClassPath(H2Utils.ADVANCED_LUCENE_IDX_CLS) && updateConfig){
    			        luceneIdx.updateIndexConfig(this.tbl,  luceneIndexOptions == null ? this.type.luceneIndexOptions(): luceneIndexOptions, forceMutateQueryEntity);
    			        return luceneIdx;
    			    }
    			}
		    }
		}
		return null;
	}
}
