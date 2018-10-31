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
package com.ibatis.sqlmap.engine.exchange;

import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMapping;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.common.StringTokenizer;
import com.ibatis.common.beans.ProbeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * DataExchange implementation for List objects
 */
public class ListDataExchange extends BaseDataExchange implements DataExchange {

    protected ListDataExchange(DataExchangeFactory dataExchangeFactory) {
        super(dataExchangeFactory);
    }

    @Override
    public void initialize(Object map) {
    }

    @Override
    public Object[] getData(StatementScope statementScope, ParameterMap parameterMap, Object parameterObject) {
        ParameterMapping[] mappings = parameterMap.getParameterMappings();
        Object[] data = new Object[mappings.length];
        for (int i = 0; i < mappings.length; i++) {
            String propName = mappings[i].getPropertyName();

            // ## sunsong
            ParameterMapping pm = mappings[i];
            Object param = parameterObject;
            if (pm.getQuickName() != null) {
                propName = pm.getQuickName();
                if (propName.isEmpty()) {
                    data[i] = pm.getQuickValue();
                    continue;
                } else {
                    param = pm.getQuickValue();
                }
            }

            // parse on the '.' notation and get nested properties
            StringTokenizer st = new StringTokenizer(propName, '.');
            if (st.countTokens() > 0) {
                // iterate list of properties to discover values
                Object tempData = param;
                while (st.hasMoreTokens()) {
                    String p = st.nextToken();
                    // is property an array reference
                    int arrayStartIndex = p.indexOf('[');
                    if (arrayStartIndex == -1) {
                        // is a normal property
                        tempData = ProbeFactory.getProbe().getObject(tempData, p);
                    } else {
                        int index = Integer.parseInt(p.substring(arrayStartIndex + 1, p.length() - 1));
                        tempData = ((List<?>) tempData).get(index);
                    }
                }
                data[i] = tempData;
            } else {
                int index = Integer.parseInt((propName.substring(propName.indexOf('[') + 1, propName.length() - 1)));
                data[i] = ((List<?>) param).get(index);
            }
        }
        return data;
    }

    @Override
    public Object setData(StatementScope statementScope, ResultMap resultMap, Object resultObject, Object[] values) {
        ResultMapping[] mappings = resultMap.getResultMappings();
        List<Object> data = new ArrayList<Object>();
        for (int i = 0; i < mappings.length; i++) {
            String propName = mappings[i].getPropertyName();
            int index = Integer.parseInt((propName.substring(1, propName.length() - 1)));
            data.set(index, values[i]);
        }
        return data;
    }

    @Override
    public Object setData(StatementScope statementScope, ParameterMap parameterMap, Object parameterObject,
        Object[] values) {
        /*- ## sunsong
        ParameterMapping[] mappings = parameterMap.getParameterMappings();
        List<Object> data = new ArrayList<Object>();
        for (int i = 0; i < mappings.length; i++) {
            if (mappings[i].isOutputAllowed()) {
                String propName = mappings[i].getPropertyName();
                int index = Integer.parseInt((propName.substring(1, propName.length() - 1)));
                data.set(index, values[i]);
            }
        }
        return data;
         */
        return null;
    }

}
