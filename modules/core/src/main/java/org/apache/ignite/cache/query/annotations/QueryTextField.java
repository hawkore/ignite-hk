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

package org.apache.ignite.cache.query.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.internal.processors.cache.query.CacheQuery;

/**
 * Annotation for fields to be indexed for full text search using Lucene. For
 * more information refer to {@link CacheQuery} documentation.
 * 
 * <p>
 * Extended to support QueryEntity definition by annotation configuration 
 * for Advanced Lucence Index.
 * <p>
 * 
 * <b>For more info and full documentation visit:</b>
 * 
 * <a href="https://hawkore.com">HAWKORE, S.L. web site</a>
 * 
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 * 
 * @see CacheQuery
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
@Inherited
public @interface QueryTextField {

    /** Default date pattern */
    static final String DEFAULT_DATE_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS Z";

    /**
     * Specifies whether the specified field is {@code hidden}.
     *
     * @return {@code True} if the field is hidden.
     */
    boolean hidden() default false;

    /**
     * Index user-specified configuration options
     */
    IndexOptions[] indexOptions() default {};

    /** The BigDecimal mappers to apply */
    BigDecimalMapper[] bigDecimalMappers() default {};

    /** The BigInteger mappers to apply */
    BigIntegerMapper[] bigIntegerMappers() default {};

    /** The Bitemporal mappers to apply */
    BitemporalMapper[] bitemporalMappers() default {};

    /** The Blob mappers to apply */
    BlobMapper[] blobMappers() default {};

    /** The Boolean mappers to apply */
    BooleanMapper[] booleanMappers() default {};

    /** The Date mappers to apply */
    DateMapper[] dateMappers() default {};

    /** The DateRage mappers to apply */
    DateRangeMapper[] dateRangeMappers() default {};

    /** The Double mappers to apply */
    DoubleMapper[] doubleMappers() default {};

    /** The Float mappers to apply */
    FloatMapper[] floatMappers() default {};

    /** The GeoPoint mappers to apply */
    GeoPointMapper[] geoPointMappers() default {};

    /** The GeoShape mappers to apply */
    GeoShapeMapper[] geoShapeMappers() default {};

    /** The Inet mappers to apply */
    InetMapper[] inetMappers() default {};

    /** The Integer mappers to apply */
    IntegerMapper[] integerMappers() default {};

    /** The Long mappers to apply */
    LongMapper[] longMappers() default {};

    /** The String mappers to apply */
    StringMapper[] stringMappers() default {};

    /** The Text mappers to apply */
    TextMapper[] textMappers() default {};

    /** The UUID mappers to apply */
    UUIDMapper[] uuidMappers() default {};

    /**
     * 
     * mapper's type
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    public enum MapperType {

        /** bytes type mapper */
        BYTES("bytes"),

        /** bigint type mapper */
        BIGINTEGER("bigint"),

        /** boolean type mapper */
        BOOLEAN("boolean"),

        /** date type mapper */
        DATE("date"),

        /** double type mapper */
        DOUBLE("double"),

        /** float type mapper */
        FLOAT("float"),

        /** inet type mapper */
        INET("inet"),

        /** integer type mapper */
        INTEGER("integer"),

        /** text type mapper */
        TEXT("text"),

        /** string type mapper */
        STRING("string"),

        /** uuid type mapper */
        UUID("uuid"),

        /** long type mapper */
        LONG("long"),

        /** bigdec type mapper */
        BIGDECIMAL("bigdec"),

        /** date_range type mapper */
        DATE_RANGE("date_range"),

        /** bitemporal type mapper */
        BITEMPORAL("bitemporal"),

        /** geo_point type mapper */
        GEO_POINT("geo_point"),

        /** geo_shape type mapper */
        GEO_SHAPE("geo_shape");

        String type;

        private MapperType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * 
     * GeoTransformation Type
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     *
     */
    public enum GeoTransformationType {

        /** bbox transformation type */
        BBOX("bbox"),

        /** buffer transformation type */
        BUFFER("buffer"),

        /** centroid transformation type */
        CENTROID("centroid"),

        /** convex hull transformation type */
        CONVEX_HULL("convex_hull");

        String type;

        private GeoTransformationType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Custom analyzer using a Lucene's {@code Analyzer}s in classpath.
     *
     * It's uses the {@code Analyzer}'s default (no args) constructor.
     * 
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     * 
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public static @interface ClasspathAnalyzer {

        /** "classpath" text analyzer type */
        static final String TYPE = "classpath";

        /**
         * the name of this custom text analyzer to be referenced on
         * {@link TextMapper#analyzer()}
         */
        String name();

        /** The {@code Analyzer} full qualified class name. */
        String className();
    }

    /**
     * 
     * Custom analyzer for tartarus.org snowball {@code Analyzer}.
     *
     * <p>
     * The supported languages are English, French, Spanish, Portuguese,
     * Italian, Romanian, German, Dutch, Swedish, Norwegian, Danish, Russian,
     * Finnish, Irish, Hungarian, Turkish, Armenian, Basque and Catalan.
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     * 
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public static @interface SnowballAnalyzer {

        /** "snowball" text analyzer type */
        static final String TYPE = "snowball";

        /**
         * the name of this custom text analyzer to be referenced on
         * {@link TextMapper#analyzer()}
         */
        String name();

        /**
         * The language.
         * <p>
         * The supported values are English, French, Spanish, Portuguese,
         * Italian, Romanian, German, Dutch, Swedish, Norwegian, Danish,
         * Russian, Finnish, Hungarian and Turkish.
         */
        String language();

        /** The comma-separated stopwords list. */
        String stopwords() default "";
    }

    /**
     * 
     * Index user-specified configuration options
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public static @interface IndexOptions {

        /** Default index options version */
        public static final int DEFAULT_INDEX_OPTIONS_VERSION = 0;

        /** Default text analyzer */
        public static final String DEFAULT_TEXT_ANALYZER = "standard";

        /** Default directory path */
        public static final String DEFAULT_DIRECTORY_PATH = "";

        /** Default max cached MB */
        public static final int DEFAULT_MAX_CACHED_MB = -1;

        /** Default ram buffer MB */
        public static final int DEFAULT_RAM_BUFFER_MB = 5;

        /** Default refresh seconds */
        public static final int DEFAULT_REFRESH_SECONDS = 60;

        /** Default partitions */
        public static final int DEFAULT_PARTITIONS = 1;

        /** Default optimizer enabled */
        public static final boolean DEFAULT_OPTIMIZER_ENABLED = true;

        /** Default optimizer CRON scheduler. Run every day at 1:00 AM */
        public static final String DEFAULT_OPTIMIZER_SCHEDULE = "0 1 * * *";

        /**
         * Index options version.
         * <p>
         * It's a sequential number that avoid update index options when using
         * dynamic sql entities functionality. Update index options will be
         * allowed if new <code>version</code> value is equals or greater than
         * current index option's <code>version</code> value. Default 0.
         * <p>
         * Must be a strictly non-negative integer.
         */
        int version() default DEFAULT_INDEX_OPTIONS_VERSION;

        /**
         * Split the index in multiple partitions. Default 1 partition
         * <p>
         * Index partitioning is useful to speed up some searches to the
         * detriment of others, depending on the implementation.
         * <p>
         * It is also useful to overcome the Lucene's hard limit of 2147483519
         * documents per local index.
         * <p>
         * Once index is created you can't change this value, unless you drop
         * the index and create it again
         */
        int partitions() default DEFAULT_PARTITIONS;

        /**
         * When Apache Ignite native persistence is enabled Lucene index will be
         * persisted under well known directory names, for better performance
         * it's assumed the index directory is on a separated HDD/SSD.
         * 
         * <pre>
         * - 1. Per global root directory "directoryPath/db/lucene", if directoryPath not provided default is "IGNITE_WORK/db/lucene"
         * |
         * --- 2. Per "cache-CACHE_NAME"
         *   |
         *   --- 3. Per indexName (TABLENAME_LUCENE_IDX)
         *     |
         *     --- 4. per index segments s_0..s_(#-1), where # is the degree of parallelism within
         * a single node per index
         * 
         * </pre>
         * 
         * Once index is created you can't change this value, unless you drop
         * the index and create it again
         * 
         * <p>
         * 
         * Will be ignored if Apache Ignite native persistence is disabled
         * 
         */
        String directoryPath() default DEFAULT_DIRECTORY_PATH;

        /**
         * The Lucene index searcher refresh frequency, in seconds. Default 60
         * seconds.
         * <p>
         * This value will be changed at runtime if changes detected on
         * {@link QueryTextField.IndexOptions}.
         * 
         */
        int refreshSeconds() default DEFAULT_REFRESH_SECONDS;

        /**
         * Memory on MB for indexWriter buffer on <b>on-heap</b> JVM memory
         * (default 5MB).
         * 
         * <pre>
         * Additional <b>on-heap</b> JVM Memory consumed per Lucene index = ramBufferMB * #partitions * #segments
         * </pre>
         * <p>
         * This value will be changed at runtime if changes detected on
         * {@link QueryTextField.IndexOptions}.
         */
        int ramBufferMB() default DEFAULT_RAM_BUFFER_MB;

        /**
         * The Lucene's max off-heap memory storage size, in MB. Use 0 for
         * unlimited.
         * 
         * <ul>
         * <li>When Apache Ignite native persistence is <b>enabled</b>,
         * <b>default</b> value will be <b>20% of associated cache's data
         * region</b> . Lucene index will share this 20% off-heap memory region
         * with other lucene indexes on same cache's data region. You could
         * change default 0.2 (20%) factor by setting
         * <b>IGNITE_LUCENE_INDEX_MAX_MEMORY_FACTOR</b> system property on
         * server nodes. Needs restart server nodes</li>
         * 
         * <li>If Apache Ignite native persistence is <b>disabled</b>,
         * <b>default</b> value will be <b>unlimited (0)</b>. Changing
         * maxCachedMB value will be allowed if current allocated memory for
         * Lucene index is less than provided value. Once exceed the memory
         * limit determined by maxCachedMB a GridOffHeapOutOfMemoryException
         * will be thrown</li>
         * </ul>
         * 
         * This value will be changed at runtime if changes detected on
         * {@link QueryTextField.IndexOptions}.
         * 
         */
        int maxCachedMB() default DEFAULT_MAX_CACHED_MB;

        /**
         * The default text {@code Analyzer}.
         * <p>
         * Once index is created you can't change this value, unless you drop
         * the index and create it again
         */
        String defaultAnalyzer() default DEFAULT_TEXT_ANALYZER;

        /**
         * Whether Lucene index automatic optimization is enabled
         * 
         * This value will be changed at runtime if changes detected on
         * {@link QueryTextField.IndexOptions}.
         */
        boolean optimizerEnabled() default DEFAULT_OPTIMIZER_ENABLED;

        /**
         * Optimizer's schedule CRON expression.
         * 
         * <p>
         * 
         * By default optimizer will run every day at 1:00 AM (0 1 * * *)
         * 
         * @See <a href=
         *      "https://apacheignite.readme.io/docs/cron-based-scheduling">Cron
         *      -Based Scheduling</a>
         * 
         *      This value will be changed at runtime if changes detected on
         *      {@link QueryTextField.IndexOptions}.
         */
        String optimizerSchedule() default DEFAULT_OPTIMIZER_SCHEDULE;

        /**
         * Custom text analyzers using a Lucene's {@code Analyzer}s in
         * classpath.
         * <p>
         * Once index is created you can change this value only by adding new
         * analyzers
         */
        ClasspathAnalyzer[] classpathAnalyzers() default {};

        /**
         * Custom text analyzers for tartarus.org snowball {@code Analyzer}s.
         * <p>
         * Once index is created you can change this value only by adding new
         * analyzers
         */
        SnowballAnalyzer[] snowballAnalyzers() default {};
    }

    /**
     * Big decimal mapper
     * 
     * <p>
     * Maps arbitrary precision signed decimal values.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer</i>
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface BigDecimalMapper {

        /** mapper's type */
        static final MapperType type = MapperType.BIGDECIMAL;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** The max allowed number of digits for the integer part. Default 32 */
        int integer_digits() default 32;

        /** The max allowed number of digits for the decimal part. Default 32 */
        int decimal_digits() default 32;
    }

    /**
     * Big integer mapper
     * 
     * <p>
     * Maps arbitrary precision signed integer values.
     * <p>
     * Supported value types: <i>String, BigInteger, Integer</i>
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface BigIntegerMapper {

        /** mapper's type */
        static final MapperType type = MapperType.BIGINTEGER;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** The max allowed number of digits. Default 32 */
        int digits() default 32;
    }

    /**
     * Bitemporal mapper
     * 
     * <p>
     * Maps four columns containing the four dates defining a bitemporal fact.
     * The mapped columns shouldn't be collections.
     * <p>
     * Supported value types: <i>String, Number, Date, UUID</i>
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public static @interface BitemporalMapper {

        /** mapper's type */
        static final MapperType type = MapperType.BITEMPORAL;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /**
         * The name of the column storing the beginning (time start) of the
         * valid date range.
         **/
        String vt_from();

        /**
         * The name of the column storing the end (time stop) of the valid date
         * range.
         **/
        String vt_to();

        /**
         * The name of the column storing the beginning (time start) of the
         * transaction date range.
         **/
        String tt_from();

        /**
         * The name of the column storing the end (time stop) of the transaction
         * date range.
         **/
        String tt_to();

        /**
         * The date pattern for parsing not-date columns and creating Lucene
         * fields. Note that it can be used to index dates with reduced
         * precision. If column is a number that does not match `pattern` will
         * be parsed as the milliseconds since January 1, 1970, 00:00:00 GMT.
         */
        String pattern() default DEFAULT_DATE_PATTERN;

        /**
         * A date representing NOW applying pattern(). Default = Long.MAX_VALUE
         * = "292278994/08/17 08:12:55.807 +0100"
         **/
        String now_value() default "292278994/08/17 08:12:55.807 +0100";
    }

    /**
     * Blob mapper
     * 
     * <p>
     * Maps a blob value.
     * <p>
     * Supported value types: <i>String, byte[], ByteBuffer</i>
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface BlobMapper {

        /** mapper's type */
        static final MapperType type = MapperType.BYTES;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;
    }

    /**
     * Boolean mapper
     * 
     * <p>
     * Maps a boolean value..
     * <p>
     * Supported value types: <i>String, Boolean</i>
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface BooleanMapper {

        /** mapper's type */
        static final MapperType type = MapperType.BOOLEAN;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;
    }

    /**
     * Date mapper
     * 
     * <p>
     * Maps dates using a either a pattern, an UNIX timestamp or a time UUID.
     * <p>
     * Supported value types: <i>String, Number, Date, UUID
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface DateMapper {

        /** mapper's type */
        static final MapperType type = MapperType.DATE;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * The date pattern for parsing not-date columns and creating Lucene
         * fields. Note that it can be used to index dates with reduced
         * precision. If column is a number that does not match `pattern` will
         * be parsed as the milliseconds since January 1, 1970, 00:00:00 GMT.
         */
        String pattern() default DEFAULT_DATE_PATTERN;
    }

    /**
     * Date range mapper
     * 
     * <p>
     * Maps a time duration/period defined by a start date and a stop date. The
     * mapped columns shouldn't be collections.
     * <p>
     * Supported value types: <i>String, Number, Date, UUID
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public static @interface DateRangeMapper {

        /** mapper's type */
        static final MapperType type = MapperType.DATE_RANGE;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /**
         * The name of the column storing the start date of the time duration to
         * be indexed.
         */
        String from();

        /**
         * The name of the column storing the stop date of the time duration to
         * be indexed.
         */
        String to();

        /**
         * The date pattern for parsing not-date columns and creating Lucene
         * fields. Note that it can be used to index dates with reduced
         * precision. If column is a number that does not match `pattern` will
         * be parsed as the milliseconds since January 1, 1970, 00:00:00 GMT.
         */
        String pattern() default DEFAULT_DATE_PATTERN;

    }

    /**
     * Double mapper
     * 
     * <p>
     * Maps a 64-bit decimal number.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface DoubleMapper {

        /** mapper's type */
        static final MapperType type = MapperType.DOUBLE;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** The Lucene's index-time boosting factor. Default 1.0f */
        float boost() default 1.0f;
    }

    /**
     * Float mapper
     * 
     * <p>
     * Maps a 32-bit decimal number.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface FloatMapper {

        /** mapper's type */
        static final MapperType type = MapperType.FLOAT;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** the Lucene's index-time boosting factor. Default 1.0f */
        float boost() default 1.0f;
    }

    /**
     * Geo point mapper
     * 
     * <p>
     * Maps a geospatial location (point) defined by two columns containing a
     * latitude and a longitude.
     * <p>
     * Indexing is based on a composite spatial strategy that stores points in a
     * doc values field and also indexes them into a geohash recursive prefix
     * tree with a certain precision level.
     * <p>
     * The low-accuracy prefix tree is used to quickly find results, maybe
     * producing some false positives, and the doc values field is used to
     * discard these false positives. The mapped columns shouldn't be
     * collections.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer
     *
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public static @interface GeoPointMapper {

        /** mapper's type */
        static final MapperType type = MapperType.GEO_POINT;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /**
         * The name of the column storing the latitude of the point to be
         * indexed.
         */
        String latitude();

        /**
         * The name of the column storing the longitude of the point to be
         * indexed.
         */
        String longitude();

        /**
         * The maximum number of levels in the underlying geohash search tree.
         * Default 11. False positives will be discarded using stored doc
         * values, so this doesn't mean precision lost. Higher values will
         * produce few false positives to be post-filtered, at the expense of
         * creating more terms in the search index.
         */
        int max_levels() default 11;
    }

    /**
     * Geo shape mapper
     * 
     * <p>
     * Maps a geographical shape stored in a text column with Well Known Text
     * (WKT) format. The supported WKT shapes are point, linestring, polygon,
     * multipoint, multilinestring and multipolygon.
     * <p>
     * It is possible to specify a sequence of geometrical transformations to be
     * applied to the shape before indexing it. It could be used for indexing
     * only the centroid of the shape, or a buffer around it, etc.
     * <p>
     * Indexing is based on a composite spatial strategy that stores shapes in a
     * doc values field and also indexes them into a geohash recursive prefix
     * tree with a certain precision level. The low-accuracy prefix tree is used
     * to quickly find results, maybe producing some false positives, and the
     * doc values field is used to discard these false positives.
     * <p>
     * This mapper depends on Java Topology Suite (JTS).
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer
     *
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface GeoShapeMapper {

        /** mapper's type */
        static final MapperType type = MapperType.GEO_SHAPE;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /**
         * Sequence of geometrical transformations to be applied to each shape
         * before indexing it.
         */
        GeoTransformation[] transformations() default {};

        /**
         * The maximum number of levels in the underlying geohash search tree.
         * Default 5. False positives will be discarded using stored doc values,
         * so this doesn't mean precision lost. Higher values will produce few
         * false positives to be post-filtered, at the expense of creating more
         * terms in the search index.
         */
        int max_levels() default 5;

        /**
         * Geo transformation
         * 
         * <p>
         * Geo shape mapper could takes a list of geometrical transformations as
         * argument. These transformations are sequentially applied to the shape
         * that is going to be indexed or searched.
         * 
         * <p>
         * <b>Transformation types</b>
         * <ul>
         * <li><b>bbox</b>: Bounding box transformation. Buffer transformation
         * returns the minimum bounding box of a shape, that is, the minimum
         * rectangle containing the shape.
         * <li><b>buffer</b>: Buffer transformation. Buffer transformation
         * returns a buffer around a shape. With this optional parameters:
         * <ul>
         * <li><b>min_distance</b>: the inside buffer distance (see available
         * Distance units above). Optional.
         * <li><b>max_distance</b>: the outside buffer distance (see available
         * Distance units above). Optional.
         * </ul>
         * <li><b>centroid</b>: Centroid transformation. Centroid transformation
         * returns the geometric center of a shape.
         * <li><b>convex_hull</b>: Convex hull transformation. Convex hull
         * transformation returns the convex envelope of a shape.
         * </ul>
         * 
         * <p>
         * <b>Distance</b>
         * <p>
         * Both geo distance search and buffer transformation take a spatial
         * distance as argument. This distance is just a string with the form
         * "1km", "1000m", etc. The following table shows the available options
         * for distance units. The default distance unit is meter.
         * 
         * <p>
         * <b>Available distance units (real unit)</b>
         * <ul>
         * <li>mm, millimeters <i>(millimeter)</i></li>
         * <li>cm, centimeters <i>(centimeter)</i>
         * <li>dm, decimeters <i>(decimeter)</i>
         * <li>m, meters <i>(meter)</i>
         * <li>dam, decameters <i>(decameter)</i>
         * <li>hm, hectometers <i>(hectometer)</i>
         * <li>km, kilometers <i>(kilometer)</i>
         * <li>ft, foots <i>(foot)</i>
         * <li>yd, yards <i>(yard)</i>
         * <li>in, inches <i>(inch)</i>
         * <li>mi, miles <i>(mile)</i>
         * <li>M, NM, mil, nautical_miles <i>(nautical mile)</i>
         * </ul>
         *
         * 
         * @author Manuel Núñez (manuel.nunez@hawkore.com)
         *
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.METHOD, ElementType.FIELD })
        public static @interface GeoTransformation {

            /** Geo transformation type */
            GeoTransformationType type();

            /**
             * The outside buffer distance. Optional. Only applied to buffer
             * transformation type. Use distance values
             */
            String max_distance() default "";

            /**
             * The inside buffer distance. Optional. Only applied to buffer
             * transformation type. Use distance values
             */
            String min_distance() default "";

        }

    }

    /**
     * Inet Address mapper
     * 
     * <p>
     * Maps an IP address. Either IPv4 and IPv6 are supported.
     * <p>
     * Supported value types: <i>InetAddress, String
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface InetMapper {

        /** mapper's type */
        static final MapperType type = MapperType.INET;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;
    }

    /**
     * Integer mapper
     * 
     * <p>
     * Maps a 32-bit integer number.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface IntegerMapper {

        /** mapper's type */
        static final MapperType type = MapperType.INTEGER;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** the Lucene's index-time boosting factor. Default 1.0f */
        float boost() default 1.0f;
    }

    /**
     * Long mapper
     * 
     * <p>
     * Maps a 64-bit integer number.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface LongMapper {

        /** mapper's type */
        static final MapperType type = MapperType.LONG;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** the Lucene's index-time boosting factor. Default 1.0f */
        float boost() default 1.0f;
    }

    /**
     * String mapper
     * 
     * <p>
     * Maps a not-analyzed text value.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer, UUID, Boolean, InetAdress
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface StringMapper {

        /** mapper's type */
        static final MapperType type = MapperType.STRING;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /** If the text will be indexed preserving its casing. */
        boolean case_sensitive() default true;
    }

    /**
     * Text mapper
     * 
     * <p>
     * Maps a language-aware text value analyzed according to the specified
     * analyzer.
     * <p>
     * Supported value types: <i>String, BigInteger, BigDecimal, Long, Double,
     * Float, Integer, UUID, Boolean, InetAdress</i>
     *
     *
     * <p>
     * <b>Custom Analyzers</b>
     * <p>
     * Analyzer definition options depend on the analyzer type. Details and
     * default values are listed in the table below.
     * <p>
     * <table>
     * <tr>
     * <th>Analyzer type</th>
     * <th>Option</th>
     * <th>Value type</th>
     * <th>Default value</th>
     * </tr>
     * <tbody>
     * <tr>
     * <td>classpath</td>
     * <td>class</td>
     * <td>string</td>
     * <td>null</td>
     * </tr>
     * <tr>
     * <td rowspan="2">snowball</td>
     * <td>language</td>
     * <td>string</td>
     * <td>null</td>
     * </tr>
     * <tr>
     * <td>stopwords</td>
     * <td>string</td>
     * <td>null</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p>
     * <b>Snowball analyzer</b>
     * <p>
     * Analyzer using a
     * <a href="http://snowball.tartarus.org/">http://snowball.tartarus.org/</a>
     * snowball filter <a href=
     * "https://lucene.apache.org/core/5_3_0/analyzers-common/org/apache/lucene/analysis/snowball/SnowballFilter.html">
     * SnowballFilter</a>
     * </p>
     * <p>
     * Supported languages: English, French, Spanish, Portuguese, Italian,
     * Romanian, German, Dutch, Swedish, Norwegian, Danish, Russian, Finnish,
     * Hungarian and Turkish.
     *
     * <p>
     * Custom analyzers (type classpath and snowball) defined on
     * {@link IndexOptions} can by referenced by this mapper's type.
     * <p>
     * Additionally, there are prebuilt analyzers for:
     * <p>
     * <table>
     * <tr>
     * <th>Analyzer name</th>
     * <th>Analyzer full package name</th>
     * </tr>
     * <tbody>
     * <tr>
     * <td>standard</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/standard/StandardAnalyzer.html">
     * org.apache.lucene.analysis.standard.StandardAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>keyword</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/core/KeywordAnalyzer.html">
     * org.apache.lucene.analysis.core.KeywordAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>stop</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/core/StopAnalyzer.html">
     * org.apache.lucene.analysis.core.StopAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>whitespace</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/core/WhitespaceAnalyzer.html">
     * org.apache.lucene.analysis.core.WhitespaceAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>simple</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/core/SimpleAnalyzer.html">
     * org.apache.lucene.analysis.core.SimpleAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>classic</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/standard/ClassicAnalyzer.html">
     * org.apache.lucene.analysis.standard.ClassicAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>arabic</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/ar/ArabicAnalyzer.html">
     * org.apache.lucene.analysis.ar.ArabicAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>armenian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/hy/ArmenianAnalyzer.html">
     * org.apache.lucene.analysis.hy.ArmenianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>basque</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/eu/BasqueAnalyzer.html">
     * org.apache.lucene.analysis.eu.BasqueAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>brazilian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/br/BrazilianAnalyzer.html">
     * org.apache.lucene.analysis.br.BrazilianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>bulgarian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/bg/BulgarianAnalyzer.html">
     * org.apache.lucene.analysis.bg.BulgarianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>catalan</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/ca/CatalanAnalyzer.html">
     * org.apache.lucene.analysis.ca.CatalanAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>cjk</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/cjk/CJKAnalyzer.html">
     * org.apache.lucene.analysis.cjk.CJKAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>czech</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/cz/CzechAnalyzer.html">
     * org.apache.lucene.analysis.cz.CzechAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>dutch</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/nl/DutchAnalyzer.html">
     * org.apache.lucene.analysis.nl.DutchAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>danish</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/da/DanishAnalyzer.html">
     * org.apache.lucene.analysis.da.DanishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>english</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/en/EnglishAnalyzer.html">
     * org.apache.lucene.analysis.en.EnglishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>finnish</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/fi/FinnishAnalyzer.html">
     * org.apache.lucene.analysis.fi.FinnishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>french</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/fr/FrenchAnalyzer.html">
     * org.apache.lucene.analysis.fr.FrenchAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>galician</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/gl/GalicianAnalyzer.html">
     * org.apache.lucene.analysis.gl.GalicianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>german</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/de/GermanAnalyzer.html">
     * org.apache.lucene.analysis.de.GermanAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>greek</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/el/GreekAnalyzer.html">
     * org.apache.lucene.analysis.el.GreekAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>hindi</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/hi/HindiAnalyzer.html">
     * org.apache.lucene.analysis.hi.HindiAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>hungarian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/hu/HungarianAnalyzer.html">
     * org.apache.lucene.analysis.hu.HungarianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>indonesian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/id/IndonesianAnalyzer.html">
     * org.apache.lucene.analysis.id.IndonesianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>irish</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/ga/IrishAnalyzer.html">
     * org.apache.lucene.analysis.ga.IrishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>italian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/it/ItalianAnalyzer.html">
     * org.apache.lucene.analysis.it.ItalianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>latvian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/lv/LatvianAnalyzer.html">
     * org.apache.lucene.analysis.lv.LatvianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>norwegian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/no/NorwegianAnalyzer.html">
     * org.apache.lucene.analysis.no.NorwegianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>persian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/fa/PersianAnalyzer.html">
     * org.apache.lucene.analysis.fa.PersianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>portuguese</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/pt/PortugueseAnalyzer.html">
     * org.apache.lucene.analysis.pt.PortugueseAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>romanian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/ro/RomanianAnalyzer.html">
     * org.apache.lucene.analysis.ro.RomanianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>russian</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/ru/RussianAnalyzer.html">
     * org.apache.lucene.analysis.ru.RussianAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>sorani</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/ckb/SoraniAnalyzer.html">
     * org.apache.lucene.analysis.ckb.SoraniAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>spanish</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/es/SpanishAnalyzer.html">
     * org.apache.lucene.analysis.es.SpanishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>swedish</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/sv/SwedishAnalyzer.html">
     * org.apache.lucene.analysis.sv.SwedishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>turkish</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/tr/TurkishAnalyzer.html">
     * org.apache.lucene.analysis.tr.TurkishAnalyzer</a></td>
     * </tr>
     * <tr>
     * <td>thai</td>
     * <td><a href=
     * "https://lucene.apache.org/core/5_5_4/analyzers-common/org/apache/lucene/analysis/th/ThaiAnalyzer.html">
     * org.apache.lucene.analysis.th.ThaiAnalyzer</a></td>
     * </tr>
     * </tbody>
     * </table>
     *
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface TextMapper {

        /** mapper's type */
        static final MapperType type = MapperType.TEXT;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;

        /**
         * The name of the text analyzer to be used. Default analyzer of the
         * schema is "standard".
         * <p>
         * Additionally to references to those analyzers defined in the
         * <b>classpathAnalyzers</b> and <b>snowballAnalyzers</b> sections of
         * the {@link IndexOptions}, there are prebuilt analyzers for Arabic,
         * Bulgarian, Brazilian, Catalan, Sorani, Czech, Danish, German, Greek,
         * English, Spanish, Basque, Persian, Finnish, French, Irish, Galician,
         * Hindi, Hungarian, Armenian, Indonesian, Italian, Latvian, Dutch,
         * Norwegian, Portuguese, Romanian, Russian, Swedish, Thai and Turkish.
         */
        String analyzer() default "";
    }

    /**
     * UUID mapper
     * 
     * <p>
     * Maps an UUID value.
     * <p>
     * Supported value types: <i>String, UUID
     *
     * @author Manuel Núñez (manuel.nunez@hawkore.com)
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.FIELD })
    public static @interface UUIDMapper {

        /** mapper's type */
        static final MapperType type = MapperType.UUID;

        /**
         * The mapper's name, lucene document's indexable field name.
         * 
         */
        String name() default "";

        /**
         * The name of the column (real table's column name) storing the value
         * to be indexed. If not provided mapper's name will be used.
         * 
         */
        String column() default "";

        /**
         * Sets if the field must be validated and mapping errors should make
         * writes fail, instead of just logging the error. Default false
         */
        boolean validated() default false;
    }
}