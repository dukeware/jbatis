/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ibatis.spring;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

/**
 * Add string attrs from servlet context to ibatis global properties.
 * 
 * @author Song Sun
 */
public class SqlMapClientFactoryBeanX extends SqlMapClientFactoryBean implements ServletContextAware {

    ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    @Override
    protected Properties getProperties(Properties props) {
        Properties p = super.getProperties(props);
        if (servletContext != null) {
            
            Enumeration<String> params = servletContext.getInitParameterNames();
            while (params.hasMoreElements()) {
                String key = params.nextElement();
                String val = servletContext.getInitParameter(key);
                p.setProperty(key, (String) val);
            }
            
            Enumeration<String> keys = servletContext.getAttributeNames();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Object val = servletContext.getAttribute(key);
                if (val instanceof String) {
                    p.setProperty(key, (String) val);
                }
            }
        }
        return p;
    }
}
