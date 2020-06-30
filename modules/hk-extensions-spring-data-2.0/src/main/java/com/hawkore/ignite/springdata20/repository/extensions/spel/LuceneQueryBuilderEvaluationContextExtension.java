package com.hawkore.ignite.springdata20.repository.extensions.spel;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.repository.query.spi.EvaluationContextExtensionSupport;

/**
 * LuceneQueryBuilder EvaluationContext Extension for Spring Data 2.0
 * <p>
 * This extension provides access to advanced lucene query builder ({@link org.hawkore.ignite.lucene.builder.Builder})
 * through SpEl expressions into your {@code @Query} definitions.
 * <p>
 * First, you need to register this extension into your spring data configuration. Sample:
 * <pre>
 * {@code @Configuration}
 * {@code @EnableIgniteRepositories}(basePackages = ... )
 * public class MyIgniteRepoConfig {
 * ...
 *      {@code @Bean}
 *      public EvaluationContextExtension luceneQueryBuilderExtension() {
 *          return new LuceneQueryBuilderEvaluationContextExtension();
 *      }
 * ...
 * }
 * </pre>
 *
 * <p>
 * Sample of usage into your {@code @Query} definitions:
 * <pre>
 * {@code @RepositoryConfig}(cacheName = "users")
 * public interface UserRepository
 * extends IgniteRepository<User, UUID>{
 *     [...]
 *
 *     {@code @Query}(value = "SELECT * from #{#entityName} where lucene = ?#{
 *     luceneQueryBuilder.search().refresh(true).filter(luceneQueryBuilder.match('email',#email)).build()}")
 *     User searchUserByEmail(@Param("email") String email);
 *
 *
 *     {@code @Query}(value = "lucene = ?#{
 *     luceneQueryBuilder.search().refresh(true).filter(luceneQueryBuilder.match('city',#city)).build()}")
 *     List<User> searchUsersByCity(@Param("city") String city, Pageable pageable);
 *
 *
 *     {@code @Query}(textQuery = true, value = "#{luceneQueryBuilder.search().refresh(true).filter
 *     (luceneQueryBuilder.match('city', #city)).build()}")
 *     List<User> searchUsersByCity({@code @Param}("city") String city, Pageable pageable);
 *      [...]
 *     }
 * </pre>
 * <p>
 * Visit <a href="https://docs.hawkore.com/private/apache-ignite-advanced-indexing">Apache Ignite advanced Indexing
 * Documentation site</a> for more info about Advanced Lucene Index and Lucene Query Builder.
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
public class LuceneQueryBuilderEvaluationContextExtension extends EvaluationContextExtensionSupport {

    private static final Map<String, Object> properties = new HashMap<>();
    private static final LuceneQueryBuilder LUCENE_QUERY_BUILDER_INSTANCE = new LuceneQueryBuilder();
    private static final String LUCENE_QUERY_BUILDER_SPEL_VAR = "luceneQueryBuilder";

    static {
        properties.put(LUCENE_QUERY_BUILDER_SPEL_VAR, LUCENE_QUERY_BUILDER_INSTANCE);
    }

    @Override
    public String getExtensionId() {
        return "HK-LUCENE-QUERY-BUILDER-SPRING-DATA-EXTENSION";
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    /*
     * just to access @{link org.hawkore.ignite.lucene.builder.Builder}
     */
    static class LuceneQueryBuilder extends org.hawkore.ignite.lucene.builder.Builder {}

}
