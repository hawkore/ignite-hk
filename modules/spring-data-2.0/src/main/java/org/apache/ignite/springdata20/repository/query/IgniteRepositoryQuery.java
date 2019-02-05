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

package org.apache.ignite.springdata20.repository.query;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteBinary;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.binary.BinaryType;
import org.apache.ignite.cache.CacheEntry;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.cache.query.TextQuery;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.binary.BinaryUtils;
import org.apache.ignite.internal.processors.cache.CacheEntryImpl;
import org.apache.ignite.internal.processors.cache.binary.CacheObjectBinaryProcessorImpl;
import org.apache.ignite.internal.processors.cache.binary.IgniteBinaryImpl;
import org.apache.ignite.internal.processors.cache.query.QueryCursorEx;
import org.apache.ignite.internal.processors.query.GridQueryFieldMetadata;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.springdata20.repository.query.IgniteQuery.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Ignite SQL query implementation.
 */
@SuppressWarnings("unchecked")
public class IgniteRepositoryQuery implements RepositoryQuery {

    private static final TreeMap<String, Class<?>> binaryFieldClass = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /** Defines the way how to process query result */
    private enum ReturnStrategy {
        /** Need to return only one value. */
        ONE_VALUE,

        /** Need to return one cache entry */
        CACHE_ENTRY,

        /** Need to return list of cache entries */
        LIST_OF_CACHE_ENTRIES,

        /** Need to return list of values */
        LIST_OF_VALUES,

        /** Need to return list of lists */
        LIST_OF_LISTS,

        /** Need to return slice */
        SLICE_OF_VALUES,

        /** Slice of cache entries. */
        SLICE_OF_CACHE_ENTRIES,

        /** Slice of lists. */
        SLICE_OF_LISTS,

        /** Need to return Page of values */
        PAGE_OF_VALUES,

        /** Need to return stream of values */
        STREAM_OF_VALUES,
    }

    /** Type. */
    private final Class<?> type;

    /** Sql. */
    private final IgniteQuery qry;

    /** Cache. */
    private final IgniteCache cache;

    /** Ignite instance */
    private final Ignite ignite;

    /** required by sql field query type for binary manipulation */
    private IgniteBinaryImpl binary;
    private BinaryType binType;

    /** Method. */
    private final Method mtd;

    /** Metadata. */
    private final RepositoryMetadata metadata;

    /** Factory. */
    private final ProjectionFactory factory;

    /** Return strategy. */
    private final ReturnStrategy returnStgy;


    /** Collocation flag. */
    private final boolean collocated;

    /** Query timeout in millis. */
    private final int timeout;

    /** */
    private final boolean enforceJoinOrder;

    /** */
    private final boolean distributedJoins;

    /** */
    private final boolean replicatedOnly;

    /** */
    private final boolean lazy;

    /** Partitions for query */
    private final int[] parts;

    /** */
    private final boolean local;

    /**
     * Detect if returned data from method is projected
     */
    private final boolean isProjecting;

    /**
     *
     * Whether provided query is a TextQuery (lucene search)
     */
    private final boolean textQuery;

    /** the return query method */
    private final QueryMethod qMethod;

    /** the return domain class of QueryMethod */
    private final Class<?> returnedDomainClass;


    /**
     * @param metadata Metadata.
     * @param qry Query.
     * @param mtd Method.
     * @param factory Factory.
     * @param cache Cache.
     */
    public IgniteRepositoryQuery(Ignite ignite, RepositoryMetadata metadata, IgniteQuery qry,
        Method mtd, ProjectionFactory factory, IgniteCache cache, org.apache.ignite.springdata20.repository.config.Query queryConfiguration) {
        type = metadata.getDomainType();
        this.qry = qry;
        this.cache = cache;
        this.ignite = ignite;
        this.metadata = metadata;
        this.mtd = mtd;
        this.factory = factory;

        // whether returned type is a projection (is an interface != cache value type)
        this.isProjecting = this.getQueryMethod().getResultProcessor().getReturnedType().isProjecting();

        // load query tunning
        if (queryConfiguration != null){
            this.collocated = queryConfiguration.collocated();
            this.timeout = queryConfiguration.timeout();
            this.enforceJoinOrder = queryConfiguration.enforceJoinOrder();
            this.distributedJoins = queryConfiguration.distributedJoins();
            this.replicatedOnly = queryConfiguration.replicatedOnly();
            this.lazy = queryConfiguration.lazy();
            this.parts = queryConfiguration.parts();
            this.textQuery = queryConfiguration.textQuery();
            this.local = queryConfiguration.local();
        }else{
            // default values
            this.collocated = false;
            this.timeout = 0;
            this.enforceJoinOrder = false;
            this.distributedJoins = false;
            this.replicatedOnly = false;
            this.lazy = false;
            this.parts = null;
            this.textQuery = false;
            this.local = false;
        }

        qMethod = this.getQueryMethod();

        returnedDomainClass = this.getQueryMethod().getReturnedObjectType();

        if (this.qry.isFieldQuery()) {
            if (type.equals(returnedDomainClass)) {
                // ensure domain class is registered on marshaller to operate in binary mode
                registerClassOnMarshaller(((IgniteEx)ignite).context(), type);
                binary = (IgniteBinaryImpl)ignite.binary();
                binType = binary.type(type);
            }
        }

        returnStgy = calcReturnType(mtd, qry.isFieldQuery());
    }

    /** {@inheritDoc} */
    @Override public Object execute(Object[] prmtrs) {
        Query qry = prepareQuery(prmtrs);

        QueryCursor qryCursor = cache.query(qry);

        return transformQueryCursor(prmtrs, qryCursor);
    }

    /** {@inheritDoc} */
    @Override public QueryMethod getQueryMethod() {
        return new QueryMethod(mtd, metadata, factory);
    }

    /**
     * @param mtd Method.
     * @param isFieldQry Is field query.
     * @return Return strategy type.
     */
    private ReturnStrategy calcReturnType(Method mtd, boolean isFieldQry) {
        Class<?> returnType = mtd.getReturnType();

        if (returnType.isAssignableFrom(ArrayList.class)) {
            if (isFieldQry) {
                if (hasAssignableGenericReturnTypeFrom(ArrayList.class, mtd))
                    return ReturnStrategy.LIST_OF_LISTS;
            }
            else if (hasAssignableGenericReturnTypeFrom(Cache.Entry.class, mtd))
                return ReturnStrategy.LIST_OF_CACHE_ENTRIES;

            return ReturnStrategy.LIST_OF_VALUES;
        }
        else if (returnType == Page.class) {
            return ReturnStrategy.PAGE_OF_VALUES;
        }
        else if (returnType == Stream.class) {
            return ReturnStrategy.STREAM_OF_VALUES;
        }
        else if (returnType == Slice.class) {
            if (isFieldQry) {
                if (hasAssignableGenericReturnTypeFrom(ArrayList.class, mtd))
                    return ReturnStrategy.SLICE_OF_LISTS;
            }
            else if (hasAssignableGenericReturnTypeFrom(Cache.Entry.class, mtd))
                return ReturnStrategy.SLICE_OF_CACHE_ENTRIES;

            return ReturnStrategy.SLICE_OF_VALUES;
        }
        else if (Cache.Entry.class.isAssignableFrom(returnType))
            return ReturnStrategy.CACHE_ENTRY;
        else
            return ReturnStrategy.ONE_VALUE;
    }

    /**
     * @param cls Class 1.
     * @param mtd Method.
     * @return if {@code mtd} return type is assignable from {@code cls}
     */
    private boolean hasAssignableGenericReturnTypeFrom(Class<?> cls, Method mtd) {
        Type[] actualTypeArguments = ((ParameterizedType)mtd.getGenericReturnType()).getActualTypeArguments();

        if (actualTypeArguments.length == 0)
            return false;

        if (actualTypeArguments[0] instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType)actualTypeArguments[0];

            Class<?> type1 = (Class)type.getRawType();

            return type1.isAssignableFrom(cls);
        }

        if (actualTypeArguments[0] instanceof Class) {
            Class typeArg = (Class)actualTypeArguments[0];

            return typeArg.isAssignableFrom(cls);
        }

        return false;
    }

    /*
     * when select fields by query H2 returns Timestamp for types java.util.Date
     * and java.sql.Timestamp
     *
     * @see org.apache.ignite.internal.processors.query.h2.H2DatabaseType
     * map.put(Timestamp.class, TIMESTAMP) map.put(java.util.Date.class,
     * TIMESTAMP) map.put(java.sql.Date.class, DATE)
     */
    @SuppressWarnings("unchecked")
    private static <T> T fixExpectedType(final Object object, final Class<T> expected) {
        if (expected != null && object instanceof java.sql.Timestamp && expected.equals(java.util.Date.class)) {
            return (T) new java.util.Date(((java.sql.Timestamp) object).getTime());
        }
        return (T) object;
    }

    /**
     * @param prmtrs Prmtrs.
     * @param qryCursor Query cursor.
     * @return Query cursor or slice
     */
    @Nullable private Object transformQueryCursor(Object[] prmtrs, QueryCursor qryCursor) {

        if (this.qry.isFieldQuery()) {

            final List<GridQueryFieldMetadata> meta = ((QueryCursorEx) qryCursor).fieldsMeta();

            QueryCursorWrapper<?, ?> cWrapper =  new QueryCursorWrapper<>((QueryCursor<List<?>>)qryCursor , row -> {
                if (type.equals(returnedDomainClass)) {
                    // transform sql query fields into cache entry's value (domain entity)
                    return rowToEntity(row, meta);
                } else {
                    return isProjecting
                        ? this.factory.createProjection(returnedDomainClass, rowToMap(row, meta))
                        : rowToMap(row, meta);
                }
            }
            );

            switch (returnStgy) {
                case PAGE_OF_VALUES:
                    return new PageImpl(cWrapper.getAll(), (Pageable) prmtrs[prmtrs.length - 1], 0);
                case LIST_OF_VALUES:
                    return cWrapper.getAll();
                case STREAM_OF_VALUES:
                    return cWrapper.stream();
                case ONE_VALUE:
                    Iterator<?> iter = cWrapper.iterator();
                    if (iter.hasNext()) {
                        Object resp = iter.next();
                        U.closeQuiet(cWrapper);
                        return resp;
                    }
                    return null;
                case SLICE_OF_VALUES:
                    return new SliceImpl(cWrapper.getAll(), (Pageable)prmtrs[prmtrs.length - 1], true);
                case SLICE_OF_LISTS:
                    return new SliceImpl(qryCursor.getAll(), (Pageable)prmtrs[prmtrs.length - 1], true);
                case LIST_OF_LISTS:
                    return qryCursor.getAll();
                default:
                    throw new IllegalStateException();
            }
        }
        else {
            Iterable<CacheEntryImpl> qryIter = (Iterable<CacheEntryImpl>)qryCursor;

            QueryCursorWrapper<?, ?> cWrapper =  new QueryCursorWrapper<>((QueryCursor<CacheEntryImpl>)qryCursor , row ->
                 isProjecting ? this.factory.createProjection(returnedDomainClass, row.getValue()) :  row.getValue()
            );

            switch (returnStgy) {
                case PAGE_OF_VALUES:
                    return new PageImpl(cWrapper.getAll(), (Pageable) prmtrs[prmtrs.length - 1], 0);
                case LIST_OF_VALUES:
                    return cWrapper.getAll();
                case STREAM_OF_VALUES:
                    return cWrapper.stream();
                case ONE_VALUE:
                    Iterator<?> iter1 = cWrapper.iterator();
                    if (iter1.hasNext()) {
                        Object resp = iter1.next();
                        U.closeQuiet(cWrapper);
                        return resp;
                    }
                    return null;
                case CACHE_ENTRY:
                    Iterator<?> iter2 = qryIter.iterator();
                    if (iter2.hasNext()) {
                        Object resp2 = iter2.next();
                        U.closeQuiet(qryCursor);
                        return resp2;
                    }
                    return null;
                case SLICE_OF_VALUES:
                    return new SliceImpl(cWrapper.getAll(), (Pageable)prmtrs[prmtrs.length - 1], true);
                case SLICE_OF_CACHE_ENTRIES:
                    return new SliceImpl(qryCursor.getAll(), (Pageable)prmtrs[prmtrs.length - 1], true);
                case LIST_OF_CACHE_ENTRIES:
                    return qryCursor.getAll();
                default:
                    throw new IllegalStateException();
            }
        }
    }


    /**
     * @param prmtrs Prmtrs.
     * @return prepared query for execution
     */
    @NotNull private Query prepareQuery(Object[] prmtrs) {
        Object[] parameters = prmtrs;
        String sql = qry.sql();

        Query query;

        checkRequiredPageable(prmtrs);

        if (!textQuery) {

            switch (qry.options()) {
                case SORTING:
                    sql = IgniteQueryGenerator.addSorting(new StringBuilder(sql),
                        (Sort)parameters[parameters.length - 1]).toString();
                    parameters = Arrays.copyOfRange(parameters, 0, parameters.length - 1);
                    break;
                case PAGINATION:
                    sql = IgniteQueryGenerator.addPaging(new StringBuilder(sql),
                        (Pageable)parameters[parameters.length - 1]).toString();
                    parameters = Arrays.copyOfRange(parameters, 0, parameters.length - 1);
                    break;
            }

            if (qry.isFieldQuery()) {
                SqlFieldsQuery sqlFieldsQry = new SqlFieldsQuery(sql);
                sqlFieldsQry.setArgs(parameters);

                sqlFieldsQry.setCollocated(collocated);
                sqlFieldsQry.setDistributedJoins(distributedJoins);
                sqlFieldsQry.setEnforceJoinOrder(enforceJoinOrder);
                sqlFieldsQry.setLazy(lazy);
                sqlFieldsQry.setLocal(local);
                if (parts != null && parts.length > 0) {
                    sqlFieldsQry.setPartitions(parts);
                }
                sqlFieldsQry.setReplicatedOnly(replicatedOnly);
                sqlFieldsQry.setTimeout(timeout, TimeUnit.MILLISECONDS);

                query = sqlFieldsQry;
            }
            else {
                SqlQuery sqlQry = new SqlQuery(type, sql);
                sqlQry.setArgs(parameters);

                sqlQry.setDistributedJoins(distributedJoins);
                sqlQry.setLocal(local);
                if (parts != null && parts.length > 0) {
                    sqlQry.setPartitions(parts);
                }
                sqlQry.setReplicatedOnly(replicatedOnly);
                sqlQry.setTimeout(timeout, TimeUnit.MILLISECONDS);

                query = sqlQry;
            }
        } else{

            int pageSize = -1;

            switch (qry.options()) {
                case SORTING:
                    parameters = Arrays.copyOfRange(parameters, 0, parameters.length - 1);
                    break;
                case PAGINATION:
                    pageSize = ((Pageable)parameters[parameters.length - 1]).getPageSize();
                    parameters = Arrays.copyOfRange(parameters, 0, parameters.length - 1);
                    break;
            }

            // just join parameters separated by space
            String textSearch = StringUtils.join(parameters, " ");
            textSearch = textSearch == null ? "": textSearch;

            TextQuery textQuery = new TextQuery(type, StringUtils.join(parameters));

            textQuery.setLocal(local);

            if (pageSize > -1){
                textQuery.setPageSize(pageSize);
            }

            query = textQuery;
        }
        return query;
    }

    private static Map<String, Object> rowToMap(final List<?> row, final List<GridQueryFieldMetadata> meta) {
        // use treemap with case insensitive property name
        final TreeMap<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < meta.size(); i++) {
            // don't want key or val columns
            final String metaField = meta.get(i).fieldName().toLowerCase();
            if (!metaField.equalsIgnoreCase(QueryUtils.KEY_FIELD_NAME)
                    && !metaField.equalsIgnoreCase(QueryUtils.VAL_FIELD_NAME)) {
                map.put(metaField, row.get(i));
            }
        }
        return map;
    }

    /*
     * convert row ( with list of field values) into domain entity
     */
    private  <V> V rowToEntity(final List<?> row, final List<GridQueryFieldMetadata> meta) {
        // additional data returned by query not present on domain object type
        final TreeMap<String, Object> metadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final BinaryObjectBuilder bldr = binary.builder(binType.typeName());

        for (int i = 0; i < row.size(); i++) {
            final GridQueryFieldMetadata fMeta = meta.get(i);
            final String metaField = fMeta.fieldName();
            // add existing entity fields to binary object
            if (binType.field(fMeta.fieldName()) != null && !metaField.equalsIgnoreCase(QueryUtils.KEY_FIELD_NAME)
                    && !metaField.equalsIgnoreCase(QueryUtils.VAL_FIELD_NAME)) {
                final Object fieldValue = row.get(i);
                if (fieldValue != null) {
                    final Class<?> clazz = getClassForBinaryField(fMeta);
                    // null values must not be set into binary objects
                    bldr.setField(metaField, fixExpectedType(fieldValue, clazz));
                }
            } else {
                // don't want key or val column... but wants null values
                if (!metaField.equalsIgnoreCase(QueryUtils.KEY_FIELD_NAME)
                        && !metaField.equalsIgnoreCase(QueryUtils.VAL_FIELD_NAME)) {
                    metadata.put(metaField, row.get(i));
                }
            }
        }
        return bldr.build().deserialize();
    }

    /*
     * Obtains real field class from resultset metadata field whether it's available
     */
    private  Class<?> getClassForBinaryField(final GridQueryFieldMetadata fieldMeta) {
        try {

            final String fieldId = fieldMeta.schemaName() + "." + fieldMeta.typeName() + "." + fieldMeta.fieldName();

            if (binaryFieldClass.containsKey(fieldId)) {
                return binaryFieldClass.get(fieldId);
            }

            Class<?> clazz = null;

            synchronized (binaryFieldClass) {

                if (binaryFieldClass.containsKey(fieldId)) {
                    return binaryFieldClass.get(fieldId);
                }

                String fieldName = null;

                // search field name on binary type (query returns case insensitive
                // field name) but BinaryType is not case insensitive
                for (final String fname : binType.fieldNames()) {
                    if (fname.equalsIgnoreCase(fieldMeta.fieldName())) {
                        fieldName = fname;
                        break;
                    }
                }

                final CacheObjectBinaryProcessorImpl proc = (CacheObjectBinaryProcessorImpl)binary.processor();

                // search for class by typeId, if not found use
                // fieldMeta.fieldTypeName() class
                clazz = BinaryUtils.resolveClass(proc.binaryContext(),
                    binary.typeId(binType.fieldTypeName(fieldName)), fieldMeta.fieldTypeName(),
                    ignite.configuration().getClassLoader(), true);

                binaryFieldClass.put(fieldId, clazz);
            }

            return clazz;

        } catch (final Exception e) {
            return null;
        }
    }

    /* validate operations that requires Pageable parameter */
    private void checkRequiredPageable(Object[] prmtrs){
        try{
            if (
                returnStgy == ReturnStrategy.PAGE_OF_VALUES ||
                    returnStgy == ReturnStrategy.SLICE_OF_VALUES ||
                    returnStgy == ReturnStrategy.SLICE_OF_CACHE_ENTRIES
            ) {
                Pageable page = (Pageable)prmtrs[prmtrs.length - 1];
                page.isPaged();
            }
        }catch (NullPointerException | IndexOutOfBoundsException | ClassCastException e){
            throw new IllegalStateException("For " + returnStgy.name()+ " you must provide on last method parameter a non null Pageable instance");
        }
    }

    private static void registerClassOnMarshaller(final GridKernalContext ctx, final Class<?> clazz) {
        try {// ensure key class registration for marshaller on cluster...
            if (!org.apache.ignite.internal.util.IgniteUtils.isJdk(clazz)) {
                org.apache.ignite.internal.util.IgniteUtils.marshal(ctx, clazz.newInstance());
            }
        } catch (final Exception e) {

        }

    }

    /**
     * Ignite QueryCursor wrapper.
     *
     * <p>
     *
     * Ensures closing underline cursor when there is no data.
     *
     * @param <T> input type
     * @param <V> transformed output type
     */
    public static class QueryCursorWrapper<T, V> extends AbstractCollection<V> implements QueryCursor<V> {

        private final QueryCursor<T> delegate;
        private final Function<T, V> transformer;

        /**
         *
         * @param delegate
         *            delegate QueryCursor with T input elements
         * @param transformer
         *            Function to transform T to V elements
         */
        public QueryCursorWrapper(final QueryCursor<T> delegate, final Function<T, V> transformer) {
            this.delegate = delegate;
            this.transformer = transformer;
        }

        /** {@inheritDoc} */
        @Override
        public Iterator<V> iterator() {

            final Iterator<T> it = this.delegate.iterator();

            return new Iterator<V>() {

                @Override
                public boolean hasNext() {
                    if (!it.hasNext()) {
                        U.closeQuiet(QueryCursorWrapper.this.delegate);
                        return false;
                    }
                    return true;
                }

                @Override
                public V next() {
                    final V r = QueryCursorWrapper.this.transformer.apply(it.next());
                    if (r != null) {
                        return r;
                    }
                    throw new NoSuchElementException();
                }
            };
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            this.delegate.close();
        }

        /** {@inheritDoc} */
        @Override
        public List<V> getAll() {
            final List<V> data = new ArrayList<>();
            this.delegate.forEach(i -> data.add(this.transformer.apply(i)));
            U.closeQuiet(this.delegate);
            return data;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            // when use toArray method, internal parent implementation
            // will grow up it until iterator has not more elements
            return 0;
        }

    }
}
