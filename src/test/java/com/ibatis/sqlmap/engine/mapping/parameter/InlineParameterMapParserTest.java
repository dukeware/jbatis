/**
 * Copyright 2004-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibatis.sqlmap.engine.mapping.parameter;

import junit.framework.TestCase;

import com.ibatis.sqlmap.engine.mapping.sql.SqlText;
import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

public class InlineParameterMapParserTest extends TestCase {

  public void testParseInlineParameterMapTypeHandlerFactoryString() {
    InlineParameterMapParser parser = new InlineParameterMapParser();
    SqlText parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn) values (1)");
    assertEquals("insert into foo (myColumn) values (1)", parseInlineParameterMap.getText());

    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn) values (#myVar#)");
    assertEquals("insert into foo (myColumn) values (?)", parseInlineParameterMap.getText());

    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn) values (#myVar:javaType=int#)");
    assertEquals("insert into foo (myColumn) values (?)", parseInlineParameterMap.getText());

    // ## sunsong
    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn, myCol2) values (#myVar:javaType=int#, '#not parameter#')");
    assertEquals("insert into foo (myColumn, myCol2) values (?, '#not parameter#')", parseInlineParameterMap.getText());

    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn, myCol2) values (#myVar:javaType=int#, '#not parameter')");
    assertEquals("insert into foo (myColumn, myCol2) values (?, '#not parameter')", parseInlineParameterMap.getText());

    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn, myCol2) values (#myVar:javaType=int#, 'not parameter#')");
    assertEquals("insert into foo (myColumn, myCol2) values (?, 'not parameter#')", parseInlineParameterMap.getText());

    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn, myCol2##) values (#myVar:javaType=int#, '#not parameter')");
    assertEquals("insert into foo (myColumn, myCol2#) values (?, '#not parameter')", parseInlineParameterMap.getText());

    parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
        "insert into foo (myColumn, myCol2$$) values (#myVar:javaType=int#, '#not parameter')");
    assertEquals("insert into foo (myColumn, myCol2$$) values (?, '#not parameter')", parseInlineParameterMap.getText());

    
    try {
      parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
          "insert into foo (myColumn) values (#myVar)");
      fail();
    } catch (Exception e) {
      assertEquals("Unterminated inline parameter in mapped statement near '#myVar)'",
          e.getMessage());
    }

    try {
      parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
          "insert into foo (myColumn) values (#myVar:javaType=int)");
      fail();
    } catch (Exception e) {
      assertEquals("Unterminated inline parameter in mapped statement near '#myVar:javaType=int)'",
          e.getMessage());
    }

    try {
      parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
          "insert into foo (myColumn) values (myVar#)");
      fail();
    } catch (Exception e) {
      assertEquals(
          "Unterminated inline parameter in mapped statement near '#)'",
          e.getMessage());
    }

    try {
      parseInlineParameterMap = parser.parseInlineParameterMap(new TypeHandlerFactory(),
          "insert into foo (myColumn) values (#myVar##)");
      fail();
    } catch (Exception e) {
      assertEquals("Unterminated inline parameter in mapped statement near '#)'",
          e.getMessage());
    }
  }

}
