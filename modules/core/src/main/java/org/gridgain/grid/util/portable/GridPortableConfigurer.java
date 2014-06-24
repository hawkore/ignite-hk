/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.portable;

import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.rest.client.message.*;
import org.gridgain.grid.portable.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.util.portable.GridPortableMarshaller.*;

/**
 * Portable configurer.
 */
public class GridPortableConfigurer {
    /** */
    private final Map<Class<?>, GridPortableClassDescriptor> descByCls = new HashMap<>();

    /** */
    private final Map<DescriptorKey, GridPortableClassDescriptor> descById = new HashMap<>();

    /** */
    private final Map<Integer, GridPortableIdMapper> mappers = new HashMap<>();

    /** */
    private final String gridName;

    /**
     * @param gridName Grid name.
     */
    public GridPortableConfigurer(String gridName) {
        this.gridName = gridName;
    }

    /**
     * @param cls Class.
     * @throws GridPortableException In case of error.
     */
    public void addDescriptor(Class<?> cls, int typeId) throws GridPortableException {
        GridPortableClassDescriptor desc = new GridPortableClassDescriptor(cls, false, typeId, null, null);

        descByCls.put(cls, desc);
        descById.put(new DescriptorKey(false, typeId), desc);
    }

    /**
     * @param cls Class.
     * @param idMapper ID mapper.
     * @param serializer Serializer.
     * @throws GridPortableException In case of error.
     */
    public void addUserTypeDescriptor(Class<?> cls, @Nullable GridPortableIdMapper idMapper,
        @Nullable GridPortableSerializer serializer) throws GridPortableException {
        assert cls != null;

        Integer id = null;

        GridPortableId idAnn = cls.getAnnotation(GridPortableId.class);

        if (idAnn != null)
            id = idAnn.id();
        else if (idMapper != null)
            id = idMapper.typeId(cls.getName());

        if (id == null)
            id = cls.getSimpleName().hashCode();

        GridPortableClassDescriptor desc = new GridPortableClassDescriptor(cls, true, id, idMapper, serializer);

        descByCls.put(cls, desc);
        descById.put(new DescriptorKey(true, id), desc);
        mappers.put(id, idMapper);
    }

    /**
     * @param portableCfg Portable configuration.
     * @return Portable context.
     * @throws GridPortableException In case of error.
     */
    public GridPortableContext configure(@Nullable GridPortableConfiguration portableCfg)
        throws GridPortableException {
        addDescriptor(Byte.class, BYTE);
        addDescriptor(Short.class, SHORT);
        addDescriptor(Integer.class, INT);
        addDescriptor(Long.class, LONG);
        addDescriptor(Float.class, FLOAT);
        addDescriptor(Double.class, DOUBLE);
        addDescriptor(Character.class, CHAR);
        addDescriptor(Boolean.class, BOOLEAN);
        addDescriptor(String.class, STRING);
        addDescriptor(UUID.class, UUID);
        addDescriptor(Date.class, DATE);
        addDescriptor(byte[].class, BYTE_ARR);
        addDescriptor(short[].class, SHORT_ARR);
        addDescriptor(int[].class, INT_ARR);
        addDescriptor(long[].class, LONG_ARR);
        addDescriptor(float[].class, FLOAT_ARR);
        addDescriptor(double[].class, DOUBLE_ARR);
        addDescriptor(char[].class, CHAR_ARR);
        addDescriptor(boolean[].class, BOOLEAN_ARR);
        addDescriptor(String[].class, STRING_ARR);
        addDescriptor(UUID[].class, UUID_ARR);
        addDescriptor(Date[].class, DATE_ARR);
        addDescriptor(Object[].class, OBJ_ARR);
        addDescriptor(ArrayList.class, COL);
        addDescriptor(HashMap.class, MAP);

        // TODO: Configure from server and client?
        addDescriptor(GridClientAuthenticationRequest.class, 0x100);
        addDescriptor(GridClientCacheRequest.class, 0x101);
        addDescriptor(GridClientLogRequest.class, 0x102);
        addDescriptor(GridClientNodeBean.class, 0x103);
        addDescriptor(GridClientNodeMetricsBean.class, 0x104);
        addDescriptor(GridClientResponse.class, 0x105);
        addDescriptor(GridClientTaskRequest.class, 0x106);
        addDescriptor(GridClientTaskResultBean.class, 0x107);
        addDescriptor(GridClientTopologyRequest.class, 0x108);

        if (portableCfg != null) {
            GridPortableIdMapper globalIdMapper = portableCfg.getIdMapper();
            GridPortableSerializer globalSerializer = portableCfg.getSerializer();

            for (GridPortableTypeConfiguration typeCfg : portableCfg.getTypeConfigurations()) {
                String clsName = typeCfg.getClassName();

                if (clsName == null)
                    throw new GridPortableException("Class name is required for portable type configuration.");

                Class<?> cls;

                try {
                    cls = Class.forName(clsName);
                }
                catch (ClassNotFoundException e) {
                    throw new GridPortableException("Portable class doesn't exist: " + clsName, e);
                }

                GridPortableIdMapper idMapper = globalIdMapper;
                GridPortableSerializer serializer = globalSerializer;

                if (typeCfg.getIdMapper() != null)
                    idMapper = typeCfg.getIdMapper();

                if (typeCfg.getSerializer() != null)
                    serializer = typeCfg.getSerializer();

                addUserTypeDescriptor(cls, idMapper, serializer);
            }
        }

        return new Context(gridName);
    }

    /** */
    private class Context implements GridPortableContext, Externalizable {
        /** */
        private String gridName;

        /**
         * For {@link Externalizable}.
         */
        public Context() {
            // No-op.
        }

        /**
         * @param gridName Grid name.
         */
        private Context(@Nullable String gridName) {
            this.gridName = gridName;
        }

        /** {@inheritDoc} */
        @Nullable @Override public GridPortableClassDescriptor descriptorForClass(Class<?> cls) {
            assert cls != null;

            return descByCls.get(cls);
        }

        /** {@inheritDoc} */
        @Nullable @Override public GridPortableClassDescriptor descriptorForTypeId(boolean userType, int typeId) {
            return descById.get(new DescriptorKey(userType, typeId));
        }

        /** {@inheritDoc} */
        @Override public int fieldId(int typeId, String fieldName) {
            GridPortableIdMapper idMapper = mappers.get(typeId);

            return idMapper != null ? idMapper.fieldId(typeId, fieldName) : fieldName.hashCode();
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeString(out, gridName);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            gridName = U.readString(in);
        }

        /**
         * @return Portable context.
         * @throws ObjectStreamException In case of error.
         */
        protected Object readResolve() throws ObjectStreamException {
            try {
                GridKernal g = GridGainEx.gridx(gridName);

                return g.context().portable().portableContext();
            }
            catch (IllegalStateException e) {
                throw U.withCause(new InvalidObjectException(e.getMessage()), e);
            }
        }
    }

    /** */
    private static class DescriptorKey {
        /** */
        private final boolean userType;

        /** */
        private final int typeId;

        /**
         * @param userType User type flag.
         * @param typeId Type ID.
         */
        private DescriptorKey(boolean userType, int typeId) {
            this.userType = userType;
            this.typeId = typeId;
        }

        @Override public boolean equals(Object other) {
            if (this == other)
                return true;

            if (other == null || getClass() != other.getClass())
                return false;

            DescriptorKey key = (DescriptorKey)other;

            return userType == key.userType && typeId == key.typeId;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = userType ? 1 : 0;

            res = 31 * res + typeId;

            return res;
        }
    }
}
