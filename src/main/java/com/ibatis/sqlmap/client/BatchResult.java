/*
 *  Copyright 2006 The Apache Software Foundation
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
package com.ibatis.sqlmap.client;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import com.ibatis.sqlmap.engine.scope.ErrorContext;

/**
 * This class holds the statement and row information for every successful batch executed by iBATIS
 */
public class BatchResult implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5329502080151453045L;
    private String sql;
    private String statementId;
    private ErrorContext errorContext;
    private int[] updateCounts;
    private transient PreparedStatement preparedStatement;

    public int total;
    public int totalRows;
    private ArrayList<Object[]> args;
    private Object[] lastArg;

    public BatchResult(int batchSize, String sql, ErrorContext ec) {
        super();
        this.statementId = ec.getObjectId();
        this.sql = sql;
        this.errorContext = ec;
        if (ec.getSql() == null) {
            this.errorContext.setSql(sql);
        }

        if (batchSize > 0) {
            args = new ArrayList<Object[]>(batchSize);
        } else {
            args = new ArrayList<Object[]>();
        }
    }

    public String getSql() {
        return sql;
    }

    public int[] getUpdateCounts() {
        return updateCounts;
    }

    public void setUpdateCounts(int[] updateCounts) {
        this.updateCounts = updateCounts;
    }

    public String getStatementId() {
        return statementId;
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    public void setPreparedStatement(PreparedStatement pstat) {
        this.preparedStatement = pstat;
        if (pstat == null) {
            args.clear();
        }
    }

    public void addArgs(Object[] os) {
        args.add(os);
        lastArg = os;
    }

    public Object[] getArgs(int i) {
        if (i < 0 || i >= args.size()) {
            return lastArg;
        }
        return args.get(i);
    }

    public ErrorContext getErrorContext() {
        return errorContext;
    }
}
