/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.engine.dialect;

import org.ibatis.client.Dialect;

import com.ibatis.sqlmap.client.lexer.SqlLexer;
import com.ibatis.sqlmap.client.lexer.TT;
import com.ibatis.sqlmap.client.lexer.Token;
import com.ibatis.sqlmap.engine.scope.ErrorContext;

/**
 * LimitOffsetPageDialect for mysql 5.5+ and postgresql and db2 9.7+
 * <p>
 * Date: 2018-01-02
 * 
 * @author Song Sun
 * @version 1.0
 */
public class LimitOffsetPageDialect implements PageDialect {

    final Dialect dialect;
    final SqlLexer parser;
    boolean count;
    int skip;
    int max;
    static final TT[] kws = new TT[] { TT.Select, TT.Order, TT.Limit };

    public LimitOffsetPageDialect(Dialect d, String sql, boolean count, int skip, int max) {
        dialect = d;
        parser = new SqlLexer(sql, kws);
        this.count = count;
        this.skip = skip;
        this.max = max;
    }

    @Override
    public PageDialect canHandle(String productNameLowerCase, int majorVersion, int minorVersion) {
        if (dialect == Dialect.db2 && (majorVersion < 9 || minorVersion < 7)) {
            return null;
        }
        Token t = parser.firstKeyword();
        if (t == null || t.type != TT.Select) {
            return null;
        }
        if (parser.indexOf(TT.Limit) == -1) {
            return this;
        }
        if (parser.topLastIndexOf(TT.Limit) == -1) {
            return this;
        }
        return null;
    }

    @Override
    public String getPageSql(ErrorContext ec) {
        StringBuilder buf = new StringBuilder();
        buf.append(" LIMIT ").append(max);
        if (skip > 0) {
            buf.append(" OFFSET ").append(skip);
        }
        String add = buf.toString();
        String sql = parser.getSql() + add;
        if (ec.getSql() == null) {
            ec.setSql(sql);
        } else {
            ec.setSql(ec.getSql() + add);
        }
        return sql;
    }

    @Override
    public String getCountSql(ErrorContext ec) {
        int oIdx = parser.topLastIndexOf(TT.Order);
        if (oIdx == -1 || parser.topIndexOf(TT.Question, oIdx + 1) > 0) {
            ec.setExtraSql("select count(1) from (...) _jbatis_tmp_cnt_");
            return "select count(1) from (" + parser.getSql() + ") _jbatis_tmp_cnt_";
        }
        Token ot = parser.getCascadeTokens().get(oIdx);
        ec.setExtraSql("select count(1) from (.. -< " + parser.getSql().substring(ot.offset) + ") _jbatis_tmp_cnt_");
        return "select count(1) from (" + parser.getSql().substring(0, ot.offset) + ") _jbatis_tmp_cnt_";
    }
}
