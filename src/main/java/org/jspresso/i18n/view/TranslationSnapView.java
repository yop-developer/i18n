/*
 * Copyright (c) 2018. All rights reserved to Maxime HAMM.
 *   This file is part of Jspresso Developer Studio
 */
package org.jspresso.i18n.view;

import com.intellij.ide.ui.laf.IntelliJLaf;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.SlowOperations;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jspresso.i18n.util.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.*;

/**
 * TranslationView
 * User: Maxime HAMM
 * Date: 14/01/2017
 */
public class TranslationSnapView extends AbstractSnapView {

    private static Logger LOG = LoggerFactory.getInstance(TranslationSnapView.class);
    public static final String NIMBLY = "Nimbly";

    public static final String BLOCK_I18N = "BLOCK_I18N";

    public static final String DELETE_KEY = "Delete";
    public static final String CREATE_KEY = "Create key";
    public static final String BLOCK_REFRESH = "BLOCK_REFRESH";

    private final ActionToolbar editActionToolBar;

    private JPanel translationWindow;
    private JTextField key;

    private LinkLabel[] flags;
    private JTextComponent[] translations;
    private LinkLabel[] languages;

    private ComboBox<MyPropertiesFileInfo> resourcesGroup;
    private JButton duplicateButton;
    private JButton deleteOrCreateKeyButton;

    private MyTranslationPaneAdapter[] translationsPaneAdaptors = null;
    private Map<Document, MyPropertiesFileAdapater> propertiesFileAdaptors = new HashMap<>();

    private AnAction leftAction;
    private AnAction rightAction;

    private TranslationModel model = null;

    private ToggleAction editAction;
    private Project project;

    /**
     * TranslationSnapView
     * @param project
     */
    public TranslationSnapView(Project project) {

        this.project = project;

        // init UI
        setLayout(new GridLayoutManager(2, 1));

        deleteOrCreateKeyButton = new JButton();
        deleteOrCreateKeyButton.setText(DELETE_KEY);
        deleteOrCreateKeyButton.addActionListener(e -> {
            if (DELETE_KEY.equals(deleteOrCreateKeyButton.getText())) {
                String key = model.getSelectedKey();
                model.deleteKey();

                initTranslationKey(key, true, model.getOriginFile(), model.getModule());
            }
            else {
                model.createKey();
            }

            refreshWhenDocumentUpdated();
        });
        deleteOrCreateKeyButton.setFont(deleteOrCreateKeyButton.getFont().deriveFont(deleteOrCreateKeyButton.getFont().getStyle(), deleteOrCreateKeyButton.getFont().getSize() -2));

        duplicateButton = new JButton();
        duplicateButton.addActionListener(e -> duplicateKey());
        duplicateButton.setIcon(SJSIcons.DUPLICATE);
        duplicateButton.setFont(duplicateButton.getFont().deriveFont(duplicateButton.getFont().getStyle(), duplicateButton.getFont().getSize() -2));
        duplicateButton.setText("Duplicate key");

        resourcesGroup = new ComboBox<>();
        resourcesGroup.setModel(new DefaultComboBoxModel<>());
        resourcesGroup.setRenderer(new DefaultListCellRenderer());

        resourcesGroup.setFont(resourcesGroup.getFont().deriveFont(resourcesGroup.getFont().getStyle(), resourcesGroup.getFont().getSize() -2));
        resourcesGroup.setBackground(deleteOrCreateKeyButton.getBackground());
        resourcesGroup.addItemListener(event -> {

            if (ItemEvent.SELECTED != event.getStateChange())
                return;

            if (Boolean.TRUE.equals(resourcesGroup.getClientProperty(BLOCK_REFRESH)))
                return;

            Object item = event.getItem();
            if (item instanceof MyPropertiesFileInfo) {
                MyPropertiesFileInfo info = (MyPropertiesFileInfo) item;
                PropertiesFile propertiesFile = info.getPropertiesFile();

                try {
                    SlowOperations.allowSlowOperations((ThrowableRunnable<Throwable>) () ->
                            loadTranslation(key.getText(), propertiesFile));
                } catch (Throwable ee) {
                    LOG.error("Translation init error", ee);
                }

            }
        });

        ActionGroup editActionGroup = createEditActionGroup();
        editActionToolBar = ActionManager.getInstance().createActionToolbar("EDIT", editActionGroup, true);
        editActionToolBar.setMinimumButtonSize(new Dimension(25, 20));


        initComponents(Collections.emptyList());

        // find potential string literal to load
        for (Editor editor : FileUtil.getEditors()) {
            int offset = editor.getCaretModel().getOffset();
            if (offset >= 0) {
                PsiElement target = findTarget(editor);
                if (target == null)
                    continue;

                Module module = JavaUtil.getModule(target);
                String text = findI18NKey(target, module);
                if (text != null) {
                    String key = StringUtil.removeQuotes(text);

                    initTranslationKey(key, false, FileUtil.getFile(editor), module);
                    return;
                }
            }
        }

    }

    /**
     * duplicateKey
     */
    private void duplicateKey() {

        Module module = model.getModule();

        boolean editable = translations[0].isEditable();

        String newKey = model.duplicateKey();

        model = new TranslationModel(newKey, model.getOriginFile(), module);
        loadTranslation(newKey, null);

        if (editable) {
            editAction.setSelected(null, true);
        }

    }

    /**
     * doEditorDocumentChanged
     */
    @Override
    protected void doEditorDocumentChanged(Editor editor, DocumentEvent e) {
        if (editor.isDisposed())
            return;

        if (! editor.getComponent().hasFocus())
            return;

        initTranslation(editor);
    }

    /**
     * doCaretPositionChanged
     */
    @Override
    protected  void doCaretPositionChanged(CaretEvent event) {

        LOG.debug("doCaretPositionChanged !");
        Boolean block = (Boolean) getClientProperty(BLOCK_I18N);
        if (Boolean.TRUE.equals(block)) {
            LOG.trace("doCaretPositionChanged : blocked - STOP");
            return;
        }

        if (model!=null && model.isViewRefreshBlocked()) {
            LOG.trace("doCaretPositionChanged : view refresh blocked - STOP");
            return;
        }

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                SlowOperations.allowSlowOperations((ThrowableRunnable<Throwable>) () ->
                        initTranslation(event.getEditor()));
            } catch (Throwable e) {
                LOG.error("Translation init error", e);
            }
        });

//        DumbService.getInstance(Objects.requireNonNull(event.getEditor().getProject())).runReadActionInSmartMode(() -> {
//            try {
//                SlowOperations.allowSlowOperations((ThrowableRunnable<Throwable>) () ->
//                        initTranslation(event.getEditor()));
//            } catch (Throwable e) {
//                LOG.error("Translation init error", e);
//            }
//        });
    }

    /**
     * Init translations
     */
    private void initTranslationKey(final String fullI18nKey, boolean force, PsiFile originFile, final Module module) {

        LOG.debug("initTranslation for key '" + fullI18nKey + "'");
        if (module == null) {
            LOG.trace("initTranslation for key '" + fullI18nKey + "' : no module found - STOP");
            return;
        }
        
        if (!force && model != null && model.getKeyPath().equals(fullI18nKey)) {
            LOG.trace("initTranslation for key '" + fullI18nKey + "' : same key already selected - STOP");
            return;
        }

        PropertiesFile currentFile = model != null ? model.getSelectedPropertiesFile() : null;
        model = new TranslationModel(fullI18nKey, originFile, module);
        if (model.getSelectedPropertiesFile() == null) {

            if (originFile instanceof PropertiesFile) {
                model.selectPropertiesFile((PropertiesFile) originFile);
            }
            else if (currentFile != null) {
                model.selectPropertiesFile(currentFile);
            }
            else {

                List<ResourceBundle> bundles = I18nUtil.getResourceBundles(module);
                if (! bundles.isEmpty()) {

                    for (PropertiesFile pf : bundles.get(0).getPropertiesFiles()) {

                        if (pf.getVirtualFile().isWritable() && "en".equals(I18nUtil.getLanguage(pf))) {

                            model.selectPropertiesFile(pf);
                            break;
                        }
                    }
                }
            }
        }

        loadTranslation(fullI18nKey, null);

    }

    /**
     * Load translations
     */
    private void loadTranslation(final String i18nKey, PropertiesFile forceFile) {

        LOG.info("loadTranslation for key '" + i18nKey + "'");
        model.setSelectedKey(i18nKey);
        if (forceFile != null) {
            LOG.trace("loadTranslation for key '" + i18nKey + "' using file '" + forceFile + "'");
            model.selectPropertiesFile(forceFile);
        }

        //
        // Update UI if necessary
        List<String> moduleLanguages = model.getLanguages();
        if (moduleLanguages.isEmpty()) {

            LOG.trace("loadTranslation for key '" + i18nKey + "' : NO LANGUAGE FOUND - STOP");
            return;
        }

        if (flags==null || flags.length != moduleLanguages.size()) {

            LOG.trace("loadTranslation for key '" + i18nKey + "' : update languages list");

            // clear resourcesGroup and listener
            if (translationWindow !=null)
                this.remove(translationWindow);

            if (this.translations !=null) {
                for (int i = 0; i < this.translations.length; i++) {
                    translations[i].getDocument().removeDocumentListener(translationsPaneAdaptors[i]);
                    translations[i].removeFocusListener(translationsPaneAdaptors[i]);
                }
            }

            for (Document doc : propertiesFileAdaptors.keySet()) {
                MyPropertiesFileAdapater listener = propertiesFileAdaptors.get(doc);
                if (listener!=null)
                    doc.removeDocumentListener(listener);
            }
            propertiesFileAdaptors.clear();

            // reload swing
            initComponents(moduleLanguages);

            this.add(translationWindow, new GridConstraints(1, 0, 1, 1,
                    GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_CAN_GROW,
                    new Dimension(100, 0), null, null));

            // setup listeners
            this.translationsPaneAdaptors = new MyTranslationPaneAdapter[this.flags.length];
            for (int i=0; i<this.flags.length; i++) {

                String lang = moduleLanguages.get(i);

                this.flags[i].setIcon(SJSIcons.getFlag(lang));

                Icon ico = IconUtil.addText(SJSIcons.TRANSPARENT, lang.toUpperCase(), 12f, SwingConstants.CENTER);
                this.languages[i].setIcon(ico);
                this.languages[i].setDisabledIcon(ico);

                this.translationsPaneAdaptors[i] = new MyTranslationPaneAdapter(i);
                this.translations[i].getDocument().addDocumentListener(translationsPaneAdaptors[i]);
                this.translations[i].addFocusListener(translationsPaneAdaptors[i]);
            }

            LOG.trace("loadTranslation for key '" + i18nKey + "' : update languages list - done");
        }

        //
        // Sets key
        LOG.trace("loadTranslation for key '" + i18nKey + "' : setup text");
        this.key.setText(i18nKey);
        this.key.setEditable(false);
        LOG.trace("loadTranslation for key '" + i18nKey + "' : setup text - done");

        //
        // Load psi translations
        boolean isWritable = false;
        boolean atLeasOneTranslations = false;
        IProperty[] translationProperties = new IProperty[this.flags.length];
        for (int i=0; i<this.flags.length; i++) {

            String lang = getLanguage(i);
            List<IProperty> psiProperties = model.getPsiProperties(lang);
            IProperty prop = psiProperties.isEmpty() ? null : psiProperties.get(0);

            if (prop !=null) {

                //
                // if property file not part if current model, do not edit it !
                // This is possible when using another module (i.e not jars)
                PsiFile containingFile = prop.getPropertiesFile().getContainingFile();
                boolean w = containingFile.isWritable();

                isWritable |= w;
                atLeasOneTranslations = true;

            }
            translationProperties[i] = prop;
            LOG.trace("loadTranslation for key '" + i18nKey + "' : find translation properties for language '" + lang + "'");
        }

        //
        // Update translations
        for (int i=0; i<this.flags.length; i++) {

            // disable fields if no translation att all
            this.translations[i].setBackground(atLeasOneTranslations&isWritable ? new JTextField().getBackground() : translationWindow.getBackground());
            this.translations[i].setEnabled(atLeasOneTranslations);
            this.translations[i].setEditable(isWritable);

            String tr = translationProperties[i] != null ? I18nUtil.unescapeKeepCR(translationProperties[i].getValue()) : "";
            LOG.trace("loadTranslation for key '" + i18nKey + "' : setup translation '" + tr + "'");
            setTranslationNoEvents(this.translations[i], tr);

            // add psi element modification listener
            if (translationProperties[i] != null) {

                Document doc = FileUtil.getDocument(translationProperties[i].getPsiElement());
                if (doc != null && !propertiesFileAdaptors.containsKey(doc)) {

                    MyPropertiesFileAdapater listener = new MyPropertiesFileAdapater();
                    doc.addDocumentListener( listener);

                    propertiesFileAdaptors.put(doc, listener);
                }
            }

            this.languages[i].setEnabled(isWritable);
        }

        // Update create or delete key button
        LOG.trace("loadTranslation for key '" + i18nKey + "' : update CRUD buttons'");
        updateCRUDButtons(atLeasOneTranslations);

        //
        // Update "open ressource button"
        LOG.trace("loadTranslation for key '" + i18nKey + "' : update Edit button");
        updateEditButton(null);
    }

    private String getLanguage(int index) {
        List<String> languages = model.getLanguages();
        if (index > languages.size()-1)
            return null;
        return languages.get(index);
    }

    /**
     * updateCRUDButtons
     */
    private void updateCRUDButtons(boolean atLeasOneTranslations) {

        boolean isWritable = translations[0].isEditable();

        deleteOrCreateKeyButton.setText(isWritable && atLeasOneTranslations ? DELETE_KEY : CREATE_KEY);
        deleteOrCreateKeyButton.setIcon(isWritable && atLeasOneTranslations ? SJSIcons.DELETE : SJSIcons.ADD);

        deleteOrCreateKeyButton.setEnabled(model.getSelectedPropertiesFile().getVirtualFile().isWritable());

        duplicateButton.setVisible(atLeasOneTranslations && isWritable);

        try {
            String tooltip = isWritable ? model.getSelectedBundleTooltip() : TranslationModel.getTooltip("*", I18nUtil.getLocalPsiPropertiesFiles(model.getModule()).get(0), model.getModule());
            deleteOrCreateKeyButton.setToolTipText(tooltip);
            duplicateButton.setToolTipText(tooltip);
        } catch (Exception ignored) {
        }

        editActionToolBar.getComponent().setVisible(model.getSelectedPropertiesFile().getVirtualFile().isWritable());
    }

    /**
     * updateEditButton
     * @param language
     */
    private void updateEditButton(String language) {

        if (language!=null && language.equals(model.getSelectedLanguage()))
            return;

        if (language !=null)
            model.setSelectedLanguage(language);

        // Update Resource selection list
        List<PropertiesFile> propertiesFiles = model.getPropertiesFiles();
        List<PropertiesFile> current = new ArrayList<>();
        for (int i=0; i<resourcesGroup.getItemCount(); i++) {
            MyPropertiesFileInfo item = (MyPropertiesFileInfo) resourcesGroup.getItemAt(i);
            current.add(item.getPropertiesFile());
        }
        if (! Arrays.equals(propertiesFiles.toArray(), current.toArray())) {
            try {
                resourcesGroup.putClientProperty(BLOCK_REFRESH, true);
                resourcesGroup.removeAllItems();
                for (PropertiesFile pf : propertiesFiles) {
                    String label = model.getShortName(pf);
                    MyPropertiesFileInfo info = new MyPropertiesFileInfo(label, pf, IntelliJLaf.class.getName());
                    resourcesGroup.addItem(info);
                }
            }
            finally {
                resourcesGroup.putClientProperty(BLOCK_REFRESH, false);
            }
        }

        // select resource in group
        PropertiesFile selectedPropertiesFile = model.getSelectedPropertiesFile();
        if (selectedPropertiesFile!=null) {
            String bestPropetiesFilePath = selectedPropertiesFile.getVirtualFile().getPath();
            for (int i = 0; i < resourcesGroup.getItemCount(); i++) {
                MyPropertiesFileInfo item = (MyPropertiesFileInfo) resourcesGroup.getItemAt(i);
                String path = item.getPropertiesFile().getVirtualFile().getPath();
                if (bestPropetiesFilePath.equals(path)) {

                    if (resourcesGroup.getSelectedIndex() != i)
                        resourcesGroup.setSelectedIndex(i);
                    break;
                }
            }
        }

        resourcesGroup.setToolTipText(model.getSelectedPropertiesFileTooltip());
    }

    /**
     * setTranslationNoEvents
     */
    private void setTranslationNoEvents(JTextComponent textComponent, String translation) {

        if (textComponent.getText().equals(translation))
            return;

        try {
            this.putClientProperty(BLOCK_I18N, true);
            textComponent.setText(translation);
        }
        finally {
            this.putClientProperty(BLOCK_I18N, false);
        }
    }

    /**
     * openResourceBundleFile
     * @param index
     */
    private void openResourceBundleFile(int index) {

        PropertiesFile propertiesFile;
        if (index<0) {
            MyPropertiesFileInfo selectedItem = (MyPropertiesFileInfo) resourcesGroup.getSelectedItem();
            propertiesFile = selectedItem.getPropertiesFile();
        }
        else {
            String selectedLanguage = getLanguage(index);
            propertiesFile = I18nUtil.getPsiPropertiesFile(model.getSelectedPropertiesFile().getResourceBundle(), selectedLanguage);
        }

        if (propertiesFile == null)
            return;

        List<IProperty> properties = propertiesFile.findPropertiesByKey(key.getText());
        if (!properties.isEmpty()) {
            PsiElement nav = properties.get(0).getPsiElement().getNavigationElement();
            if (nav instanceof Navigatable) {
                ((Navigatable) nav).navigate(true);
                return;
            }
        }

        ((Navigatable)propertiesFile.getContainingFile().getNavigationElement()).navigate(true);
    }

    /**
     * refreshWhenDocumentUpdated
     */
    private void refreshWhenDocumentUpdated() {

        if (model == null)
            return;

        PsiDocumentManager.getInstance(project).performWhenAllCommitted(
                () -> loadTranslation(model.getSelectedKey(), null)
        );
    }

    /**
     * findI18NKey
     */
    private String findI18NKey(PsiReference target, Module module) {

        // multi references
        if (target instanceof PsiMultiReference) {

            PsiMultiReference mref = (PsiMultiReference)target;
            for (PsiReference ref : mref.getReferences()) {

                String key = findI18NKey(ref, module);
                if (key != null)
                    return key;
            }

            return null;
        }

        return target.getCanonicalText();
    }

    /**
     * findI18NKey
     */
    private String findI18NKey(PsiElement target, Module module) {

        if (target instanceof Property) {

            Property property = (Property)target;
            return property.getFirstChild().getText().trim();
        }

        String text = target.getText();
        if (!text.startsWith("'") && !text.startsWith("\""))
            return null;

        return target.getText();
    }

    /**
     * createEditActionGroup
     */
    protected ActionGroup createEditActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();

        // edit action
        this.editAction = new ToggleAction("Edit key") {

            @Override
            public boolean isSelected(AnActionEvent e) {
                return key.isEditable();
            }

            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                key.setEditable(state);
                if (state)
                    key.grabFocus();
                else if (key.hasFocus())
                    key.transferFocus();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                if  (deleteOrCreateKeyButton.getText().equals(CREATE_KEY)) {
                    e.getPresentation().setIcon(SJSIcons.FIND);
                    e.getPresentation().setText("Select another key or create a new key...", false);
                }
                else {
                    e.getPresentation().setIcon(SJSIcons.EDIT);
                    e.getPresentation().setText("Rename key...", false);
                }
            }
        };

        group.add(editAction);

        return group;
    }

    /**
     * createPreviousNextActionGroup
     */
    protected ActionGroup createPreviousNextActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();

        // left
        leftAction = new AnAction("Left") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                if (model.scrollLeft())
                    loadTranslation(model.getSelectedKey(), null);
            }
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setVisible(model.getKeyPath().indexOf('.')>0);
                e.getPresentation().setEnabled(model.getSelectedKey().length()<model.getKeyPath().length());
            }
        };
        leftAction.getTemplatePresentation().setIcon(SJSIcons.LEFT);
        leftAction.getTemplatePresentation().setText("Previous in dot notation", false);
        group.add(leftAction);

        // right
        rightAction = new AnAction("Right") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                if (model.scrollRight())
                    loadTranslation(model.getSelectedKey(), null);
            }
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setVisible(model.getKeyPath().indexOf('.')>0);
                e.getPresentation().setEnabled(model.getSelectedKey().indexOf('.')>0);
            }
        };
        rightAction.getTemplatePresentation().setIcon(SJSIcons.RIGHT);
        rightAction.getTemplatePresentation().setText("Next in dot notation", false);
        group.add(rightAction);


        return group;
    }

    /**
     * initTranslation
     */
    private void initTranslation(final Editor editor) {
        if (editor.isDisposed()) {
            LOG.trace("initTranslation : editor is disposed - END");
            return;
        }

        Project project = editor.getProject();
        if (project == null) {
            LOG.trace("initTranslation : no project found - END");
            return;
        }

        if (!project.equals(this.project))
            return;

        Boolean block = (Boolean) getClientProperty(BLOCK_I18N);
        if (Boolean.TRUE.equals(block)) {
            LOG.trace("initTranslation : blocked - END");
            return;
        }

        PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.getDocument(),
                () -> {

                    LOG.trace("initTranslation : search for reference...");

                    // Search for reference
                    Module module = null;
                    String key = null;
                    PsiReference referenceTarget = findReferenceTarget(editor);

                    LOG.trace("initTranslation : reference : " + referenceTarget);
                    if (referenceTarget != null) {

                        module = JavaUtil.getModule(referenceTarget.getElement());
                        String text = findI18NKey(referenceTarget, module);
                        LOG.trace("initTranslation : text : " + referenceTarget);
                        if (text != null && !text.contains(" "))
                            key = StringUtil.removeQuotes(text);

                    }

                    if (key == null) {

                        LOG.trace("initTranslation : key not found yet...");
                        // Search for element
                        PsiElement target = findTarget(editor);
                        LOG.trace("initTranslation : target : " + target);
                        if (target != null) {

                            module = JavaUtil.getModule(target);
                            String text = findI18NKey(target, module);
                            LOG.trace("initTranslation : text : " + text);
                            if (text != null && !text.contains(" "))
                                key = StringUtil.removeQuotes(text);
                        }
                    }

                    LOG.trace("initTranslation : key : " + key);
                    if (key != null)
                        initTranslationKey(key, false, FileUtil.getFile(editor), module);
                });
    }

    /**
     * findTarget
     */
    protected PsiReference findReferenceTarget(Editor editor) {

        int offset = editor.getCaretModel().getOffset();

        Project project = editor.getProject();
        if (project == null)
            return null;

        if (!project.equals(this.project))
            return null;

        Document document = editor.getDocument();

        PsiFile file = FileUtil.getFile(document, project);
        if (file == null)
            return null;

        PsiReference referenceAt = file.findReferenceAt(offset);
        if (referenceAt == null)
            return null;

        return referenceAt;
    }

    /**
     * findTarget
     */
    protected PsiElement findTarget(Editor editor) {
        int offset = editor.getCaretModel().getOffset();

        Project project = editor.getProject();
        if (project == null)
            return null;

        if (!project.equals(this.project))
            return null;

        Document document = editor.getDocument();

        PsiFile file = FileUtil.getFile(document, project);
        if (file == null)
            return null;

        PsiElement element = file.findElementAt(offset);
        if (element == null)
            return null;

        Property property = SPsiUtils.findSurrounding(element, Property.class);
        if (property !=null) {
            return property.getNavigationElement();
        }

        if (element instanceof TreeElement) {
            TreeElement token = (TreeElement)element;
            if (JavaTokenType.STRING_LITERAL.equals(token.getElementType())) {
                return element;
            }
        }

        return null;
    }

    /*****************************************************
     * initComponents
     */
    private void initComponents(List<String> langs) {

        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Maxime HAMM
        translationWindow = new JPanel();
        key = new JTextField() {
            @Override
            public void setEditable(boolean b) {
                super.setEditable(b);

                JComponent fake = b ? new JTextField() : new JLabel();
                setBorder(fake.getBorder());
                setBackground(fake.getBackground());
                setFont(fake.getFont().deriveFont(fake.getFont().getStyle() | Font.BOLD, fake.getFont().getSize() + 0f));
            }

            
        };
        key.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (key.isEditable()) {
                    key.setEditable(false);
                    model.renameKey(key.getText());
                    refreshWhenDocumentUpdated();
                }
            }
        });
        key.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                int k = evt.getKeyCode();
                if (k == KeyEvent.VK_ENTER)
                    key.transferFocus();
            }
        });

        // JFormDesigner evaluation mark
        translationWindow.setLayout(new GridLayoutManager(5, 1, JBUI.insetsBottom(10), -1, -1));

        {
            JPanel topPanel = new JPanel();
            topPanel.setLayout(new GridLayoutManager(1, 3, JBUI.insets(0,13, 0,7), -1, -1));

            //--- edit button ---
            JPanel p = new JPanel();
            p.add(editActionToolBar.getComponent());  // wrap the edit button to keep layout will hiding it

            topPanel.add(p, new GridConstraints(0, 0, 1, 1,
                    GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_FIXED,
                    new Dimension(35, 35),  new Dimension(35, 35), new Dimension(35, 35)));

            //---- key ----
            key.setEditable(false);
            topPanel.add(key, new GridConstraints(0, 1, 1, 1,
                    GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_GROW |  GridConstraints.SIZEPOLICY_CAN_SHRINK,
                    GridConstraints.SIZEPOLICY_FIXED,
                    new Dimension(30, 25), null, null, 0));

            //--- key next and previous buttons ---
            ActionGroup prevNextActionGroup = createPreviousNextActionGroup();
            ActionToolbar prevNextActionToolBar = ActionManager.getInstance().createActionToolbar("PN", prevNextActionGroup, true);
            prevNextActionToolBar.setMinimumButtonSize(new Dimension(14, 20));

            topPanel.add(prevNextActionToolBar.getComponent(), new GridConstraints(0, 2, 1, 1,
                    GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED,
                    null, new Dimension(45, 20), null));

            translationWindow.add(topPanel, new GridConstraints(0, 0, 1, 1,
                    GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_GROW |  GridConstraints.SIZEPOLICY_CAN_SHRINK, // | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    new Dimension(36, 30), null, null));
        }

        
        //---- flag ----
        flags = new LinkLabel[langs.size()];
        translations = new JTextComponent[langs.size()];
        languages = new LinkLabel[langs.size()];

        JPanel translationPanel = new JPanel();
        if (langs.size()>0) {

            translationPanel.setLayout(new GridLayoutManager(langs.size(), 3, JBUI.insets(10, 15, 10, 5), -1, -1));

            for (int i = 0; i < langs.size(); i++) {
                int finalI = i;

                flags[i] = LinkLabel.create(null, () -> openResourceBundleFile(finalI));
                flags[i].setAlignmentY(0.0F);
                flags[i].setMinimumSize(new Dimension(25, 20));
                flags[i].setToolTipText("Open properties file...");
                flags[i].setHoveringIcon(SJSIcons.MOVE_TO);

                translationPanel.add(flags[i], new GridConstraints(i, 0, 1, 1,
                        GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        null, null, null));

                //---- translation ----
                translations[i] = new JTextPane();
                translations[i].setPreferredSize(new Dimension(-1, 37));
                translations[i].setBorder(new EtchedBorder());
                translations[i].setAutoscrolls(true);
                translations[i].setFont(flags[i].getFont());
                JBScrollPane scroller = new JBScrollPane(translations[i]); // TIPS : Use scroller otherwise the textarea will not shrink !!
                translationPanel.add(scroller, new GridConstraints(i, 1, 1, 1,
                        GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        null, null, null));

                //---- lang ----
                languages[i] = LinkLabel.create(null, () -> googleTranslation(finalI));
                languages[i].setMinimumSize(new Dimension(30, 20));
                languages[i].setHorizontalTextPosition(SwingConstants.LEFT);
                translationPanel.add(languages[i], new GridConstraints(i, 2, 1, 1,
                        GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null, null, null));
                languages[i].setToolTipText("Google translate...");
                languages[i].setHoveringIcon(SJSIcons.GOOGLE_TRANSALTE);
            }
        }

        JScrollPane scroll = ScrollPaneFactory.createScrollPane(translationPanel, true);
        scroll.setPreferredSize(new Dimension(translationPanel.getPreferredSize().width + scroll.getVerticalScrollBar().getPreferredSize().width + 5, -1));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setMinimumSize(new Dimension(-1, 50));

        translationWindow.add(scroll, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW ,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null,
                null));

        //---- Bottom ----
        JPanel bottom = new JPanel();
        bottom.setLayout(new GridBagLayout());  // TIPS : Use java.awt.GridBagLayout because IntelliJ layout manger went to silly spaces...

        resourcesGroup.setMinimumAndPreferredWidth(120);
        bottom.add(resourcesGroup, new GridBagConstraints(0, 0, 1, 1,
                1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,  new Insets(0, 50, 0, 0), 0, 0));

        bottom.add(duplicateButton, new GridBagConstraints(1, 0, 1, 1,
                0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,  new Insets(0, 5, 0, 0), 0, 0));


        bottom.add(deleteOrCreateKeyButton, new GridBagConstraints(2, 0, 1, 1,
                0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,  new Insets(0, 3, 0, 43), 0, 0));

        translationWindow.add(bottom, new GridConstraints(2, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null));

    }

    private void googleTranslation(int index) {

        if (! this.translations[index].isEditable())
            return;

        Module module = model.getModule();
        String sourceLanguage = null;
        String sourceTranslation = null;

        // get target language
        String targetLanguage = getLanguage(index);

        // try to use best laguage
        String key = this.key.getText();
        String bestLanguage = I18nUtil.getBestLanguage(module);
        if (sourceLanguage != null) {

            String translation = I18nUtil.getTranslation(key, bestLanguage, module);
            if (translation != null) {

                sourceLanguage = bestLanguage;
                sourceTranslation = translation;
            }
        }

        // try using other languages
        if (sourceLanguage == null) {

            for (IProperty property : I18nUtil.getPsiProperties(key, null, module)) {

                String translation = I18nUtil.unescapeKeepCR(property.getValue());
                if (translation == null || translation.trim().isEmpty())
                    continue;

                String lang =  I18nUtil.getLanguage(property.getPropertiesFile());
                if (lang.equals(targetLanguage))
                    continue;

                sourceLanguage = lang;
                sourceTranslation = translation;

                break;
            }
        }

        // use the translation key if no best choice
        if (sourceLanguage == null) {

            sourceLanguage = Locale.ENGLISH.getLanguage();
            sourceTranslation = I18nUtil.prepareKeyForGoogleTranslation(key);
        }

        // well no more suggestions !
        Project project = module.getProject();
        if (sourceLanguage == null) {

            Messages.showErrorDialog(project, "No source found for translation !", NIMBLY);
            return;
        }

        // do translation
        String finalSourceLanguage = sourceLanguage;
        String finalSourceTranslation = sourceTranslation;

        ProgressManager.getInstance()
                .run(new Task.Backgroundable(project, "Google translate", true) {

                    private boolean canceled = false;

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {

                        String translation;
                        try {

                            translation = I18nUtil.googleTranslate(key, targetLanguage, finalSourceLanguage, finalSourceTranslation);

                        } catch (SocketTimeoutException | UnknownHostException e) {
                            if (canceled)
                                return;

                            LOG.warn("Communication error", e);
                            Messages.showErrorDialog(module.getProject(), "Communication error. Check your internet connection and proxy settings", NIMBLY);

                            return;

                        } catch (Exception e) {
                            if (canceled)
                                return;

                            LOG.error("Translation error", e);
                            Messages.showErrorDialog(module.getProject(), "Translation error. See logs for more informations", NIMBLY);

                            return;
                        }

                        if (canceled)
                            return;

                        if (translation == null) {
                            Messages.showInfoMessage(module.getProject(), "No translation found", NIMBLY);
                            return;
                        }

                        // Do update key
                        PropertiesFile targetFile = I18nUtil.getPsiPropertiesSiblingFile(model.getSelectedPropertiesFile(), targetLanguage, module);
                        if (targetFile!=null) {
                            ApplicationManager.getApplication().invokeLater(
                                    () -> I18nUtil.doUpdateTranslation(key, translation, targetFile, true));
                        }

                     }

                    @Override
                    public boolean shouldStartInBackground() {
                        return true;
                    }

                    @Override
                    public void onCancel() {
                        canceled = true;
                    }
                });
    }

    /*******************************************$
     *  MyTranslationPaneAdapter
     */
    private class MyTranslationPaneAdapter extends com.intellij.ui.DocumentAdapter implements FocusListener {

        private int index;

        public MyTranslationPaneAdapter(int index) {
            this.index = index;
        }

        @Override
        protected void textChanged(javax.swing.event.DocumentEvent e) {
            Boolean block = (Boolean) TranslationSnapView.this.getClientProperty(BLOCK_I18N);
            if (!Boolean.TRUE.equals(block)) {

                PsiDocumentManager.getInstance(model.getModule().getProject()).performLaterWhenAllCommitted(
                    () -> {
                        try {
                            SlowOperations.allowSlowOperations((ThrowableRunnable<Throwable>) () ->
                                    model.updateKey(getLanguage(index), translations[index].getText()));
                        } catch (Throwable ee) {
                            LOG.error("Translation init error", ee);
                        }

                    }
                );

            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            String language = getLanguage(index);
            if (language!=null)
                updateEditButton(language);
        }

        @Override
        public void focusLost(FocusEvent e) {
        }


    }


    /*******************************************$
     *  MyPropertiesFileAdapater
     */
    private class MyPropertiesFileAdapater extends DocumentAdapter {

        private PropertyKeyImpl key = null;

        @Override
        public void beforeDocumentChange(DocumentEvent e) {

            if (model == null)
                return;

            PropertyKeyImpl k = findPropertyKey(e);
            if (k!=null && k.getText().equals(model.getSelectedKey()))
                key = k;

        }

        @Override
        public void documentChanged(DocumentEvent e) {

            if (model == null)
                return;

            if (model.isViewRefreshBlocked())
                return;

            Module module = model.getModule();
            PsiDocumentManager.getInstance(
                    module.getProject()).performForCommittedDocument(e.getDocument(),
                    () -> {
                        refreshWhenDocumentUpdated();
                    });
        }


        public PropertyKeyImpl findPropertyKey(DocumentEvent event) {
            PsiFile file = FileUtil.getFile(event.getDocument(), model.getModule().getProject());
            PsiElement elementAt = file.findElementAt(event.getOffset());
            if (! (elementAt instanceof PropertyKeyImpl))
                elementAt =  file.findElementAt(event.getOffset()-1);

            if (elementAt instanceof PropertyKeyImpl) {
                return (PropertyKeyImpl) elementAt;
            }
            return null;
        }
    }

    /*******************************************$
     *  MyPropertiesFileInfo
     */
    private class MyPropertiesFileInfo extends UIManager.LookAndFeelInfo {

        private final PropertiesFile propertiesFile;

        public MyPropertiesFileInfo(String label, PropertiesFile propertiesFile, String className) {
            super(label, className);
            this.propertiesFile = propertiesFile;
        }

        public PropertiesFile getPropertiesFile() {
            return propertiesFile;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    
}
