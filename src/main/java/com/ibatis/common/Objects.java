/*-
 * Copyright 2012 Owl Group
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 */

package com.ibatis.common;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Objects for internal use only
 * <p>
 * Date: 2014-10-27,10:09:35 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public final class Objects {

    final Object[] array;

    Objects(Object ... args) {
        array = args;
    }

    public int hashCode() {
        int hashCode = 1;
        if (array != null) {
            for (Object o : array) {
                hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
            }
        }
        return hashCode;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Objects) {
            Objects os = (Objects) o;
            if (array == os.array) {
                return true;
            }
            if (array == null || os.array == null) {
                return false;
            }
            if (array.length != os.array.length) {
                return false;
            }
            for (int i = array.length - 1; i > 0; i--) {
                Object o1 = array[i];
                Object o2 = os.array[i];
                if (!(o1 == null ? o2 == null : o1.equals(o2)))
                    return false;
            }
            return true;
        }

        return false;
    }

    /**
     * Helps to avoid using {@code @SuppressWarnings( "unchecked"})} when casting to a generic type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }

    public static Object getKey(Object... args) {
        return new Objects(args);
    }

    public static int indexOf(StringBuilder buf, char delim, int from) {
        boolean inQuote = false;
        for (int i = from; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (inQuote) {
                if (c == '\'') {
                    inQuote = false;
                }
            } else {
                if (c == delim) {
                    return i;
                } else if (c == '\'') {
                    inQuote = true;
                }
            }
        }
        return -1;
    }

    /**
     * Append date object with format 'yyyy-MM-dd HH:mm:ss'
     */
    public static void outputDate(Appendable a, Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(d.getTime());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        try {
            a.append('\'');
            padInt(year, 4, a);
            a.append('-');
            padInt(month, 2, a);
            a.append('-');
            padInt(day, 2, a);
            a.append(' ');
            padInt(hour, 2, a);
            a.append(':');
            padInt(minute, 2, a);
            a.append(':');
            padInt(second, 2, a);
            a.append('\'');
        } catch (IOException e) {
        }
    }

    static void padInt(int val, int width, Appendable buf) throws IOException {
        if (val < 0) {
            val = 0;
        }
        String s = Integer.toString(val);
        if (s.length() > width) {
            s = s.substring(s.length() - width);
        }
        for (int j = width - s.length(); j > 0; j--) {
            buf.append('0');
        }
        buf.append(s);
    }
}
