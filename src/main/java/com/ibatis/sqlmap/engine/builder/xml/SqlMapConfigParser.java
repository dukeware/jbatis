package com.ibatis.sqlmap.engine.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Types;
import java.util.Properties;

import org.ibatis.client.Dialect;
import org.ibatis.client.SqlMapClient;
import org.w3c.dom.Node;

import com.ibatis.common.resources.Resources;
import com.ibatis.common.xml.Nodelet;
import com.ibatis.common.xml.NodeletParser;
import com.ibatis.common.xml.NodeletUtils;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.engine.config.SqlMapConfiguration;
import com.ibatis.sqlmap.engine.datasource.DataSourceFactory;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory;
import com.ibatis.sqlmap.engine.transaction.TransactionConfig;
import com.ibatis.sqlmap.engine.transaction.TransactionManager;

public class SqlMapConfigParser {

    protected final NodeletParser parser = new NodeletParser();
    private XmlParserState state = new XmlParserState();

    private boolean usingStreams = false;

    public XmlParserState getState() {
        return state;
    }

    public SqlMapConfigParser() {
        this(null);
    }

    public SqlMapConfigParser(String dialect) {
        parser.setValidation(true);
        parser.setEntityResolver(new SqlMapClasspathEntityResolver());

        addSqlMapConfigNodelets();
        addGlobalPropNodelets();
        addSettingsNodelets();
        addTypeAliasNodelets();
        addTypeHandlerNodelets();
        addTransactionManagerNodelets();
        addSqlMapNodelets();
        addResultObjectFactoryNodelets();
        if (dialect != null) {
            Dialect d = Dialect.forName(dialect);
            if (d == null) {
                throw new IllegalArgumentException(dialect);
            }
            state.setDialect(d);
        }
    }

    public SqlMapClient parse(Reader reader, Properties props) {
        if (props != null)
            state.setGlobalProps(props);
        return parse(reader);
    }

    public SqlMapClient parse(Reader reader) {
        try {
            usingStreams = false;

            parser.parse("", reader);
            return state.getConfig().getClient();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred.  Cause: " + e, e);
        }
    }

    public SqlMapClient parse(InputStream inputStream, Properties props) {
        if (props != null)
            state.setGlobalProps(props);
        return parse(inputStream);
    }

    public SqlMapClient parse(InputStream inputStream) {
        try {
            usingStreams = true;

            parser.parse("", inputStream);
            return state.getConfig().getClient();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred.  Cause: " + e, e);
        }
    }

    private void addSqlMapConfigNodelets() {
        parser.addNodelet("/sqlMapConfig/end()", new Nodelet() {
            public void process(Node node) throws Exception {
                state.getConfig().finalizeSqlMapConfig();
            }
        });
    }

    private void addGlobalPropNodelets() {
        parser.addNodelet("/sqlMapConfig/properties", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String resource = attributes.getProperty("resource");
                String url = attributes.getProperty("url");
                state.setGlobalProperties(resource, url);
            }
        });
    }

    private void addSettingsNodelets() {
        parser.addNodelet("/sqlMapConfig/settings", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                SqlMapConfiguration config = state.getConfig();

                String attr = attributes.getProperty("classInfoCacheEnabled");
                boolean classInfoCacheEnabled = (attr == null || "true".equals(attr));
                config.setClassInfoCacheEnabled(classInfoCacheEnabled);

                attr = attributes.getProperty("lazyLoadingEnabled");
                boolean lazyLoadingEnabled = "true".equals(attr);
                config.setLazyLoadingEnabled(lazyLoadingEnabled);

                attr = attributes.getProperty("statementCachingEnabled");
                boolean statementCachingEnabled = (attr == null || "true".equals(attr));
                config.setStatementCachingEnabled(statementCachingEnabled);

                attr = attributes.getProperty("cacheModelsEnabled");
                boolean cacheModelsEnabled = (attr == null || "true".equals(attr));
                config.setCacheModelsEnabled(cacheModelsEnabled);

                attr = attributes.getProperty("enhancementEnabled");
                boolean enhancementEnabled = (attr == null || "true".equals(attr));
                config.setEnhancementEnabled(enhancementEnabled);

                attr = attributes.getProperty("databasePagingQueryEnabled");
                boolean databasePagingQueryEnabled = (attr == null || "true".equals(attr));
                config.setDatabasePagingQueryEnabled(databasePagingQueryEnabled);

                attr = attributes.getProperty("useColumnLabel");
                boolean useColumnLabel = (attr == null || "true".equals(attr));
                config.setUseColumnLabel(useColumnLabel);

                attr = attributes.getProperty("forceMultipleResultSetSupport");
                boolean forceMultipleResultSetSupport = "true".equals(attr);
                config.setForceMultipleResultSetSupport(forceMultipleResultSetSupport);

                attr = attributes.getProperty("defaultStatementTimeout");
                config.setDefaultStatementTimeout(attr);

                attr = attributes.getProperty("useStatementNamespaces");
                boolean useStatementNamespaces = "true".equals(attr);
                state.setUseStatementNamespaces(useStatementNamespaces);

                attr = attributes.getProperty("jdbcTypeForNull");
                Integer jdbcTypeForNull = null;
                if ("NULL".equals(attr)) {
                    jdbcTypeForNull = Types.NULL;
                } else if ("VARCHAR".equals(attr)) {
                    jdbcTypeForNull = Types.VARCHAR;
                } else if ("OTHER".equals(attr)) {
                    jdbcTypeForNull = Types.OTHER;
                }
                config.setJdbcTypeForNull(jdbcTypeForNull);

                String forceCacheModelType = attributes.getProperty("forceCacheModelType");
                if (forceCacheModelType != null && !forceCacheModelType.isEmpty()) {
                    config.setForceCacheModelType(forceCacheModelType);
                    config.setDefaultCacheModelType(forceCacheModelType);
                } else {
                    String defaultCacheModelType = attributes.getProperty("defaultCacheModelType");
                    if (defaultCacheModelType != null && !defaultCacheModelType.isEmpty()) {
                        config.setDefaultCacheModelType(defaultCacheModelType);
                    }
                }
            }
        });
    }

    private void addTypeAliasNodelets() {
        parser.addNodelet("/sqlMapConfig/typeAlias", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties prop = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String alias = prop.getProperty("alias");
                String type = prop.getProperty("type");
                state.getConfig().getTypeHandlerFactory().putTypeAlias(alias, type);
            }
        });
    }

    private void addTypeHandlerNodelets() {
        parser.addNodelet("/sqlMapConfig/typeHandler", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties prop = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String jdbcType = prop.getProperty("jdbcType");
                String javaType = prop.getProperty("javaType");
                String callback = prop.getProperty("callback");

                javaType = state.getConfig().getTypeHandlerFactory().resolveAlias(javaType);
                callback = state.getConfig().getTypeHandlerFactory().resolveAlias(callback);

                state.getConfig().newTypeHandler(Resources.classForName(javaType), jdbcType,
                    Resources.instantiate(callback));
            }
        });
    }

    private void addTransactionManagerNodelets() {
        parser.addNodelet("/sqlMapConfig/transactionManager/property", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String name = attributes.getProperty("name");
                String value = NodeletUtils.parsePropertyTokens(attributes.getProperty("value"), state.getGlobalProps());
                state.getTxProps().setProperty(name, value);
            }
        });
        parser.addNodelet("/sqlMapConfig/transactionManager/end()", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String type = attributes.getProperty("type");
                boolean commitRequired = "true".equals(attributes.getProperty("commitRequired"));

                state.getConfig().getErrorContext().setActivity("configuring the transaction manager");
                type = state.getConfig().getTypeHandlerFactory().resolveAlias(type);
                TransactionManager txManager;
                try {
                    state.getConfig().getErrorContext().setMoreInfo("Check the transaction manager type or class.");
                    TransactionConfig config = (TransactionConfig) Resources.instantiate(type);
                    config.setDataSource(state.getDataSource());
                    state.getConfig().getErrorContext()
                        .setMoreInfo("Check the transactio nmanager properties or configuration.");
                    config.setProperties(state.getTxProps());
                    config.setForceCommit(commitRequired);
                    config.setDataSource(state.getDataSource());
                    state.getConfig().getErrorContext().setMoreInfo(null);
                    txManager = new TransactionManager(config);
                } catch (Exception e) {
                    if (e instanceof SqlMapException) {
                        throw (SqlMapException) e;
                    } else {
                        throw new SqlMapException(
                            "Error initializing TransactionManager.  Could not instantiate TransactionConfig.  Cause: "
                                + e, e);
                    }
                }
                state.getConfig().setTransactionManager(txManager);
            }
        });
        parser.addNodelet("/sqlMapConfig/transactionManager/dataSource/property", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String name = attributes.getProperty("name");
                String value = NodeletUtils.parsePropertyTokens(attributes.getProperty("value"), state.getGlobalProps());
                state.getDsProps().setProperty(name, value);
            }
        });
        parser.addNodelet("/sqlMapConfig/transactionManager/dataSource/end()", new Nodelet() {
            public void process(Node node) throws Exception {
                state.getConfig().getErrorContext().setActivity("configuring the data source");

                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());

                String type = attributes.getProperty("type");
                Properties props = state.getDsProps();

                type = state.getConfig().getTypeHandlerFactory().resolveAlias(type);
                try {
                    state.getConfig().getErrorContext().setMoreInfo("Check the data source type or class.");
                    DataSourceFactory dsFactory = (DataSourceFactory) Resources.instantiate(type);
                    state.getConfig().getErrorContext()
                        .setMoreInfo("Check the data source properties or configuration.");
                    dsFactory.initialize(props);
                    state.setDataSource(dsFactory.getDataSource());
                    state.getConfig().getErrorContext().setMoreInfo(null);
                } catch (Exception e) {
                    if (e instanceof SqlMapException) {
                        throw (SqlMapException) e;
                    } else {
                        throw new SqlMapException(
                            "Error initializing DataSource.  Could not instantiate DataSourceFactory.  Cause: " + e, e);
                    }
                }
            }
        });
    }

    protected void addSqlMapNodelets() {
        parser.addNodelet("/sqlMapConfig/sqlMap", new Nodelet() {
            public void process(Node node) throws Exception {
                state.getConfig().getErrorContext().setActivity("loading the SQL Map resource");

                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());

                String resource = attributes.getProperty("resource");
                String url = attributes.getProperty("url");

                // ## sunsong
                if (usingStreams || Resources.getCharset() == null) {
                    InputStream inputStream = null;
                    try {
                        if (resource != null) {
                            state.getConfig().getErrorContext().setResource(resource);
                            inputStream = Resources.getResourceAsStream(resource);
                        } else if (url != null) {
                            state.getConfig().getErrorContext().setResource(url);
                            inputStream = Resources.getUrlAsStream(url);
                        } else {
                            throw new SqlMapException("The <sqlMap> element requires either a resource or a url attribute.");
                        }
    
                        new SqlMapParser(state).parse(resource, inputStream);
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    Reader reader = null;
                    try {
                        if (resource != null) {
                            state.getConfig().getErrorContext().setResource(resource);
                            reader = Resources.getResourceAsReader(resource);
                        } else if (url != null) {
                            state.getConfig().getErrorContext().setResource(url);
                            reader = Resources.getUrlAsReader(url);
                        } else {
                            throw new SqlMapException("The <sqlMap> element requires either a resource or a url attribute.");
                        }
    
                        new SqlMapParser(state).parse(resource, reader);
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        });
    }

    private void addResultObjectFactoryNodelets() {
        parser.addNodelet("/sqlMapConfig/resultObjectFactory", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String type = attributes.getProperty("type");

                state.getConfig().getErrorContext().setActivity("configuring the Result Object Factory");
                ResultObjectFactory rof;
                try {
                    rof = (ResultObjectFactory) Resources.instantiate(type);
                    state.getConfig().setResultObjectFactory(rof);
                } catch (Exception e) {
                    throw new SqlMapException("Error instantiating resultObjectFactory: " + type, e);
                }

            }
        });
        parser.addNodelet("/sqlMapConfig/resultObjectFactory/property", new Nodelet() {
            public void process(Node node) throws Exception {
                Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
                String name = attributes.getProperty("name");
                String value = NodeletUtils.parsePropertyTokens(attributes.getProperty("value"), state.getGlobalProps());
                state.getConfig().getDelegate().getResultObjectFactory().setProperty(name, value);
            }
        });
    }

}
