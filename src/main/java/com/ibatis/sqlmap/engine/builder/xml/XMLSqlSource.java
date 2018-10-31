package com.ibatis.sqlmap.engine.builder.xml;

import com.ibatis.common.xml.*;
import com.ibatis.sqlmap.engine.config.*;
import com.ibatis.sqlmap.engine.mapping.parameter.*;
import com.ibatis.sqlmap.engine.mapping.sql.*;
import com.ibatis.sqlmap.engine.mapping.sql.dynamic.*;
import com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements.*;
import com.ibatis.sqlmap.engine.mapping.sql.raw.*;

import org.w3c.dom.*;

import java.util.Properties;

public class XMLSqlSource implements SqlSource {

    private static final InlineParameterMapParser PARAM_PARSER = new InlineParameterMapParser();

    private XmlParserState state;
    private Node parentNode;

    public XMLSqlSource(XmlParserState config, Node parentNode) {
        this.state = config;
        this.parentNode = parentNode;
    }

    @Override
    public Sql getSql() {
        state.getConfig().getErrorContext().setActivity("processing an SQL statement");

        boolean isDynamic = false;
        StringBuilder sqlBuffer = new StringBuilder();
        DynamicSql dynamic = new DynamicSql(state.getConfig().getClient().getDelegate());
        isDynamic = parseDynamicTags(parentNode, dynamic, sqlBuffer, isDynamic, false);
        if (isDynamic) {
            return dynamic;
        } else {
            String sqlStatement = sqlBuffer.toString();
            return new RawSql(sqlStatement);
        }
    }

    private boolean parseDynamicTags(Node node, DynamicParent dynamic, StringBuilder sqlBuffer, boolean isDynamic,
        boolean postParseRequired) {
        state.getConfig().getErrorContext().setActivity("parsing dynamic SQL tags");

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodeName = child.getNodeName();
            if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {

                String data = ((CharacterData) child).getData();
                data = SqlText.cleanSql(data, i == 0 && parentNode == node);
                data = NodeletUtils.parsePropertyTokens(data, state.getGlobalProps());

                SqlText sqlText;

                if (postParseRequired) {
                    sqlText = new SqlText();
                    sqlText.setPostParseRequired(postParseRequired);
                    sqlText.setText(data);
                } else {
                    sqlText = PARAM_PARSER.parseInlineParameterMap(state.getConfig().getClient().getDelegate()
                        .getTypeHandlerFactory(), data, null);
                    sqlText.setPostParseRequired(postParseRequired);
                }
                if (!sqlText.isWhiteSpace()) {
                    dynamic.addChild(sqlText);

                    sqlBuffer.append(data);
                }
            } else if ("include".equals(nodeName)) {
                Properties attributes = NodeletUtils.parseAttributes(child, state.getGlobalProps());
                String refid = (String) attributes.get("refid");
                Node includeNode = state.getSqlIncludes().get(refid);
                if (includeNode == null) {
                    String nsrefid = state.applyNamespace(refid);
                    includeNode = state.getSqlIncludes().get(nsrefid);
                    if (includeNode == null) {
                        throw new RuntimeException("Could not find SQL statement to include with refid '" + refid + "'");
                    }
                }
                isDynamic = parseDynamicTags(includeNode, dynamic, sqlBuffer, isDynamic, false);
            } else {
                state.getConfig().getErrorContext().setMoreInfo("Check the dynamic tags.");

                SqlTagHandler handler = SqlTagHandlerFactory.getSqlTagHandler(nodeName);
                if (handler != null) {
                    isDynamic = true;

                    Properties ps = NodeletUtils.parseAttributes(child, state.getGlobalProps());

                    SqlTag tag = new SqlTag(ps.hashCode());
                    tag.setName(nodeName);
                    tag.setHandler(handler);

                    tag.setPrependAttr(prop(ps, "prepend", "pre"));
                    tag.setPropertyAttr(prop(ps, "property", "p"));
                    tag.setRemoveFirstPrepend(prop(ps, "removeFirstPrepend", "rm"));

                    tag.setOpenAttr(prop(ps, "open", "o"));
                    tag.setCloseAttr(prop(ps, "close", "c"));

                    tag.setComparePropertyAttr(prop(ps, "compareProperty", "cp"));
                    tag.setCompareValueAttr(prop(ps, "compareValue", "cv"));
                    tag.setConjunctionAttr(prop(ps, "conjunction", "conj"));

                    // an iterate ancestor requires a post parse

                    if (dynamic instanceof SqlTag) {
                        SqlTag parentSqlTag = (SqlTag) dynamic;
                        if (parentSqlTag.isPostParseRequired() || tag.getHandler() instanceof IterateTagHandler) {
                            tag.setPostParseRequired(true);
                        }
                    } else if (dynamic instanceof DynamicSql) {
                        if (tag.getHandler() instanceof IterateTagHandler) {
                            tag.setPostParseRequired(true);
                        }
                    }

                    dynamic.addChild(tag);

                    if (child.hasChildNodes()) {
                        isDynamic = parseDynamicTags(child, tag, sqlBuffer, isDynamic, tag.isPostParseRequired());
                    }
                }
            }
        }
        state.getConfig().getErrorContext().setMoreInfo(null);
        return isDynamic;
    }

    static String prop(Properties ps, String key, String alias) {
        return ps.getProperty(key, ps.getProperty(alias));
    }

}
