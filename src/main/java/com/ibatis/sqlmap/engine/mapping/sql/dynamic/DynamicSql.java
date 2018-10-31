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
package com.ibatis.sqlmap.engine.mapping.sql.dynamic;

import com.ibatis.common.Objects;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.parameter.InlineParameterMapParser;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.mapping.sql.SqlChild;
import com.ibatis.sqlmap.engine.mapping.sql.SqlText;
import com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.*;
import com.ibatis.sqlmap.engine.mapping.sql.simple.SimpleDynamicSql;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.scope.StatementScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DynamicSql implements Sql, DynamicParent {

    private static final InlineParameterMapParser PARAM_PARSER = new InlineParameterMapParser();

    private final List<SqlChild> children = new ArrayList<SqlChild>(3);
    private SqlMapExecutorDelegate delegate;

    public DynamicSql(SqlMapExecutorDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getSql(StatementScope statementScope, Object parameterObject) {
        String sql = statementScope.getDynamicSql();
        if (sql == null) {
            process(statementScope, parameterObject);
            sql = statementScope.getDynamicSql();
        }
        return sql;
    }

    @Override
    public ParameterMap getParameterMap(StatementScope statementScope, Object parameterObject) {
        ParameterMap map = statementScope.getDynamicParameterMap();
        if (map == null) {
            process(statementScope, parameterObject);
            map = statementScope.getDynamicParameterMap();
        }
        return map;
    }

    @Override
    public ResultMap getResultMap(StatementScope statementScope, Object parameterObject) {
        return statementScope.getResultMap();
    }

    @Override
    public void cleanup(StatementScope statementScope) {
        statementScope.setDynamicSql(null);
        statementScope.setDynamicParameterMap(null);
    }

    private void process(StatementScope statementScope, Object parameterObject) {
        SqlTagContext ctx = new SqlTagContext();
        List<SqlChild> localChildren = children;
        processBodyChildren(statementScope, ctx, parameterObject, localChildren.iterator());

        ParameterMap map = new ParameterMap(delegate);
        map.setId(statementScope.getStatement().getId() + "-InlineParameterMap");
        map.setParameterClass(((MappedStatement) statementScope.getStatement()).getParameterClass());
        map.setParameterMappingList(ctx.getParameterMappings());

        String dynSql = ctx.getBodyText();

        // Processes $substitutions$ after DynamicSql
        if (SimpleDynamicSql.isSimpleDynamicSql(dynSql)) {
            dynSql = new SimpleDynamicSql(delegate, dynSql).getSql(statementScope, parameterObject);
        }

        statementScope.setDynamicSql(dynSql);
        statementScope.setDynamicParameterMap(map);
    }

    private void processBodyChildren(StatementScope statementScope, SqlTagContext ctx, Object parameterObject,
        Iterator<SqlChild> localChildren) {
        processBodyChildren(statementScope, ctx, parameterObject, localChildren, ctx.getStringBuilder());
    }

    private void processBodyChildren(StatementScope statementScope, SqlTagContext ctx, Object parameterObject,
        Iterator<SqlChild> localChildren, StringBuilder buf) {
        while (localChildren.hasNext()) {
            SqlChild child = localChildren.next();
            if (child instanceof SqlText) {
                SqlText sqlText = (SqlText) child;
                String sqlStatement = sqlText.getText();
                if (sqlText.isWhiteSpace()) {
                    buf.append(sqlStatement);
                } else if (!sqlText.isPostParseRequired()) {

                    // BODY OUT
                    buf.append(sqlStatement);

                    ParameterMapping[] mappings = sqlText.getParameterMappings();
                    if (mappings != null) {
                        for (int i = 0, n = mappings.length; i < n; i++) {
                            ctx.addParameterMapping(mappings[i]);
                        }
                    }
                } else {

                    IterateContext itCtx = ctx.peekIterateContext();

                    if (null != itCtx && itCtx.isAllowNext()) {
                        itCtx.next();
                        itCtx.setAllowNext(false);
                        if (!itCtx.hasNext()) {
                            itCtx.setFinal(true);
                        }
                    }

                    Map<String, IterateIndex> itMap = new HashMap<String, IterateIndex>(); // ## sunsong
                    if (itCtx != null) {
                        StringBuilder sb = new StringBuilder(sqlStatement);
                        iteratePropertyReplace(sb, '#', itCtx, itMap);
                        iteratePropertyReplace(sb, '$', itCtx, itMap);
                        sqlStatement = sb.toString();
                    }

                    sqlText = PARAM_PARSER.parseInlineParameterMap(delegate.getTypeHandlerFactory(), sqlStatement);

                    ParameterMapping[] mappings = sqlText.getParameterMappings();
                    buf.append(sqlText.getText());
                    if (mappings != null) {
                        for (ParameterMapping pm : mappings) {
                            // ## sunsong
                            String propName = pm.getPropertyName();
                            IterateIndex itIdx = itMap.get(propName);
                            if (itIdx != null && propName.startsWith(itIdx.getProcessKey())) {
                                pm.setQucikValue(itIdx.getProcessValue());
                                pm.setQuickName(propName.substring(itIdx.getProcessKey().length()));
                            }
                            ctx.addParameterMapping(pm);
                        }
                    }
                }
            } else if (child instanceof SqlTag) {
                SqlTag tag = (SqlTag) child;
                SqlTagHandler handler = tag.getHandler();
                int response = SqlTagHandler.INCLUDE_BODY;
                int rfpDepth = ctx.removeFirtPrependStackSize();
                do {
                    StringBuilder body = new StringBuilder();

                    response = handler.doStartFragment(ctx, tag, parameterObject);
                    if (response != SqlTagHandler.SKIP_BODY) {

                        processBodyChildren(statementScope, ctx, parameterObject, tag.getChildren(), body);
                        response = handler.doEndFragment(ctx, tag, parameterObject, body);
                        handler.doPrepend(ctx, tag, parameterObject, body);

                        if (response != SqlTagHandler.SKIP_BODY) {
                            if (body.length() > 0) {
                                buf.append(body.toString());
                            }
                        }

                    }
                } while (response == SqlTagHandler.REPEAT_BODY);

                if (ctx.removeFirtPrependStackSize() > rfpDepth) {
                    ctx.popRemoveFirstPrependMarker();
                }

                IterateContext ic = ctx.peekIterateContext();
                if (ic != null && ic.getTag() == tag) {
                    ctx.setAttribute(tag, null);
                    ctx.popIterateContext();
                }

            }
        }
    }

    void iteratePropertyReplace(StringBuilder buf, char delim, IterateContext iterate, Map<String, IterateIndex> itMap) {
        {
            int startIndex = 0;
            int endIndex = -1;
            while (startIndex >= 0 && startIndex < buf.length()) {
                startIndex = Objects.indexOf(buf, delim, endIndex + 1);
                endIndex = Objects.indexOf(buf, delim, startIndex + 1);
                if (startIndex >= 0 && endIndex >= 0) {
                    // ## sunsong
                    String sstr = buf.substring(startIndex + 1, endIndex);
                    IterateIndex ii = iterate.addIndexToTagProperty(sstr);
                    String rstr = ii.getProcessString();
                    if (!sstr.equals(rstr)) {
                        if (ii.hasProcessValue()) {
                            itMap.put(rstr, ii);
                        }
                        buf.replace(startIndex + 1, endIndex, rstr);
                        endIndex += rstr.length() - sstr.length();
                    }
                }
            }
        }
    }

    public List<SqlChild> getChildren() {
        return children;
    }

    @Override
    public void addChild(SqlChild child) {
        children.add(child);
    }

    @Override
    public int hashCodex() {
        int ret = getClass().getName().hashCode();
        for (SqlChild sc : children) {
            ret += sc.hashCodex();
        }
        return ret;
    }

    @Override
    public String headText() {
        for (SqlChild sc : children) {
            if (sc instanceof SqlText) {
                return ((SqlText) sc).getText();
            }
        }
        return "";
    }
}
