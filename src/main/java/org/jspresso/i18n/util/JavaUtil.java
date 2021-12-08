/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.util;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.codeStyle.ImportHelper;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.*;

import static com.intellij.openapi.roots.JavaProjectRootsUtil.isForGeneratedSources;

/**
 * JavaUtils
 * User: Maxime HAMM
 * Date: 10/06/2016
 *
 * @see com.intellij.psi.util.MethodSignatureUtil
 */
public class JavaUtil {

    public static String TEST_MODULE_PATH = null;

    /**
     * Get's module scope
     */
    public static GlobalSearchScope getJavaSearchScope(Module module) {
        return module.getModuleWithDependenciesAndLibrariesScope(true);
    }

    /**
     * Get's module
     */
    public static Module getModule(PsiElement element) {
        if (element == null)
            return null;

        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            PsiFile file = element.getContainingFile();
            if (file !=null)
                module = ModuleUtilCore.findModuleForPsiElement(file);
        }
        return module;
    }

    /**
     * Get's module root path
     */
    @Nullable
    public static String getModuleRootPath(Module module) {
        if (TEST_MODULE_PATH != null)
            return TEST_MODULE_PATH;

        VirtualFile moduleRoot = getModuleRoot(module);
        return moduleRoot != null ? moduleRoot.getPath() : null;
    }

    /**
     * Get's module root path
     */
    @Nullable
    public static VirtualFile getModuleRoot(Module module) {
         if (module == null)
            return null;

        return VirtualFileManager.getInstance().findFileByUrl("file://" + module.getModuleFilePath().substring(0, module.getModuleFilePath().lastIndexOf('/')));
    }

    /**
     * Find package
     */
    public static PsiPackage findPackage(String qualifieName, Module module) {
        if (module == null)
            return null;
        return JavaPsiFacade.getInstance(module.getProject()).findPackage(qualifieName);
    }

}
