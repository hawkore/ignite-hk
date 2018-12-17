/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkore.ignite.lucene.schema.mapping;

import java.util.Date;
import java.util.Optional;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hawkore.ignite.lucene.IndexException;
import org.apache.lucene.search.SortedNumericSortField;

/**
 * A {@link Mapper} to map a long field.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class LongMapper extends SingleColumnMapper.SingleFieldMapper<Long> {

	private static final FieldType TYPE_NOT_STORED_OMMIT_NORMS = new FieldType();
	private static final FieldType TYPE_STORED_OMMIT_NORMS=new FieldType();

	static {
		TYPE_NOT_STORED_OMMIT_NORMS.setTokenized(true);
		TYPE_NOT_STORED_OMMIT_NORMS.setOmitNorms(false);
		TYPE_NOT_STORED_OMMIT_NORMS.setIndexOptions(IndexOptions.DOCS);
		TYPE_NOT_STORED_OMMIT_NORMS.setNumericType(FieldType.NumericType.LONG);
		TYPE_NOT_STORED_OMMIT_NORMS.freeze();

		TYPE_STORED_OMMIT_NORMS.setTokenized(true);
		TYPE_STORED_OMMIT_NORMS.setOmitNorms(false);
		TYPE_STORED_OMMIT_NORMS.setIndexOptions(IndexOptions.DOCS);
		TYPE_STORED_OMMIT_NORMS.setNumericType(FieldType.NumericType.LONG);
		TYPE_STORED_OMMIT_NORMS.setStored(true);
		TYPE_STORED_OMMIT_NORMS.freeze();
	}


	/** The default boost. */
	public static final Float DEFAULT_BOOST = 1.0f;

	/** The boost. */
	public final Float boost;

	/**
	 * Builds a new {@link LongMapper} using the specified boost.
	 *
	 * @param field the name of the field
	 * @param column the name of the column to be mapped
	 * @param validated if the field must be validated
	 * @param boost the boost
	 */
	public LongMapper(String field, String column, Boolean validated, Float boost) {
		super(field, column, true, validated, null, Long.class, NUMERIC_TYPES_WITH_DATE);
		this.boost = boost == null ? DEFAULT_BOOST : boost;
	}

	/** {@inheritDoc} */
	@Override
	protected Long doBase(String name, Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		} else if (value instanceof Date) {
			return ((Date) value).getTime();
		} else if (value instanceof String) {
			try {
				return Double.valueOf((String) value).longValue();
			} catch (NumberFormatException e) {
				throw new IndexException("Field '{}' with value '{}' can not be parsed as long", name, value);
			}
		}
		throw new IndexException("Field '{}' requires a long, but found '{}'", name, value);
	}

	/** {@inheritDoc} */
	@Override
	public Optional<Field> indexedField(String name, Long value) {
		LongField field = null;
		if(boost==DEFAULT_BOOST){
			field = new LongField(name, value, STORE);
		}else{
			//You cannot set an index-time boost on an unindexed field, or one that omits norms if boost!=1.0f
			FieldType type = (STORE == Field.Store.YES) ? TYPE_STORED_OMMIT_NORMS : TYPE_NOT_STORED_OMMIT_NORMS;
			field = new LongField(name, value, type);
		}
		field.setBoost(boost);
		return Optional.of(field);
	}

	/** {@inheritDoc} */
	@Override
	public Optional<Field> sortedField(String name, Long value) {
		return Optional.of(new SortedNumericDocValuesField(name, value));
	}

	/** {@inheritDoc} */
	@Override
	public SortField sortField(String name, boolean reverse) {
		return new SortedNumericSortField(name, Type.LONG, reverse);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return toStringHelper(this).add("boost", boost).toString();
	}

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((boost == null) ? 0 : boost.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        LongMapper other = (LongMapper) obj;
        if (boost == null) {
            if (other.boost != null)
                return false;
        } else if (!boost.equals(other.boost))
            return false;
        return true;
    }
	
	
}
