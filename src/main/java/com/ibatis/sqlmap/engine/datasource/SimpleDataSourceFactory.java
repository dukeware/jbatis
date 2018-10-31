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
package com.ibatis.sqlmap.engine.datasource;

import com.ibatis.common.jdbc.SimpleDataSource;

import javax.sql.DataSource;

import java.util.Map;

/**
 * DataSourceFactory implementation for the iBATIS SimpleDataSource
 */
public class SimpleDataSourceFactory implements DataSourceFactory {

    private DataSource dataSource;

    @Override
    public void initialize(Map<Object, Object> map) {
        dataSource = new SimpleDataSource(map);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

}