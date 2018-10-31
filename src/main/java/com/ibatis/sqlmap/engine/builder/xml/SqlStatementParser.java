package com.ibatis.sqlmap.engine.builder.xml;

import com.ibatis.common.xml.*;
import com.ibatis.common.StringTokenizer;
import com.ibatis.common.resources.*;
import com.ibatis.sqlmap.engine.config.*;
import com.ibatis.sqlmap.engine.mapping.sql.SqlText;
import com.ibatis.sqlmap.engine.mapping.statement.*;
import com.ibatis.sqlmap.client.*;

import org.w3c.dom.CharacterData;
import org.w3c.dom.*;

import java.util.HashMap;
import java.util.Properties;

public class SqlStatementParser {

    private XmlParserState state;
    private boolean strict;
    private HashMap<String, String> statemetCache = new HashMap<String, String>();

    public SqlStatementParser(XmlParserState config, boolean strict) {
        this.state = config;
        this.strict = strict;
    }

    public void resetStatemetCache() {
        statemetCache.clear();
    }

    public void parseGeneralStatement(Node node, MappedStatement statement, boolean canBatch, String batchReverseAttr) {

        // get attributes
        Properties attributes = NodeletUtils.parseAttributes(node, state.getGlobalProps());
        String id = attributes.getProperty("id");
        // ## sunsong
        if (strict && id.indexOf('#') >= 0) {
            throw new SqlMapException("Illegal statement id: " + id);
        }
        if (statemetCache.put(id, "") != null) {
            throw new SqlMapException("There is already a statement: " + id);
        }
        String parameterMapName = state.applyNamespace(attributes.getProperty("parameterMap"));
        String parameterClassName = attributes.getProperty("parameterClass");
        String resultMapName = attributes.getProperty("resultMap");
        String resultClassName = attributes.getProperty("resultClass");
        String cacheModelName = state.applyNamespace(attributes.getProperty("cacheModel"));
        String resultSetType = attributes.getProperty("resultSetType");
        String fetchSize = attributes.getProperty("fetchSize");
        String allowRemapping = attributes.getProperty("remapResults");
        String timeout = attributes.getProperty("timeout");

        if (state.isUseStatementNamespaces()) {
            id = state.applyNamespace(id);
        }
        String[] additionalResultMapNames = null;
        if (resultMapName != null) {
            StringTokenizer st = new StringTokenizer(resultMapName, ',', true);
            int tc = st.countTokens();
            if (tc > 0) {
                resultMapName = state.applyNamespace(st.nextToken());
            } else {
                resultMapName = null;
            }
            if (tc > 1) {
                additionalResultMapNames = new String[tc - 1];
                for (int i = 0; i < additionalResultMapNames.length; i++) {
                    additionalResultMapNames[i] = state.applyNamespace(st.nextToken());
                }
            }
        }

        String[] additionalResultClassNames = null;
        if (resultClassName != null) {
            StringTokenizer st = new StringTokenizer(resultClassName, ',', true);
            int tc = st.countTokens();
            if (tc > 0) {
                resultClassName = st.nextToken();
            } else {
                resultClassName = null;
            }
            if (tc > 1) {
                additionalResultClassNames = new String[tc - 1];
                for (int i = 0; i < additionalResultClassNames.length; i++) {
                    additionalResultClassNames[i] = st.nextToken();
                }
            }
        }

        state.getConfig().getErrorContext().setMoreInfo("Check the parameter class.");
        Class<?> parameterClass = resolveClass(parameterClassName);

        state.getConfig().getErrorContext().setMoreInfo("Check the result class.");
        Class<?> resultClass = resolveClass(resultClassName);
        Class<?>[] additionalResultClasses = null;
        if (additionalResultClassNames != null) {
            additionalResultClasses = new Class[additionalResultClassNames.length];
            for (int i = 0; i < additionalResultClassNames.length; i++) {
                additionalResultClasses[i] = resolveClass(additionalResultClassNames[i]);
            }
        }

        Integer timeoutInt = timeout == null ? null : new Integer(timeout);
        Integer fetchSizeInt = fetchSize == null ? null : new Integer(fetchSize);
        boolean allowRemappingBool = "true".equals(allowRemapping);

        if (batchReverseAttr != null) {
            if ("true".equals(attributes.getProperty(batchReverseAttr))) {
                canBatch = !canBatch;
            }
        }

        MappedStatementConfig statementConf = state.getConfig().newMappedStatementConfig(id, statement,
            new XMLSqlSource(state, node), parameterMapName, parameterClass, resultMapName, additionalResultMapNames,
            resultClass, additionalResultClasses, resultSetType, fetchSizeInt, allowRemappingBool, timeoutInt,
            cacheModelName, canBatch);

        if (state.getConfig().getDelegate().getSqlExecutor().isCheckSql()) {
            statement.checkSql(state.getConfig().getErrorContext());
        }
        findAndParseSelectKey(node, statementConf);
        findAndParseFlushCache(node, statementConf);
        findAndParseFlushCacheRoot(node, statementConf);
    }

    private Class<?> resolveClass(String resultClassName) {
        try {
            if (resultClassName != null) {
                return Resources.classForName(state.getConfig().getTypeHandlerFactory().resolveAlias(resultClassName));
            } else {
                return null;
            }
        } catch (ClassNotFoundException e) {
            throw new SqlMapException("Error.  Could not initialize class.  Cause: " + e, e);
        }
    }

    private void findAndParseSelectKey(Node node, MappedStatementConfig config) {
        state.getConfig().getErrorContext().setActivity("parsing select key tags");
        boolean foundSQLFirst = false;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                String data = ((CharacterData) child).getData();
                // ## sunsong
                data = SqlText.cleanSql(data, i == 0);
                if (SqlText.isNotEmpty(data)) {
                    foundSQLFirst = true;
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE && "selectKey".equals(child.getNodeName())) {
                Properties attributes = NodeletUtils.parseAttributes(child, state.getGlobalProps());
                String keyPropName = attributes.getProperty("keyProperty");
                String resultClassName = attributes.getProperty("resultClass");
                boolean genKey = !child.hasChildNodes();
                String type = attributes.getProperty("type");
                if (genKey) {
                    foundSQLFirst = true;
                } else if (type != null) {
                    foundSQLFirst = !"pre".equals(type);
                }
                config.setSelectKeyStatement(new XMLSqlSource(state, child), resultClassName, keyPropName,
                    foundSQLFirst, genKey);
                break;
            }
        }
        state.getConfig().getErrorContext().setMoreInfo(null);

    }
    private void findAndParseFlushCacheRoot(Node node, MappedStatementConfig config) {
        state.getConfig().getErrorContext().setActivity("parsing flushCacheRoot tags");
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "flushCacheRoot".equals(child.getNodeName())) {
                Properties attributes = NodeletUtils.parseAttributes(child, state.getGlobalProps());
                String name = attributes.getProperty("name");
                if (name != null && !name.isEmpty()) {
                    config.addFlushCacheRoot(name.toLowerCase().trim());
                }
            }
        }
        state.getConfig().getErrorContext().setMoreInfo(null);
    }

    private void findAndParseFlushCache(Node node, MappedStatementConfig config) {
        state.getConfig().getErrorContext().setActivity("parsing flushCache tags");
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "flushCache".equals(child.getNodeName())) {
                Properties attributes = NodeletUtils.parseAttributes(child, state.getGlobalProps());
                String entityClass = attributes.getProperty("entityClass");
                if (entityClass != null && !entityClass.isEmpty()) {
                    entityClass = state.getConfig().getTypeHandlerFactory().resolveAlias(entityClass);
                    Class<?> clazz = null;
                    try {
                        state.getConfig().getErrorContext().setMoreInfo("Check the entity class.");
                        clazz = Resources.classForName(entityClass);
                    } catch (Exception e) {
                        throw new RuntimeException(
                            "Error configuring flushCache.  Could not set ResultClass.  Cause: " + e, e);
                    }

                    config.addFlushEntityCache(clazz);
                } else {
                    String cacheModel = attributes.getProperty("cacheModel");
                    if (cacheModel == null || cacheModel.isEmpty()) {
                        throw new RuntimeException("Error configuring flushOnFlush.  cacheModel is emtpy.");
                    }
                    config.addFlushCacheModel(state.applyNamespace(cacheModel));
                }
            }
        }
        state.getConfig().getErrorContext().setMoreInfo(null);
    }
}
