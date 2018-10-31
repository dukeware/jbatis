/*-
 * Copyright 2009-2017 Owl Group
 * All rights reserved.
 */
package com.ibatis.sqlmap.client.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokens
 * <p>
 * Date: 2018-01-02
 * 
 * @author Song Sun
 * @version 1.0
 */
public class Tokens extends Token {
    private List<Token> tokens = new ArrayList<Token>();
    private List<Token> cascade;

    public Tokens(int offset) {
        super(TT.Clause, offset);
    }

    public void addToken(Token t) {
        tokens.add(t);
    }

    public List<Token> getTokens() {
        return tokens;
    }

    int size() {
        return tokens.size();
    }

    public Tokens trim() {
        List<Token> tokens = getTokens();
        int start = 0;
        while (tokens.get(start).type == TT.Space && start < tokens.size()) {
            start++;
        }

        int end = tokens.size() - 1;
        while (tokens.get(end).type == TT.Space && end > start) {
            end--;
        }

        if (start > 0 || end < tokens.size() - 1) {
            this.tokens = tokens.subList(start, end + 1);
        }

        return this;
    }

    Tokens cascade() {
        List<Token> cascade = new ArrayList<Token>();
        int level = 0;
        Tokens curr = null;
        for (Token t : getTokens()) {
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

    @Override
    public String toString() {
        return type + " " + cascade;
    }

    static void calcRoots(List<String> list, List<Token> ts) {
        int fromIdx = indexOf(ts, TT.From);
        if (fromIdx > 0) {
            for (int i = 1; i < fromIdx; i++) {
                Token t = ts.get(i);
                if (t.type == TT.Clause) {
                    List<Token> sub = ((Tokens) t).getCascadeTokens();
                    Token head = firstKeyword(sub);
                    if (head != null && head.type == TT.Select) {
                        calcRoots(list, sub);
                    }
                }
            }
            int rootEndIdx = indexOf(ts, TT.Group, fromIdx + 1);
            if (rootEndIdx < 0) {
                rootEndIdx = indexOf(ts, TT.Where, fromIdx + 1);
            }
            if (rootEndIdx < 0) {
                rootEndIdx = indexOf(ts, TT.Order, fromIdx + 1);
            }
            if (rootEndIdx < 0) {
                rootEndIdx = indexOf(ts, TT.Limit, fromIdx + 1);
            }
            if (rootEndIdx < 0) {
                rootEndIdx = ts.size();
            }
            int start = fromIdx + 1;
            int idx = start;
            for (; idx < rootEndIdx; idx++) {
                Token t = ts.get(idx);
                if (t.type == TT.Comma || t.type == TT.Join) {
                    calcRoots(list, ts, start, idx);
                    start = idx + 1;
                }
            }
            calcRoots(list, ts, start, idx);

            for (int i = rootEndIdx + 1; i < ts.size(); i++) {
                Token t = ts.get(i);
                if (t.type == TT.Clause) {
                    List<Token> sub = ((Tokens) t).getCascadeTokens();
                    Token head = firstKeyword(sub);
                    if (head != null && head.type == TT.Select) {
                        calcRoots(list, sub);
                    }
                }
            }
        }
    }

    public static Token firstKeyword(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.type.word && t.type.text != null) {
                return t;
            }
        }
        return null;
    }

    static void calcRoots(List<String> list, List<Token> ts, int start, int end) {
        int idx = indexOf(ts, TT.Clause, start);
        if (idx > 0 && idx < end) {
            Token t = ts.get(idx);
            calcRoots(list, ((Tokens) t).getCascadeTokens());
            return;
        }
        idx = indexOf(ts, TT.On, start);
        if (idx > start && idx < end) {
            end = idx;
        }
        addRoot(list, ts, start, end);
    }

    static void addRoot(List<String> list, List<Token> ts, int start, int end) {
        StringBuilder buf = new StringBuilder();
        for (int i = start; i < end; i++) {
            Token t = ts.get(i);
            if (t.type == TT.Word) {
                if (buf.length() == 0 || buf.charAt(buf.length() - 1) == '.') {
                    buf.append(t.text);
                } else {
                    break;
                }
            } else if (t.type == TT.Dot) {
                if (buf.length() > 0 && buf.charAt(buf.length() - 1) != '.') {
                    buf.append('.');
                } else {
                    break;
                }
            }
        }
        if (buf.length() > 0) {
            String root = buf.toString();
            list.add(root);
        }
    }

    public static int indexOf(List<Token> tokens, TT tt) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).type == tt) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(List<Token> tokens, TT tt, int fromIndex) {
        int idx = 0;
        if (idx < fromIndex) {
            idx = fromIndex;
        }
        for (int i = idx; i < tokens.size(); i++) {
            if (tokens.get(i).type == tt) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(List<Token> tokens, TT tt) {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i).type == tt) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(List<Token> tokens, TT tt, int fromIndex) {
        int idx = tokens.size() - 1;
        if (idx > fromIndex) {
            idx = fromIndex;
        }
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == tt) {
                return i;
            }
        }
        return -1;
    }

}