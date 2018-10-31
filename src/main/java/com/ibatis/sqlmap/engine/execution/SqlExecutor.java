package com.ibatis.sqlmap.engine.execution;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import com.ibatis.sqlmap.client.BatchResult;
import com.ibatis.sqlmap.client.event.PageHandler;
import com.ibatis.sqlmap.engine.config.SqlMapConfiguration;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * Classes responsible for executing the SQL implement this interface Support for custom SQL Executors.
 */
public interface SqlExecutor {
    //
    // Constants
    //
    /**
     * Constant to let us know not to skip anything
     */
    int ZERO = 0;
    /**
     * Constant to let us know to include all records
     */
    int NO_LIMIT = -1;
    int ONE = 1;
    int TWO = 2;

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
    public int executeUpdate(String id, StatementScope statementScope, Connection conn, String sql, Object[] parameters)
        throws SQLException;

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
    public int addBatch(StatementScope statementScope, Connection conn, String sql,
        Object[] parameters) throws SQLException;

    /**
     * Execute a batch of statements
     *
     * @param sessionScope
     *            - the session scope
     * @return - the number of rows impacted by the batch
     * @throws SQLException
     *             - if a statement fails
     */
    public int executeBatch(SessionScope sessionScope) throws SQLException;

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
    public List<BatchResult> executeBatchDetailed(SessionScope sessionScope) throws SQLException, BatchException;

    /**
     * Long form of the method to execute a query
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the SQL statement to execute
     * @param parameters
     *            - the parameters for the statement
     * @param skipResults
     *            - the number of results to skip
     * @param maxResults
     *            - the maximum number of results to return
     * @param callback
     *            - the row handler for the query
     * @throws SQLException
     *             - if the query fails
     */
    public void executeQuery(String id, StatementScope statementScope, Connection conn, String sql, Object[] parameters,
        int skipResults, int maxResults, RowHandlerCallback callback) throws SQLException;

    /**
     * Long form of the method to execute a query
     *
     * @param statementScope
     *            - the request scope
     * @param conn
     *            - the database connection
     * @param sql
     *            - the SQL statement to execute
     * @param parameters
     *            - the parameters for the statement
     * @param skipResults
     *            - the number of results to skip
     * @param maxResults
     *            - the maximum number of results to return
     * @param callback
     *            - the row handler for the query
     * @param pageHandler
     *            - the page handler for the query
     * @throws SQLException
     *             - if the query fails
     */
    public void executeQueryPage(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, int skipResults, int maxResults, RowHandlerCallback callback, PageHandler pageHandler)
        throws SQLException;

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
    public int executeUpdateProcedure(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters) throws SQLException;

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
     * @param skipResults
     *            - the number of results to skip
     * @param maxResults
     *            - the maximum number of results to return
     * @param callback
     *            - a row handler for processing the results
     * @throws SQLException
     *             - if the procedure fails
     */
    public void executeQueryProcedure(String id, StatementScope statementScope, Connection conn, String sql,
        Object[] parameters, int skipResults, int maxResults, RowHandlerCallback callback) throws SQLException;

    /**
     * Clean up any batches on the session
     *
     * @param sessionScope
     *            - the session to clean up
     */
    public void cleanup(SessionScope sessionScope);

    /**
     * Initializing SQL Executor by passing configuration and global properties.
     * 
     * @param config
     *            - the configuration class, which contains complete configuration info
     * @param globalProps
     */
    public void init(SqlMapConfiguration config, Properties globalProps);

    boolean isDebugSql();

    @Deprecated
    boolean isTraceSql();

    boolean isCheckSql();

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
     * @return - the generated key if has selectKey or the number of records changed.
     * @throws SQLException
     *             - if the insert fails
     */
    public <T> T executeInsert(String id, StatementScope statementScope, Connection connection, String sql,
        Object[] parameters, ResultMap keyResultMap) throws SQLException;
}
