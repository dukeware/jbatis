package com.ibatis.sqlmap.engine.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ibatis.common.StringTokenizer;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.result.Discriminator;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMapping;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.type.CustomTypeHandler;
import com.ibatis.sqlmap.engine.type.TypeHandler;
import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

@SuppressWarnings("unchecked")
public class ResultMapConfig {
    private SqlMapConfiguration config;
    private ErrorContext errorContext;
    private SqlMapClientImpl client;
    private SqlMapExecutorDelegate delegate;
    TypeHandlerFactory typeHandlerFactory;
    private ResultMap resultMap;
    private List<ResultMapping> resultMappingList;
    private int resultMappingIndex;
    private Discriminator discriminator;

    ResultMapConfig(SqlMapConfiguration config, String id, Class<?> resultClass, String groupBy,
        String extendsResultMap) {
        this.config = config;
        this.errorContext = config.getErrorContext();
        this.client = config.getClient();
        this.delegate = config.getDelegate();
        this.typeHandlerFactory = config.getTypeHandlerFactory();
        this.resultMap = new ResultMap(client.getDelegate());
        this.resultMappingList = new ArrayList<ResultMapping>();
        errorContext.setActivity("building a result map");
        errorContext.setObjectId(id + " result map");
        resultMap.setId(id);
        resultMap.setResource(errorContext.getResource());
        if (groupBy != null && groupBy.length() > 0) {
            StringTokenizer parser = new StringTokenizer(groupBy, ',', true);
            while (parser.hasMoreTokens()) {
                resultMap.addGroupByProperty(parser.nextToken());
            }
        }
        resultMap.setResultClass(resultClass);
        errorContext.setMoreInfo("Check the extended result map.");
        if (extendsResultMap != null) {
            ResultMap extendedResultMap = (ResultMap) client.getDelegate().getResultMap(extendsResultMap);
            ResultMapping[] resultMappings = extendedResultMap.getResultMappings();
            for (int i = 0; i < resultMappings.length; i++) {
                resultMappingList.add(resultMappings[i]);
            }
            List<ResultMapping> nestedResultMappings = extendedResultMap.getNestedResultMappings();
            if (nestedResultMappings != null) {
                for (ResultMapping nested : nestedResultMappings) {
                    resultMap.addNestedResultMappings(nested);
                }
            }
            if (groupBy == null || groupBy.length() == 0) {
                Set<String> set = extendedResultMap.groupByProps();
                if (set != null) {
                    for (String groupByProp : set) {
                        resultMap.addGroupByProperty(groupByProp);
                    }
                }
            }
        }
        errorContext.setMoreInfo("Check the result mappings.");
        resultMappingIndex = resultMappingList.size();
        resultMap.setResultMappingList(resultMappingList);
        client.getDelegate().addResultMap(resultMap);
    }

    public void setDiscriminator(String columnName, Integer columnIndex, Class<?> javaClass, String jdbcType,
        String nullValue, Object typeHandlerImpl) {
        TypeHandler handler;
        if (typeHandlerImpl != null) {
            if (typeHandlerImpl instanceof TypeHandlerCallback) {
                handler = new CustomTypeHandler((TypeHandlerCallback) typeHandlerImpl);
            } else if (typeHandlerImpl instanceof TypeHandler) {
                handler = (TypeHandler) typeHandlerImpl;
            } else {
                throw new RuntimeException(
                    "The class '' is not a valid implementation of TypeHandler or TypeHandlerCallback");
            }
        } else {
            handler = config.resolveTypeHandler(client.getDelegate().getTypeHandlerFactory(),
                resultMap.getResultClass(), "", javaClass, jdbcType, true);
        }
        ResultMapping mapping = new ResultMapping();
        mapping.setColumnName(columnName);
        mapping.setJdbcTypeName(jdbcType);
        mapping.setTypeHandler(handler);
        mapping.setNullValue(nullValue);
        mapping.setJavaType(javaClass);
        if (columnIndex != null) {
            mapping.setColumnIndex(columnIndex.intValue());
        }
        discriminator = new Discriminator(delegate, mapping);
        resultMap.setDiscriminator(discriminator);
    }

    public void addDiscriminatorSubMap(Object value, String resultMap) {
        if (discriminator == null) {
            throw new RuntimeException("The discriminator is null, but somehow a subMap was reached.  This is a bug.");
        }
        discriminator.addSubMap(value.toString(), resultMap);
    }

    public void addResultMapping(String propertyName, String columnName, Integer columnIndex, Class<?> javaClass,
        String jdbcType, String nullValue, String notNullColumn, String statementName, String resultMapName, Object impl) {
        errorContext.setObjectId(propertyName + " mapping of the " + resultMap.getId() + " result map");
        TypeHandler handler;
        if (impl != null) {
            if (impl instanceof TypeHandlerCallback) {
                handler = new CustomTypeHandler((TypeHandlerCallback) impl);
            } else if (impl instanceof TypeHandler) {
                handler = (TypeHandler) impl;
            } else {
                throw new RuntimeException("The class '" + impl
                    + "' is not a valid implementation of TypeHandler or TypeHandlerCallback");
            }
        } else {
            handler = config.resolveTypeHandler(client.getDelegate().getTypeHandlerFactory(),
                resultMap.getResultClass(), propertyName, javaClass, jdbcType, true);
        }
        ResultMapping mapping = new ResultMapping();
        mapping.setPropertyName(propertyName);
        mapping.setColumnName(columnName);
        mapping.setJdbcTypeName(jdbcType);
        mapping.setTypeHandler(handler);
        mapping.setNullValue(nullValue);
        mapping.setNotNullColumn(notNullColumn);
        mapping.setStatementName(statementName);
        mapping.setNestedResultMapName(resultMapName);
        if (resultMapName != null && resultMapName.length() > 0) {
            resultMap.addNestedResultMappings(mapping);
        }
        mapping.setJavaType(javaClass);
        if (columnIndex != null) {
            mapping.setColumnIndex(columnIndex.intValue());
        } else {
            resultMappingIndex++;
            mapping.setColumnIndex(resultMappingIndex);
        }
        resultMappingList.add(mapping);
        resultMap.setResultMappingList(resultMappingList);
    }

}
