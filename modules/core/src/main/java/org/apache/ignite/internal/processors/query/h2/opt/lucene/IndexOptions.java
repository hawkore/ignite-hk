package org.apache.ignite.internal.processors.query.h2.opt.lucene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ignite.cache.query.annotations.QueryTextField;
import org.apache.ignite.internal.processors.query.QueryTypeDescriptorImpl;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.processors.query.h2.opt.lucene.partitioning.Partitioner;
import org.apache.ignite.internal.processors.query.h2.opt.lucene.partitioning.PartitionerOnNone;
import org.hawkore.ignite.lucene.IndexException;
import org.hawkore.ignite.lucene.common.JsonSerializer;
import org.hawkore.ignite.lucene.schema.Schema;
import org.hawkore.ignite.lucene.schema.SchemaBuilder;

/**
 * 
 * Index user-specified configuration options parser.
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 */
public class IndexOptions {

    /**
     * Index options version.
     * <p>
     * Is a sequential number that avoid update index options when using dynamic
     * sql entities functionality (lucene index alteration).
     * <p>
     * Update index options will be allowed if new <code>version</code> value is
     * equals or greater than current index option's <code>version</code> value.
     * <p>
     * Default 0
     */
    private static final String VERSION_OPTION = "version";
    /**
     * The default index configuration version. Default 0.
     */
    public static final int DEFAULT_VERSION = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_INDEX_OPTIONS_VERSION;

    /**
     * The default text analyzer. Default "standard"
     */
    public static final String DEFAULT_TEXT_ANALYZER = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_TEXT_ANALYZER;

    /**
     * The Lucene index searcher refresh frequency, in seconds. Default 60
     * seconds.
     * 
     * <p>
     * This value could be changed at runtime.
     * 
     */
    private static final String REFRESH_SECONDS_OPTION = "refresh_seconds";
    /**
     * The default refresh seconds. Default 60 seconds.
     */
    public static final double DEFAULT_REFRESH_SECONDS = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_REFRESH_SECONDS;

    /**
     * When <b>native persistence is enabled</b>, Advanced Lucene index's well
     * known directory structure allows to mount on separate HHD/SSD index's
     * data for better performance.
     * <p>
     * 
     * By default IGNITE_WORK directory
     * 
     * <p>
     * 
     * Directory structure:
     * 
     * <pre>
     * IGNITE_WORK or configured directoryPath
     *   └── db
     *       └── lucene
     *           └── cache-CACHE_NAME (per cache name)
     *               └── TABLENAME_LUCENE_IDX (per lucene index name)
     *                   ├── s_0 (per degree of parallelism)
     *                   ├── ...
     *                   └── s_(#-1)
     * </pre>
     * 
     * Where # on (#-1) is the degree of parallelism within a single node per
     * index. See
     * {@link org.apache.ignite.configuration.CacheConfiguration#getQueryParallelism}
     * 
     * <p>
     * Once index is created you can't change this value, unless you drop the
     * index and create it again
     * 
     * <p>
     * 
     * Will be ignored if native persistence is not enabled.
     * 
     */
    private static final String DIRECTORY_PATH_OPTION = "directory_path";
    /**
     * The default directory path. By default IGNITE_WORK directory.
     */
    public static final String DEFAULT_DIRECTORY_PATH = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_DIRECTORY_PATH;

    /**
     * Memory on MB for indexWriter buffer on on-heap memory (default 5MB).
     * 
     * <pre>
     * Total ON-HEAP Memory consumption per Lucene index = ramBufferMB * #partitions * #segments
     * </pre>
     * 
     * <p>
     * This value could be changed at runtime only if native persistence is
     * enabled, otherwise will be ignored.
     */
    private static final String RAM_BUFFER_MB_OPTION = "ram_buffer_mb";
    /**
     * The default ram buffer = 5MB
     */
    public static final double DEFAULT_RAM_BUFFER_MB = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_RAM_BUFFER_MB;

    /**
     * Split the index in multiple partitions. Default is
     * {@link PartitionerOnNone}.
     *
     * Index partitioning is useful to speed up some searches to the detriment
     * of others, depending on the implementation.
     *
     * It is also useful to overcome the Lucene's hard limit of 2147483519
     * documents per local index.
     * 
     * Once index is created you can't change this value, unless you drop the
     * index and create it again
     */
    private static final String PARTITIONER_OPTION = "partitioner";
    /**
     * The default lucene index {@link PartitionerOnNone}
     */
    public static final Partitioner DEFAULT_PARTITIONER = new PartitionerOnNone();

    /**
     * The Lucene's max off-heap memory storage size, in MB.
     * 
     * <p>
     * Default values:
     * <ul>
     * <li>If <b>native persistence is disabled</b>, default value will be
     * unlimited (0).</li>
     * <li>If <b>native persistence is enabled</b>, default value will be 20% of
     * associated cache's data region. Lucene index will share this 20% off-heap
     * memory with another lucene indexes per associated cache's data region.
     * You could change default 0.2 factor (20%) by setting
     * <b>IGNITE_LUCENE_INDEX_MAX_MEMORY_FACTOR</b> system property on server
     * nodes (restart server nodes to apply changes on default memory factor).
     * 
     * <pre>
     *  For example, associated cache's data region "A" has a max allowed
     *  off-heap memory = 100 MB, then shared off-heap memory = 20 MB for all lucene
     *  indexes "within" data region "A". 
     *  
     *  Total off-heap that may be consumed = 100MB (data region A) + 20MB (lucene indexes within data region A).
     * </pre>
     * 
     * </li>
     * </ul>
     *
     * Allowed values:
     * <ul>
     * <li><b>-1</b> will use shared 20% (or determined by
     * IGNITE_LUCENE_INDEX_MAX_MEMORY_FACTOR) of associated data region max
     * memory, if native persistence is enabled.</li>
     * <li><b>0</b> unlimited off-heap memory</li>
     * <li><b>>0</b> MB max off-heap memory</li>
     * </ul>
     * 
     * If you set a value for <code>maxCachedMB >= 0</code>, lucene index will
     * use its own off-heap memory.
     * 
     * <p>
     * Note that if <b>persistence is disabled</b>, a
     * GridOffHeapOutOfMemoryException will be thrown in case of consumed memory
     * is out of assigned <b>maxCachedMB</b>.
     * <p>
     * 
     * This value could be changed at runtime as long as current consumed memory
     * by lucene index is lower than new assigned one.
     * 
     */
    private static final String MAX_CACHED_MB_OPTION = "max_cached_mb";
    /**
     * The default OFF-HEAP memory for Lucene Index
     */
    public static final double DEFAULT_MAX_CACHED_MB = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_MAX_CACHED_MB;

    /**
     * Optimizer's schedule CRON expression.
     * 
     * <p>
     * 
     * By default optimizer will run every day at 1:00 AM (0 1 * * *)
     * 
     * @See <a href="https://apacheignite.readme.io/docs/cron-based-scheduling">
     *      Cron-Based Scheduling</a>
     * 
     *      This value will be changed at runtime if changes detected on
     *      {@link QueryTextField.IndexOptions}.
     */
    private static final String OPTIMIZER_SCHEDULE_OPTION = "optimizer_schedule";
    /**
     * The default optimizer schedule. By default optimizer will run every day
     * at 1:00 AM (0 1 * * *)
     */
    public static final String DEFAULT_OPTIMIZER_SCHEDULE = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_OPTIMIZER_SCHEDULE;

    /**
     * Whether Lucene index automatic optimization is enabled
     * 
     * This value will be changed at runtime if changes detected on
     * {@link QueryTextField.IndexOptions}.
     */
    private static final String OPTIMIZER_ENABLED_OPTION = "optimizer_enabled";
    /**
     * The default optimizer activation. By default true.
     */
    public static final Boolean DEFAULT_OPTIMIZER_ENABLED = org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions.DEFAULT_OPTIMIZER_ENABLED;

    private static final String SCHEMA_OPTION = "schema";

    private Map<String, String> options;

    private double refreshSeconds;

    private Schema schema;

    private String directoryPath;

    private double maxCachedMB;

    private double ramBufferMB;

    private boolean optimizerEnabled;

    private String optimizerSchedule;

    private Partitioner partitioner;

    private int version;

    private int parseVersion(Map<String, String> options) {
        return this.parseStrictlyNonNegativeInteger(options, VERSION_OPTION, DEFAULT_VERSION);
    }

    private double parseRefresh(Map<String, String> options) {
        return this.parseStrictlyPositiveDouble(options, REFRESH_SECONDS_OPTION, DEFAULT_REFRESH_SECONDS);
    }

    private double parseRamBuffer(Map<String, String> options) {
        return this.parseStrictlyPositiveDouble(options, RAM_BUFFER_MB_OPTION, DEFAULT_RAM_BUFFER_MB);
    }

    private double parseMaxCached(Map<String, String> options) {
        return this.parseDouble(options, MAX_CACHED_MB_OPTION, DEFAULT_MAX_CACHED_MB);
    }

    private String parseDirectoryPath(Map<String, String> options) {
        return Optional.ofNullable(options.get(DIRECTORY_PATH_OPTION)).map(value -> value)
            .orElseGet(() -> DEFAULT_DIRECTORY_PATH);
    }

    private Schema parseSchema(Map<String, String> options) {
        return Optional.ofNullable(options.get(SCHEMA_OPTION)).map(value -> SchemaBuilder.fromJson(value).build())
            .orElseThrow(() -> new IndexException(SCHEMA_OPTION + "is required"));
    }

    private SchemaBuilder parseSchemaBuilder(Map<String, String> options) {
        return Optional.ofNullable(options.get(SCHEMA_OPTION)).map(SchemaBuilder::fromJson)
            .orElseThrow(() -> new IndexException(SCHEMA_OPTION + "is required"));
    }

    private Partitioner parsePartitioner(Map<String, String> options) {
        return Optional.ofNullable(options.get(PARTITIONER_OPTION))
            .map(Partitioner::fromJson)
            .orElseGet(() -> DEFAULT_PARTITIONER);
    }

    private boolean parseOptimizerEnabled(Map<String, String> options) {
        return Optional.ofNullable(options.get(OPTIMIZER_ENABLED_OPTION)).map(Boolean::parseBoolean)
            .orElseGet(() -> DEFAULT_OPTIMIZER_ENABLED);
    }

    private String parseOptimizerSchedule(Map<String, String> options) {
        return Optional.ofNullable(options.get(OPTIMIZER_SCHEDULE_OPTION)).orElseGet(() -> DEFAULT_OPTIMIZER_SCHEDULE);
    }

    private double parseDouble(Map<String, String> options, String name, double d) {
        return Optional.ofNullable(options.get(name)).map(value -> {
            Double i = null;
            try {
                i = Double.valueOf(value);
            } catch (Exception e) {
                throw new IndexException("'" + name + "' must be a decimal, found: " + value);
            }
            return i;

        }).orElseGet(() -> d);
    }

    private double parseStrictlyPositiveDouble(Map<String, String> options, String name, double d) {
        return Optional.ofNullable(options.get(name)).map(value -> {
            Double i = null;
            try {
                i = Double.valueOf(value);
            } catch (Exception e) {
                throw new IndexException("'" + name + "' must be a decimal, found: " + value);
            }
            if (i <= 0) {
                throw new IndexException("'" + name + "' must be a strictly positive decimal, found: " + value);
            }
            return i;

        }).orElseGet(() -> d);
    }

    private int parseStrictlyNonNegativeInteger(Map<String, String> options, String name, int d) {
        return Optional.ofNullable(options.get(name)).map(value -> {
            Integer i = null;
            try {
                i = Integer.valueOf(value);
            } catch (Exception e) {
                throw new IndexException("'" + name + "' must be a integer, found: " + value);
            }
            if (i < 0) {
                throw new IndexException("'" + name + "' must be a strictly non-negative integer, found: " + value);
            }
            return i;

        }).orElseGet(() -> d);
    }

    /**
     * 
     * @return options
     */
    public Map<String, String> options() {
        return this.options;
    }

    /**
     * 
     * @return refreshSeconds
     */
    public double refreshSeconds() {
        return this.refreshSeconds;
    }

    /**
     * 
     * @return schema
     */
    public Schema schema() {
        return this.schema;
    }

    /**
     * Replace schema
     * 
     * @param schema
     * @return schema
     */
    public Schema schema(Schema schema) {
        this.schema = schema;
        return this.schema;
    }

    /**
     * 
     * @return directoryPath
     */
    public String directoryPath() {
        return this.directoryPath;
    }

    /**
     * 
     * @return maxCachedMB
     */
    public double maxCachedMB() {
        return this.maxCachedMB;
    }

    /**
     * 
     * @return ramBufferMB
     */
    public double ramBufferMB() {
        return this.ramBufferMB;
    }

    /**
     * 
     * @return partitioner
     */
    public Partitioner partitioner() {
        return this.partitioner;
    }

    /**
     * 
     * @return index options version
     */
    public int version() {
        return this.version;
    }

    /**
     * 
     * @return whether optimizer is enabled
     */
    public boolean optimizerEnabled() {
        return this.optimizerEnabled;
    }

    /**
     * 
     * @return optimizer schedule CRON expression
     */
    public String optimizerSchedule() {
        return this.optimizerSchedule;
    }

    /**
     * Constructs IndexOptions from json string
     * 
     * @param jsonIndexOptions
     * @throws IndexException
     */
    @SuppressWarnings("unchecked")
    public IndexOptions(String jsonIndexOptions) {
        try {
            this.options = JsonSerializer.fromString(jsonIndexOptions, HashMap.class);
        } catch (Exception e) {
            throw new IndexException("Index options bad formatted", e);
        }
        this.schema = parseSchema(options);

        this.refreshSeconds = parseRefresh(options);
        this.directoryPath = parseDirectoryPath(options);
        this.maxCachedMB = parseMaxCached(options);
        this.partitioner = parsePartitioner(options);
        this.ramBufferMB = parseRamBuffer(options);
        this.optimizerEnabled = parseOptimizerEnabled(options);
        this.optimizerSchedule = parseOptimizerSchedule(options);
        this.version = parseVersion(options);
    }

    /**
     * 
     * @param typeDesc
     * @param all - return all mapped columns even if not defined on typeDesc
     * @return  QueryEntity's mapped columns by lucene index's schema
     */
    public List<String> mappedColumns(QueryTypeDescriptorImpl typeDesc, boolean all) {

        List<String> complexTypeColumns = typeDesc.properties().entrySet().stream()
            .filter(e -> Map.class.isAssignableFrom(e.getValue().type()) ||
                List.class.isAssignableFrom(e.getValue().type()) ||
                Set.class.isAssignableFrom(e.getValue().type()))
            .map(Entry::getKey).collect(Collectors.toList());

        List<String> allColumns = typeDesc.properties().entrySet().stream()
            .map(Entry::getKey).collect(Collectors.toList());
        
        return this.schema().mappers.values()
            .stream()
            .flatMap(x -> x.mappedColumns.stream())
            .map(cell -> {
                final String normalizedSyntheticCell = QueryUtils.normalizeObjectName(cell, true);
                // lucene mapped columns could contains synthetics column names (._key, ._value) for
                // collection/maps of complex types so we need to remove synthetics ones
                Optional<String> realColumnName = complexTypeColumns.stream().filter(normalizedSyntheticCell::startsWith)
                    .findFirst();
                
                if (realColumnName.isPresent()) {
                    return cell.substring(0, realColumnName.get().length());
                }
                
                if (allColumns.contains(normalizedSyntheticCell) || all) {
                    return cell;
                }
                
                return null;
            }).filter(Objects::nonNull)
            .collect(Collectors.toSet()).stream().collect(Collectors.toList());
    }

    /**
     * @return Schema builder
     */
    public SchemaBuilder schemaBuilder() {
        return parseSchemaBuilder(this.options);
    }

    /**
     * Return whether update is required and allowed
     * 
     * @param newIndexOptions
     *            new index options
     * 
     * @return whether update is required and allowed
     */
    public boolean allowedConfigUpdate(Object newIndexOptions) {
        if (this == newIndexOptions)
            return false;
        if (newIndexOptions == null)
            return true;
        if (getClass() != newIndexOptions.getClass())
            return true;
        IndexOptions other = (IndexOptions) newIndexOptions;

        // only allow update options is new version >= current version
        if (version < other.version)
            return false;

        // allowed safe changes on index configuration
        if (Double.doubleToLongBits(maxCachedMB) != Double.doubleToLongBits(other.maxCachedMB))
            return true;
        if (Double.doubleToLongBits(ramBufferMB) != Double.doubleToLongBits(other.ramBufferMB))
            return true;
        if (optimizerEnabled != other.optimizerEnabled)
            return true;
        if (optimizerSchedule == null) {
            if (other.optimizerSchedule != null)
                return true;
        } else if (!optimizerSchedule.equals(other.optimizerSchedule))
            return true;

        return Double.doubleToLongBits(refreshSeconds) != Double.doubleToLongBits(other.refreshSeconds);
    }
}
