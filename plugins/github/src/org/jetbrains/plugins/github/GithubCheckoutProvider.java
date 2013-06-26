/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.github;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConsumer;
import git4idea.actions.BasicAction;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.checkout.GitCloneDialog;
import git4idea.commands.Git;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author oleg
 */
public class GithubCheckoutProvider implements CheckoutProvider {

  private static Logger LOG = GithubUtil.LOG;

  @Override
  public void doCheckout(@NotNull final Project project, @Nullable final Listener listener) {
    if (!GithubUtil.testGitExecutable(project)) {
      return;
    }
    BasicAction.saveAll();

    final AtomicReference<List<RepositoryInfo>> repositoryInfoRef = new AtomicReference<List<RepositoryInfo>>();
    ProgressManager.getInstance().run(new Task.Modal(project, "Access to GitHub", true) {
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          GithubUtil.runAndGetValidAuth(project, indicator, new ThrowableConsumer<GithubAuthData, IOException>() {
            @Override
            public void consume(GithubAuthData authData) throws IOException {
              repositoryInfoRef.set(GithubUtil.getAvailableRepos(authData));
            }
            });
        }
        catch (IOException e) {
          LOG.info(e);
          GithubUtil.notifyError(project, "Couldn't get the list of GitHub repositories", GithubUtil.getErrorTextFromException(e));
        }
      }
    });
    final List<RepositoryInfo> availableRepos = repositoryInfoRef.get();
    if (availableRepos == null){
      return;
    }
    Collections.sort(availableRepos, new Comparator<RepositoryInfo>() {
      @Override
      public int compare(final RepositoryInfo r1, final RepositoryInfo r2) {
        final int comparedOwners = r1.getOwnerName().compareTo(r2.getOwnerName());
        return comparedOwners != 0 ? comparedOwners : r1.getName().compareTo(r2.getName());
      }
    });

    final GitCloneDialog dialog = new GitCloneDialog(project);
    // Add predefined repositories to history
    for (int i = availableRepos.size() - 1; i>=0; i--){
      dialog.prependToHistory(availableRepos.get(i).getCloneUrl());
    }
    dialog.show();
    if (!dialog.isOK()) {
      return;
    }
    dialog.rememberSettings();
    final VirtualFile destinationParent = LocalFileSystem.getInstance().findFileByIoFile(new File(dialog.getParentDirectory()));
    if (destinationParent == null) {
      return;
    }
    final String sourceRepositoryURL = dialog.getSourceRepositoryURL();
    final String directoryName = dialog.getDirectoryName();
    final String parentDirectory = dialog.getParentDirectory();

    Git git = ServiceManager.getService(Git.class);
    GitCheckoutProvider.clone(project, git, listener, destinationParent, sourceRepositoryURL, directoryName, parentDirectory);
  }

  @Override
  public String getVcsName() {
    return "Git_Hub";
  }
}
