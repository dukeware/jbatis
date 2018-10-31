package com.ibatis.sqlmap.engine.builder.xml;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.common.resources.*;
import com.ibatis.sqlmap.engine.config.*;

import javax.sql.DataSource;

import org.ibatis.client.Dialect;
import org.w3c.dom.Node;

import java.util.*;

public class XmlParserState {
    private static final ILog log = ILogFactory.getLog(XmlParserState.class);

    private SqlMapConfiguration config = new SqlMapConfiguration(this);

    private final Properties globalProps = new Properties(System.getProperties());
    private final Properties txProps = new Properties();
    private final Properties dsProps = new Properties();
    private final Properties cacheProps = new Properties();
    private boolean useStatementNamespaces = false;
    private Map<String, Node> sqlIncludes = new HashMap<String, Node>();

    private ParameterMapConfig paramConfig;
    private ResultMapConfig resultConfig;
    private CacheModelConfig cacheConfig;

    private String namespace;
    private DataSource dataSource;
    private Dialect dialect;

    public SqlMapConfiguration getConfig() {
        return config;
    }

    public void setGlobalProps(Properties props) {
        if (props != null) {
            String className = globalProps.getProperty("sql_executor_class");
            globalProps.putAll(props);
            txProps.putAll(props);
            dsProps.putAll(props);

            // Check for custom executors
            String customizedSQLExecutor = globalProps.getProperty("sql_executor_class");
            config.getErrorContext().setActivity("Loading SQLExecutor.");
            if (customizedSQLExecutor != null && !customizedSQLExecutor.equals(className)) {
                try {
                    config.getClient().getDelegate().setCustomExecutor(customizedSQLExecutor);
                    config.getClient().getDelegate().getSqlExecutor().init(config, globalProps);
                } catch (Exception e) {
                    config.getErrorContext().setCause(e);
                    config.getErrorContext().setMoreInfo(
                        "Loading of customizedSQLExecutor failed. Please check Properties file.");
                }
            }
        }
    }

    public Properties getGlobalProps() {
        return globalProps;
    }

    public Properties getTxProps() {
        return txProps;
    }

    public Properties getDsProps() {
        return dsProps;
    }

    public Properties getCacheProps() {
        return cacheProps;
    }

    public void setUseStatementNamespaces(boolean useStatementNamespaces) {
        this.useStatementNamespaces = useStatementNamespaces;
    }

    public boolean isUseStatementNamespaces() {
        return useStatementNamespaces;
    }

    public Map<String, Node> getSqlIncludes() {
        return sqlIncludes;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String applyNamespace(String id) {
        String newId = id;
        if (namespace != null && namespace.length() > 0 && id != null && id.indexOf('.') < 0) {
            newId = namespace + "." + id;
        }
        return newId;
    }

    public CacheModelConfig getCacheConfig() {
        return cacheConfig;
    }

    public void setCacheConfig(CacheModelConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    public ParameterMapConfig getParamConfig() {
        return paramConfig;
    }

    public void setParamConfig(ParameterMapConfig paramConfig) {
        this.paramConfig = paramConfig;
    }

    public ResultMapConfig getResultConfig() {
        return resultConfig;
    }

    public void setResultConfig(ResultMapConfig resultConfig) {
        this.resultConfig = resultConfig;
    }

    public void setGlobalProperties(String resource, String url) {
        config.getErrorContext().setActivity("loading global properties");
        try {
            Properties props;
            if (resource != null) {
                config.getErrorContext().setResource(resource);
                props = Resources.getResourceAsProperties(resource);
            } else if (url != null) {
                config.getErrorContext().setResource(url);
                props = Resources.getUrlAsProperties(url);
            } else {
                throw new RuntimeException("The " + "properties"
                    + " element requires either a resource or a url attribute.");
            }

            // Merge properties with those passed in programmatically
            if (props != null) {
                props.putAll(globalProps);
                setGlobalProps(props);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties.  Cause: " + e, e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

    Map<String, String> parsedResources = new LinkedHashMap<String, String>();
    public void addSqlMapResource(String resource) {
        if (parsedResources.put(resource, "") != null) {
            log.warn("Maybe re-parse sqlmap resource: " + resource);
        } else {
            log.debug("Parse mapping resource -> " + resource);
        }
    }
}
