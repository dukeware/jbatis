package com.ibatis.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * Page for internal use only
 * <p>
 * Date: 2017-12-30
 * 
 * @author Song Sun
 * @version 1.0
 */
public class Page<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int total;
    public final List<T> list;

    public Page(int t, ArrayList<T> p) {
        total = t;
        list = p;
    }
}