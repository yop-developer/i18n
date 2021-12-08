/*
 * Copyright (c) 2017. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * JspressoSnapWindowFactory
 * User: Maxime HAMM
 * Date: 14/01/2017
 *
 * @See QuickDocOnMouseOverManager
 */
public class JspressoSnapWindowFactory  implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        // Translation
        Content content = contentFactory.createContent(new TranslationSnapView(project), "Fast I18N", false);
        toolWindow.getContentManager().addContent(content);
    }

}
