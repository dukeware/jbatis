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
package com.ibatis.sqlmap.engine.mapping.statement;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibatis.common.Objects;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.type.TypeHandler;
import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

@SuppressWarnings("unchecked")
public class MappedRowHandler<K, V> implements RowHandler {

    Map<K, V> map = new LinkedHashMap<K, V>();
    String keyProp;
    Class<K> keyType;
    String valueProp;
    Class<V> valueType;
    int keyIdx;
    int valueIdx;
    TypeHandler keyHander;
    TypeHandler valueHandler;
    boolean isUseColumnLabel;
    private TypeHandlerFactory typeHandlerFactory;

    public MappedRowHandler(TypeHandlerFactory typeHandlerFactory, boolean isUseColumnLabel, String keyProp,
        Class<K> keyType, String valueProp, Class<V> valueType) {
        this.typeHandlerFactory = typeHandlerFactory;
        this.isUseColumnLabel = isUseColumnLabel;
        this.keyProp = keyProp;
        this.keyType = keyType;
        this.valueProp = valueProp;
        this.valueType = valueType;
    }

    @Override
    public void handleRow(Object rsObject) throws SQLException {
        if (rsObject instanceof ResultSet) {
            ResultSet rs = (ResultSet) rsObject;
            if (keyIdx != 0 && valueIdx != 0) {
                K key = null;
                if (keyHander != null) {
                    key = Objects.uncheckedCast(keyHander.getResult(rs, keyIdx));
                } else {
                    key = Objects.uncheckedCast(typeHandlerFactory.getUnkownTypeHandler().getResult(rs, keyIdx));
                }
                V value = null;
                if (valueHandler != null) {
                    value = Objects.uncheckedCast(valueHandler.getResult(rs, valueIdx));
                } else {
                    value = Objects.uncheckedCast(typeHandlerFactory.getUnkownTypeHandler().getResult(rs, valueIdx));
                }
                map.put(key, value);
            }
        }
    }

    public void handleMeta(ResultSetMetaData md) throws SQLException {
        if (keyType != null) {
            keyHander = typeHandlerFactory.getTypeHandler(keyType);
        }

        if (valueType != null) {
            valueHandler = typeHandlerFactory.getTypeHandler(valueType);
        }

        for (int i = 1; i <= md.getColumnCount(); i++) {
            String colName = isUseColumnLabel ? md.getColumnLabel(i) : md.getColumnName(i);
            if (keyProp.equalsIgnoreCase(colName)) {
                keyIdx = i;
            }
            if (valueProp.equalsIgnoreCase(colName)) {
                valueIdx = i;
            }
        }
    }

    public Map<K, V> getMap() {
        return map;
    }

    @Override
    public Integer getRows() {
        return map == null ? null : map.size();
    }

    public void setMap(Map<K, V> map) {
        this.map = map;
    }
    
}
