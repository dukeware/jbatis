/*-
 * Copyright (c) 2007-2008 Owlgroup.
 * All rights reserved. 
 * TooManyResultException.java
 * Date: 2008-10-21
 * Author: Song Sun
 */
package org.ibatis.client;

import java.sql.SQLException;

/**
 * TooManyResultException
 * <p>
 * Date: 2008-10-21, 13:59:28 +0800
 * 
 * @author Song Sun
 * @version 1.0
 */
public class TooManyResultException extends SQLException {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7499818815190642587L;

    public TooManyResultException(int resultSize) {
        super("executeQueryForObject returned too many results.", null,
            resultSize);
    }
}
