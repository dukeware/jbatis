/*
 *  Copyright 2004 Clinton Begin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ibatis.sqlmap.engine.execution;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.ibatis.common.Objects;
import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.client.event.PageHandler;
import com.ibatis.sqlmap.client.event.RowSetHandler;
import com.ibatis.sqlmap.client.event.TotalRowHandler;
import com.ibatis.sqlmap.engine.config.SqlMapConfiguration;
import com.ibatis.sqlmap.engine.dialect.PageDialect;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactoryUtil;
import com.ibatis.sqlmap.engine.mapping.statement.DefaultRowHandler;
import com.ibatis.sqlmap.engine.mapping.statement.MappedRowHandler;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * Class responsible for executing the SQL
 */
public class DefaultSqlExecutor implements SqlExecutor {

    SqlMapExecutorDelegate delegate;

    //
    // Public Methods
    //

    public DefaultSqlExecutor(SqlMapExecutorDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Execute an update
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the sql statement to execute
     * @param parameters
     *            - the parameters for the sql statement
     * @return - the number of records changed
     * @throws SQLException
     *             - if the update fails
     */
    @Override
    public int executeUpdate(String id, StatementScope statementScope, Connection conn, String sql, Object[] parameters)
        throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("executing update");
        if (errorContext.getSql() == null)
            errorContext.setSql(sql);
        PreparedStatement ps = null;
        setupResultObjectFactory(statementScope);
        int rows = 0;
        try {
            errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
            ps = prepareStatement(statementScope.getSession(), conn, sql, false);
            setStatementTimeout(statementScope.getStatement(), ps);
            errorContext.setMoreInfo("Check the parameters (set parameters failed).");
            Object[] args = statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
            errorContext.setArgs(args);
            errorContext.setMoreInfo("Check the statement (update failed).");
            ps.execute();
            rows = ps.getUpdateCount();
            // errorContext.setSql(null);
        } finally {
            closeStatement(statementScope.getSession(), ps);
            cleanupResultObjectFactory();
        }
        return rows;
    }

    /**
     * Execute an insert
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the sql statement to execute
     * @param parameters
     *            - the parameters for the sql statement
     * @return - the generated key
     * @throws SQLException
     *             - if the insert fails
     */
    @Override
    public <T> T executeInsert(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, ResultMap keyResultMap) throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("executing update");
        if (errorContext.getSql() == null)
            errorContext.setSql(sql);
        PreparedStatement ps = null;
        setupResultObjectFactory(statementScope);
        T object = null;
        try {
            errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
            ps = prepareStatement(statementScope.getSession(), conn, sql, true);
            setStatementTimeout(statementScope.getStatement(), ps);
            errorContext.setMoreInfo("Check the parameters (set parameters failed).");
            Object[] args = statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
            errorContext.setArgs(args);
            errorContext.setMoreInfo("Check the statement (update failed).");
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            DefaultRowHandler rowHandler = new DefaultRowHandler();
            statementScope.setResultMap(keyResultMap);
            RowHandlerCallback callback = new RowHandlerCallback(keyResultMap, null, rowHandler);
            handleResults(statementScope, rs, ZERO, 1, callback);
            // errorContext.setSql(null);
            List<T> list = Objects.uncheckedCast(rowHandler.getList());

            if (list.size() > 0) {
                object = list.get(0);
            }
        } finally {
            closeStatement(statementScope.getSession(), ps);
            cleanupResultObjectFactory();
        }
        return object;
    }

    /**
     * Adds a statement to a batch
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the sql statement
     * @param parameters
     *            - the parameters for the statement
     * @throws SQLException
     *             - if the statement fails
     */
    @Override
    public int addBatch(StatementScope statementScope, Connection conn, String sql,
        Object[] parameters) throws SQLException {
        return doAddBatch(false, statementScope, conn, sql, parameters);
    }

    public int doAddBatch(boolean debug, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters) throws SQLException {
        Batch batch = statementScope.getSession().getBatch();
        if (batch == null || batch.isCleanup()) {
            batch = new Batch(statementScope.getSession().getBatchSize(), debug);
            statementScope.getSession().setBatch(batch);
        }
        batch.addBatch(statementScope, conn, sql, parameters);
        if (batch.batchSize > 0 && batch.getSize() >= batch.batchSize) {
            return executeBatch(statementScope.getSession());
        }
        return 0;
    }

    /**
     * Execute a batch of statements
     *
     * @param sessionScope
     *            - the session scope
     * @return - the number of rows impacted by the batch
     * @throws SQLException
     *             - if a statement fails
     */
    @Override
    public int executeBatch(SessionScope sessionScope) throws SQLException {
        int rows = 0;
        Batch batch = sessionScope.getBatch();
        if (batch != null && !batch.isCleanup()) {
            try {
                rows = batch.executeBatch();
            } finally {
                batch.cleanupBatch(sessionScope);
            }
        }
        return rows;
    }

    /**
     * Execute a batch of statements
     *
     * @param sessionScope
     *            - the session scope
     * @return - a List of BatchResult objects (may be null if no batch has been initiated). There will be one
     *         BatchResult object in the list for each sub-batch executed
     * @throws SQLException
     *             if a database access error occurs, or the drive does not support batch statements
     * @throws BatchException
     *             if the driver throws BatchUpdateException
     */
    @Override
    public List<BatchResult> executeBatchDetailed(SessionScope sessionScope) throws SQLException, BatchException {
        List<BatchResult> answer = null;
        Batch batch = sessionScope.getBatch();
        if (batch != null && !batch.isCleanup()) {
            try {
                answer = batch.executeBatchDetailed();
            } finally {
                batch.cleanupBatch(sessionScope);
            }
        }
        return answer;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibatis.sqlmap.engine.execution.SqlExecutor#executeQueryPage(java.lang.String,
     *      com.ibatis.sqlmap.engine.scope.StatementScope, java.sql.Connection, java.lang.String, java.lang.Object[],
     *      int, int, com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback,
     *      com.ibatis.sqlmap.client.event.PageHandler)
     */
    @Override
    public void executeQueryPage(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, int skip, int max, RowHandlerCallback callback, PageHandler pageHandler)
        throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("executing query page");

        PageDialect pageDialect = delegate.getPageDialect(id, sql, true, skip, max);
        String countSql = null;
        if (pageDialect != null) {
            sql = pageDialect.getPageSql(errorContext);
            countSql = pageDialect.getCountSql(errorContext);
        }
        if (errorContext.getSql() == null)
            errorContext.setSql(sql);
        PreparedStatement ps = null;
        ResultSet rs = null;
        setupResultObjectFactory(statementScope);
        try {
            errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
            boolean nostat = statementScope.getStatement() == null;
            Integer rsType = nostat ? null : statementScope.getStatement().getResultSetType();
            if (rsType != null) {
                ps = prepareStatement(statementScope.getSession(), conn, sql, rsType);
            } else {
                ps = prepareStatement(statementScope.getSession(), conn, sql, false);
            }
            setStatementTimeout(statementScope.getStatement(), ps);
            Integer fetchSize = nostat ? null : statementScope.getStatement().getFetchSize();
            if (fetchSize != null) {
                try {
                    ps.setFetchSize(fetchSize.intValue());
                } catch (Exception e) {
                    statementScope.getStatement().setFetchSize(null);
                }
            }
            errorContext.setMoreInfo("Check the parameters (set parameters failed).");
            Object[] args = statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
            errorContext.setArgs(args);
            errorContext.setMoreInfo("Check the statement (query failed).");
            ps.execute();
            errorContext.setMoreInfo("Check the results (failed to retrieve results).");

            // Begin ResultSet Handling
            if (pageDialect != null) {
                rs = getFirstResultSet(statementScope, ps);
                handleResults(statementScope, rs, ZERO, NO_LIMIT, callback);
                closeResultSet(rs);
                closeStatement(statementScope.getSession(), ps);

                errorContext.setMoreInfo("Check the results (failed to count total).");
                ps = prepareStatement(statementScope.getSession(), conn, countSql, false);
                if (countSql.contains("?")) {
                    statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
                }

                rs = ps.executeQuery();
                rs.next();
                int total = rs.getInt(1);
                pageHandler.setTotal(total);
                errorContext.setTotal(total);
            } else {
                rs = handleMultipleResults(ps, statementScope, skip, max, callback);
            }
            // End ResultSet Handling
        } finally {
            try {
                closeResultSet(rs);
            } finally {
                closeStatement(statementScope.getSession(), ps);
                cleanupResultObjectFactory();
            }
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibatis.sqlmap.engine.execution.SqlExecutor#executeQuery(java.lang.String,
     *      com.ibatis.sqlmap.engine.scope.StatementScope, java.sql.Connection, java.lang.String, java.lang.Object[],
     *      int, int, com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback)
     */
    @Override
    public void executeQuery(String id, StatementScope statementScope, Connection conn, String sql, Object[] parameters,
        int skip, int max, RowHandlerCallback callback) throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("executing query");
        PageDialect pageDialect = delegate.getPageDialect(id, sql, false, skip, max);
        if (pageDialect != null) {
            sql = pageDialect.getPageSql(errorContext);
        }
        if (errorContext.getSql() == null)
            errorContext.setSql(sql);
        PreparedStatement ps = null;
        ResultSet rs = null;
        setupResultObjectFactory(statementScope);
        try {
            errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
            boolean nostat = statementScope.getStatement() == null;
            Integer rsType = nostat ? null : statementScope.getStatement().getResultSetType();
            if (rsType != null) {
                ps = prepareStatement(statementScope.getSession(), conn, sql, rsType);
            } else {
                ps = prepareStatement(statementScope.getSession(), conn, sql, false);
            }
            setStatementTimeout(statementScope.getStatement(), ps);
            Integer fetchSize = nostat ? null : statementScope.getStatement().getFetchSize();
            if (fetchSize != null) {
                try {
                    ps.setFetchSize(fetchSize.intValue());
                } catch (Exception e) {
                    statementScope.getStatement().setFetchSize(null);
                }
            }
            errorContext.setMoreInfo("Check the parameters (set parameters failed).");
            Object[] args = statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
            errorContext.setArgs(args);
            errorContext.setMoreInfo("Check the statement (query failed).");
            ps.execute();
            errorContext.setMoreInfo("Check the results (failed to retrieve results).");

            // Begin ResultSet Handling
            if (pageDialect != null) {
                rs = getFirstResultSet(statementScope, ps);
                handleResults(statementScope, rs, ZERO, NO_LIMIT, callback);
            } else {
                rs = handleMultipleResults(ps, statementScope, skip, max, callback);
            }
            // End ResultSet Handling
        } finally {
            try {
                closeResultSet(rs);
            } finally {
                closeStatement(statementScope.getSession(), ps);
                cleanupResultObjectFactory();
            }
        }
    }

    /**
     * Execute a stored procedure that updates data
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the SQL to call the procedure
     * @param parameters
     *            - the parameters for the procedure
     * @return - the rows impacted by the procedure
     * @throws SQLException
     *             - if the procedure fails
     */
    @Override
    public int executeUpdateProcedure(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters) throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("executing update procedure");
        if (errorContext.getSql() == null)
            errorContext.setSql(sql);
        CallableStatement cs = null;
        setupResultObjectFactory(statementScope);
        int rows = 0;
        try {
            errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
            cs = prepareCall(statementScope.getSession(), conn, sql);
            setStatementTimeout(statementScope.getStatement(), cs);
            ParameterMap parameterMap = statementScope.getParameterMap();
            ParameterMapping[] mappings = parameterMap.getParameterMappings();
            errorContext.setMoreInfo("Check the output parameters (register output parameters failed).");
            registerOutputParameters(cs, mappings);
            errorContext.setMoreInfo("Check the parameters (set parameters failed).");
            Object[] args = parameterMap.setParameters(statementScope, cs, parameters);
            errorContext.setArgs(args);
            errorContext.setMoreInfo("Check the statement (update procedure failed).");
            cs.execute();
            rows = cs.getUpdateCount();
            errorContext.setMoreInfo("Check the output parameters (retrieval of output parameters failed).");
            retrieveOutputParameters(statementScope, cs, mappings, parameters, null);
            // errorContext.setSql(null);
        } finally {
            closeStatement(statementScope.getSession(), cs);
            cleanupResultObjectFactory();
        }
        return rows;
    }

    /**
     * Execute a stored procedure
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the sql to call the procedure
     * @param parameters
     *            - the parameters for the procedure
     * @param skip
     *            - the number of results to skip
     * @param max
     *            - the maximum number of results to return
     * @param callback
     *            - a row handler for processing the results
     * @throws SQLException
     *             - if the procedure fails
     */
    @Override
    public void executeQueryProcedure(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, int skip, int max, RowHandlerCallback callback) throws SQLException {
        ErrorContext errorContext = statementScope.getErrorContext();
        errorContext.setActivity("executing query procedure");
        if (errorContext.getSql() == null)
            errorContext.setSql(sql);
        CallableStatement cs = null;
        ResultSet rs = null;
        setupResultObjectFactory(statementScope);
        try {
            errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
            Integer rsType = statementScope.getStatement().getResultSetType();
            if (rsType != null) {
                cs = prepareCall(statementScope.getSession(), conn, sql, rsType);
            } else {
                cs = prepareCall(statementScope.getSession(), conn, sql);
            }
            setStatementTimeout(statementScope.getStatement(), cs);
            Integer fetchSize = statementScope.getStatement().getFetchSize();
            if (fetchSize != null) {
                try {
                    cs.setFetchSize(fetchSize.intValue());
                } catch (SQLException e) {
                    statementScope.getStatement().setFetchSize(null);
                }
            }
            ParameterMap parameterMap = statementScope.getParameterMap();
            ParameterMapping[] mappings = parameterMap.getParameterMappings();
            errorContext.setMoreInfo("Check the output parameters (register output parameters failed).");
            registerOutputParameters(cs, mappings);
            errorContext.setMoreInfo("Check the parameters (set parameters failed).");
            Object[] args = parameterMap.setParameters(statementScope, cs, parameters);
            errorContext.setArgs(args);
            errorContext.setMoreInfo("Check the statement (update procedure failed).");
            cs.execute();
            errorContext.setMoreInfo("Check the results (failed to retrieve results).");

            // Begin ResultSet Handling
            rs = handleMultipleResults(cs, statementScope, skip, max, callback);
            // End ResultSet Handling
            errorContext.setMoreInfo("Check the output parameters (retrieval of output parameters failed).");
            retrieveOutputParameters(statementScope, cs, mappings, parameters, callback);

            // errorContext.setSql(null);
        } finally {
            try {
                closeResultSet(rs);
            } finally {
                closeStatement(statementScope.getSession(), cs);
                cleanupResultObjectFactory();
            }
        }
    }

    @Override
    public void init(SqlMapConfiguration config, Properties globalProps) {
        delegate = config.getDelegate();
    }

    private ResultSet handleMultipleResults(PreparedStatement ps, StatementScope statementScope, int skip, int max,
        RowHandlerCallback callback) throws SQLException {
        ResultSet rs;
        rs = getFirstResultSet(statementScope, ps);
        if (rs != null) {
            handleResults(statementScope, rs, skip, max, callback);
        }

        // Multiple ResultSet handling
        if (callback.getRowHandler() instanceof DefaultRowHandler) {
            MappedStatement statement = statementScope.getStatement();
            DefaultRowHandler defaultRowHandler = ((DefaultRowHandler) callback.getRowHandler());
            if (statement != null && statement.hasMultipleResultMaps()) {
                List<List<?>> multipleResults = new ArrayList<List<?>>();
                multipleResults.add(defaultRowHandler.getList());
                ResultMap[] resultMaps = statement.getAdditionalResultMaps();
                int i = 0;
                while (moveToNextResultsSafely(statementScope, ps)) {
                    if (i >= resultMaps.length)
                        break;
                    ResultMap rm = resultMaps[i];
                    statementScope.setResultMap(rm);
                    rs = ps.getResultSet();
                    DefaultRowHandler rh = new DefaultRowHandler();
                    handleResults(statementScope, rs, skip, max, new RowHandlerCallback(rm, null, rh));
                    multipleResults.add(rh.getList());
                    i++;
                }
                defaultRowHandler.setList(multipleResults);
                statementScope.setResultMap(statement.getResultMap());
            } else {
                while (moveToNextResultsSafely(statementScope, ps))
                    ;
            }
        }
        // End additional ResultSet handling
        return rs;
    }

    private ResultSet getFirstResultSet(StatementScope scope, Statement stmt) throws SQLException {
        ResultSet rs = null;
        boolean hasMoreResults = true;
        while (hasMoreResults) {
            rs = stmt.getResultSet();
            if (rs != null) {
                break;
            }
            hasMoreResults = moveToNextResultsIfPresent(scope, stmt);
        }
        return rs;
    }

    private boolean moveToNextResultsIfPresent(StatementScope scope, Statement stmt) throws SQLException {
        boolean moreResults;
        // This is the messed up JDBC approach for determining if there are more results
        boolean movedToNextResultsSafely = moveToNextResultsSafely(scope, stmt);
        int updateCount = stmt.getUpdateCount();

        moreResults = !(!movedToNextResultsSafely && (updateCount == -1));

        // ibatis-384: workaround for mysql not returning -1 for stmt.getUpdateCount()
        if (moreResults == true) {
            moreResults = !(!movedToNextResultsSafely && !isMultipleResultSetSupportPresent(scope, stmt));
        }

        return moreResults;
    }

    private boolean moveToNextResultsSafely(StatementScope scope, Statement stmt) throws SQLException {
        if (isMultipleResultSetSupportPresent(scope, stmt)) {
            return stmt.getMoreResults();
        }
        return false;
    }

    /**
     * checks whether multiple result set support is present - either by direct support of the database driver or by
     * forcing it
     */
    private boolean isMultipleResultSetSupportPresent(StatementScope scope, Statement stmt) throws SQLException {
        return forceMultipleResultSetSupport(scope) || stmt.getConnection().getMetaData().supportsMultipleResultSets();
    }

    private boolean forceMultipleResultSetSupport(StatementScope scope) {
        return ((SqlMapClientImpl) scope.getSession().getSqlMapClient()).getDelegate()
            .isForceMultipleResultSetSupport();
    }

    private void handleResults(StatementScope statementScope, ResultSet rs, int skip, int max,
        RowHandlerCallback callback) throws SQLException {
        if (skip < 0) {
            skip = 0;
        }
        try {
            statementScope.setResultSet(rs);

            RowSetHandler rsh = null;
            if (callback.getRowHandler() instanceof RowSetHandler) {
                rsh = (RowSetHandler) callback.getRowHandler();
            }
            if (rsh != null) {
                rsh.handleResultSet(rs);
                return;
            }

            TotalRowHandler trh = null;
            if (callback.getRowHandler() instanceof TotalRowHandler) {
                trh = (TotalRowHandler) callback.getRowHandler();
            }
            MappedRowHandler<?, ?> mrh = null;
            if (callback.getRowHandler() instanceof MappedRowHandler<?, ?>) {
                mrh = (MappedRowHandler<?, ?>) callback.getRowHandler();
            }
            ResultMap resultMap = statementScope.getResultMap();
            int total = 0;
            if (resultMap != null || mrh != null) {
                // Skip Results
                if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
                    if (skip > 0) {
                        rs.absolute(skip);
                        if (trh != null)
                            total = trh.handleTotal(skip);
                    }
                } else {
                    int i = 0;
                    for (; i < skip; i++) {
                        if (!rs.next()) {
                            if (trh != null)
                                total = trh.handleTotal(i);
                            statementScope.getErrorContext().setTotal(total);
                            return;
                        }
                    }

                    if (trh != null)
                        total = trh.handleTotal(skip);
                }

                if (mrh != null) {
                    ResultSetMetaData md = rs.getMetaData();
                    mrh.handleMeta(md);
                }
                // Get Results
                int resultsFetched = 0;
                while ((max <= NO_LIMIT || resultsFetched < max) && rs.next()) {
                    if (mrh != null) {
                        mrh.handleRow(rs);
                    } else {
                        Object[] columnValues = resultMap.resolveSubMap(statementScope, rs).getResults(statementScope,
                            rs);
                        callback.handleResultObject(statementScope, columnValues, rs);
                    }
                    resultsFetched++;
                }
                if (trh != null) {
                    while (rs.next()) {
                        resultsFetched++;
                    }
                    total = trh.handleTotal(resultsFetched);
                }
            }
            statementScope.getErrorContext().setTotal(total);
        } finally {
            statementScope.setResultSet(null);
        }
    }

    private void retrieveOutputParameters(StatementScope statementScope, CallableStatement cs,
        ParameterMapping[] mappings, Object[] parameters, RowHandlerCallback callback) throws SQLException {
        for (int i = 0; i < mappings.length; i++) {
            ParameterMapping mapping = ((ParameterMapping) mappings[i]);
            if (mapping.isOutputAllowed()) {
                if ("java.sql.ResultSet".equalsIgnoreCase(mapping.getJavaTypeName())) {
                    ResultSet rs = (ResultSet) cs.getObject(i + 1);
                    ResultMap resultMap;
                    if (mapping.getResultMapName() == null) {
                        resultMap = statementScope.getResultMap();
                        handleOutputParameterResults(statementScope, resultMap, rs, callback);
                    } else {
                        SqlMapClientImpl client = (SqlMapClientImpl) statementScope.getSession().getSqlMapClient();
                        resultMap = client.getDelegate().getResultMap(mapping.getResultMapName());
                        DefaultRowHandler rowHandler = new DefaultRowHandler();
                        RowHandlerCallback handlerCallback = new RowHandlerCallback(resultMap, null, rowHandler);
                        handleOutputParameterResults(statementScope, resultMap, rs, handlerCallback);
                        parameters[i] = rowHandler.getList();
                    }
                    rs.close();
                } else {
                    parameters[i] = mapping.getTypeHandler().getResult(cs, i + 1);
                }
            }
        }
    }

    private void registerOutputParameters(CallableStatement cs, ParameterMapping[] mappings) throws SQLException {
        for (int i = 0; i < mappings.length; i++) {
            ParameterMapping mapping = ((ParameterMapping) mappings[i]);
            if (mapping.isOutputAllowed()) {
                if (null != mapping.getTypeName() && !mapping.getTypeName().isEmpty()) { // @added
                    cs.registerOutParameter(i + 1, mapping.getJdbcType(), mapping.getTypeName());
                } else {
                    if (mapping.getNumericScale() != null
                        && (mapping.getJdbcType() == Types.NUMERIC || mapping.getJdbcType() == Types.DECIMAL)) {
                        cs.registerOutParameter(i + 1, mapping.getJdbcType(), mapping.getNumericScale().intValue());
                    } else {
                        cs.registerOutParameter(i + 1, mapping.getJdbcType());
                    }
                }
            }
        }
    }

    private void handleOutputParameterResults(StatementScope statementScope, ResultMap resultMap, ResultSet rs,
        RowHandlerCallback callback) throws SQLException {
        ResultMap orig = statementScope.getResultMap();
        try {
            statementScope.setResultSet(rs);
            if (resultMap != null) {
                statementScope.setResultMap(resultMap);

                // Get Results
                while (rs.next()) {
                    Object[] columnValues = resultMap.resolveSubMap(statementScope, rs).getResults(statementScope, rs);
                    callback.handleResultObject(statementScope, columnValues, rs);
                }
            }
        } finally {
            statementScope.setResultSet(null);
            statementScope.setResultMap(orig);
        }
    }

    /**
     * Clean up any batches on the session
     *
     * @param sessionScope
     *            - the session to clean up
     */
    @Override
    public void cleanup(SessionScope sessionScope) {
        Batch batch = sessionScope.getBatch();
        if (batch != null) {
            if (!batch.isCleanup()) {
                batch.cleanupBatch(sessionScope);
            }
            sessionScope.setBatch(null);
        }
    }

    private static PreparedStatement prepareStatement(SessionScope sessionScope, Connection conn, String sql,
        Integer rsType) throws SQLException {
        Object key = Objects.getKey(sql, rsType, ResultSet.CONCUR_READ_ONLY);
        SqlMapExecutorDelegate delegate = ((SqlMapClientImpl) sessionScope.getSqlMapExecutor()).getDelegate();
        if (sessionScope.hasPreparedStatementFor(key)) {
            return sessionScope.getPreparedStatement(key);
        } else {
            PreparedStatement ps = conn.prepareStatement(sql, rsType.intValue(), ResultSet.CONCUR_READ_ONLY);
            sessionScope.putPreparedStatement(delegate, key, ps);
            return ps;
        }
    }

    private static CallableStatement prepareCall(SessionScope sessionScope, Connection conn, String sql, Integer rsType)
        throws SQLException {
        Object key = Objects.getKey("call", sql, rsType, ResultSet.CONCUR_READ_ONLY);
        SqlMapExecutorDelegate delegate = ((SqlMapClientImpl) sessionScope.getSqlMapExecutor()).getDelegate();
        if (sessionScope.hasPreparedStatementFor(key)) {
            return (CallableStatement) sessionScope.getPreparedStatement(key);
        } else {
            CallableStatement cs = conn.prepareCall(sql, rsType.intValue(), ResultSet.CONCUR_READ_ONLY);
            sessionScope.putPreparedStatement(delegate, key, cs);
            return cs;
        }
    }

    static PreparedStatement prepareStatement(SessionScope sessionScope, Connection conn, String sql, boolean genKey)
        throws SQLException {
        Object key = Objects.getKey(sql, genKey);
        SqlMapExecutorDelegate delegate = ((SqlMapClientImpl) sessionScope.getSqlMapExecutor()).getDelegate();
        if (sessionScope.hasPreparedStatementFor(key)) {
            return sessionScope.getPreparedStatement(key);
        } else {
            PreparedStatement ps = genKey ? conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                    : conn.prepareStatement(sql);
            sessionScope.putPreparedStatement(delegate, key, ps);
            return ps;
        }
    }

    static CallableStatement prepareCall(SessionScope sessionScope, Connection conn, String sql) throws SQLException {
        Object key = Objects.getKey("call", sql);
        SqlMapExecutorDelegate delegate = ((SqlMapClientImpl) sessionScope.getSqlMapExecutor()).getDelegate();
        if (sessionScope.hasPreparedStatementFor(key)) {
            return (CallableStatement) sessionScope.getPreparedStatement(key);
        } else {
            CallableStatement cs = conn.prepareCall(sql);
            sessionScope.putPreparedStatement(delegate, key, cs);
            return cs;
        }
    }

    static void closeStatement(SessionScope sessionScope, PreparedStatement ps) {
        if (ps != null) {
            if (!sessionScope.hasPreparedStatement(ps)) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * @param rs
     */
    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    static void setStatementTimeout(MappedStatement mappedStatement, Statement statement) throws SQLException {
        if (mappedStatement != null && mappedStatement.getTimeout() != null) {
            statement.setQueryTimeout(mappedStatement.getTimeout().intValue());
        }
    }

    private void setupResultObjectFactory(StatementScope statementScope) {
        SqlMapClientImpl client = (SqlMapClientImpl) statementScope.getSession().getSqlMapClient();
        String sid = null;
        if (statementScope.getStatement() != null) {
            sid = statementScope.getStatement().getId();
        }
        ResultObjectFactoryUtil.setupResultObjectFactory(client.getResultObjectFactory(), sid);
    }

    private void cleanupResultObjectFactory() {
        ResultObjectFactoryUtil.cleanupResultObjectFactory();
    }

    @Override
    public boolean isDebugSql() {
        return false;
    }

    @Override
    public boolean isTraceSql() {
        return false;
    }

    @Override
    public boolean isCheckSql() {
        return false;
    }
}
