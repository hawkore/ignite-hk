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

package org.apache.ignite.internal.processors.query.h2.sql;

import org.h2.expression.Function;

/**
 * Full list of available functions see at {@link Function}
 *
 * HK-PATCHED: support more aggregate functions
 */
public enum GridSqlFunctionType {
    // Aggregate functions.
    /** */
    COUNT_ALL("COUNT(*)"),

    /** */
    COUNT,

    /** */
    SUM,

    /** */
    MIN,

    /** */
    MAX,

    /** */
    AVG,

    /** */
    GROUP_CONCAT,

    // Functions with special handling.
    /** */
    CASE,

    /** */
    CAST,

    /** */
    CONVERT,

    /** */
    EXTRACT,

    /** */
    SYSTEM_RANGE,

    /** TABLE and TABLE_DISTINCT */
    TABLE,

    /** Constant for all other aggregate functions. */
    UNKNOWN_AGG_FUNCTION,

    /** Constant for all other functions. */
    UNKNOWN_FUNCTION;

    /** */
    private final String name;

    /**
     */
    GridSqlFunctionType() {
        name = name();
    }

    /**
     * @param name Name.
     */
    GridSqlFunctionType(String name) {
        this.name = name;
    }

    /**
     * @return Function name.
     */
    public String functionName() {
        return name;
    }
}
