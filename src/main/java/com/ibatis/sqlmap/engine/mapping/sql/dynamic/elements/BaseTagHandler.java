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

import com.ibatis.sqlmap.engine.mapping.sql.SqlText;

public abstract class BaseTagHandler implements SqlTagHandler {

    @Override
    public int doStartFragment(SqlTagContext ctx, SqlTag tag, Object parameterObject) {
        ctx.pushRemoveFirstPrependMarker(tag);
        return SqlTagHandler.INCLUDE_BODY;
    }

    @Override
    public int doEndFragment(SqlTagContext ctx, SqlTag tag, Object parameterObject, StringBuilder bodyContent) {
        if (tag.isCloseAvailable() && !(tag.getHandler() instanceof IterateTagHandler)) {
            if (SqlText.isNotEmpty(bodyContent)) {
                bodyContent.append(tag.getCloseAttr());
            }
        }
        return SqlTagHandler.INCLUDE_BODY;
    }

    @Override
    public void doPrepend(SqlTagContext ctx, SqlTag tag, Object parameterObject, StringBuilder bodyContent) {

        if (tag.isOpenAvailable() && !(tag.getHandler() instanceof IterateTagHandler)) {
            if (SqlText.isNotEmpty(bodyContent)) {
                bodyContent.insert(0, tag.getOpenAttr());
            }
        }

        if (tag.isPrependAvailable()) {
            if (SqlText.isNotEmpty(bodyContent)) {
                if (tag.getParent() != null && ctx.peekRemoveFirstPrependMarker()) {
                    ctx.disableRemoveFirstPrependMarker();
                } else {
                    bodyContent.insert(0, tag.getPrependAttr());
                }
            }
        }
    }
}
