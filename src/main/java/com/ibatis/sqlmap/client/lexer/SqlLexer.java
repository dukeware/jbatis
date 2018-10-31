/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.client.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SqlLexer
 * <p>
 * Date: 2018-01-02
 * 
 * @author Song Sun
 * @version 1.0
 */
public class SqlLexer {
    private final String sql;
    private final TT[] keywords;
    private List<Token> tokens;
    private List<Token> cascade;

    public SqlLexer(String sql) {
        this(sql, TT.words);
    }

    public SqlLexer(String sql, TT[] words) {
        this.sql = sql != null ? sql : "sql_is_null";
        this.keywords = words;
    }

    public int indexOf(TT tt) {
        return Tokens.indexOf(getTokens(), tt);
    }

    public int indexOf(TT tt, int fromIndex) {
        return Tokens.indexOf(getTokens(), tt, fromIndex);
    }

    public int lastIndexOf(TT tt) {
        return Tokens.lastIndexOf(getTokens(), tt);
    }

    public int lastIndexOf(TT tt, int fromIndex) {
        return Tokens.lastIndexOf(getTokens(), tt, fromIndex);
    }

    public int topIndexOf(TT tt) {
        return Tokens.indexOf(getCascadeTokens(), tt);
    }

    public int topIndexOf(TT tt, int fromIndex) {
        return Tokens.indexOf(getCascadeTokens(), tt, fromIndex);
    }

    public int topLastIndexOf(TT tt) {
        return Tokens.lastIndexOf(getCascadeTokens(), tt);
    }

    public int topLastIndexOf(TT tt, int fromIndex) {
        return Tokens.lastIndexOf(getCascadeTokens(), tt, fromIndex);
    }

    public SqlLexer trim() {
        List<Token> tokens = getTokens();
        int start = 0;
        while (tokens.get(start).type == TT.Space && start < tokens.size()) {
            start++;
        }

        int end = tokens.size() - 1;
        while (end > start) {
            TT tt = tokens.get(end).type;
            if (tt == TT.Space || tt == TT.SemiColon) {
                end--;
            } else {
                break;
            }
        }

        if (start > 0 || end < tokens.size() - 1) {
            this.tokens = tokens.subList(start, end + 1);
        }

        return this;
    }

    public List<Token> getTokens() {
        if (tokens == null) {
            parse();
        }
        return tokens;
    }

    public Token firstKeyword() {
        return Tokens.firstKeyword(getTokens());
    }

    public String getSql() {
        return sql;
    }

    SqlLexer parse() {
        int offset = 0;
        List<Token> list = new ArrayList<Token>();
        boolean inQuote = false;
        TT tt = null;
        StringBuilder buf = new StringBuilder(64);
        for (final char c : sql.toCharArray()) {
            if (inQuote) {
                buf.append(c);
                if (c == '\'') {
                    inQuote = false;
                }
            } else {
                switch (c) {
                case '\'': {
                    closeToken(tt, list, buf, offset);
                    inQuote = true;
                    tt = TT.Literal;
                    buf.append(c);
                    break;
                }
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                case '\f': {
                    if (tt != TT.Space) {
                        closeToken(tt, list, buf, offset);
                        tt = TT.Space;
                        buf.append(' ');
                    }
                    break;
                }
                case '*': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Asterisk;
                    buf.append(c);
                    break;
                }
                case '.': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Dot;
                    buf.append(c);
                    break;
                }
                case '(': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Lp;
                    buf.append(c);
                    break;
                }
                case ')': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Rp;
                    buf.append(c);
                    break;
                }
                case ',': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Comma;
                    buf.append(c);
                    break;
                }
                /*-
                case '`': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Backquote;
                    buf.append(c);
                    break;
                }
                 */
                case ';': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.SemiColon;
                    buf.append(c);
                    break;
                }
                case '?': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Question;
                    buf.append(c);
                    break;
                }
                case '=':
                case '+':
                case '-':
                case '/':
                case '%':
                case '^':
                case '>':
                case '<':
                case '!':
                case '&':
                case '|':
                case ':':
                case '~': {
                    closeToken(tt, list, buf, offset);
                    tt = TT.Operator;
                    buf.append(c);
                    break;
                }
                default:
                    if (tt != TT.Word) {
                        closeToken(tt, list, buf, offset);
                    }
                    tt = TT.Word;
                    buf.append(c);
                }
            }
            offset++;
        }
        closeToken(tt, list, buf, offset);
        tt = null;
        tokens = list;
        return this;
    }

    void closeToken(TT tt, List<Token> list, StringBuilder buf, int offset) {
        if (tt != null && buf.length() > 0) {
            String word = buf.toString();
            if (tt == TT.Word) {
                tt = toKeyword(tt, word);
            }
            list.add(new Token(tt, offset - word.length(), word));
        }
        buf.setLength(0);
    }

    TT toKeyword(TT tt, String word) {
        int lw = word.length();
        String w = null;
        for (TT kw : keywords) {
            int lk = kw.text.length();
            if (lw == lk) {
                if (w == null) {
                    w = word.toUpperCase(Locale.ROOT);
                }
                if (w.equals(kw.text)) {
                    return kw;
                }
            }
        }
        return tt;
    }

    SqlLexer cascade() {
        List<Token> tokens = getTokens();
        List<Token> cascade = new ArrayList<Token>();
        int level = 0;
        Tokens curr = null;
        for (Token t : tokens) {
            if (t.type == TT.Lp) {
                if (level == 0) {
                    cascade.add(t);
                    curr = new Tokens(t.offset + 1);
                } else {
                    curr.addToken(t);
                }
                level++;
            } else if (t.type == TT.Rp) {
                level--;
                if (level > 0 && curr != null) {
                    curr.addToken(t);
                } else {
                    if (curr != null) {
                        curr.trim();
                        if (curr.size() > 0) {
                            cascade.add(curr);
                        }
                        curr = null;
                    }
                    cascade.add(t);
                }
            } else if (level == 0) {
                cascade.add(t);
            } else {
                curr.addToken(t);
            }
        }
        this.cascade = cascade;
        return this;
    }

    public List<Token> getCascadeTokens() {
        if (cascade == null) {
            cascade();
        }
        return cascade;
    }

    public List<String> getRoots() {
        List<Token> tokens = getTokens();
        if (tokens.isEmpty()) {
            return Collections.emptyList();
        }
        Token head = firstKeyword();
        if (head == null) {
            return Collections.emptyList();
        }
        TT tt = head.type;
        List<String> list = new ArrayList<String>(5);
        if (tt == TT.Select) {
            Tokens.calcRoots(list, getCascadeTokens());
        } else if (tt == TT.Insert || tt == TT.Replace) {
            int intoIdx = indexOf(TT.Into, 1);
            if (intoIdx > 0) {
                int lpIdx = indexOf(TT.Lp, intoIdx + 1);
                if (lpIdx > 0) {
                    Tokens.addRoot(list, tokens, intoIdx + 1, lpIdx);
                }
            }
        } else if (tt == TT.Update) {
            int setIdx = indexOf(TT.Set, 1);
            if (setIdx > 0) {
                Tokens.addRoot(list, tokens, 1, setIdx);
            }
        } else if (tt == TT.Delete) {
            int fromIdx = indexOf(TT.From, 1);
            if (fromIdx > 0) {
                int whereIdx = indexOf(TT.Where, fromIdx + 1);
                if (whereIdx > 0) {
                    Tokens.addRoot(list, tokens, fromIdx + 1, whereIdx);
                } else {
                    Tokens.addRoot(list, tokens, fromIdx + 1, tokens.size());
                }
            }
        } else {
            list.add(getSql());
        }
        return list;
    }
}
