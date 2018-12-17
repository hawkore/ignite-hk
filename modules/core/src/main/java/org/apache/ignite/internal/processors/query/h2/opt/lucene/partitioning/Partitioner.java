package org.apache.ignite.internal.processors.query.h2.opt.lucene.partitioning;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.lucene.index.Term;
import org.hawkore.ignite.lucene.IndexException;
import org.hawkore.ignite.lucene.common.JsonSerializer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * Index Partitioner
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 *
 */
public interface Partitioner {

    /**
     * @param keys
     *            - key conditions to reduce number of index's partitions to search into.
     * @param afterKeyTerm
     * @return list of partition index and after key term to search into
     */
    List<IgniteBiTuple<Integer, Optional<Term>>> getCursors(List<Object> keys, Term afterKeyTerm);

    /**
     * Calculate partition for param
     * 
     * @param byThis
     * @return partition
     */
    int getPartitionFor(Object byThis);

    //
    /**
     * get number of partitions
     * 
     * @return number of partitions
     */
    int getPartitions();

    /**
     * Returns the Builder represented by the specified JSON string.
     *
     * @param json
     *            a JSON string representing a Partitioner
     * @return the partitioner builder represented by json param
     * @throws IOException
     */
    public static IBuilder builderFromJson(String json) {
        try {
            return JsonSerializer.fromString(json, Partitioner.IBuilder.class);
        } catch (IOException e) {
            throw new IndexException(e, "Unparseable JSON partitioner: {}: {}", e.getMessage(), json);
        }
    }

    /**
     * Returns the Partitioner represented by the specified JSON string.
     *
     * @param metadata
     *            the indexed table metadata
     * @param json
     *            a JSON string representing a Partitioner
     * @return the partitioner represented by json param
     * @throws IOException
     */
    public static Partitioner fromJson(String json) {
        return builderFromJson(json).build();
    }

    /**
     * IBuilder
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     *
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = PartitionerOnNone.Builder.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PartitionerOnNone.Builder.class, name = "none"),
            @JsonSubTypes.Type(value = PartitionerOnToken.Builder.class, name = "token")
    })
    /**
     * IBuilder
     * 
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     */
    interface IBuilder {
        public Partitioner build();
    }
}
