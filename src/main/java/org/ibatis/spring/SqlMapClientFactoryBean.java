/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ibatis.spring;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.sql.DataSource;

import org.ibatis.client.SqlMapClient;
import org.ibatis.client.SqlMapClientBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.ObjectUtils;

import com.ibatis.common.logging.ILog;
import com.ibatis.common.logging.ILogFactory;
import com.ibatis.common.resources.Resources;
import com.ibatis.common.xml.NodeletException;
import com.ibatis.sqlmap.engine.builder.xml.SqlMapConfigParser;
import com.ibatis.sqlmap.engine.builder.xml.SqlMapParser;
import com.ibatis.sqlmap.engine.config.SqlMapConfiguration;
import com.ibatis.sqlmap.engine.impl.ExtendedSqlMapClient;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.transaction.TransactionConfig;
import com.ibatis.sqlmap.engine.transaction.TransactionManager;
import com.ibatis.sqlmap.engine.transaction.external.ExternalTransactionConfig;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates an iBATIS
 * {@link com.ibatis.sqlmap.client.SqlMapClient}. This is the usual way to set up a shared iBATIS SqlMapClient in a
 * Spring application context; the SqlMapClient can then be passed to iBATIS-based DAOs via dependency injection.
 *
 * <p>
 * Either {@link org.springframework.jdbc.datasource.DataSourceTransactionManager} or
 * {@link org.springframework.transaction.jta.JtaTransactionManager} can be used for transaction demarcation in
 * combination with a SqlMapClient, with JTA only necessary for transactions which span multiple databases.
 *
 * <p>
 * Allows for specifying a DataSource at the SqlMapClient level. This is preferable to per-DAO DataSource references, as
 * it allows for lazy loading and avoids repeated DataSource references in every DAO.
 *
 * <p>
 * <b>Note:</b> As of Spring 2.5.5, this class (finally) requires iBATIS 2.3 or higher. The new "mappingLocations"
 * feature requires iBATIS 2.3.2.
 *
 * @author Juergen Hoeller
 * @since 24.02.2004
 * @see #setConfigLocation
 * @see #setDataSource
 * @see SqlMapClientTemplate#setSqlMapClient
 * @see SqlMapClientTemplate#setDataSource
 */
public class SqlMapClientFactoryBean implements FactoryBean<SqlMapClient>, InitializingBean {

    private static final ILog log = ILogFactory.getLog(SqlMapClientFactoryBean.class);

    private static final ThreadLocal<LobHandler> configTimeLobHandlerHolder = new ThreadLocal<LobHandler>();

    /**
     * Return the LobHandler for the currently configured iBATIS SqlMapClient, to be used by TypeHandler implementations
     * like ClobStringTypeHandler.
     * <p>
     * This instance will be set before initialization of the corresponding SqlMapClient, and reset immediately
     * afterwards. It is thus only available during configuration.
     * 
     * @see #setLobHandler
     * @see org.springframework.orm.ibatis.support.ClobStringTypeHandler
     * @see org.springframework.orm.ibatis.support.BlobByteArrayTypeHandler
     * @see org.springframework.orm.ibatis.support.BlobSerializableTypeHandler
     */
    public static LobHandler getConfigTimeLobHandler() {
        return configTimeLobHandlerHolder.get();
    }

    private Resource[] configLocations;

    private Resource[] mappingLocations;

    private Properties sqlMapClientProperties;

    private DataSource dataSource;

    private boolean useTransactionAwareDataSource = true;

    private Class<?> transactionConfigClass = ExternalTransactionConfig.class;

    private Properties transactionConfigProperties;

    private LobHandler lobHandler;

    private SqlMapClient sqlMapClient;

    public SqlMapClientFactoryBean() {
        this.transactionConfigProperties = new Properties();
        this.transactionConfigProperties.setProperty("SetAutoCommitAllowed", "false");
    }

    /**
     * Set the location of the iBATIS SqlMapClient config file. A typical value is "WEB-INF/sql-map-config.xml".
     * 
     * @see #setConfigLocations
     */
    public void setConfigLocation(Resource configLocation) {
        this.configLocations = (configLocation != null ? new Resource[] { configLocation } : null);
    }

    /**
     * Set multiple locations of iBATIS SqlMapClient config files that are going to be merged into one unified
     * configuration at runtime.
     */
    public void setConfigLocations(Resource[] configLocations) {
        this.configLocations = configLocations;
    }

    /**
     * Set locations of iBATIS sql-map mapping files that are going to be merged into the SqlMapClient configuration at
     * runtime.
     * <p>
     * This is an alternative to specifying "&lt;sqlMap&gt;" entries in a sql-map-client config file. This property
     * being based on Spring's resource abstraction also allows for specifying resource patterns here: e.g.
     * "/myApp/*-map.xml".
     * <p>
     * Note that this feature requires iBATIS 2.3.2; it will not work with any previous iBATIS version.
     */
    public void setMappingLocations(Resource[] mappingLocations) {
        this.mappingLocations = mappingLocations;
    }

    /**
     * Set optional properties to be passed into the SqlMapClientBuilder, as alternative to a
     * <code>&lt;properties&gt;</code> tag in the sql-map-config.xml file. Will be used to resolve placeholders in the
     * config file.
     * 
     * @see #setConfigLocation
     */
    public void setSqlMapClientProperties(Properties sqlMapClientProperties) {
        this.sqlMapClientProperties = sqlMapClientProperties;
    }

    /**
     * Set the DataSource to be used by iBATIS SQL Maps. This will be passed to the SqlMapClient as part of a
     * TransactionConfig instance.
     * <p>
     * If specified, this will override corresponding settings in the SqlMapClient properties. Usually, you will specify
     * DataSource and transaction configuration <i>either</i> here <i>or</i> in SqlMapClient properties.
     * <p>
     * Specifying a DataSource for the SqlMapClient rather than for each individual DAO allows for lazy loading, for
     * example when using PaginatedList results.
     * <p>
     * With a DataSource passed in here, you don't need to specify one for each DAO. Passing the SqlMapClient to the
     * DAOs is enough, as it already carries a DataSource. Thus, it's recommended to specify the DataSource at this
     * central location only.
     * <p>
     * Thanks to Brandon Goodin from the iBATIS team for the hint on how to make this work with Spring's integration
     * strategy!
     * 
     * @see #setTransactionConfigClass
     * @see #setTransactionConfigProperties
     * @see com.ibatis.sqlmap.client.SqlMapClient#getDataSource
     * @see SqlMapClientTemplate#setDataSource
     * @see SqlMapClientTemplate#queryForPaginatedList
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String dialect;

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    /**
     * Set whether to use a transaction-aware DataSource for the SqlMapClient, i.e. whether to automatically wrap the
     * passed-in DataSource with Spring's TransactionAwareDataSourceProxy.
     * <p>
     * Default is "true": When the SqlMapClient performs direct database operations outside of Spring's
     * SqlMapClientTemplate (for example, lazy loading or direct SqlMapClient access), it will still participate in
     * active Spring-managed transactions.
     * <p>
     * As a further effect, using a transaction-aware DataSource will apply remaining transaction timeouts to all
     * created JDBC Statements. This means that all operations performed by the SqlMapClient will automatically
     * participate in Spring-managed transaction timeouts.
     * <p>
     * Turn this flag off to get raw DataSource handling, without Spring transaction checks. Operations on Spring's
     * SqlMapClientTemplate will still detect Spring-managed transactions, but lazy loading or direct SqlMapClient
     * access won't.
     * 
     * @see #setDataSource
     * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
     * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
     * @see SqlMapClientTemplate
     * @see com.ibatis.sqlmap.client.SqlMapClient
     */
    public void setUseTransactionAwareDataSource(boolean useTransactionAwareDataSource) {
        this.useTransactionAwareDataSource = useTransactionAwareDataSource;
    }

    /**
     * Set the iBATIS TransactionConfig class to use. Default is
     * <code>com.ibatis.sqlmap.engine.transaction.external.ExternalTransactionConfig</code>.
     * <p>
     * Will only get applied when using a Spring-managed DataSource. An instance of this class will get populated with
     * the given DataSource and initialized with the given properties.
     * <p>
     * The default ExternalTransactionConfig is appropriate if there is external transaction management that the
     * SqlMapClient should participate in: be it Spring transaction management, EJB CMT or plain JTA. This should be the
     * typical scenario. If there is no active transaction, SqlMapClient operations will execute SQL statements
     * non-transactionally.
     * <p>
     * JdbcTransactionConfig or JtaTransactionConfig is only necessary when using the iBATIS SqlMapTransactionManager
     * API instead of external transactions. If there is no explicit transaction, SqlMapClient operations will
     * automatically start a transaction for their own scope (in contrast to the external transaction mode, see above).
     * <p>
     * <b>It is strongly recommended to use iBATIS SQL Maps with Spring transaction management (or EJB CMT).</b> In this
     * case, the default ExternalTransactionConfig is fine. Lazy loading and SQL Maps operations without explicit
     * transaction demarcation will execute non-transactionally.
     * <p>
     * Even with Spring transaction management, it might be desirable to specify JdbcTransactionConfig: This will still
     * participate in existing Spring-managed transactions, but lazy loading and operations without explicit transaction
     * demaration will execute in their own auto-started transactions. However, this is usually not necessary.
     * 
     * @see #setDataSource
     * @see #setTransactionConfigProperties
     * @see com.ibatis.sqlmap.engine.transaction.TransactionConfig
     * @see com.ibatis.sqlmap.engine.transaction.external.ExternalTransactionConfig
     * @see com.ibatis.sqlmap.engine.transaction.jdbc.JdbcTransactionConfig
     * @see com.ibatis.sqlmap.engine.transaction.jta.JtaTransactionConfig
     * @see com.ibatis.sqlmap.client.SqlMapTransactionManager
     */
    public void setTransactionConfigClass(Class<?> transactionConfigClass) {
        if (transactionConfigClass == null || !TransactionConfig.class.isAssignableFrom(transactionConfigClass)) {
            throw new IllegalArgumentException("Invalid transactionConfigClass: does not implement "
                + "com.ibatis.sqlmap.engine.transaction.TransactionConfig");
        }
        this.transactionConfigClass = transactionConfigClass;
    }

    /**
     * Set properties to be passed to the TransactionConfig instance used by this SqlMapClient. Supported properties
     * depend on the concrete TransactionConfig implementation used:
     * <p>
     * <ul>
     * <li><b>ExternalTransactionConfig</b> supports "DefaultAutoCommit" (default: false) and "SetAutoCommitAllowed"
     * (default: true). Note that Spring uses SetAutoCommitAllowed = false as default, in contrast to the iBATIS
     * default, to always keep the original autoCommit value as provided by the connection pool.
     * <li><b>JdbcTransactionConfig</b> does not supported any properties.
     * <li><b>JtaTransactionConfig</b> supports "UserTransaction" (no default), specifying the JNDI location of the JTA
     * UserTransaction (usually "java:comp/UserTransaction").
     * </ul>
     * 
     * @see com.ibatis.sqlmap.engine.transaction.TransactionConfig#initialize
     * @see com.ibatis.sqlmap.engine.transaction.external.ExternalTransactionConfig
     * @see com.ibatis.sqlmap.engine.transaction.jdbc.JdbcTransactionConfig
     * @see com.ibatis.sqlmap.engine.transaction.jta.JtaTransactionConfig
     */
    public void setTransactionConfigProperties(Properties transactionConfigProperties) {
        this.transactionConfigProperties = transactionConfigProperties;
    }

    /**
     * Set the LobHandler to be used by the SqlMapClient. Will be exposed at config time for TypeHandler
     * implementations.
     * 
     * @see #getConfigTimeLobHandler
     * @see com.ibatis.sqlmap.engine.type.TypeHandler
     * @see org.springframework.orm.ibatis.support.ClobStringTypeHandler
     * @see org.springframework.orm.ibatis.support.BlobByteArrayTypeHandler
     * @see org.springframework.orm.ibatis.support.BlobSerializableTypeHandler
     */
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.lobHandler != null) {
            // Make given LobHandler available for SqlMapClient configuration.
            // Do early because because mapping resource might refer to custom types.
            configTimeLobHandlerHolder.set(this.lobHandler);
        }

        try {
            SqlMapClientImpl impl = buildSqlMapClient(this.configLocations, this.mappingLocations,
                this.sqlMapClientProperties);
            this.sqlMapClient = impl;
            // Tell the SqlMapClient to use the given DataSource, if any.
            if (this.dataSource != null) {
                TransactionConfig transactionConfig = (TransactionConfig) this.transactionConfigClass.newInstance();
                DataSource dataSourceToUse = this.dataSource;
                if (this.useTransactionAwareDataSource && !(this.dataSource instanceof TransactionAwareDataSourceProxy)) {
                    dataSourceToUse = new TransactionAwareDataSourceProxy(this.dataSource);
                }
                impl.setRealDataSource(dataSource);
                transactionConfig.setDataSource(dataSourceToUse);
                transactionConfig.setProperties(this.transactionConfigProperties);
                applyTransactionConfig(impl, transactionConfig);
            }
        }

        finally {
            if (this.lobHandler != null) {
                // Reset LobHandler holder.
                configTimeLobHandlerHolder.remove();
            }
        }
    }

    /**
     * Build a SqlMapClient instance based on the given standard configuration.
     * <p>
     * The default implementation uses the standard iBATIS {@link SqlMapClientBuilder} API to build a SqlMapClient
     * instance based on an InputStream (if possible, on iBATIS 2.3 and higher) or on a Reader (on iBATIS up to version
     * 2.2).
     * 
     * @param configLocations
     *            the config files to load from
     * @param properties
     *            the SqlMapClient properties (if any)
     * @return the SqlMapClient instance (never <code>null</code>)
     * @throws IOException
     *             if loading the config file failed
     */
    protected SqlMapClientImpl buildSqlMapClient(Resource[] configLocations, Resource[] mappingLocations,
        Properties properties) throws IOException {

        if (ObjectUtils.isEmpty(configLocations)) {
            throw new IllegalArgumentException("At least 1 'configLocation' entry is required");
        }

        SqlMapClientImpl client = null;
        SqlMapConfigParser configParser = new SqlMapConfigParser(dialect);
        synchronized (configParser.getState()) {
            properties = getProperties(properties);
            for (Resource configLocation : configLocations) {
                InputStream is = configLocation.getInputStream();
                try {
                    client = (SqlMapClientImpl) configParser.parse(is, properties);
                } catch (RuntimeException ex) {
                    log.error("Failed to parse config resource: " + configLocation, ex);
                    throw new NestedIOException("Failed to parse config resource: " + configLocation, ex);
                }
            }

            if (mappingLocations != null) {
                boolean needRefresh = false;
                SqlMapParser mapParser = new SqlMapParser(configParser.getState());
                SqlMapConfiguration config = configParser.getState().getConfig();
                for (Resource res : mappingLocations) {
                    try {
                        String str = res.toString();
                        config.getErrorContext().setResource(getResourcePath(res));
                        mapParser.parse(str, res.getInputStream());
                        needRefresh = true;
                    } catch (NodeletException ex) {
                        log.error("Failed to parse mapping resource: " + res, ex);
                        throw new NestedIOException("Failed to parse mapping resource: " + res, ex);
                    }
                }
                if (needRefresh) {
                    config.finalizeSqlMapConfig();
                }
            }
        }

        return client;
    }

    static String getResourcePath(Resource res) {
        if (res instanceof ContextResource) {
            return ((ContextResource) res).getPathWithinContext();
        }
        if (res instanceof ClassPathResource) {
            ClassPathResource cpRes = (ClassPathResource) res;
            return cpRes.getPath();
        }
        if (res instanceof FileSystemResource) {
            FileSystemResource cpRes = (FileSystemResource) res;
            return cpRes.getPath();
        }
        try {
            URL url = res.getURL();
            if (url != null) {
                String path = url.toString();
                int idx = path.lastIndexOf('!');
                if (idx > 0) {
                    return path.substring(idx + 1);
                }
            }
        } catch (IOException e) {
        }

        if (res.getFilename() != null) {
            return res.getFilename();
        }
        return res.getDescription();
    }

    protected Properties getProperties(Properties props) {
        Properties p = new Properties();
        if (props != null) {
            p.putAll(props);
        }
        p.putAll(Resources.getIbatisIniProperties());
        return p;
    }

    /**
     * Apply the given iBATIS TransactionConfig to the SqlMapClient.
     * <p>
     * The default implementation casts to ExtendedSqlMapClient, retrieves the maximum number of concurrent transactions
     * from the SqlMapExecutorDelegate, and sets an iBATIS TransactionManager with the given TransactionConfig.
     * 
     * @param sqlMapClient
     *            the SqlMapClient to apply the TransactionConfig to
     * @param transactionConfig
     *            the iBATIS TransactionConfig to apply
     * @see com.ibatis.sqlmap.engine.impl.ExtendedSqlMapClient
     * @see com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate#setTxManager
     */
    protected void applyTransactionConfig(ExtendedSqlMapClient sqlMapClient, TransactionConfig transactionConfig) {
        sqlMapClient.getDelegate().setTxManager(new TransactionManager(transactionConfig));
    }

    @Override
    public SqlMapClient getObject() {
        return this.sqlMapClient;
    }

    @Override
    public Class<? extends SqlMapClient> getObjectType() {
        return (this.sqlMapClient != null ? this.sqlMapClient.getClass() : SqlMapClient.class);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
