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
 * OffsetFetchPageDialect for sqlserver 2012+ and oracle 12c+
 * <p>
 * Date: 2018-01-05
 * 
 * @author Song Sun
 * @version 1.0
 */
public class OffsetFetchPageDialect implements PageDialect {

    final Dialect dialect;
    final SqlLexer parser;
    boolean count;
    int skip;
    int max;
    static final TT[] kws1 = new TT[] { TT.Select, TT.Top, TT.Order, TT.Offset };
    static final TT[] kws2 = new TT[] { TT.Select, TT.Top, TT.Offset, TT.Fetch, TT.Update };

    public OffsetFetchPageDialect(Dialect d, String sql, boolean count, int skip, int max) {
        dialect = d;
        if (dialect == Dialect.sqlserver) {
            parser = new SqlLexer(sql, kws1);
        } else {
            parser = new SqlLexer(sql, kws2);
        }
        this.count = count;
        this.skip = skip;
        this.max = max;
    }

    @Override
    public PageDialect canHandle(String productNameLowerCase, int majorVersion, int minorVersion) {
        if (dialect == Dialect.sqlserver) {
            // https://technet.microsoft.com/en-us/library/gg699618(v=sql.110).aspx
            if (majorVersion < 11) { // Need sql server 2012 +
                return null;
            }
        } else if (dialect == Dialect.oracle) {
            // https://docs.oracle.com/database/121/SQLRF/statements_10002.htm#BABBADDD
            if (majorVersion < 12) { // Need oracle 12c+
                return null;
            }
        } else {
            return null;
        }

        Token t = parser.firstKeyword();
        if (t == null || t.type != TT.Select) {
            return null;
        }

        if (parser.topIndexOf(TT.Top) > 0) {
            return null;
        }

        if (dialect == Dialect.sqlserver) {
            int oIdx = parser.topLastIndexOf(TT.Order);
            if (oIdx < 0) {
                return null;
            }
            if (parser.topIndexOf(TT.Offset, oIdx) > 0) {
                return null;
            }
        }

        if (dialect == Dialect.oracle) {
            if (parser.topLastIndexOf(TT.Offset) > 0) {
                return null;
            }
            if (parser.topLastIndexOf(TT.Fetch) > 0) {
                return null;
            }
            if (parser.topLastIndexOf(TT.Update) > 0) {
                return null;
            }
        }

        return this;
    }

    @Override
    public String getPageSql(ErrorContext ec) {
        StringBuilder buf = new StringBuilder();
        if (skip < 0) {
            skip = 0;
        }
        buf.append(" OFFSET ").append(skip).append(" ROWS");
        buf.append(" FETCH NEXT ").append(max).append(" ROWS ONLY");
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
        if (dialect == Dialect.oracle) { // oracle
            if (oIdx == -1 || parser.topIndexOf(TT.Question, oIdx + 1) > 0) {
                ec.setExtraSql("select count(1) from (...)");
                return "select count(1) from (" + parser.getSql() + ")";
            }
            Token ot = parser.getCascadeTokens().get(oIdx);
            ec.setExtraSql("select count(1) from (.. -< " + parser.getSql().substring(ot.offset) + ")");
            return "select count(1) from (" + parser.getSql().substring(0, ot.offset) + ")";
        } else { // sqlserver
            if (oIdx == -1 || parser.topIndexOf(TT.Question, oIdx + 1) > 0) {
                ec.setExtraSql("select count(1) from (...) _jbatis_tmp_cnt_");
                return "select count(1) from (" + parser.getSql() + ") _jbatis_tmp_cnt_";
            }
            Token ot = parser.getCascadeTokens().get(oIdx);
            ec.setExtraSql("select count(1) from (.. -< " + parser.getSql().substring(ot.offset) + ") _jbatis_tmp_cnt_");
            return "select count(1) from (" + parser.getSql().substring(0, ot.offset) + ") _jbatis_tmp_cnt_";
        }
    }
}
