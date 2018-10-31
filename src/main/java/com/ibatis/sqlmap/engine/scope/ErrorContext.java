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

import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;

import com.ibatis.common.Objects;

/**
 * An error context to help us create meaningful error messages
 */
public class ErrorContext {

    private String conn;
    private String resource;
    private String activity;
    private String objectId;
    private String sql, extraSql;
    private Object[] args;
    private String batchInfo;
    private String moreInfo, debugInfo;
    private Object cause;
    private int total;

    /**
     * Getter for the resource causing the problem
     * 
     * @return - the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * Setter for the resource causing the problem
     * 
     * @param resource
     *            - the resource
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * Getter for the activity that was happening when the error happened
     * 
     * @return - the activity
     */
    public String getActivity() {
        return activity;
    }

    /**
     * Getter for the activity that was happening when the error happened
     * 
     * @param activity
     *            - the activity
     */
    public void setActivity(String activity) {
        this.activity = activity;
    }

    /**
     * Getter for the object ID where the problem happened
     * 
     * @return - the object id
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Setter for the object ID where the problem happened
     * 
     * @param objectId
     *            - the object id
     */
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    /**
     * @return the sql
     */
    public String getSql() {
        return sql;
    }

    /**
     * @param sql
     *            the sql to set
     */
    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getExtraSql() {
        return extraSql;
    }

    public void setExtraSql(String sql) {
        this.extraSql = sql;
    }

    public void setBatchInfo(String batchInfo) {
        this.batchInfo = batchInfo;
    }

    public void setDebugInfo(String debugInfo) {
        this.debugInfo = debugInfo;
    }

    /**
     * Getter for more information about the error
     * 
     * @return - more information
     */
    public String getMoreInfo() {
        return moreInfo;
    }

    /**
     * Setter for more information about the error
     * 
     * @param moreInfo
     *            - more information
     */
    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
    }

    /**
     * Getter for the cause of the error
     * 
     * @return - the cause
     */
    public Object getCause() {
        return cause;
    }

    /**
     * Setter for the cause of the error
     * 
     * @param cause
     *            - the cause
     */
    public void setCause(Object cause) {
        this.cause = cause;
    }

    public String toStr(List<ErrorContext> list) {
        StringBuilder message = new StringBuilder();
        toStr(Prefix, message);
        if (list != null) {
            for (ErrorContext ec : list) {
                ec.toStr(Prefix2, message);
            }
        }
        return message.toString();
    }

    public String toStr() {
        StringBuilder message = new StringBuilder();
        toStr(Prefix, message);
        return message.toString();
    }

    void toStr(String Prefix, StringBuilder message) {
        // resource
        if (objectId != null) {
            message.append(Prefix);
            message.append("Current_obj - ");
            message.append(objectId);
            if (conn != null) {
                message.append(" of ");
                message.append(conn);
            }
            if (resource != null) {
                message.append(" in ");
                message.append(resource);
            }
        } else if (activity != null) {
            message.append(Prefix);
            message.append("Current_act - ");
            message.append(activity);
            if (conn != null) {
                message.append(" of ");
                message.append(conn);
            }
            if (resource != null) {
                message.append(" in ");
                message.append(resource);
            }
        }

        // sql
        if (sql != null) {
            message.append(Prefix);
            message.append("Current_sql - ");
            message.append(sql);
        }
        if (extraSql != null) {
            message.append(Prefix);
            message.append("Extra_sql - ");
            message.append(extraSql);
        }

        // args
        if (args != null && args.length > 0) {
            message.append(Prefix);
            if (batchInfo != null)
                message.append("Lasted_args - ");
            else {
                message.append("Current_args - ");
            }
            asList(message, args);
        }

        if (batchInfo != null) {
            message.append(Prefix);
            message.append(batchInfo);
        }

        // debug info
        if (debugInfo != null) {
            message.append(Prefix);
            message.append(debugInfo);
        }
    }

    static final String Prefix = "\n  +-- ";
    static final String Prefix2 = "\n  +--- ";

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();

        // resource
        if (resource != null) {
            message.append(Prefix);
            message.append("The error occurred in ");
            message.append(resource);
            message.append('.');
        }

        // activity
        if (activity != null) {
            message.append(Prefix);
            message.append("The error occurred while ");
            message.append(activity);
            message.append('.');
        }

        // conn
        if (conn != null) {
            message.append(Prefix);
            message.append("Current connection - ");
            message.append(conn);
        }

        // object
        if (objectId != null) {
            message.append(Prefix);
            message.append("Current object - ");
            message.append(objectId);
        }
        // sql
        if (sql != null) {
            message.append(Prefix);
            message.append("Current sql - ");
            message.append(sql);
        }
        // extraSql
        if (extraSql != null) {
            message.append(Prefix);
            message.append("Extra sql - ");
            message.append(extraSql);
        }

        // args
        if (args != null && args.length > 0) {
            message.append(Prefix);
            if (batchInfo != null)
                message.append("Lasted args - ");
            else
                message.append("Current args - ");
            asList(message, args);
        }

        if (batchInfo != null) {
            message.append(Prefix);
            message.append(batchInfo);
        }

        // more info
        if (moreInfo != null) {
            message.append(Prefix);
            message.append(moreInfo);
        }

        // cause
        if (cause != null) {
            if (cause instanceof SQLException) {
                message.append(Prefix);
                message.append("ErrorCode: ");
                message.append(((SQLException) cause).getErrorCode());
                message.append(", SqlState: ");
                message.append(((SQLException) cause).getSQLState());
                message.append(", Cause: ");
            } else {
                message.append(Prefix);
                message.append("Cause: ");
            }
            message.append(String.valueOf(cause).replace('\n', ' '));
        }

        return message.toString();
    }

    static final NumberFormat nf = NumberFormat.getNumberInstance();
    static {
        nf.setMaximumFractionDigits(5);
        nf.setMaximumIntegerDigits(10);
        nf.setGroupingUsed(false);
    }

    static void asList(StringBuilder buf, Object[] array) {
        buf.append("{ ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            Object obj = array[i];
            if (obj == null) {
                buf.append("null");
            } else if (obj instanceof Number) {
                double d = ((Number) obj).doubleValue();
                if (d <= 65535d && d >= -65536d) {
                    buf.append(nf.format(d));
                } else {
                    buf.append(obj);
                }
            } else if (obj instanceof Date) {
                Objects.outputDate(buf, (Date) obj);
            } else if (obj instanceof String) {
                String s = (String) obj;
                if (s.indexOf('\n') >= 0 || s.length() > 256) {
                    buf.append("(...skip ").append(s.length()).append(" chars...)");
                } else {
                    buf.append("'").append(s).append("'");
                }
            } else if (obj instanceof Boolean) {
                buf.append(obj);
            } else if (obj instanceof byte[]) {
                buf.append("byte[").append(((byte[]) obj).length).append(']');
            } else if (obj instanceof char[]) {
                buf.append("char[").append(((char[]) obj).length).append(']');
            } else if (obj instanceof Character) {
                buf.append("'").append(obj).append("'");
            } else if (obj instanceof Enum) {
                buf.append("'").append(((Enum<?>) obj).name()).append("'");
            } else {
                buf.append(obj.getClass().getSimpleName()).append("@")
                    .append(Integer.toHexString(System.identityHashCode(obj)));
            }
        }
        buf.append(" }");
    }

    /**
     * Clear the error context
     */
    public void reset() {
        resource = null;
        activity = null;
        objectId = null;
        sql = null;
        args = null;
        moreInfo = null;
        cause = null;
    }

    public void setArgs(Object[] args) {
        if (args != null && args.length > 0) {
            this.args = args;
        }
    }

    public void setConnection(Connection conn) {
        this.conn = String.valueOf(conn.hashCode());
    }

    public void setConnectionCode(String conn) {
        this.conn = conn;
    }

    public void setTotal(int totalResults) {
        total = totalResults;
    }

    public int getTotal() {
        return total;
    }
}
