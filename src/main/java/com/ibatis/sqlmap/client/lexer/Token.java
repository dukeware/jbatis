/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.client.lexer;

/**
 * Token
 * <p>
 * Date: 2018-01-02
 * 
 * @author Song Sun
 * @version 1.0
 */
public class Token {
    public final TT type;
    public final int offset;
    public final String text;

    protected Token(TT type, int offset) {
        this.type = type;
        this.offset = offset;
        this.text = null;
    }

    public Token(TT type, int offset, String text) {
        this.type = type;
        this.offset = offset;
        this.text = text;
    }

    @Override
    public String toString() {
        return type + ": " + text;
    }

}
