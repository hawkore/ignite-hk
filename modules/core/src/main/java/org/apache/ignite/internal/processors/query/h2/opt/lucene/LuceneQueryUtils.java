package org.apache.ignite.internal.processors.query.h2.opt.lucene;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.annotations.QueryTextField;
import org.apache.ignite.cache.query.annotations.QueryTextField.ClasspathAnalyzer;
import org.apache.ignite.cache.query.annotations.QueryTextField.SnowballAnalyzer;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.lang.IgniteBiTuple;
import org.hawkore.ignite.lucene.IndexException;
import org.hawkore.ignite.lucene.builder.Builder;
import org.hawkore.ignite.lucene.builder.common.GeoTransformation;
import org.hawkore.ignite.lucene.builder.index.Index;
import org.hawkore.ignite.lucene.builder.index.Partitioner.None;
import org.hawkore.ignite.lucene.builder.index.Partitioner.OnToken;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.BigDecimalMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.BigIntegerMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.BitemporalMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.BlobMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.BooleanMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.DateMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.DateRangeMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.DoubleMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.FloatMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.GeoPointMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.GeoShapeMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.InetMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.IntegerMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.LongMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.StringMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.TextMapper;
import org.hawkore.ignite.lucene.builder.index.schema.mapping.UUIDMapper;

/**
 * 
 * LuceneQueryUtils
 *
 * @author Manuel Núñez (manuel.nunez@hawkore.com)
 *
 *
 */
public class LuceneQueryUtils {

    /**
     * 
     */
    private LuceneQueryUtils() {
        // just hidden
    }

    /**
     * BYTES_ARRAY_CLASS
     */
    public static final Class<?> BYTES_ARRAY_CLASS = (new byte[] {}).getClass();

    /**
     * Builds QueryEntity from key and value classes
     * 
     * @param keyCls
     * @param valCls
     * @return QueryEntity
     */
    public static QueryEntity getQueryEntityFromKeyAndValueClasses(
        Class<?> keyCls,
        Class<?> valCls) {
        return QueryEntity.getQueryEntityFromKeyAndValueClasses(keyCls, valCls);
    }

    /**
     * Builds index options from Index
     * 
     * @param idx
     * @return IndexOptions
     */
    public static IndexOptions indexOptions(Index idx) {
        return indexOptions(idx.build());
    }

    /**
     * Builds index options from json string
     * 
     * @param jsonIndexOptions
     * 
     * @return IndexOptions
     */
    public static IndexOptions indexOptions(String jsonIndexOptions) {
        return new IndexOptions(jsonIndexOptions);
    }

    /**
     * Build lucene index definition by extended {@link QueryTextField}
     * annotation
     * 
     * @param keyspace
     * @param tableName
     * @param valueTextIndex
     * @param textFields
     * @return Index definition
     */
    public static Index luceneIndex(String keyspace, String tableName, List<QueryTextField> valueTextIndex,
        Map<String, List<QueryTextField>> textFields) {

        Index idx = Builder.index(tableName);

        // process annotations at class level
        if (valueTextIndex != null) {
            // apply index configuration
            valueTextIndex.stream().filter(ann -> processIndexOptionsAnnotation(idx, ann)).findFirst();

            valueTextIndex.forEach(ann -> processIndexAnnotation(QueryUtils.TYPE_ANNOTATION_SUFFIX, idx, ann));
        }

        // process annotations at field level
        if (textFields != null && !textFields.isEmpty()) {
            textFields.forEach(
                (nestedfieldname, anns) -> anns.forEach(ann -> processIndexAnnotation(nestedfieldname, idx, ann)));
        }

        // add index keyspace (schema)
        idx.keyspace(keyspace);

        return idx;
    }

    /*
     * WHEN need to add or change properties on QueryTextField.IndexOptions:
     * 
     * <pre> -> add new configuration property to
     * org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions ->
     * add new configuration property to
     * com.hawkore.ignite.internal.processors.query.h2.opt.lucene.IndexOptions
     * -> add new configuration property to
     * org.hawkore.ignite.lucene.builder.index.Index </pre>
     * 
     */
    private static boolean processIndexOptionsAnnotation(Index idx, QueryTextField ann) {

        if (ann.indexOptions().length > 0) {

            org.apache.ignite.cache.query.annotations.QueryTextField.IndexOptions indexOptions = ann.indexOptions()[0];

            idx.version(indexOptions.version())
                .maxCachedMb(indexOptions.maxCachedMB())
                .ramBufferMb(indexOptions.ramBufferMB())
                .refreshSeconds(indexOptions.refreshSeconds())
                .optimizerEnabled(indexOptions.optimizerEnabled())
                .optimizerSchedule(indexOptions.optimizerSchedule())
                .directoryPath(indexOptions.directoryPath());

            if (indexOptions.partitions() > 1) {
                idx.partitioner(new OnToken(indexOptions.partitions()));
            } else {
                idx.partitioner(new None());
            }

            idx.defaultAnalyzer(indexOptions.defaultAnalyzer());

            // add custom text "classpath" analyzers
            IntStream.range(0, indexOptions.classpathAnalyzers().length).forEach(i -> {
                ClasspathAnalyzer analizer = indexOptions.classpathAnalyzers()[i];
                idx.analyzer(analizer.name(),
                    new org.hawkore.ignite.lucene.builder.index.schema.analysis.ClasspathAnalyzer(
                        analizer.className()));
            });

            // add custom text "snowball" analyzers
            IntStream.range(0, indexOptions.snowballAnalyzers().length).forEach(i -> {
                SnowballAnalyzer analizer = indexOptions.snowballAnalyzers()[i];
                org.hawkore.ignite.lucene.builder.index.schema.analysis.SnowballAnalyzer snow = new org.hawkore.ignite.lucene.builder.index.schema.analysis.SnowballAnalyzer(
                    analizer.language());
                if (analizer.stopwords() != null && analizer.stopwords().length() > 0) {
                    snow.stopwords(analizer.stopwords());
                }
                idx.analyzer(analizer.name(), snow);
            });

            return true;
        }

        return false;
    }

    private static void processIndexAnnotation(String nestedPropertyName, Index idx, QueryTextField ann) {

        IntStream.range(0, ann.bigDecimalMappers().length).forEach(i -> {
            IgniteBiTuple<String, BigDecimalMapper> biMapper = mapper(nestedPropertyName, ann.bigDecimalMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.bigIntegerMappers().length).forEach(i -> {
            IgniteBiTuple<String, BigIntegerMapper> biMapper = mapper(nestedPropertyName, ann.bigIntegerMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.bitemporalMappers().length).forEach(i -> {
            IgniteBiTuple<String, BitemporalMapper> biMapper = mapper(nestedPropertyName, ann.bitemporalMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.blobMappers().length).forEach(i -> {
            IgniteBiTuple<String, BlobMapper> biMapper = mapper(nestedPropertyName, ann.blobMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.booleanMappers().length).forEach(i -> {
            IgniteBiTuple<String, BooleanMapper> biMapper = mapper(nestedPropertyName, ann.booleanMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.dateMappers().length).forEach(i -> {
            IgniteBiTuple<String, DateMapper> biMapper = mapper(nestedPropertyName, ann.dateMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.dateRangeMappers().length).forEach(i -> {
            IgniteBiTuple<String, DateRangeMapper> biMapper = mapper(nestedPropertyName, ann.dateRangeMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.doubleMappers().length).forEach(i -> {
            IgniteBiTuple<String, DoubleMapper> biMapper = mapper(nestedPropertyName, ann.doubleMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.floatMappers().length).forEach(i -> {
            IgniteBiTuple<String, FloatMapper> biMapper = mapper(nestedPropertyName, ann.floatMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.geoPointMappers().length).forEach(i -> {
            IgniteBiTuple<String, GeoPointMapper> biMapper = mapper(nestedPropertyName, ann.geoPointMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.geoShapeMappers().length).forEach(i -> {
            IgniteBiTuple<String, GeoShapeMapper> biMapper = mapper(nestedPropertyName, ann.geoShapeMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.inetMappers().length).forEach(i -> {
            IgniteBiTuple<String, InetMapper> biMapper = mapper(nestedPropertyName, ann.inetMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.integerMappers().length).forEach(i -> {
            IgniteBiTuple<String, IntegerMapper> biMapper = mapper(nestedPropertyName, ann.integerMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.longMappers().length).forEach(i -> {
            IgniteBiTuple<String, LongMapper> biMapper = mapper(nestedPropertyName, ann.longMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.stringMappers().length).forEach(i -> {
            IgniteBiTuple<String, StringMapper> biMapper = mapper(nestedPropertyName, ann.stringMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.textMappers().length).forEach(i -> {
            IgniteBiTuple<String, TextMapper> biMapper = mapper(nestedPropertyName, ann.textMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
        IntStream.range(0, ann.uuidMappers().length).forEach(i -> {
            IgniteBiTuple<String, UUIDMapper> biMapper = mapper(nestedPropertyName, ann.uuidMappers()[i]);
            idx.mapper(biMapper.getKey(), biMapper.getValue());
        });
    }

    /**
     * SQL column to map
     * 
     * @param defaultNestedPropertyName
     * @param column
     *            - name of mapped column, comes from mapper annotation column
     *            parameter
     * @param required
     *            - if column name is required
     * 
     * @return SQL column to map
     */
    private static String columnName(String defaultNestedPropertyName, String column, boolean required) {

        boolean isTypeLevelAnnotation = defaultNestedPropertyName.contains(QueryUtils.TYPE_ANNOTATION_SUFFIX);
        String realDefaultNestedPropertyName = isTypeLevelAnnotation
            ? defaultNestedPropertyName.replace(QueryUtils.TYPE_ANNOTATION_SUFFIX, "")
            : defaultNestedPropertyName;

        if (required && StringUtils.isBlank(column)) {
            throw new IndexException(
                "column name parameter must not be null on mapper definition");
        }

        if (StringUtils.isBlank(column)) {
            return null;
        }

        // if field is provided is a replacement for
        // last part of realDefaultNestedPropertyName
        String[] nestedColumnNames = realDefaultNestedPropertyName.split("\\.");

        nestedColumnNames[nestedColumnNames.length - 1] = column;

        return StringUtils.join(nestedColumnNames, ".");
    }

    /**
     * Mapper's name - Lucene Document's field name
     * 
     * @param defaultNestedPropertyName
     *            - contains real nested QueryEntity field name:
     *            prop1.prop2.propN
     * @param name
     *            - name of lucene field, comes from mapper annotation field
     *            parameter. Whether not provided and not required, default
     *            value will be defaultNestedPropertyName
     * 
     * @param required
     *            - if mapper name is required
     * 
     * @return lucene field name
     */
    private static String mapperName(String defaultNestedPropertyName, String name, boolean required) {

        // defaultNestedPropertyName may contains
        // QueryUtils.TYPE_ANNOTATION_SUFFIX... so
        // we need to extract real property by removing it and apply
        // field
        boolean isTypeLevelAnnotation = defaultNestedPropertyName.contains(QueryUtils.TYPE_ANNOTATION_SUFFIX);
        String realDefaultNestedPropertyName = isTypeLevelAnnotation
            ? defaultNestedPropertyName.replace(QueryUtils.TYPE_ANNOTATION_SUFFIX, "")
            : defaultNestedPropertyName;

        if ((isTypeLevelAnnotation || required) && StringUtils.isBlank(name)) {
            // nestedPropertyName is parent name, so we need to append to
            // defined field on annotation ... if not found throws exception
            throw new IndexException(
                "field parameter must not be null on mapper definition");
        }

        // if not provided, lucene field will be real realNestedFieldName
        if (StringUtils.isBlank(name)) {
            return realDefaultNestedPropertyName;
        }

        // if field is provided is a replacement for
        // last part of realDefaultNestedPropertyName
        String[] nestedColumnNames = realDefaultNestedPropertyName.split("\\.");

        nestedColumnNames[nestedColumnNames.length - 1] = name;

        return StringUtils.join(nestedColumnNames, ".");
    }

    private static IgniteBiTuple<String, BigDecimalMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.BigDecimalMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.bigDecimalMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false))
                .decimalDigits(ann.decimal_digits()).integerDigits(ann.integer_digits()));
    }

    private static IgniteBiTuple<String, BigIntegerMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.BigIntegerMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.bigIntegerMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false))
                .digits(ann.digits()));
    }

    private static IgniteBiTuple<String, BitemporalMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.BitemporalMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), true),
            Builder.bitemporalMapper(
                columnName(nestedPropertyName, ann.vt_from(), true),
                columnName(nestedPropertyName, ann.vt_to(), true),
                columnName(nestedPropertyName, ann.tt_from(), true),
                columnName(nestedPropertyName, ann.tt_to(), true))
                .nowValue(columnName(nestedPropertyName, ann.now_value(), true))
                .pattern(ann.pattern()));
    }

    private static IgniteBiTuple<String, BlobMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.BlobMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.blobMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false)));
    }

    private static IgniteBiTuple<String, BooleanMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.BooleanMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.booleanMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false)));
    }

    private static IgniteBiTuple<String, DateMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.DateMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.dateMapper().validated(ann.validated()).column(columnName(nestedPropertyName, ann.column(), false))
                .pattern(ann.pattern()));
    }

    private static IgniteBiTuple<String, DateRangeMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.DateRangeMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), true),
            Builder.dateRangeMapper(
                columnName(nestedPropertyName, ann.from(), true),
                columnName(nestedPropertyName, ann.to(), true)).validated(ann.validated()).pattern(ann.pattern()));
    }

    private static IgniteBiTuple<String, DoubleMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.DoubleMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.doubleMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false))
                .boost(ann.boost()));
    }

    private static IgniteBiTuple<String, FloatMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.FloatMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.floatMapper().validated(ann.validated()).column(columnName(nestedPropertyName, ann.column(), false))
                .boost(ann.boost()));
    }

    private static IgniteBiTuple<String, GeoPointMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.GeoPointMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), true),
            Builder.geoPointMapper(
                columnName(nestedPropertyName, ann.latitude(), true),
                columnName(nestedPropertyName, ann.longitude(), true)));
    }

    private static IgniteBiTuple<String, GeoShapeMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.GeoShapeMapper ann) {
        GeoShapeMapper mapper = Builder.geoShapeMapper().validated(ann.validated())
            .column(columnName(nestedPropertyName, ann.column(), false)).maxLevels(ann.max_levels());

        if (ann.transformations() != null && ann.transformations().length > 0) {
            GeoTransformation[] transformations = new GeoTransformation[ann.transformations().length];

            for (int i = 0; i < ann.transformations().length; i++) {
                org.apache.ignite.cache.query.annotations.QueryTextField.GeoShapeMapper.GeoTransformation transform = ann
                    .transformations()[i];
                switch (transform.type()) {
                case BBOX:
                    transformations[i] = Builder.bbox();
                    break;
                case BUFFER:
                    transformations[i] = Builder.buffer().maxDistance(transform.max_distance())
                        .minDistance(transform.min_distance());
                    break;
                case CENTROID:
                    transformations[i] = Builder.centroid();
                    break;
                case CONVEX_HULL:
                    transformations[i] = Builder.convexHull();
                    break;
                default:
                    break;
                }
            }
            mapper.transform(transformations);
        }
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            mapper);
    }

    private static IgniteBiTuple<String, InetMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.InetMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.inetMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false)));
    }

    private static IgniteBiTuple<String, IntegerMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.IntegerMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.integerMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false))
                .boost(ann.boost()));
    }

    private static IgniteBiTuple<String, LongMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.LongMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.longMapper().validated(ann.validated()).column(columnName(nestedPropertyName, ann.column(), false))
                .boost(ann.boost()));
    }

    private static IgniteBiTuple<String, StringMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.StringMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.stringMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false))
                .caseSensitive(ann.case_sensitive()));
    }

    private static IgniteBiTuple<String, TextMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.TextMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.textMapper().validated(ann.validated()).column(columnName(nestedPropertyName, ann.column(), false))
                .analyzer(StringUtils.defaultIfBlank(ann.analyzer(), null)));
    }

    private static IgniteBiTuple<String, UUIDMapper> mapper(String nestedPropertyName,
        org.apache.ignite.cache.query.annotations.QueryTextField.UUIDMapper ann) {
        return new IgniteBiTuple<>(mapperName(nestedPropertyName, ann.name(), false),
            Builder.uuidMapper().validated(ann.validated())
                .column(columnName(nestedPropertyName, ann.column(), false)));
    }
}
