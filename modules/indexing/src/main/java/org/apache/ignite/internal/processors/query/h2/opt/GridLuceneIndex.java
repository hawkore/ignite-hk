package org.apache.ignite.internal.processors.query.h2.opt;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.util.lang.GridCloseableIterator;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.spi.indexing.IndexingQueryFilter;

/**
 * Abstract GridLuceneIndex to support Lucene Index as GridH2Index
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 *
 */
public abstract class GridLuceneIndex extends GridH2IndexBase implements AutoCloseable {

    /**
     * Legacy text query
     * 
     * @param qry Query.
     * @param filters Filters over result.
     * @return Query result.
     * @throws IgniteCheckedException If failed.
     */
    public abstract <K, V> GridCloseableIterator<IgniteBiTuple<K, V>> query(String qry, IndexingQueryFilter filters)
        throws IgniteCheckedException;

    /**
     * Update Advanced Lucene index config at runtime
     * 
     * @param tbl the table
     * @param luceneIndexOptions the lucene index configuration
     * @param forceMutateQueryEntity whether force replace lucene index configuration
     */
    public abstract void updateIndexConfig(GridH2Table tbl, String luceneIndexOptions, boolean forceMutateQueryEntity);


}
