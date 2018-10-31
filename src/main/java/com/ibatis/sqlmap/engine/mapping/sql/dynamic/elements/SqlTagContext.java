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
package com.ibatis.sqlmap.engine.mapping.sql.dynamic.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;

public class SqlTagContext {

    private StringBuilder buf;

    private HashMap<SqlTag, IterateContext> attributes;

    private StringBuilder rfpStack;
    private LinkedList<IterateContext> iterateContextStack;

    private ArrayList<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();

    public SqlTagContext() {
        buf = new StringBuilder();
        attributes = new HashMap<SqlTag, IterateContext>();
        rfpStack = new StringBuilder();
        iterateContextStack = new LinkedList<IterateContext>();
    }

    public StringBuilder getStringBuilder() {
        return buf;
    }

    public String getBodyText() {
        return buf.toString();
    }

    public void setAttribute(SqlTag key, IterateContext value) {
        attributes.put(key, value);
    }

    public IterateContext getAttribute(SqlTag key) {
        return attributes.get(key);
    }

    public void addParameterMapping(ParameterMapping mapping) {
        parameterMappings.add(mapping);
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    public int removeFirtPrependStackSize() {
        return rfpStack.length();
    }

    /**
     * examine the value of the top RemoveFirstPrependMarker object on the stack.
     * 
     * @return was the first prepend removed
     */
    public boolean peekRemoveFirstPrependMarker() {
        return rfpStack.charAt(rfpStack.length() - 2) == '1';
    }

    /**
     * pop the first RemoveFirstPrependMarker once the recursion is on it's way out of the recursion loop and return
     * it's internal value.
     *
     * @param tag
     */
    public void popRemoveFirstPrependMarker() {
        rfpStack.setLength(rfpStack.length() - 1);
    }

    /**
     * push a new RemoveFirstPrependMarker object with the specified internal state
     * 
     * @param tag
     */
    public void pushRemoveFirstPrependMarker(SqlTag tag) {

        if (tag.getHandler() instanceof DynamicTagHandler) {
            // this was added to retain default behavior
            if (tag.isPrependAvailable()) {
                rfpStack.append('1');
            } else {
                rfpStack.append('0');
            }
        } else if (tag.getRemoveFirstPrepend() > 0) {
            // you must be specific about the removal otherwise it
            // will function as ibatis has always functioned and add
            // the prepend
            rfpStack.append('1');
        } else if (!tag.isPrependAvailable() && tag.getRemoveFirstPrepend() == 0 && tag.getParent() != null) {
            // if no prepend or removeFirstPrepend is specified
            // we need to look to the parent tag for default values
            if (tag.getParent().getRemoveFirstPrepend() > 0) {
                rfpStack.append('1');
            }
        } else {
            rfpStack.append('0');
        }

    }

    /**
     * set a new internal state for top RemoveFirstPrependMarker object
     *
     */
    public void disableRemoveFirstPrependMarker() {
        rfpStack.setCharAt(rfpStack.length() - 2, '0');
    }

    public void reEnableRemoveFirstPrependMarker() {
        rfpStack.setCharAt(rfpStack.length() - 1, '1');
    }

    /**
     * iterate context is stored here for nested dynamic tags in the body of the iterate tag
     * 
     * @param iterateContext
     */
    public void pushIterateContext(IterateContext iterateContext) {
        iterateContextStack.addFirst(iterateContext);
    }

    /**
     * iterate context is removed here from the stack when iterate tag is finished being processed
     * 
     * @return the top element of the context stack
     */
    public IterateContext popIterateContext() {
        IterateContext retVal = null;
        if (!iterateContextStack.isEmpty()) {
            retVal = iterateContextStack.removeFirst();
        }
        return retVal;
    }

    /**
     * iterate context is removed here from the stack when iterate tag is finished being processed
     * 
     * @return the top element on the context stack
     */
    public IterateContext peekIterateContext() {
        IterateContext retVal = null;
        if (!iterateContextStack.isEmpty()) {
            retVal = iterateContextStack.getFirst();
        }
        return retVal;
    }

}