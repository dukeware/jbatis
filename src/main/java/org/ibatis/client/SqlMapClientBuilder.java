/*-
 * Copyright (c) 2007-2008 Owlgroup.
 * All rights reserved. 
 * SqlMapClientBuilder2.java
 * Date: 2008-10-7
 * Author: Song Sun
 */
package org.ibatis.client;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.engine.builder.xml.SqlMapConfigParser;

/**
 * Builds SqlMapClient instances from a supplied resource (e.g. XML configuration file)
 * <p/>
 * The SqlMapClientBuilder class is responsible for parsing configuration documents and building the SqlMapClient
 * instance. Its current implementation works with XML configuration files (e.g. sql-map-config.xml).
 * <p/>
 * Date: 2008-10-7
 * 
 * @author Song Sun
 * @version 1.0
 */
public class SqlMapClientBuilder {

    static Properties getProperties(Properties props) {
        Properties p = new Properties();
        if (props != null) {
            p.putAll(props);
        }
        p.putAll(Resources.getIbatisIniProperties());
        return p;
    }

    /**
     * Builds an SqlMapClient using the specified reader.
     * 
     * @param reader
     *            A Reader instance that reads an sql-map-config.xml file. The reader should read an well formed
     *            sql-map-config.xml file.
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(Reader reader) {
        SqlMapConfigParser parser = new SqlMapConfigParser();
        return parser.parse(reader, getProperties(null));
    }

    /**
     * Builds an SqlMapClient using the specified reader and database dialect.
     * 
     * @param reader
     *            A Reader instance that reads an sql-map-config.xml file. The reader should read an well formed
     *            sql-map-config.xml file.
     * @param dialect
     *            the database dialect
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(Reader reader, String dialect) {
        SqlMapConfigParser parser = new SqlMapConfigParser(dialect);
        return parser.parse(reader, getProperties(null));
    }

    /**
     * Builds an SqlMapClient using the specified reader and properties file.
     * <p/>
     * 
     * @param reader
     *            A Reader instance that reads an sql-map-config.xml file. The reader should read an well formed
     *            sql-map-config.xml file.
     * @param props
     *            Properties to be used to provide values to dynamic property tokens in the sql-map-config.xml
     *            configuration file. This provides an easy way to achieve some level of programmatic configuration.
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(Reader reader, Properties props) {
        SqlMapConfigParser parser = new SqlMapConfigParser();
        return parser.parse(reader, getProperties(props));
    }

    /**
     * Builds an SqlMapClient using the specified reader, properties file and database dialect.
     * <p/>
     * 
     * @param reader
     *            A Reader instance that reads an sql-map-config.xml file. The reader should read an well formed
     *            sql-map-config.xml file.
     * @param props
     *            Properties to be used to provide values to dynamic property tokens in the sql-map-config.xml
     *            configuration file. This provides an easy way to achieve some level of programmatic configuration.
     * @param dialect
     *            the database dialect
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(Reader reader, Properties props, String dialect) {
        SqlMapConfigParser parser = new SqlMapConfigParser(dialect);
        return parser.parse(reader, getProperties(props));
    }

    /**
     * Builds an SqlMapClient using the specified input stream.
     * 
     * @param inputStream
     *            An InputStream instance that reads an sql-map-config.xml file. The stream should read a well formed
     *            sql-map-config.xml file.
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(InputStream inputStream) {
        SqlMapConfigParser parser = new SqlMapConfigParser();
        return parser.parse(inputStream, getProperties(null));
    }

    /**
     * Builds an SqlMapClient using the specified input stream and database dialect.
     * 
     * @param inputStream
     *            An InputStream instance that reads an sql-map-config.xml file. The stream should read a well formed
     *            sql-map-config.xml file.
     * @param dialect
     *            the database dialect
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(InputStream inputStream, String dialect) {
        SqlMapConfigParser parser = new SqlMapConfigParser(dialect);
        return parser.parse(inputStream, getProperties(null));
    }

    /**
     * Builds an SqlMapClient using the specified input stream and properties file.
     * <p/>
     * 
     * @param inputStream
     *            An InputStream instance that reads an sql-map-config.xml file. The stream should read an well formed
     *            sql-map-config.xml file.
     * @param props
     *            Properties to be used to provide values to dynamic property tokens in the sql-map-config.xml
     *            configuration file. This provides an easy way to achieve some level of programmatic configuration.
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(InputStream inputStream, Properties props) {
        SqlMapConfigParser parser = new SqlMapConfigParser();
        return parser.parse(inputStream, getProperties(props));
    }

    /**
     * Builds an SqlMapClient using the specified input stream, properties file and database dialect.
     * <p/>
     * 
     * @param inputStream
     *            An InputStream instance that reads an sql-map-config.xml file. The stream should read an well formed
     *            sql-map-config.xml file.
     * @param props
     *            Properties to be used to provide values to dynamic property tokens in the sql-map-config.xml
     *            configuration file. This provides an easy way to achieve some level of programmatic configuration.
     * @param dialect
     *            the database dialect
     * @return An SqlMapClient instance.
     */
    public static SqlMapClient buildSqlMapClient(InputStream inputStream, Properties props, String dialect) {
        SqlMapConfigParser parser = new SqlMapConfigParser(dialect);
        return parser.parse(inputStream, getProperties(props));
    }
}
