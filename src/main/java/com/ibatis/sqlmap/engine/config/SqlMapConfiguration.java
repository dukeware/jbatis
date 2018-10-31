package com.ibatis.sqlmap.engine.config;


import org.ibatis.cglib.ClassInfo;

import com.ibatis.common.beans.Probe;
import com.ibatis.common.beans.ProbeFactory;
import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;
import com.ibatis.sqlmap.engine.accessplan.AccessPlanFactory;
import com.ibatis.sqlmap.engine.builder.xml.XmlParserState;
import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.transaction.TransactionManager;
import com.ibatis.sqlmap.engine.type.CustomTypeHandler;
import com.ibatis.sqlmap.engine.type.TypeHandler;
import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

@SuppressWarnings("unchecked")
public class SqlMapConfiguration {
    static final ILog log = ILogFactory.getLog(SqlMapConfiguration.class.getPackage().getName());
    private static final Probe PROBE = ProbeFactory.getProbe();
    private ErrorContext errorContext;
    private SqlMapExecutorDelegate delegate;
    private TypeHandlerFactory typeHandlerFactory;
    private SqlMapClientImpl client;
    private Integer defaultStatementTimeout;

    public SqlMapConfiguration(XmlParserState state) {
        errorContext = new ErrorContext();
        delegate = new SqlMapExecutorDelegate(state);
        typeHandlerFactory = delegate.getTypeHandlerFactory();
        client = new SqlMapClientImpl(delegate);
        registerDefaultTypeAliases();
    }

    public TypeHandlerFactory getTypeHandlerFactory() {
        return typeHandlerFactory;
    }

    public ErrorContext getErrorContext() {
        return errorContext;
    }

    public SqlMapClientImpl getClient() {
        return client;
    }

    public SqlMapExecutorDelegate getDelegate() {
        return delegate;
    }

    public void setClassInfoCacheEnabled(boolean classInfoCacheEnabled) {
        errorContext.setActivity("setting class info cache enabled/disabled");
        ClassInfo.setCacheEnabled(classInfoCacheEnabled);
    }

    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
        errorContext.setActivity("setting lazy loading enabled/disabled");
        client.getDelegate().setLazyLoadingEnabled(lazyLoadingEnabled);
    }

    public void setStatementCachingEnabled(boolean statementCachingEnabled) {
        errorContext.setActivity("setting statement caching enabled/disabled");
        client.getDelegate().setStatementCacheEnabled(statementCachingEnabled);
    }

    public void setCacheModelsEnabled(boolean cacheModelsEnabled) {
        errorContext.setActivity("setting cache models enabled/disabled");
        client.getDelegate().setCacheModelsEnabled(cacheModelsEnabled);
    }

    public void setEnhancementEnabled(boolean enhancementEnabled) {
        errorContext.setActivity("setting enhancement enabled/disabled");
        client.getDelegate().setEnhancementEnabled(enhancementEnabled);
        AccessPlanFactory.setBytecodeEnhancementEnabled(enhancementEnabled);
    }

    public void setDatabasePagingQueryEnabled(boolean databasePagingQueryEnabled) {
        errorContext.setActivity("setting database paging query enabled/disabled");
        client.getDelegate().setDatabasePagingQueryEnabled(databasePagingQueryEnabled);
    }

    public void setUseColumnLabel(boolean useColumnLabel) {
        client.getDelegate().setUseColumnLabel(useColumnLabel);
    }

    public void setJdbcTypeForNull(Integer jdbcTypeForNull) {
        client.getDelegate().setJdbcTypeForNull(jdbcTypeForNull);
    }

    public void setForceCacheModelType(String forceCacheModelType) {
        client.getDelegate().setForceCacheModelType(forceCacheModelType);
    }

    public void setDefaultCacheModelType(String defaultCacheModelType) {
        client.getDelegate().setDefaultCacheModelType(defaultCacheModelType);
    }

    public void setForceMultipleResultSetSupport(boolean forceMultipleResultSetSupport) {
        client.getDelegate().setForceMultipleResultSetSupport(forceMultipleResultSetSupport);
    }

    public void setDefaultStatementTimeout(String defaultTimeout) {
        errorContext.setActivity("setting default timeout");
        if (defaultTimeout != null) {
            try {
                defaultStatementTimeout = Integer.valueOf(defaultTimeout);;
            } catch (NumberFormatException e) {
                throw new SqlMapException("Specified defaultStatementTimeout '" + defaultTimeout + "' is not a valid integer");
            }
        }
    }

    public void setTransactionManager(TransactionManager txManager) {
        delegate.setTxManager(txManager);
    }

    public void setResultObjectFactory(ResultObjectFactory rof) {
        delegate.setResultObjectFactory(rof);
    }

    public void newTypeHandler(Class<?> javaType, String jdbcType, Object callback) {
        try {
            errorContext.setActivity("building a building custom type handler");
            TypeHandlerFactory typeHandlerFactory = client.getDelegate().getTypeHandlerFactory();
            TypeHandler typeHandler;
            if (callback instanceof TypeHandlerCallback) {
                typeHandler = new CustomTypeHandler((TypeHandlerCallback) callback);
            } else if (callback instanceof TypeHandler) {
                typeHandler = (TypeHandler) callback;
            } else {
                throw new RuntimeException("The object '" + callback
                    + "' is not a valid implementation of TypeHandler or TypeHandlerCallback");
            }
            errorContext.setMoreInfo("Check the javaType attribute '" + javaType
                + "' (must be a classname) or the jdbcType '" + jdbcType + "' (must be a JDBC type name).");
            if (jdbcType != null && jdbcType.length() > 0) {
                typeHandlerFactory.register(javaType, jdbcType, typeHandler);
            } else {
                typeHandlerFactory.register(javaType, typeHandler);
            }
        } catch (Exception e) {
            throw new SqlMapException("Error registering occurred.  Cause: " + e, e);
        }
        errorContext.setMoreInfo(null);
        errorContext.setObjectId(null);
    }

    public CacheModelConfig newCacheModelConfig(String id, CacheController controller) {
        return new CacheModelConfig(this, id, controller);
    }

    public ParameterMapConfig newParameterMapConfig(String id, Class<?> parameterClass) {
        return new ParameterMapConfig(this, id, parameterClass);
    }

    public ResultMapConfig newResultMapConfig(String id, Class<?> resultClass, String groupBy, String extended) {
        return new ResultMapConfig(this, id, resultClass, groupBy, extended);
    }

    public MappedStatementConfig newMappedStatementConfig(String id, MappedStatement statement, SqlSource processor,
        String parameterMapName, Class<?> parameterClass, String resultMapName, String[] additionalResultMapNames,
        Class<?> resultClass, Class<?>[] additionalResultClasses, String resultSetType, Integer fetchSize,
        boolean allowRemapping, Integer timeout, String cacheModelName, boolean canBatch) {
        return new MappedStatementConfig(this, id, statement, processor, parameterMapName, parameterClass,
            resultMapName, additionalResultMapNames, resultClass, additionalResultClasses, cacheModelName,
            resultSetType, fetchSize, allowRemapping, timeout, defaultStatementTimeout, canBatch);
    }

    public void finalizeSqlMapConfig() {
        delegate.finalizeSqlMapConfig();
    }

    TypeHandler resolveTypeHandler(TypeHandlerFactory typeHandlerFactory, Class<?> clazz, String propertyName,
        Class<?> javaType, String jdbcType) {
        return resolveTypeHandler(typeHandlerFactory, clazz, propertyName, javaType, jdbcType, false);
    }

    TypeHandler resolveTypeHandler(TypeHandlerFactory typeHandlerFactory, Class<?> clazz, String propertyName,
        Class<?> javaType, String jdbcType, boolean useSetterToResolve) {
        TypeHandler handler;
        if (clazz == null) {
            // Unknown
            handler = typeHandlerFactory.getUnkownTypeHandler();
        } else if (java.util.Map.class.isAssignableFrom(clazz)) {
            // Map
            if (javaType == null) {
                handler = typeHandlerFactory.getUnkownTypeHandler(); // BUG 1012591
            } else {
                handler = typeHandlerFactory.getTypeHandler(javaType, jdbcType);
            }
        } else if (typeHandlerFactory.getTypeHandler(clazz, jdbcType) != null) {
            // Primitive
            handler = typeHandlerFactory.getTypeHandler(clazz, jdbcType);
        } else {
            // JavaBean
            if (javaType == null) {
                if (useSetterToResolve) {
                    Class<?> type = PROBE.getPropertyTypeForSetter(clazz, propertyName);
                    handler = typeHandlerFactory.getTypeHandler(type, jdbcType);
                } else {
                    Class<?> type = PROBE.getPropertyTypeForGetter(clazz, propertyName);
                    handler = typeHandlerFactory.getTypeHandler(type, jdbcType);
                }
            } else {
                handler = typeHandlerFactory.getTypeHandler(javaType, jdbcType);
            }
        }
        return handler;
    }

    private void registerDefaultTypeAliases() {
        // TRANSACTION ALIASES
        typeHandlerFactory.putTypeAlias("JDBC", "com.ibatis.sqlmap.engine.transaction.jdbc.JdbcTransactionConfig");
        typeHandlerFactory.putTypeAlias("JTA", "com.ibatis.sqlmap.engine.transaction.jta.JtaTransactionConfig");
        typeHandlerFactory.putTypeAlias("EXTERNAL", "com.ibatis.sqlmap.engine.transaction.external.ExternalTransactionConfig");

        // DATA SOURCE ALIASES
        typeHandlerFactory.putTypeAlias("SIMPLE", "com.ibatis.sqlmap.engine.datasource.SimpleDataSourceFactory");
        typeHandlerFactory.putTypeAlias("DBCP", "com.ibatis.sqlmap.engine.datasource.DbcpDataSourceFactory");
        typeHandlerFactory.putTypeAlias("JNDI", "com.ibatis.sqlmap.engine.datasource.JndiDataSourceFactory");

        // CACHE ALIASES
        // use a string for OSCache to avoid unnecessary loading of properties upon init
        typeHandlerFactory.putTypeAlias("FIFO", "com.ibatis.sqlmap.engine.cache.fifo.FifoCacheController");
        typeHandlerFactory.putTypeAlias("LRU", "com.ibatis.sqlmap.engine.cache.lru.LruCacheController");
        typeHandlerFactory.putTypeAlias("MEMORY", "com.ibatis.sqlmap.engine.cache.memory.MemoryCacheController");
        typeHandlerFactory.putTypeAlias("OSCACHE", "com.ibatis.sqlmap.engine.cache.oscache.OSCacheController");
        typeHandlerFactory.putTypeAlias("EHCACHE", "com.ibatis.sqlmap.engine.cache.ehcache.EhCacheController");

        // TYPE ALIASEs
        // typeHandlerFactory.putTypeAlias("dom", DomTypeMarker.class.getName());
        // typeHandlerFactory.putTypeAlias("domCollection", DomCollectionTypeMarker.class.getName());
        // typeHandlerFactory.putTypeAlias("xml", XmlTypeMarker.class.getName());
        // typeHandlerFactory.putTypeAlias("xmlCollection", XmlCollectionTypeMarker.class.getName());
    }

    public String toCacheModelType(String type) {
        return getDelegate().toCacheModelType(type);
    }

}
