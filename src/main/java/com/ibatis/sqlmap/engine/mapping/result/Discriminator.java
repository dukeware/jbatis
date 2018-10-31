package com.ibatis.sqlmap.engine.mapping.result;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;

/**
 * Created by IntelliJ IDEA. User: cbegin Date: May 13, 2005 Time: 11:11:05 PM To change this template use File |
 * Settings | File Templates.
 */
public class Discriminator {

    private SqlMapExecutorDelegate delegate;
    private ResultMapping resultMapping;
    private Map<String, Object> subMaps;

    public Discriminator(SqlMapExecutorDelegate delegate, ResultMapping resultMapping) {
        this.delegate = delegate;
        this.resultMapping = resultMapping;
    }

    public void setResultMapping(ResultMapping resultMapping) {
        this.resultMapping = resultMapping;
    }

    public ResultMapping getResultMapping() {
        return resultMapping;
    }

    public void addSubMap(String discriminatorValue, String resultMapName) {
        if (subMaps == null) {
            subMaps = new LinkedHashMap<String, Object>();
        }
        subMaps.put(discriminatorValue, resultMapName);
    }

    public ResultMap getSubMap(String s) {
        return (ResultMap) subMaps.get(s);
    }

    public void bindSubMaps() {
        if (subMaps != null) {
            for (Map.Entry<String, Object> me : subMaps.entrySet()) {
                if (me.getValue() instanceof String) {
                    subMaps.put(me.getKey(), delegate.getResultMap((String) me.getValue()));
                }
            }
        }
    }

}
