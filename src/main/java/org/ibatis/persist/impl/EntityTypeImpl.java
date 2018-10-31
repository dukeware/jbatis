package org.ibatis.persist.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ibatis.cglib.ClassInfo;
import org.ibatis.cglib.Invoker;
import org.ibatis.persist.Cacheable;
import org.ibatis.persist.Column;
import org.ibatis.persist.Entity;
import org.ibatis.persist.Id;
import org.ibatis.persist.IdClass;
import org.ibatis.persist.PersistenceException;
import org.ibatis.persist.Table;
import org.ibatis.persist.Transient;
import org.ibatis.persist.meta.Attribute;
import org.ibatis.persist.meta.EntityType;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;

@SuppressWarnings("unchecked")
public class EntityTypeImpl<E> implements EntityType<E> {
    private static final ILog log = ILogFactory.getLog(EntityTypeImpl.class);
    EntityManager entityManager;
    Class<E> entityClass;
    String entityName;
    String namespace;
    String tableName;
    String tableQName;
    Class<?> idClass;
    Attribute<E, ?> idAttr;
    boolean cacheable;
    String cacheType;
    int cacheMinutes;
    Set<Class<?>> cacheRoots = new HashSet<Class<?>>();
    Exception error;
    private Map<String, Attribute<E, ?>> attributes = new LinkedHashMap<String, Attribute<E, ?>>();
    private Map<String, Attribute<E, ?>> keys = new LinkedHashMap<String, Attribute<E, ?>>();

    private List<String> attrNames = new ArrayList<String>();

    public EntityTypeImpl(Class<E> entityClass, EntityManager entityManager) {
        this.entityManager = entityManager;
        if (entityClass == null) {
            setError(new PersistenceException("entity class is null."));
            return;
        }
        if (entityClass.getEnclosingClass() != null) {
            setError(new PersistenceException("entity " + entityClass + " is not top-level."));
            return;
        }
        Constructor<E> con = null;
        try {
            con = entityClass.getDeclaredConstructor();
        } catch (Exception e) {
        }
        if (con == null || !Modifier.isPublic(con.getModifiers())) {
            setError(new PersistenceException("entity " + entityClass + " has no public default constructor."));
            return;
        }
        Entity en = entityClass.getAnnotation(Entity.class);
        if (en == null) {
            setError(new PersistenceException("entity " + entityClass + " has no @Entity annotation."));
            return;
        }
        Table tb = entityClass.getAnnotation(Table.class);
        if (tb == null) {
            setError(new PersistenceException("entity " + entityClass + " has no @Table annotation."));
            return;
        }
        this.entityClass = entityClass;
        IdClass idc = entityClass.getAnnotation(IdClass.class);
        if (idc != null) {
            idClass = idc.value();
        }
        entityName = en.name();
        if (entityName.isEmpty()) {
            entityName = entityClass.getSimpleName();
        }

        namespace = en.namespace();
        if (namespace.isEmpty()) {
            namespace = entityClass.getPackage().getName();
        }

        Cacheable ca = entityClass.getAnnotation(Cacheable.class);
        if (ca != null) {
            cacheable = ca.value();
        }
        if (cacheable) {
            cacheType = ca.type();
            cacheMinutes = ca.minutes();
            for (Class<?> cr : ca.roots()) {
                cacheRoots.add(cr);
            }
        }

        tableName = tb.name();
        if (tableName.isEmpty()) {
            tableName = entityName;
        }
        tableQName = tableName;

        if (!tb.schema().isEmpty()) {
            tableQName = tb.schema() + "." + tableQName;
        }

        try {
            ClassInfo ci = ClassInfo.getInstance(entityClass, true);
            List<String> attrs = ci.getPropertyNames();
            for (String attr : attrs) {
                Class<?> type = ci.getGetterType(attr);
                Invoker getter = ci.getGetInvoker(attr);
                Invoker setter = ci.getSetInvoker(attr);

                Column c = getter.getAnnotation(Column.class);
                if (c == null) {
                    c = setter.getAnnotation(Column.class);
                }
                if (c == null) {
                    Transient t = getter.getAnnotation(Transient.class);
                    if (t == null) {
                        t = setter.getAnnotation(Transient.class);
                    }
                    if (t != null) {
                        continue;
                    }
                }

                String colName = attr;
                if (c != null && !c.name().isEmpty()) {
                    colName = c.name();
                }
                if (entityManager.getDelegate().getTypeHandlerFactory().getTypeHandler(type) == null) {
                    if (c != null) {
                        setError(new PersistenceException(
                            "Attr '" + attr + "' type '" + type + "' is illegal in entity class: " + entityClass));
                        return;
                    } else {
                        log.error("Attr '" + attr + "' ignored in entity class: " + entityClass
                            + " because of bad type: " + type);
                        continue;
                    }
                }
                Attribute<E, ?> pa = new Attribute(attr, colName, type, getter, setter);
                Id id = pa.getAnnotation(Id.class);
                if (id == null) {
                    attributes.put(attr, pa);
                    attrNames.add(attr);
                } else {
                    if (!PrimaryKeyTypes.contains(type)) {
                        setError(new PersistenceException("Attr '" + attr + "' type '" + type
                            + "' is illegal @Id type in entity class: " + entityClass));
                        return;
                    }
                    idAttr = pa;
                    keys.put(attr, pa);
                    attributes.put(attr, pa);
                    attrNames.add(attr);
                }
            }
        } catch (Exception e) {
            log.error("Failed to init entity class: " + entityClass + ", " + e, e);
            setError(new PersistenceException("Failed to init entity class: " + entityClass + ", " + e, e));
            return;
        }

        if (attributes.isEmpty()) {
            setError(new PersistenceException("entity " + entityClass + " has no attr with @Column annotation."));
            return;
        }
        if (keys.size() > 1) {
            if (idClass == null) {
                setError(new PersistenceException("entity " + entityClass + " have multiple Id but without @IdClass."));
                return;
            } else {
                List<String> list = null;
                try {
                    list = ClassInfo.getInstance(idClass, true).getPropertyNames();
                } catch (Exception e) {
                    setError(new PersistenceException(
                        "entity " + entityClass + " has bad IdClass:" + idClass.getName() + ", " + e.getMessage()));
                    return;
                }
                for (String keyName : keys.keySet()) {
                    if (!list.contains(keyName)) {
                        setError(new PersistenceException(
                            "entity " + entityClass + " has bad IdClass:" + idClass.getName()));
                        return;
                    }
                }
            }
        }
    }

    public void setError(Exception error) {
        this.error = error;
        // log.error(error.getMessage(), error);
    }

    public boolean isFailed() {
        return error != null;
    }

    public Map<String, Attribute<E, ?>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Map<String, Attribute<E, ?>> getIdAttributes() {
        return Collections.unmodifiableMap(keys);
    }

    public String getResourceLocation() {
        return "ibatis-entity/" + entityClass.getName() + ".xml";
    }

    public String getErrorMessage() {
        if (error != null) {
            return error.getMessage() != null ? error.getMessage() : error.toString();
        }
        return null;
    }

    public String getEntityQName() {
        return namespace + "." + entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public String getInsertStatementId() {
        return entityName + "#insert";
    }

    public Object getInsertParameter(E e) {
        return e;
    }

    public String getUpdateStatementId() {
        return entityName + "#update";
    }

    public Object getUpdateParameter(E e) {
        return e;
    }

    public String getDeleteStatementId() {
        return entityName + "#delete";
    }

    public Object getDeleteParameter(Object key) {
        if (keys.isEmpty()) {
            throw new UnsupportedOperationException("entity " + entityClass + " has no attr with @Id annotation.");
        }
        if (idClass != null && idClass.isInstance(key)) {
            return key;
        } else if (keys.size() == 1) {
            return key;
        }
        throw new IllegalArgumentException("bad id " + key + " for entity " + entityClass);
    }

    public String getFindStatementId() {
        return entityName + "#find";
    }

    public String getEntityCacheModelId() {
        return entityName + "#cache";
    }

    public Object getFindParameter(Object key) {
        if (keys.isEmpty()) {
            throw new UnsupportedOperationException("entity " + entityClass + " has no attr with @Id annotation.");
        }
        if (idClass != null && idClass.isInstance(key)) {
            return key;
        } else if (keys.size() == 1) {
            return key;
        }
        throw new IllegalArgumentException("bad id " + key + " for entity " + entityClass);
    }

    public String buildSqlMapXml() {
        StringBuilder pw = new StringBuilder(4096);
        pw.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        pw.append("<!DOCTYPE sqlMap PUBLIC '-//iBATIS.org//DTD SQL Map 2.4//EN' 'sql-map-2.4.dtd'>\n");
        pw.append("\n");
        pw.append("<sqlMap namespace='").append(namespace).append("'>\n");
        buildInsertXml(pw);
        buildUpdateXml(pw);
        buildDeleteXml(pw);
        buildFindXml(pw);
        pw.append("</sqlMap>");
        return pw.toString();
    }

    void buildInsertXml(StringBuilder pw) {
        Attribute<E, ?> genKey = null;
        if (idAttr != null) {
            if (idAttr.getAnnotation(Id.class).auto()) {
                genKey = idAttr;
            }
        }
        pw.append("\n");
        pw.append("  <insert id='").append(getInsertStatementId()).append("' parameterClass='").append(entityClass.getName()).append("'>\n");
        pw.append("    INSERT INTO ").append(tableQName).append(" (");
        boolean first = true;
        for (Attribute<E, ?> pa : attributes.values()) {
            if (genKey == pa) {
                continue;
            }
            if (first) {
                pw.append("\n");
            } else {
                pw.append(",\n");
            }
            pw.append("      ").append(pa.getColumn());
            first = false;
        }
        pw.append("\n");
        pw.append("    ) VALUES (");
        first = true;
        for (Attribute<E, ?> pa : attributes.values()) {
            if (genKey == pa) {
                continue;
            }
            if (first) {
                pw.append("\n");
            } else {
                pw.append(",\n");
            }
            pw.append("      #").append(pa.getName()).append("#");
            first = false;
        }
        pw.append("\n");
        pw.append("    )\n");
        if (genKey != null) {
            String rc = genKey.getType().getName();
            pw.append("    <selectKey resultClass='").append(rc).append("' keyProperty='").append(genKey.getName()).append("'/>\n");
        }
        pw.append("  </insert>\n");
    }

    void buildUpdateXml(StringBuilder pw) {
        if (keys.isEmpty()) {
            return;
        }
        pw.append("\n");
        pw.append("  <update id='").append(getUpdateStatementId()).append("' parameterClass='").append(entityClass.getName()).append("'>\n");
        pw.append("    UPDATE ").append(tableQName).append(" SET");
        boolean first = true;
        for (Attribute<E, ?> pa : attributes.values()) {
            if (keys.containsValue(pa)) {
                continue;
            }
            if (first) {
                pw.append("\n");
            } else {
                pw.append(",\n");
            }
            pw.append("      ").append(pa.getColumn()).append(" = #").append(pa.getName()).append("#");
            first = false;
        }
        pw.append("\n");
        pw.append("    WHERE ");
        first = true;
        for (Attribute<E, ?> pa : keys.values()) {
            if (!first) {
                pw.append("\n");
                pw.append("      AND ");
            }
            pw.append(pa.getColumn()).append(" = #").append(pa.getName()).append("#\n");
            first = false;
        }
        pw.append("\n");
        pw.append("  </update>\n");
    }

    void buildDeleteXml(StringBuilder pw) {
        if (keys.isEmpty()) {
            return;
        }
        pw.append("\n");
        if (idClass != null) {
            pw.append("  <delete id='").append(getDeleteStatementId()).append("' parameterClass='").append(idClass.getName()).append("'>\n");
        } else {
            pw.append(
                "  <delete id='").append(getDeleteStatementId()).append("' parameterClass='").append(idAttr.getType().getName()).append("'>\n");
        }
        pw.append("    DELETE FROM ").append(tableQName);
        pw.append("\n");
        pw.append("    WHERE ");
        boolean first = true;
        for (Attribute<E, ?> pa : keys.values()) {
            if (!first) {
                pw.append("\n");
                pw.append("      AND ");
            }
            pw.append(pa.getColumn()).append(" = #").append(pa.getName()).append("#");
            first = false;
        }
        pw.append("\n");
        pw.append("  </delete>\n");
    }

    void buildFindXml(StringBuilder pw) {
        if (keys.isEmpty()) {
            return;
        }
        if (cacheable) {
            pw.append("\n");
            pw.append("  <cacheModel id='").append(getEntityCacheModelId()).append("' type='").append(cacheType).append("'>\n");
            pw.append("    <flushInterval minutes='").append(cacheMinutes).append("' />\n");
            pw.append("    <flushOnExecute statement='").append(getInsertStatementId()).append("' />\n");
            pw.append("    <flushOnExecute statement='").append(getDeleteStatementId()).append("' />\n");
            pw.append("    <flushOnExecute statement='").append(getUpdateStatementId()).append("' />\n");
            for (Class<?> clazz : cacheRoots) {
                pw.append("    <flushOnFlash entityClass='").append(clazz.getName()).append("' />\n");
            }
            pw.append("  </cacheModel>\n");
        }
        pw.append("\n");
        if (idClass != null) {
            pw.append("  <select id='").append(getFindStatementId()).append("' ");
            if (cacheable) {
                pw.append("cacheModel='").append(getEntityCacheModelId()).append("' ");
            }
            pw.append("parameterClass='").append(idClass.getName()).append("' resultClass='")
                .append(entityClass.getName()).append("'>\n");
        } else {
            pw.append("  <select id='").append(getFindStatementId()).append("' ");
            if (cacheable) {
                pw.append("cacheModel='").append(getEntityCacheModelId()).append("' ");
            }
            pw.append("parameterClass='").append(idAttr.getType().getName()).append("' resultClass='")
                .append(entityClass.getName()).append("'>\n");
        }
        pw.append("    SELECT ");
        boolean first = true;
        for (Attribute<E, ?> pa : attributes.values()) {
            if (first) {
                pw.append("\n");
            } else {
                pw.append(",\n");
            }
            pw.append("      ").append(pa.getColumn()).append(" AS ").append(pa.getName());
            first = false;
        }
        pw.append("\n");
        pw.append("    FROM ").append(tableQName);
        pw.append("\n");
        pw.append("    WHERE ");
        first = true;
        for (Attribute<E, ?> pa : keys.values()) {
            if (!first) {
                pw.append("\n");
                pw.append("      AND ");
            }
            pw.append(pa.getColumn()).append(" = #").append(pa.getName()).append("#");
            first = false;
        }
        pw.append("\n");
        pw.append("  </select>\n");
    }

    @Override
    public String getName() {
        return entityName;
    }

    @Override
    public Class<E> getJavaType() {
        return entityClass;
    }

    @Override
    public Attribute<E, ?> locateAttribute(String name) {
        Attribute<E, ?> a = attributes.get(name);
        if (a == null) {
            for (String n : attributes.keySet()) {
                if (n.equalsIgnoreCase(name)) {
                    return attributes.get(n);
                }
            }
        }
        return a;
    }

    @Override
    public List<String> getAttributeNames() {
        return attrNames;
    }

    @Override
    public String getTableName() {
        return tableQName;
    }

    static final Set<Class<?>> PrimaryKeyTypes = new HashSet<Class<?>>();

    static {
        PrimaryKeyTypes.add(byte.class);
        PrimaryKeyTypes.add(Byte.class);
        PrimaryKeyTypes.add(char.class);
        PrimaryKeyTypes.add(Character.class);
        PrimaryKeyTypes.add(short.class);
        PrimaryKeyTypes.add(Short.class);
        PrimaryKeyTypes.add(int.class);
        PrimaryKeyTypes.add(Integer.class);
        PrimaryKeyTypes.add(long.class);
        PrimaryKeyTypes.add(Long.class);
        PrimaryKeyTypes.add(String.class);
        PrimaryKeyTypes.add(Date.class);
        PrimaryKeyTypes.add(java.sql.Date.class);
        PrimaryKeyTypes.add(BigDecimal.class);
        PrimaryKeyTypes.add(BigInteger.class);
    }
}
