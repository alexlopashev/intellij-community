/**
 * @author Alexey
 */
package com.intellij.lang.properties.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.editor.ResourceBundleAsVirtualFile;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
>>>> ORIGINAL ResourceBundleNode.java#1
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.editor.ResourceBundleAsVirtualFile;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.PropertiesFileType;
==== THEIRS ResourceBundleNode.java#2
==== YOURS ResourceBundleNode.java
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.editor.ResourceBundleAsVirtualFile;
import com.intellij.lang.properties.ResourceBundle;
<<<<
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ResourceBundleNode extends ProjectViewNode<ResourceBundle>{
  public ResourceBundleNode(Project project, ResourceBundle resourceBundle, final ViewSettings settings) {
    super(project, resourceBundle, settings);
  }

  public ResourceBundleNode(Project project, Object value, final ViewSettings viewSettings) {
    this(project, (ResourceBundle)value, viewSettings);
  }

  public Collection<AbstractTreeNode> getChildren() {
    List<PropertiesFile> propertiesFiles = getValue().getPropertiesFiles();
    List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    for (PropertiesFile propertiesFile : propertiesFiles) {
      AbstractTreeNode node = new PsiFileNode(myProject, propertiesFile, getSettings());
      children.add(node);
    }
    return children;
  }

  public boolean contains(VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    if (!(psiFile instanceof PropertiesFile)) return false;
    PropertiesFile propertiesFile = (PropertiesFile)psiFile;
    return getValue().getPropertiesFiles().contains(propertiesFile);
  }

  public void update(PresentationData presentation) {
    presentation.setIcons(ResourceBundle.ICON);
    presentation.setPresentableText("Resource Bundle '"+getValue().getBaseName()+"'");
  }

  public boolean canNavigateToSource() {
    return true;
  }

  public void navigate(final boolean requestFocus) {
    OpenFileDescriptor descriptor = new OpenFileDescriptor(getProject(), new ResourceBundleAsVirtualFile(getValue()));
    FileEditorManager.getInstance(getProject()).openTextEditor(descriptor, requestFocus);
  }
}