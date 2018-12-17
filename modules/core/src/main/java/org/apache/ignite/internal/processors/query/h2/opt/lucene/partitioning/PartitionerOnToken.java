package org.apache.ignite.internal.processors.query.h2.opt.lucene.partitioning;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.lucene.index.Term;
import org.h2.value.Value;
import org.hawkore.ignite.lucene.IndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * PartitionerOnToken
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 *
 */
public class PartitionerOnToken implements Partitioner {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(PartitionerOnToken.class);

    private GridCacheContext<?, ?> ctx;

    private int partitions;

    /**
     * @param partitions
     */
    public PartitionerOnToken(int partitions) {
        if (partitions <= 0) {
            throw new IndexException("The number of partitions should be strictly positive but found " + partitions);
        }
        this.partitions = partitions;
    }

    /**
     * Add Ignite context, required before call getPartitionFor
     * 
     * @param ctx
     * @return this
     */
    public PartitionerOnToken withContext(GridCacheContext<?, ?> ctx) {
        this.ctx = ctx;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public List<IgniteBiTuple<Integer, Optional<Term>>> getCursors(List<Object> keys, Term afterKeyTerm) {
        // optimized search using primary keys
        if (keys != null && !keys.isEmpty()) {

            Set<Integer> parts = new HashSet<>();

            for (Object key : keys) {
                if (key != null) {
                    parts.add(getPartitionFor(key));
                }
            }

            if (!parts.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Optimized Lucene search partitions by key conditions {}. Total index search partitions {} -> optimized to {} search partition(s): {} ",
                        Arrays.toString(keys.toArray()), this.partitions, parts.size(),
                        Arrays.toString(parts.toArray()));
                }
                // returns cursor for a reduced number of partitions
                return parts.stream()
                    .map(p -> new IgniteBiTuple<Integer, Optional<Term>>(p, Optional.ofNullable(afterKeyTerm)))
                    .collect(Collectors.toList());
            }
        }
        // returns cursors for all partitions
        return IntStream.range(0, getPartitions())
            .mapToObj(i -> new IgniteBiTuple<Integer, Optional<Term>>(i, Optional.ofNullable(afterKeyTerm)))
            .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public int getPartitionFor(Object keyValue) {

        assert keyValue != null;

        CacheObject key;

        Object o;
        if (keyValue instanceof Value) {
            o = ((Value) keyValue).getObject();
        } else {
            o = keyValue;
        }
        assert o != null;
        if (o instanceof CacheObject)
            key = (CacheObject) o;
        else
            key = ctx.toCacheKeyObject(o);

        return ctx.affinity().partition(key) % this.partitions;
    }

    /** {@inheritDoc} */
    @Override
    public int getPartitions() {
        return this.partitions;
    }

    /** PartitionerOnToken builder. */
    static class Builder implements Partitioner.IBuilder {

        private final int partitions;

        /**
         * Partitions
         * 
         * @param partitions
         */
        public Builder(@JsonProperty("partitions") int partitions) {
            this.partitions = partitions;
        }

        /** {@inheritDoc} */
        @Override
        public Partitioner build() {
            return new PartitionerOnToken(partitions);
        }
    }

}