package org.apache.ignite.internal.processors.query.h2.opt;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.lang.GridCloseableIterator;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.spi.indexing.IndexingQueryFilter;

/**
 * Abstract GridLuceneIndex to support Lucene Index as GridH2Index
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 */
public abstract class GridLuceneIndex extends GridH2IndexBase implements AutoCloseable {

    /**
     * Constructor.
     *
     * @param tbl
     *     Table.
     */
    protected GridLuceneIndex(GridH2Table tbl) {
        super(tbl);
    }

    /**
     * Legacy text query
     *
     * @param qry
     *     Query.
     * @param filters
     *     Filters over result.
     * @return Query result.
     * @throws IgniteCheckedException
     *     If failed.
     */
    public abstract <K, V> GridCloseableIterator<IgniteBiTuple<K, V>> query(String qry,
        IndexingQueryFilter filters,
        int limit) throws IgniteCheckedException;

    /**
     * Update Advanced Lucene index config at runtime
     *
     * @param tbl
     *     the table
     * @param luceneIndexOptions
     *     the lucene index configuration
     * @param forceMutateQueryEntity
     *     whether force replace lucene index configuration
     */
    public abstract void updateIndexConfig(GridH2Table tbl, String luceneIndexOptions, boolean forceMutateQueryEntity);

    /**
     * Stores given data in this fulltext index.
     *
     * @param k
     *     Key.
     * @param v
     *     Value.
     * @param ver
     *     Version.
     * @param expires
     *     Expiration time.
     * @throws IgniteCheckedException
     *     If failed.
     */
    public void store(CacheObject k, CacheObject v, GridCacheVersion ver, long expires) throws IgniteCheckedException {
        // nop - legacy support
    }

    /**
     * Removes entry for given key from this index.
     *
     * @param key
     *     Key.
     * @throws IgniteCheckedException
     *     If failed.
     */
    public void remove(CacheObject key) throws IgniteCheckedException {
        // nop - legacy support
    }

}
