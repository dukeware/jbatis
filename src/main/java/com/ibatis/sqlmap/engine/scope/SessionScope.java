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
package com.ibatis.sqlmap.engine.scope;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.SqlMapTransactionManager;
import com.ibatis.sqlmap.engine.execution.Batch;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.transaction.Transaction;
import com.ibatis.sqlmap.engine.transaction.TransactionState;

/**
 * A Session based implementation of the Scope interface
 */
public class SessionScope {
    // Used by Any
    private SqlMapClient sqlMapClient;
    private SqlMapExecutor sqlMapExecutor;
    private SqlMapTransactionManager sqlMapTxMgr;
    // Used by TransactionManager
    private Transaction transaction;
    private TransactionState transactionState;
    // Used by SqlMapExecutorDelegate.setUserProvidedTransaction()
    private TransactionState savedTransactionState;
    // Used by StandardSqlMapClient and GeneralStatement
    private int batchSize; // 0: not in batch, 1+: limited batch size, -1: unlimited batch size
    // ## auto batch tx
    public boolean autoBatch;
    // Used by SqlExecutor
    private Batch batch;
    private boolean commitRequired;
    private Map<Object, PreparedStatement> preparedStatements;

    /**
     * Default constructor
     */
    public SessionScope() {
        this.preparedStatements = new HashMap<Object, PreparedStatement>();
        this.batchSize = 0;
    }

    /**
     * Get the SqlMapClient for the session
     *
     * @return - the SqlMapClient
     */
    public SqlMapClient getSqlMapClient() {
        return sqlMapClient;
    }

    /**
     * Set the SqlMapClient for the session
     *
     * @param sqlMapClient
     *            - the SqlMapClient
     */
    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }

    /**
     * Get the SQL executor for the session
     *
     * @return - the SQL executor
     */
    public SqlMapExecutor getSqlMapExecutor() {
        return sqlMapExecutor;
    }

    /**
     * Get the SQL executor for the session
     *
     * @param sqlMapExecutor
     *            - the SQL executor
     */
    public void setSqlMapExecutor(SqlMapExecutor sqlMapExecutor) {
        this.sqlMapExecutor = sqlMapExecutor;
    }

    /**
     * Get the transaction manager
     *
     * @return - the transaction manager
     */
    public SqlMapTransactionManager getSqlMapTxMgr() {
        return sqlMapTxMgr;
    }

    /**
     * Set the transaction manager
     *
     * @param sqlMapTxMgr
     *            - the transaction manager
     */
    public void setSqlMapTxMgr(SqlMapTransactionManager sqlMapTxMgr) {
        this.sqlMapTxMgr = sqlMapTxMgr;
    }

    /**
     * Tells us if we are in batch mode or not
     *
     * @return - true if we are working with a batch
     */
    public boolean isInBatch() {
        return batchSize != 0;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Turn batch mode on or off
     *
     * @param inBatch
     *            - the switch
     */
    public void setInBatch(int inBatch) {
        this.batchSize = inBatch;
    }

    /**
     * Getter for the session transaction
     *
     * @return - the transaction
     */
    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * Setter for the session transaction
     *
     * @param transaction
     *            - the transaction
     */
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Getter for the transaction state of the session
     *
     * @return - the state
     */
    public TransactionState getTransactionState() {
        return transactionState;
    }

    /**
     * Setter for the transaction state of the session
     *
     * @param transactionState
     *            - the new transaction state
     */
    public void setTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    /**
     * Getter for the batch of the session
     *
     * @return - the batch
     */
    public Batch getBatch() {
        return batch;
    }

    /**
     * Stter for the batch of the session
     *
     * @param batch
     *            the new batch
     */
    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    /**
     * Getter to tell if a commit is required for the session
     *
     * @return - true if a commit is required
     */
    public boolean isCommitRequired() {
        return commitRequired;
    }

    /**
     * Setter to tell the session that a commit is required for the session
     *
     * @param commitRequired
     *            - the flag
     */
    public void setCommitRequired(boolean commitRequired) {
        this.commitRequired = commitRequired;
    }

    public boolean hasPreparedStatementFor(Object key) {
        return preparedStatements.containsKey(key);
    }

    public boolean hasPreparedStatement(PreparedStatement ps) {
        return preparedStatements.containsValue(ps);
    }

    public PreparedStatement getPreparedStatement(Object key) throws SQLException {
        if (!hasPreparedStatementFor(key))
            throw new SqlMapException("Could not get prepared statement.  This is likely a bug.");
        PreparedStatement ps = (PreparedStatement) preparedStatements.get(key);
        return ps;
    }

    public void putPreparedStatement(SqlMapExecutorDelegate delegate, Object key, PreparedStatement ps) {
        if (delegate.isStatementCacheEnabled()) {
            if (!isInBatch()) {
                if (hasPreparedStatementFor(key))
                    throw new SqlMapException("Duplicate prepared statement found.  This is likely a bug.");
                preparedStatements.put(key, ps);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
    }
    
    public void cleanup() {
        for (PreparedStatement ps : preparedStatements.values()) {
            try {
                ps.close();
            } catch (Exception e) {
                // ignore -- we don't care if this fails at this point.
            }
        }
        preparedStatements.clear();
    }

    /**
     * Saves the current transaction state
     */
    public void saveTransactionState() {
        savedTransactionState = transactionState;
    }

    /**
     * Restores the previously saved transaction state
     */
    public void recallTransactionState() {
        transactionState = savedTransactionState;
    }

}
