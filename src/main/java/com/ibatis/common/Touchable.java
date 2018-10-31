/*-
 * Copyright 2015 Owl Group
 * All rights reserved.
 */
package com.ibatis.common;

import java.io.File;

/**
 * Touchable for internal use only
 * <p>
 * Date: 2016-10-28
 * 
 * @author Song Sun
 * @version 1.0
 */
public interface Touchable {
    void onTouch(File statusFile);
}
