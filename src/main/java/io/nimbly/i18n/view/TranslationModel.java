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

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import io.nimbly.i18n.util.I18nUtil;
import io.nimbly.i18n.util.JavaUtil;
import io.nimbly.i18n.util.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * TranslationModel
 * User: Maxime HAMM
 * Date: 16/01/2017
 */
public class TranslationModel {

    private static final Logger LOG = LoggerFactory.getInstance(TranslationModel.class);

    private String keyPath;
    private final Module module;

    private String selectedKey;
    private String selectedLanguage;

    private PropertiesFile selectedPropertiesFile;
    private boolean viewRefreshBlocked;
    private final PsiFile originFile;

    public TranslationModel(String keyPath, PsiFile originFile, Module module) {

        LOG.debug("TranslationModel instanciation for key '" + keyPath + "'");
        this.module = module;
        this.keyPath = keyPath;
        this.originFile = originFile;

        String defaultLanguage = I18nUtil.getPreferedLanguage();
        LOG.trace("TranslationModel instanciation for key '" + keyPath + "' - prefered language : '" + defaultLanguage + "'");

        if (defaultLanguage == null) {

            defaultLanguage = Locale.ENGLISH.getLanguage();
            I18nUtil.setLastUsedLanguage(defaultLanguage);
        }

        if (originFile instanceof PropertiesFile && originFile.isWritable()) {
            selectedPropertiesFile = (PropertiesFile) originFile;
            defaultLanguage = I18nUtil.getLanguage((PropertiesFile) originFile);
        }
        else {
            selectedPropertiesFile = I18nUtil.getBestPropertiesFile(keyPath, module);
        }

        assert defaultLanguage != null;
        selectedLanguage = defaultLanguage.toLowerCase();
        LOG.trace("TranslationModel instanciation for key '" + keyPath + "' - module language selected : '" + defaultLanguage + "'");

        LOG.debug("TranslationModel instanciation for key '" + keyPath + "' - selectedPropertiesFile : '" + selectedPropertiesFile + "'");
    }

    public String getKeyPath() {
        return keyPath;
    }

    public List<PropertiesFile> getPropertiesFiles() {

        List<PropertiesFile> files = I18nUtil.getPsiPropertiesFiles(selectedLanguage, module);
        if (files.isEmpty() && selectedPropertiesFile!=null && !module.equals(JavaUtil.getModule(selectedPropertiesFile.getContainingFile()))) {

            files = I18nUtil.getPsiPropertiesFiles(selectedLanguage, JavaUtil.getModule(selectedPropertiesFile.getContainingFile()));
        }

        return files;
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    public PsiFile getOriginFile() {
        return originFile;
    }

    public void updateKey(String language, String translation) {

        PropertiesFile targetFile = I18nUtil.getPsiPropertiesSiblingFile(getSelectedPropertiesFile(), language, module);
        I18nUtil.doUpdateTranslation(selectedKey, translation, targetFile, true);
    }

    public List<String> getLanguages() {

        LOG.trace("getLanguages for key '" + keyPath + "'");
        if (selectedPropertiesFile == null) {

            LOG.warn("getLanguages for key '" + keyPath + "' - NO SELECTED PROPERTIES FILE - STOP");
            return Collections.emptyList();
        }

        return I18nUtil.getLanguages(selectedPropertiesFile.getResourceBundle());
    }

    public void setSelectedLanguage(String selectedLanguage) {
        this.selectedLanguage = selectedLanguage;

        if (selectedPropertiesFile!=null) {
            selectedPropertiesFile = I18nUtil.getPsiPropertiesSiblingFile(selectedPropertiesFile, selectedLanguage, module);
        }
    }

    public PropertiesFile getSelectedPropertiesFile() {
        return selectedPropertiesFile;
    }

    public List<IProperty> getPsiProperties(String language) {
        if (selectedPropertiesFile == null)
            return Collections.emptyList();

        PropertiesFile propertiesFile = I18nUtil.getPsiPropertiesSiblingFile(selectedPropertiesFile, language, module);
        if (propertiesFile == null)
            return Collections.emptyList();
        return I18nUtil.getPsiProperties(propertiesFile, selectedKey, language, module);
    }

    public boolean hasAtLeastOneTranslation() {
        for (String lang : getLanguages()) {
            if (! getPsiProperties(lang).isEmpty())
                return true;
        }
        return false;
    }

    public Module getModule() {
        return module;
    }

    public String getSelectedLanguage() {
        return selectedLanguage;
    }

    public void setSelectedKey(String newKey) {
        this.selectedKey = newKey;
    }

    public boolean scrollLeft() {
        if (selectedKey.length()<keyPath.length()) {
            int i = keyPath.substring(0, keyPath.length()-selectedKey.length()-1).lastIndexOf('.');
            selectedKey = keyPath.substring(i+1);
            return true;
        }
        return false;
    }

    public boolean scrollRight() {
        int i = selectedKey.indexOf('.');
        if (i>0) {
            selectedKey = selectedKey.substring(i+1);
            return true;
        }
        return false;
    }

    public void selectPropertiesFile(PropertiesFile propertiesFile) {
        this.selectedPropertiesFile = propertiesFile;
    }

    public void deleteKey() {

        try {
            viewRefreshBlocked = true;
            I18nUtil.doDeleteTranslationKey(selectedPropertiesFile.getResourceBundle(), selectedKey, module);
        }
        finally {
            viewRefreshBlocked = false;
        }

        PsiDocumentManager.getInstance(module.getProject()).commitAllDocuments();

        selectedPropertiesFile = I18nUtil.getBestPropertiesFile(keyPath, module);
    }

    /**
     * duplicateKey
     */
    public String duplicateKey() {

        String key = getSelectedKey();
        ResourceBundle sourceBundle = selectedPropertiesFile.getResourceBundle();
        Module module = getModule();

        String newKey;
        try {
            viewRefreshBlocked = true;
            newKey = I18nUtil.doDuplicateTranslationKey(key, sourceBundle, sourceBundle, getSelectedPropertiesFile());
        }
        finally {
            viewRefreshBlocked = false;
        }

        PsiDocumentManager.getInstance(module.getProject()).commitAllDocuments();

        selectedPropertiesFile = I18nUtil.getBestPropertiesFile(newKey, module);

        return newKey;
    }



    public void createKey() {

        // check if writable
        PsiFile containingFile = getSelectedPropertiesFile().getContainingFile();
        boolean writable = containingFile.isWritable();
        if (!writable)
            return;

        try {
            viewRefreshBlocked = true;
            I18nUtil.doCreateTranslationKey(getSelectedPropertiesFile().getResourceBundle(), selectedKey, JavaUtil.getModule(containingFile));
        }
        finally {
            viewRefreshBlocked = false;
        }

        PsiDocumentManager.getInstance(module.getProject()).commitAllDocuments();
    }



    public void renameKey(String newKeyName) {

        try {
            viewRefreshBlocked = true;
            I18nUtil.doRenameI18nKey(getSelectedPropertiesFile().getResourceBundle(), selectedKey, newKeyName, module);
        }
        finally {
            viewRefreshBlocked = false;
        }


        PsiDocumentManager.getInstance(module.getProject()).commitAllDocuments();

        if (keyPath.equals(selectedKey))
            keyPath = newKeyName;
        else
            keyPath = newKeyName + keyPath.substring(selectedKey.length());

        selectedKey = newKeyName;
    }

    public String getSelectedBundle() {
        if (selectedPropertiesFile == null)
            return null;
        return I18nUtil.getBundle(selectedPropertiesFile);
    }

    public String getSelectedPropertiesFileTooltip() {
        if (selectedPropertiesFile == null)
            return null;
       return getTooltip(selectedLanguage, selectedPropertiesFile, module);
    }

    public String getSelectedBundleTooltip() {
        if (selectedPropertiesFile == null)
            return null;
        return getTooltip("*", selectedPropertiesFile, module);
    }

    public static String getTooltip(String langOrStar, PropertiesFile propertiesFile, Module module) {

        String basePath = JavaUtil.getModuleRootPath(module);
        if (basePath == null) {
            return null;
        }

        String source = propertiesFile.getVirtualFile().getPath();
        int i = source.lastIndexOf("_");
        int j = source.indexOf(".", i + 1);
        source = source.substring(0, i + 1) + langOrStar + source.substring(j);

        int idx = source.indexOf("!/");
        if (idx <0) {
            return "./" + com.intellij.openapi.util.io.FileUtil.getRelativePath(basePath, source, '/');
        }
        else {
            String jar = source.substring(0, idx);
            jar = jar.substring(jar.lastIndexOf('/')+1);
            return "[" + jar + "] " + source.substring(idx+2);
        }
    }

    public String getShortName(PropertiesFile propertiesFile) {

        String name = propertiesFile.getVirtualFile().getName();
        name = name.substring(0, name.indexOf('_'));

        String source = propertiesFile.getVirtualFile().getPath();
        int idx = source.indexOf("!/");
        if (idx <0) {

            Module m = JavaUtil.getModule(propertiesFile.getContainingFile());
            if (m.equals(getModule())) {
                return name;
            }
            else {
                return name + " [" + m.getName() + "] ";
            }
        }
        else {
            String jar = source.substring(0, idx);
            jar = jar.substring(jar.lastIndexOf('/')+1);
            return name + " [" + jar + "] ";
        }
    }

    public boolean isViewRefreshBlocked() {
        return viewRefreshBlocked;
    }
}
