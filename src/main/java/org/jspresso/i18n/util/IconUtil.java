/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
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
