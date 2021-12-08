/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.util;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Logger
 * User: Maxime HAMM
 * Date: 17/03/2017
 */
public class LoggerFactory {

    public static Logger getInstance(Class clazz) {
        return getInstance(clazz, null);
    }

    public static Logger getInstance(Class clazz, String suffix) {

        String canonicalName = clazz.getName();
        if (canonicalName.startsWith("org.jspresso.i18n"))
            canonicalName = "i18n.." + clazz.getSimpleName();
        if (suffix != null) {
            canonicalName += "." + suffix;
        }

        return Logger.getInstance(canonicalName);
    }
}
