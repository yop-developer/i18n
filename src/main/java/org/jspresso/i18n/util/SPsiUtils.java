/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * SPsiUtils
 * User: Maxime HAMM
 * Date: 07/06/2016
 */
public class SPsiUtils {

    /**
     * Find parent of type
     */
    public static <T extends PsiElement> T findSurrounding(PsiElement bean, Class<T> clazz) {
        PsiElement b = bean.getParent();
        while (b!=null && ! (b instanceof PsiFile)) {
            if (clazz.isAssignableFrom(b.getClass())) {
                return (T)b;
            }
            b = b.getParent();
        }
        return null;
    }
}
