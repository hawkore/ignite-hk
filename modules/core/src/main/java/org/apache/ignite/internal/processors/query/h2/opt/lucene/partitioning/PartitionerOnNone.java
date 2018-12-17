package org.apache.ignite.internal.processors.query.h2.opt.lucene.partitioning;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.lucene.index.Term;

/**
 * 
 * One partition per index
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 *
 */
public class PartitionerOnNone implements Partitioner {

    /** */
    private final int DEFAULT_NUMBER_OF_PARTITIONS = 1;

    /** */
    private final int DEFAULT_PARTITION_IDX = 0;

    /** {@inheritDoc} */
    @Override
    public List<IgniteBiTuple<Integer, Optional<Term>>> getCursors(List<Object> keys, Term afterKeyTerm) {
        return Arrays.asList(
            new IgniteBiTuple<Integer, Optional<Term>>(DEFAULT_PARTITION_IDX, Optional.ofNullable(afterKeyTerm)));
    }

    /** {@inheritDoc} */
    @Override
    public int getPartitionFor(Object byThis) {
        return DEFAULT_PARTITION_IDX;
    }

    /** {@inheritDoc} */
    @Override
    public int getPartitions() {
        return DEFAULT_NUMBER_OF_PARTITIONS;
    }

    /** PartitionerOnNone builder. */
    static class Builder implements Partitioner.IBuilder {

        /**
         * 
         */
        public Builder() {
            // default constructor
        }
        
        /** {@inheritDoc} */
        @Override
        public Partitioner build() {
            return new PartitionerOnNone();
        }

    }

}