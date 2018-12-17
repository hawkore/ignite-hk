package org.apache.ignite.internal.processors.query.schema.operation;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * 
 * Schema indexes rebuild operation 
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 *
 */
public class SchemaIndexesRebuildOperation extends SchemaAbstractOperation {
    /** */
    private static final long serialVersionUID = 0L;

    /** Table name. */
    private final String tblName;
    
    /** Indexes name. */
    private final List<String> idxNames;
    
    /** Do not wait for completion */
    private final boolean async;
    
    /** Index rebuild parallelism level */
    private final int parallel;
    
    /**
     * Constructor.
     *
     * @param opId Operation id.
     * @param cacheName Cache name.
     * @param schemaName Schema name.
     * @param tblName table name.
     * @param idxNames Indexes name.
     * @param ifExists Ignore operation if index doesn't exist.
     * @param parallel Index creation parallelism level.
     */
    public SchemaIndexesRebuildOperation(UUID opId, String cacheName, String schemaName, String tblName, boolean async, String[] idxNames, int parallel) {
        super(opId, cacheName, schemaName);
        this.idxNames = idxNames!=null ? Arrays.asList(idxNames): null;
        this.tblName = tblName;
        this.async = async;
        this.parallel = parallel < 0 ? 0 : parallel;
    }


    /**
     * 
     * @return list of indexe names
     */
    public List<String> indexNames() {
        return idxNames;
    }
 
    /**
     * @return the tblName
     */
    public String tableName() {
        return tblName;
    }

    

    /**
     * @return the async
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Gets indexes rebuild parallelism level.
     *
     * @return Indexes rebuild parallelism level.
     */
    public int parallel() {
        return parallel;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(SchemaIndexesRebuildOperation.class, this, "parent", super.toString());
    }
}
