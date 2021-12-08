/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.util;

/**
 * StringUtil
 * User: Maxime HAMM
 * Date: 12/06/2016
 */
public class StringUtil extends com.intellij.openapi.util.text.StringUtil {

    /**
     * Remove quotes
     */
    public static String removeQuotes(String s) {
        StringBuilder sb = new StringBuilder(s);
        int length = sb.length();
        if (length >0 && (sb.charAt(0) == '\'' || sb.charAt(0) == '\"')) {
            sb.deleteCharAt(0);
            length--;
        }
        if (length>0 && (sb.charAt(length-1) == '\'' || sb.charAt(length-1) == '\"')) {
            sb.deleteCharAt(length-1);
        }
        return sb.toString();
    }

}
