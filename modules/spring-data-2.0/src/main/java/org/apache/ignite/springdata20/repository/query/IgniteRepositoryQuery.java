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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.cache.Cache;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.binary.BinaryType;
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
import org.apache.ignite.springdata20.repository.query.StringQuery.ParameterBinding;
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
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Ignite query implementation.
 * <p>
 * <p>
 * Features:
 * <ol>
 * <li> Supports query tuning parameters</li>
 * <li> Supports projections</li>
 * <li> Supports Page and Stream responses</li>
 * <li> Supports SqlFieldsQuery resultset transformation into the domain entity</li>
 * <li> Supports named parameters (:myParam) into SQL queries, declared using @Param("myParam")</li>
 * <li> Supports advanced parameter binding and SpEL expressions into SQL queries
 * <ol>
 * <li><b>Template variables</b>:
 * <ol>
 * <li>{@code #entityName} - the simple class name of the domain entity</li>
 * </ol>
 * </li>
 * <li><b>Method parameter expressions</b>: Parameters are exposed for indexed access ([0] is the first query method's
 * param) or via the name declared using @Param. The actual SpEL expression binding is triggered by '?#'. Example:
 * ?#{[0]} or ?#{#myParamName}</li>
 * <li><b>Advanced SpEL expressions</b>: While advanced parameter binding is a very useful feature, the real power of
 * SpEL stems from the fact, that the expressions can refer to framework abstractions or other application components
 * through SpEL EvaluationContext extension model.</li>
 * </ol>
 * Examples:
 * <pre>
 * {@code @Query}(value = "SELECT * from #{#entityName} where email = :email")
 * User searchUserByEmail({@code @Param}("email") String email);
 *
 * {@code @Query}(value = "SELECT * from #{#entityName} where country = ?#{[0] and city = ?#{[1]}")
 * List<User> searchUsersByCity({@code @Param}("country") String country, {@code @Param}("city") String city,
 * Pageable pageable);
 *
 * {@code @Query}(value = "SELECT * from #{#entityName} where email = ?")
 * User searchUserByEmail(String email);
 *
 * {@code @Query}(value = "SELECT * from #{#entityName} where lucene = ?#{
 * luceneQueryBuilder.search().refresh(true).filter(luceneQueryBuilder.match('city',#city)).build()}")
 * List<User> searchUsersByCity({@code @Param}("city") String city, Pageable pageable);
 * </pre>
 * </li>
 * <li> Supports SpEL expressions into Text queries ({@link TextQuery}). Examples:
 * <pre>
 * {@code @Query}(textQuery = true, value = "email: #{#email}")
 * User searchUserByEmail({@code @Param}("email") String email);
 *
 * {@code @Query}(textQuery = true, value = "#{#textToSearch}")
 * List<User> searchUsersByText({@code @Param}("textToSearch") String text, Pageable pageable);
 *
 * {@code @Query}(textQuery = true, value = "#{[0]}")
 * List<User> searchUsersByText(String textToSearch, Pageable pageable);
 *
 * {@code @Query}(textQuery = true, value = "#{luceneQueryBuilder.search().refresh(true).filter(luceneQueryBuilder
 * .match('city', #city)).build()}")
 * List<User> searchUserByCity({@code @Param}("city") String city, Pageable pageable);
 * </pre>
 * </li>
 * </ol>
 * <p>
 * Visit <a href="https://docs.hawkore.com/private/apache-ignite-advanced-indexing">Apache Ignite advanced Indexing
 * Documentation site</a> for more info about Advanced Lucene Index and Lucene Query Builder.
 *
 * @author Apache Ignite Team
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
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
        STREAM_OF_VALUES,}

    /** Type. */
    private final Class<?> type;
    /** Sql. */
    private final IgniteQuery qry;
    /** Cache. */
    private final IgniteCache cache;
    /** Ignite instance */
    private final Ignite ignite;
    /** required by qryStr field query type for binary manipulation */
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
    /**
     *
     */
    private final boolean enforceJoinOrder;
    /**
     *
     */
    private final boolean distributedJoins;
    /**
     *
     */
    private final boolean replicatedOnly;
    /**
     *
     */
    private final boolean lazy;
    /** Partitions for query */
    private final int[] parts;
    /**
     *
     */
    private final boolean local;
    /**
     * Detect if returned data from method is projected
     */
    private final boolean hasProjection;
    private final boolean hasDynamicProjection;
    private final int dynamicProjectionIndex;
    /** the return query method */
    private final QueryMethod qMethod;
    /** the return domain class of QueryMethod */
    private final Class<?> returnedDomainClass;
    private final SpelExpressionParser expressionParser;
    /** could provide ExtensionAwareQueryMethodEvaluationContextProvider */
    private final QueryMethodEvaluationContextProvider queryMethodEvaluationContextProvider;

    /**
     * @param metadata
     *     Metadata.
     * @param qry
     *     Query.
     * @param mtd
     *     Method.
     * @param factory
     *     Factory.
     * @param cache
     *     Cache.
     */
    public IgniteRepositoryQuery(Ignite ignite,
        RepositoryMetadata metadata,
        IgniteQuery qry,
        Method mtd,
        ProjectionFactory factory,
        IgniteCache cache,
        org.apache.ignite.springdata20.repository.config.Query queryConfiguration,
        QueryMethodEvaluationContextProvider queryMethodEvaluationContextProvider) {
        type = metadata.getDomainType();
        this.qry = qry;
        this.cache = cache;
        this.ignite = ignite;
        this.metadata = metadata;
        this.mtd = mtd;
        this.factory = factory;
        this.expressionParser = new SpelExpressionParser();
        this.queryMethodEvaluationContextProvider = queryMethodEvaluationContextProvider;

        // load query tunning
        if (queryConfiguration != null) {
            this.collocated = queryConfiguration.collocated();
            this.timeout = queryConfiguration.timeout();
            this.enforceJoinOrder = queryConfiguration.enforceJoinOrder();
            this.distributedJoins = queryConfiguration.distributedJoins();
            this.replicatedOnly = queryConfiguration.replicatedOnly();
            this.lazy = queryConfiguration.lazy();
            this.parts = queryConfiguration.parts();
            this.local = queryConfiguration.local();
        } else {
            // default values
            this.collocated = false;
            this.timeout = 0;
            this.enforceJoinOrder = false;
            this.distributedJoins = false;
            this.replicatedOnly = false;
            this.lazy = false;
            this.parts = null;
            this.local = false;
        }

        qMethod = this.getQueryMethod();

        // control projection
        this.hasDynamicProjection = this.getQueryMethod().getParameters().hasDynamicProjection();
        this.hasProjection = hasDynamicProjection || this.getQueryMethod().getResultProcessor().getReturnedType()
                                                         .isProjecting();

        this.dynamicProjectionIndex = this.qMethod.getParameters().getDynamicProjectionIndex();

        returnedDomainClass = this.getQueryMethod().getReturnedObjectType();

        if (this.qry.isFieldQuery()) {
            // ensure domain class is registered on marshaller to transform row to entity
            registerClassOnMarshaller(((IgniteEx)ignite).context(), type);
            binary = (IgniteBinaryImpl)ignite.binary();
            binType = binary.type(type);
        }

        returnStgy = calcReturnType(mtd, qry.isFieldQuery());
    }

    /** {@inheritDoc} */
    @Override
    public Object execute(Object[] values) {

        Query qry = prepareQuery(values);

        QueryCursor qryCursor = cache.query(qry);

        return transformQueryCursor(values, qryCursor);
    }

    /** {@inheritDoc} */
    @Override
    public QueryMethod getQueryMethod() {
        return new QueryMethod(mtd, metadata, factory);
    }

    /**
     * @param mtd
     *     Method.
     * @param isFieldQry
     *     Is field query.
     * @return Return strategy type.
     */
    private ReturnStrategy calcReturnType(Method mtd, boolean isFieldQry) {
        Class<?> returnType = mtd.getReturnType();

        if (returnType.isAssignableFrom(ArrayList.class)) {
            if (isFieldQry) {
                if (hasAssignableGenericReturnTypeFrom(ArrayList.class, mtd)) {
                    return ReturnStrategy.LIST_OF_LISTS;
                }
            } else if (hasAssignableGenericReturnTypeFrom(Cache.Entry.class, mtd)) {
                return ReturnStrategy.LIST_OF_CACHE_ENTRIES;
            }

            return ReturnStrategy.LIST_OF_VALUES;
        } else if (returnType == Page.class) {
            return ReturnStrategy.PAGE_OF_VALUES;
        } else if (returnType == Stream.class) {
            return ReturnStrategy.STREAM_OF_VALUES;
        } else if (returnType == Slice.class) {
            if (isFieldQry) {
                if (hasAssignableGenericReturnTypeFrom(ArrayList.class, mtd)) {
                    return ReturnStrategy.SLICE_OF_LISTS;
                }
            } else if (hasAssignableGenericReturnTypeFrom(Cache.Entry.class, mtd)) {
                return ReturnStrategy.SLICE_OF_CACHE_ENTRIES;
            }

            return ReturnStrategy.SLICE_OF_VALUES;
        } else if (Cache.Entry.class.isAssignableFrom(returnType)) {
            return ReturnStrategy.CACHE_ENTRY;
        } else {
            return ReturnStrategy.ONE_VALUE;
        }
    }

    /**
     * @param cls
     *     Class 1.
     * @param mtd
     *     Method.
     * @return if {@code mtd} return type is assignable from {@code cls}
     */
    private boolean hasAssignableGenericReturnTypeFrom(Class<?> cls, Method mtd) {

        Type genericReturnType = mtd.getGenericReturnType();

        if (!(genericReturnType instanceof ParameterizedType)) {
            return false;
        }

        Type[] actualTypeArguments = ((ParameterizedType)genericReturnType).getActualTypeArguments();

        if (actualTypeArguments.length == 0) {
            return false;
        }

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
     * and java.qryStr.Timestamp
     *
     * @see org.apache.ignite.internal.processors.query.h2.H2DatabaseType
     * map.put(Timestamp.class, TIMESTAMP) map.put(java.util.Date.class,
     * TIMESTAMP) map.put(java.qryStr.Date.class, DATE)
     */
    @SuppressWarnings("unchecked")
    private static <T> T fixExpectedType(final Object object, final Class<T> expected) {
        if (expected != null && object instanceof java.sql.Timestamp && expected.equals(java.util.Date.class)) {
            return (T)new java.util.Date(((java.sql.Timestamp)object).getTime());
        }
        return (T)object;
    }

    /**
     * @param prmtrs
     *     Prmtrs.
     * @param qryCursor
     *     Query cursor.
     * @return Query cursor or slice
     */
    @Nullable
    private Object transformQueryCursor(Object[] prmtrs, QueryCursor qryCursor) {

        final Class<?> returnClass;

        if (hasProjection) {
            if (hasDynamicProjection) {
                returnClass = (Class<?>)prmtrs[dynamicProjectionIndex];
            } else {
                returnClass = returnedDomainClass;
            }
        } else {
            returnClass = returnedDomainClass;
        }

        if (this.qry.isFieldQuery()) {

            final List<GridQueryFieldMetadata> meta = ((QueryCursorEx)qryCursor).fieldsMeta();

            QueryCursorWrapper<?, ?> cWrapper = new QueryCursorWrapper<>((QueryCursor<List<?>>)qryCursor, row -> {
                if (type.equals(returnClass)) {
                    // transform qryStr query fields into cache entry's value (domain entity)
                    return rowToEntity(row, meta);
                } else {
                    if (hasProjection) {
                        return this.factory.createProjection(returnClass, rowToMap(row, meta));
                    }
                    return rowToMap(row, meta);
                }
            });

            switch (returnStgy) {
                case PAGE_OF_VALUES:
                    return new PageImpl(cWrapper.getAll(), (Pageable)prmtrs[prmtrs.length - 1], 0);
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
        } else {
            Iterable<CacheEntryImpl> qryIter = (Iterable<CacheEntryImpl>)qryCursor;

            QueryCursorWrapper<?, ?> cWrapper = new QueryCursorWrapper<>((QueryCursor<CacheEntryImpl>)qryCursor,
                row -> {
                    if (hasProjection) {
                        return this.factory.createProjection(returnClass, row.getValue());
                    }
                    return row.getValue();
                });

            switch (returnStgy) {
                case PAGE_OF_VALUES:
                    return new PageImpl(cWrapper.getAll(), (Pageable)prmtrs[prmtrs.length - 1], 0);
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
     * Extract bindable values
     *
     * @param values
     *     values invoking query method
     * @param queryMethodParams
     *     query method parameter definitions
     * @param queryBindings
     *     All parameters found on query string that need to be binded
     * @return new list of parameters
     */
    private Object[] extractBindableValues(Object[] values,
        Parameters<?, ?> queryMethodParams,
        List<ParameterBinding> queryBindings) {

        // no binding params then exit
        if (queryBindings.isEmpty()) {
            return values;
        }

        Object[] newValues = new Object[queryBindings.size()];

        // map bindable parameters from query method: (index/name) - index
        HashMap<String, Integer> methodParams = new HashMap<>();

        // create an evaluation context for custom query
        EvaluationContext queryEvalContext = queryMethodEvaluationContextProvider
                                                 .getEvaluationContext(queryMethodParams, values);

        // By default queryEvalContext:
        // - make accesible query method parameters by index:
        // @Query("select u from User u where u.age = ?#{[0]}")
        // List<User> findUsersByAge(int age);
        // - make accesible query method parameters by name:
        // @Query("select u from User u where u.firstname = ?#{#customer.firstname}")
        // List<User> findUsersByCustomersFirstname(@Param("customer") Customer customer);

        // query method param's index by name and position
        queryMethodParams.getBindableParameters().forEach(p -> {
            if (p.getName().isPresent()) {
                // map by name (annotated by @Param)
                methodParams.put(p.getName().get(), p.getIndex());
            }
            // map by position
            methodParams.put(String.valueOf(p.getIndex()), p.getIndex());
        });

        // process all parameters on query and extract new values to bind
        for (int i = 0; i < queryBindings.size(); i++) {
            ParameterBinding p = queryBindings.get(i);

            if (p.isExpression()) {
                // Evaluate SpEl expressions (synthetic parameter value) , example ?#{#customer.firstname}
                newValues[i] = this.expressionParser.parseExpression(p.getExpression()).getValue(queryEvalContext);
            } else {
                // Extract parameter value by name or position respectively from invoking values
                newValues[i] = values[methodParams.get(
                    p.getName() != null ? p.getName() : String.valueOf(p.getRequiredPosition() - 1))];
            }
        }

        return newValues;
    }

    /**
     * @param prmtrs
     *     Prmtrs.
     * @return prepared query for execution
     */
    @NotNull
    private Query prepareQuery(Object[] values) {

        Object[] parameters = values;

        String queryString = qry.qryStr();

        Query query;

        checkRequiredPageable(values);

        if (!qry.isTextQuery()) {

            if (!qry.isAutogenerated()) {
                StringQuery squery = new ExpressionBasedStringQuery(queryString, metadata, expressionParser);
                queryString = squery.getQueryString();
                parameters = extractBindableValues(parameters, this.getQueryMethod().getParameters(),
                    squery.getParameterBindings());
            }

            switch (qry.options()) {
                case SORTING:
                    queryString = IgniteQueryGenerator
                                      .addSorting(new StringBuilder(queryString), (Sort)values[values.length - 1])
                                      .toString();
                    if (qry.isAutogenerated()) {
                        parameters = Arrays.copyOfRange(parameters, 0, values.length - 1);
                    }
                    break;
                case PAGINATION:
                    queryString = IgniteQueryGenerator
                                      .addPaging(new StringBuilder(queryString), (Pageable)values[values.length - 1])
                                      .toString();
                    if (qry.isAutogenerated()) {
                        parameters = Arrays.copyOfRange(parameters, 0, values.length - 1);
                    }
                    break;
            }

            if (qry.isFieldQuery()) {
                SqlFieldsQuery sqlFieldsQry = new SqlFieldsQuery(queryString);
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
            } else {
                SqlQuery sqlQry = new SqlQuery(type, queryString);
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
        } else {

            int pageSize = -1;

            switch (qry.options()) {
                case PAGINATION:
                    pageSize = ((Pageable)parameters[parameters.length - 1]).getPageSize();
                    break;
            }

            // check if queryString contains SpEL template expressions and evaluate them if any
            if (queryString.contains("#{")) {
                EvaluationContext queryEvalContext = queryMethodEvaluationContextProvider
                                                         .getEvaluationContext(this.getQueryMethod().getParameters(),
                                                             values);

                Object eval = this.expressionParser.parseExpression(queryString, ParserContext.TEMPLATE_EXPRESSION)
                                  .getValue(queryEvalContext);

                if (!(eval instanceof String)) {
                    throw new IllegalStateException(
                        "TextQuery with SpEL expressions must produce a String response, but found " + eval.getClass()
                                                                                                           .getName()
                            + ". Please, check your expression: " + queryString);
                }
                queryString = (String)eval;
            }

            TextQuery textQuery = new TextQuery(type, queryString);

            textQuery.setLocal(local);

            if (pageSize > -1) {
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
            if (!metaField.equalsIgnoreCase(QueryUtils.KEY_FIELD_NAME) && !metaField.equalsIgnoreCase(
                QueryUtils.VAL_FIELD_NAME)) {
                map.put(metaField, row.get(i));
            }
        }
        return map;
    }

    /*
     * convert row ( with list of field values) into domain entity
     */
    private <V> V rowToEntity(final List<?> row, final List<GridQueryFieldMetadata> meta) {
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
                if (!metaField.equalsIgnoreCase(QueryUtils.KEY_FIELD_NAME) && !metaField.equalsIgnoreCase(
                    QueryUtils.VAL_FIELD_NAME)) {
                    metadata.put(metaField, row.get(i));
                }
            }
        }
        return bldr.build().deserialize();
    }

    /*
     * Obtains real field class from resultset metadata field whether it's available
     */
    private Class<?> getClassForBinaryField(final GridQueryFieldMetadata fieldMeta) {
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
                clazz = BinaryUtils.resolveClass(proc.binaryContext(), binary.typeId(binType.fieldTypeName(fieldName)),
                    fieldMeta.fieldTypeName(), ignite.configuration().getClassLoader(), true);

                binaryFieldClass.put(fieldId, clazz);
            }

            return clazz;
        } catch (final Exception e) {
            return null;
        }
    }

    /* validate operations that requires Pageable parameter */
    private void checkRequiredPageable(Object[] prmtrs) {
        try {
            if (returnStgy == ReturnStrategy.PAGE_OF_VALUES || returnStgy == ReturnStrategy.SLICE_OF_VALUES
                    || returnStgy == ReturnStrategy.SLICE_OF_CACHE_ENTRIES) {
                Pageable page = (Pageable)prmtrs[prmtrs.length - 1];
                page.isPaged();
            }
        } catch (NullPointerException | IndexOutOfBoundsException | ClassCastException e) {
            throw new IllegalStateException(
                "For " + returnStgy.name() + " you must provide on last method parameter a non null Pageable instance");
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
     * <p>
     * Ensures closing underline cursor when there is no data.
     *
     * @param <T>
     *     input type
     * @param <V>
     *     transformed output type
     */
    public static class QueryCursorWrapper<T, V> extends AbstractCollection<V> implements QueryCursor<V> {

        private final QueryCursor<T> delegate;
        private final Function<T, V> transformer;

        /**
         * @param delegate
         *     delegate QueryCursor with T input elements
         * @param transformer
         *     Function to transform T to V elements
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
