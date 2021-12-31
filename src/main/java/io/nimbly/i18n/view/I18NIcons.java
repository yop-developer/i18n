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
package io.nimbly.i18n.view;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import io.nimbly.i18n.util.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * I18NIcons
 * User: Maxime HAMM
 * Date: 31/05/2016
 */
public interface I18NIcons {

    Logger LOG = LoggerFactory.getInstance(I18NIcons.class);
    
    Icon GOOGLE_TRANSALTE = IconLoader.getIcon("/io/nimbly/i18n/icons/g_trans.png");

    Icon EDIT = IconLoader.getIcon("/io/nimbly/i18n/icons/edit.png");
    Icon FIND = IconLoader.getIcon("/io/nimbly/i18n/icons/find.png");
    Icon DELETE = IconLoader.getIcon("/io/nimbly/i18n/icons/delete.png");
    Icon ADD = IconLoader.getIcon("/io/nimbly/i18n/icons/add.png");
    Icon DUPLICATE = IconLoader.getIcon("/io/nimbly/i18n/icons/add.png");

    Icon LEFT = IconLoader.getIcon("/io/nimbly/i18n/icons/left.png");
    Icon RIGHT = IconLoader.getIcon("/io/nimbly/i18n/icons/right.png");
    Icon MOVE_TO = IconLoader.getIcon("/io/nimbly/i18n/icons/move-to-button.png");
    Icon TRANSPARENT = IconLoader.getIcon("/io/nimbly/i18n/icons/transparent.png");

    Map<String, Icon> FLAGS = new HashMap<>();

    static Icon getFlag(String country) {

        Icon icon = FLAGS.get(country);
        if (icon !=null)
            return icon;

        String path = "/io/nimbly/i18n/icons/languages/" + country + ".png";
        try {
            icon = IconLoader.findIcon(path);  // do not use 'getIcon' which logs an error exception !
        } catch (Throwable ignored) {
            LOG.trace("Icon '" + path + "' not found");
        }
        FLAGS.put(country, icon);

        return icon;
    }

}
