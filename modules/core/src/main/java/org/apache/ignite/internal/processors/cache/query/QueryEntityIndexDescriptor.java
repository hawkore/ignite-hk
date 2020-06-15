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

package org.apache.ignite.internal.processors.cache.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.cache.CacheException;

import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.query.annotations.QueryTextField;
import org.apache.ignite.internal.processors.query.GridQueryIndexDescriptor;
import org.apache.ignite.internal.processors.query.h2.opt.lucene.LuceneQueryUtils;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Index descriptor.
 */
public class QueryEntityIndexDescriptor implements GridQueryIndexDescriptor {
    /** Fields sorted by order number. */
    private final Collection<T2<String, Integer>> fields = new TreeSet<>(
        new Comparator<T2<String, Integer>>() {
            @Override public int compare(T2<String, Integer> o1, T2<String, Integer> o2) {
                if (o1.get2().equals(o2.get2())) // Order is equal, compare field names to avoid replace in Set.
                    return o1.get1().compareTo(o2.get1());

                return o1.get2() < o2.get2() ? -1 : 1;
            }
        });

    /** */
    private final QueryIndexType type;

    /** */
    private final int inlineSize;

    /** Fields which should be indexed in descending order. */
    private Collection<String> descendings;

    private String luceneIndexOptions;
    
    /** QueryTextField at type level */
    private List<QueryTextField> typeTextAnnotations;
    
    /** QueryTextField annotations at field level */
    private Map<String, List<QueryTextField>> fieldTextAnnotations;
    
    /**
     * @param type Type.
     * @param inlineSize Inline size.
     */
    QueryEntityIndexDescriptor(QueryIndexType type, int inlineSize) {
        assert type != null;

        this.type = type;
        this.inlineSize = inlineSize;
    }

    /**
     * @param type Type.
     */
    QueryEntityIndexDescriptor(QueryIndexType type) {
        this(type, -1);
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public Collection<String> fields() {
        Collection<String> res = new ArrayList<>(fields.size());

        for (T2<String, Integer> t : fields)
            res.add(t.get1());

        return res;
    }

    /** {@inheritDoc} */
    @Override public boolean descending(String field) {
        return descendings != null && descendings.contains(field);
    }

    /**
     * Adds field to this index.
     *
     * @param field Field name.
     * @param orderNum Field order number in this index.
     * @param descending Sort order.
     */
    public void addField(String field, int orderNum, boolean descending, List<QueryTextField> ann) {
        fields.add(new T2<>(field, orderNum));

        if (descending) {
            if (descendings == null)
                descendings = new HashSet<>();

            descendings.add(field);
        }
        
		if (ann!=null){
			if (getFieldTextAnnotations().containsKey(field))
				throw new CacheException("Duplicate text QueryTextField for field: " + field+" on type "+this.name());
			getFieldTextAnnotations().put(field, ann);
        }
    }

    /** {@inheritDoc} */
    @Override public QueryIndexType type() {
        return type;
    }

    /** {@inheritDoc} */
    @Override public int inlineSize() {
        return inlineSize;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(QueryEntityIndexDescriptor.class, this);
    }

    public List<QueryTextField> getTypeTextAnnotations() {
        if(typeTextAnnotations==null){
            typeTextAnnotations= new ArrayList<>();
        }
        return typeTextAnnotations;
    }


    public void setTypeTextAnnotations(List<QueryTextField> typeTextAnnotations) {
        this.typeTextAnnotations = typeTextAnnotations;   
    }


    public Map<String, List<QueryTextField>> getFieldTextAnnotations() {
        if(fieldTextAnnotations==null){
            fieldTextAnnotations= new HashMap<>();
        }
        return fieldTextAnnotations;
    }

    public void setFieldTextAnnotations(Map<String, List<QueryTextField>> fieldTextAnnotations) {
        this.fieldTextAnnotations = fieldTextAnnotations;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setLuceneIndexOptions(String luceneIndexOptions) {
       this.luceneIndexOptions = luceneIndexOptions;
    }

    /** {@inheritDoc} */
    @Override
    public String luceneIndexOptions() {
        if (this.luceneIndexOptions == null){
            this.luceneIndexOptions = LuceneQueryUtils.luceneIndex(null, null, this.getTypeTextAnnotations(), this.getFieldTextAnnotations()).build();
        }
        return this.luceneIndexOptions;
    }
}
