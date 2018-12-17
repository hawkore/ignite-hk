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

package org.apache.ignite.internal.processors.query.schema.operation;

import java.util.UUID;

import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Update/Register QueryEntity definition schema
 */
public class SchemaRegisterQueryEntityOperation extends SchemaAbstractOperation {
    /** */
    private static final long serialVersionUID = 0L;

    /** Target table name. */
    private final String tblName;
    
    /** Force rebuild indexes */
    private final boolean forceRebuildIndexes; 
    
    /** Force mutate QueryEntity, force replace definition */
    private final boolean forceMutateQueryEntity;
    
    /** Do not wait for completion */
    private final boolean async;
    
    /** Indexes creation/rebuild parallelism level */
    private final int parallel;
    
    /** Query entity to register or update */
    private final QueryEntity queryEntity;

    /**
     * Constructor.
     * 
     * @param opId
     * @param cacheName
     * @param schemaName
     * @param tblName
     * @param queryEntity
     * @param forceRebuildIndexes
     * @param forceMutateQueryEntity
     * @param async
     * @param parallel
     */
    public SchemaRegisterQueryEntityOperation(UUID opId,  String cacheName, String schemaName, String tblName, QueryEntity queryEntity, boolean forceRebuildIndexes, boolean forceMutateQueryEntity, boolean async, int parallel) {
        super(opId, cacheName, schemaName);
        this.tblName=tblName;
        this.queryEntity = queryEntity;
        this.forceRebuildIndexes = forceRebuildIndexes;
        this.forceMutateQueryEntity = forceMutateQueryEntity;
        this.async = async;
        this.parallel = parallel < 0 ? 0 : parallel;
    }

    /**
     * @return Index params.
     */
    public  QueryEntity queryEntity() {
        return queryEntity;
    }

    

    /**
     * @return the forceRebuildIndexes
     */
    public boolean isForceRebuildIndexes() {
        return forceRebuildIndexes;
    }

    /**
     * @return Target table name.
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
     * @return the forceMutateQueryEntity
     */
    public boolean isForceMutateQueryEntity() {
        return forceMutateQueryEntity;
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
        return S.toString(SchemaRegisterQueryEntityOperation.class, this, "parent", super.toString());
    }
}
