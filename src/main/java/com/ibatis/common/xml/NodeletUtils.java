package com.ibatis.common.xml;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Properties;

public class NodeletUtils {

    public static boolean getBooleanAttribute(Properties attribs, String name, boolean def) {
        String value = attribs.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return "true".equals(value);
        }
    }

    public static int getIntAttribute(Properties attribs, String name, int def) {
        String value = attribs.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return Integer.parseInt(value);
        }
    }

    public static Properties parseAttributes(Node n) {
        return parseAttributes(n, null);
    }

    public static Properties parseAttributes(Node n, Properties variables) {
        Properties attributes = new Properties();
        NamedNodeMap attributeNodes = n.getAttributes();
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Node attribute = attributeNodes.item(i);
            String value = parsePropertyTokens(attribute.getNodeValue(), variables);
            attributes.put(attribute.getNodeName(), value);
        }
        return attributes;
    }

    public static String parsePropertyTokens(String string, Properties variables) {
        final String OPEN = "${";
        final char CLOSE = '}';

        String newString = string;
        if (newString != null && variables != null) {
            int start = newString.indexOf(OPEN);
            int end = newString.indexOf(CLOSE);

            while (start >= 0 && end > start) {
                String prepend = newString.substring(0, start);
                String append = newString.substring(end + 1);
                String propName = newString.substring(start + 2, end);
                // ## sunsong
                int commaIndex = propName.indexOf(':');
                String defaultValue = propName;
                if (commaIndex >= 0) {
                    defaultValue = propName.substring(commaIndex + 1);
                    propName = propName.substring(0, commaIndex);
                }

                String propValue = variables.getProperty(propName);
                if (propValue == null) {
                    // ## newString = prepend + propName + append;
                    newString = prepend + defaultValue + append;
                } else {
                    newString = prepend + propValue + append;
                }
                start = newString.indexOf(OPEN);
                end = newString.indexOf(CLOSE);
            }
        }
        return newString;
    }

}
