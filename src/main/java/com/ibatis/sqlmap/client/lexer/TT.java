/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.client.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * TT
 * <p>
 * Date: 2018-01-02
 * 
 * @author Song Sun
 * @version 1.0
 */
public enum TT {
    Literal(true, false, null),
    // -------------
    Operator(false, true, null),
    Space(false, true, " "),
    Dot(false, true, "."),
    Asterisk(false, true, "*"),
    Question(false, true, "?"),
    Comma(false, true, ","),
    // Backquote(false, true, "`"),
    SemiColon(false, true, ";"),
    Lp(false, true, "("),
    Rp(false, true, ")"),
    // -------------
    Word(false, false, null),
    Select(false, false, "SELECT"),
    Top(false, false, "TOP"),
    As(false, false, "AS"),
    Fetch(false, false, "FETCH"),
    From(false, false, "FROM"),
    Join(false, false, "JOIN"),
    On(false, false, "ON"),
    Where(false, false, "WHERE"),
    And(false, false, "AND"),
    Or(false, false, "OR"),
    Group(false, false, "GROUP"),
    Having(false, false, "HAVING"),
    By(false, false, "BY"),
    Order(false, false, "ORDER"),
    Limit(false, false, "LIMIT"),
    Offset(false, false, "OFFSET"),
    Update(false, false, "UPDATE"),
    Set(false, false, "SET"),
    Delete(false, false, "DELETE"),
    Insert(false, false, "INSERT"),
    Into(false, false, "INTO"),
    Values(false, false, "VALUES"),
    Replace(false, false, "REPLACE"),
    // -------------
    Clause(false, false, null);
    public final boolean literal;
    public final boolean symbol;
    public final boolean word;
    public final String text;

    private TT(boolean literal, boolean symbol, String text) {
        this.literal = literal;
        this.symbol = symbol;
        this.word = !literal && !symbol;
        this.text = text;
    }

    static final TT[] words;
    static {
        List<TT> list = new ArrayList<TT>();
        for (TT t : values()) {
            if (t.word && t.text != null) {
                list.add(t);
            }
        }
        words = list.toArray(new TT[list.size()]);
    }
}
