/*-
 * Copyright 2009-2019 Owl Group
 * All rights reserved.
 */
package com.ibatis.common;

/**
 * StringTokenizer for internal use only
 * <p>
 * Date: 2018-05-02
 * 
 * @author Song Sun
 * @version 1.0
 */
public class StringTokenizer {
    private int currPosition;
    private int newPosition;
    private int maxPosition;
    private String str;
    final char delim;
    boolean skipSpace;
    char eq;

    public StringTokenizer(String str, char delim) {
        this(str, delim, false);
    }

    public StringTokenizer(String str, char delim, boolean skipSpace) {
        this(str, delim, '\0', skipSpace);
    }

    public StringTokenizer(String str, char delim, char eq, boolean skipSpace) {
        currPosition = 0;
        newPosition = -1;
        this.str = str;
        maxPosition = str.length();
        this.delim = delim;
        this.eq = eq;
        this.skipSpace = skipSpace;
    }

    int skipDelimiters(int startPos) {
        int position = startPos;
        while (position < maxPosition) {
            char c = str.charAt(position);
            if (c != delim && (eq == 0 || c != eq) && (!skipSpace || c != ' '))
                break;
            position++;
        }
        return position;
    }

    int scanToken(int startPos) {
        int position = startPos;
        while (position < maxPosition) {
            char c = str.charAt(position);
            if (c == delim || (eq > 0 && c == eq) || (skipSpace && c == ' '))
                break;
            position++;
        }
        return position;
    }

    public boolean hasMoreTokens() {
        newPosition = skipDelimiters(currPosition);
        return (newPosition < maxPosition);
    }

    public String nextToken() {
        currPosition = (newPosition >= 0) ? newPosition : skipDelimiters(currPosition);

        newPosition = -1;

        if (currPosition >= maxPosition)
            throw new IllegalStateException();
        int start = currPosition;
        currPosition = scanToken(currPosition);
        return str.substring(start, currPosition);
    }

    public int countTokens() {
        int count = 0;
        int currpos = currPosition;
        while (currpos < maxPosition) {
            currpos = skipDelimiters(currpos);
            if (currpos >= maxPosition)
                break;
            currpos = scanToken(currpos);
            count++;
        }
        return count;
    }

    /*-
    public static void main(String[] args) {
        String s = " , aaa, bbb,, cccc,ddd,,eeee ,,";
        StringTokenizer st = new StringTokenizer(s, ',', false);
        while (st.hasMoreTokens()) {
            System.out.println(st.nextToken());
        }
        System.out.println("------");
        st = new StringTokenizer(s, ',', true);
        while (st.hasMoreTokens()) {
            System.out.println(st.nextToken());
        }
        System.out.println("------");
        st = new StringTokenizer(
            "propertyName,javaType=string=,jdbcType=VARCHAR,,mode=IN,nullValue=N/A,handler=string,numericScale=2", ',',
            '=', false);
        while (st.hasMoreTokens()) {
            System.out.println(st.nextToken());
        }
        System.out.println("------");
        st = new StringTokenizer("account-result", ',', true);
        System.out.println(st.countTokens());
        while (st.hasMoreTokens()) {
            System.out.println(st.nextToken());
        }
    }
     */
}
