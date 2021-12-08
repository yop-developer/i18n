/*
 * I18N
 * Copyright (C) 2021  Maxime HAMM - NIMBLY CONSULTING - maxime.hamm.pro@gmail.com
 *
 * This document is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package org.jspresso.i18n.util;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.intellij.util.IconUtil.textToIcon;

/**
 * IconUtils
 * User: Maxime HAMM
 * Date: 10/01/2017
 */
public class IconUtil {

    public static final int SPACING = 10;

    /**
     * Add text
     */
    @NotNull
    public static Icon addText(@NotNull Icon base, @NotNull String text, float size, int position) {
        LayeredIcon icon = new LayeredIcon(2);
        icon.setIcon(base, 0);
        icon.setIcon(textToIcon(text, new JLabel(), JBUI.scale(size)), 1, position);
        return icon;
    }

    /**
     * scale
     */
    public static Icon scale(@NotNull final Icon source, double factor) {
        return com.intellij.util.IconUtil.scale(source, factor);
    }

    public static double getScaleFactorToFit(Dimension original, Dimension toFit) {
        double dScale = 1d;
        if (original != null && toFit != null) {
            double dScaleWidth = getScaleFactor(original.width, toFit.width);
            double dScaleHeight = getScaleFactor(original.height, toFit.height);
            dScale = Math.max(dScaleHeight, dScaleWidth);
        }
        return dScale;
    }

    private static double getScaleFactor(int iMasterSize, int iTargetSize) {
        return (double) iTargetSize / (double) iMasterSize;
    }



    public static Icon getIcon(String path, Dimension dimensionToFit) {

        Icon icon = IconLoader.getIcon(path);

        double factor = getScaleFactorToFit(new Dimension(icon.getIconWidth(), icon.getIconHeight()), dimensionToFit);

        return scale(icon, factor);
    }

}
