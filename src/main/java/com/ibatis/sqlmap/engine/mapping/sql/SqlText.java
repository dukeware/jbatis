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
package com.ibatis.sqlmap.engine.mapping.sql;

import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;

public class SqlText implements SqlChild {

    private String text;
    private boolean isWhiteSpace;
    private boolean postParseRequired;

    private ParameterMapping[] parameterMappings;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.isWhiteSpace = !isNotEmpty(text);
    }

    public boolean isWhiteSpace() {
        return isWhiteSpace;
    }

    public ParameterMapping[] getParameterMappings() {
        return parameterMappings;
    }

    public void setParameterMappings(ParameterMapping[] parameterMappings) {
        this.parameterMappings = parameterMappings;
    }

    public boolean isPostParseRequired() {
        return postParseRequired;
    }

    public void setPostParseRequired(boolean postParseRequired) {
        this.postParseRequired = postParseRequired;
    }

    public static boolean isNotEmpty(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isSpace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    enum Stat {
        Normal,
        Space,
        Quote
    }

    public static String cleanSql(String sql, boolean leading) {
        StringBuilder buf = new StringBuilder(sql.length());
        if (!leading) {
            buf.append(' ');
        }
        Stat state = Stat.Space;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            switch (state) {
            case Normal: {
                if (c == '\'') {
                    state = Stat.Quote;
                    buf.append(c);
                } else if (isSpace(c)) {
                    state = Stat.Space;
                    buf.append(' ');
                } else {
                    buf.append(c);
                }
                break;
            }
            case Space: {
                if (c == '\'') {
                    state = Stat.Quote;
                    buf.append(c);
                } else if (!isSpace(c)) {
                    state = Stat.Normal;
                    buf.append(c);
                }
                break;
            }
            case Quote: {
                if (c == '\'') {
                    state = Stat.Normal;
                    buf.append(c);
                } else {
                    buf.append(c);
                }
                break;
            }
            }
        }
        if (state == Stat.Quote) {
            throw new IllegalStateException(sql);
        }
        if (state == Stat.Normal) {
            buf.append(' ');
        }
        return buf.toString();
    }

    public static boolean isSpace(char ch) {
        // 0x09 HTab
        // 0x0A LF
        // 0x0B VTab
        // 0x0C FF
        // 0x0D CR
        // 0x20 SP
        return (ch <= '\r' && ch >= '\t') || ch == ' ';
    }

    public static boolean isNotEmpty(StringBuilder str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isSpace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int hashCodex() {
        return getClass().getName().hashCode() + (text == null ? 0 : text.hashCode());
    }
}
