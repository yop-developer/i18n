/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * EditorUtil
 * User: Maxime HAMM
 * Date: 06/01/2017
 */
public class FileUtil {

    public static final Editor[] getEditors() {
        EditorFactory factory = EditorFactory.getInstance();
        if (factory == null)
            return null;

        return factory.getAllEditors();
    }

    public static PsiFile getFile(Editor editor) {
        return PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
    }

    public static PsiFile getFile(Document document, Project project) {
        return PsiDocumentManager.getInstance(project).getPsiFile(document);
    }

    public static PsiFile getFile(PsiElement element) {
        if (! element.isValid())
            return null;
        return element.getContainingFile();
    }

    @Nullable
    public static PsiFile getFile(VirtualFile file, Project project) {
        return PsiManager.getInstance(project).findFile(file);
    }

    @Nullable
    public static PsiDirectory getDirectory(@NotNull VirtualFile directory, @NotNull Project project) {
        return PsiManager.getInstance(project).findDirectory(directory);
    }

    public static Document getDocument(PsiElement psiElement) {
        PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile == null)
            return null;

        VirtualFile file = containingFile.getVirtualFile();
        if (file == null)
            return null;

        return FileDocumentManager.getInstance().getDocument(file);
    }
}
