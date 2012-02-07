/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package git4idea.branch;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.*;
import git4idea.repo.GitRepository;
import git4idea.util.GitPreservingProcess;
import git4idea.util.GitUIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static git4idea.commands.GitMessageWithFilesDetector.Event.LOCAL_CHANGES_OVERWRITTEN_BY_CHECKOUT;
import static git4idea.commands.GitMessageWithFilesDetector.Event.UNTRACKED_FILES_OVERWRITTEN_BY;
import static git4idea.util.GitUIUtil.code;

/**
 * Represents {@code git checkout} operation.
 * Fails to checkout if there are unmerged files.
 * Fails to checkout if there are untracked files that would be overwritten by checkout. Shows the list of files.
 * If there are local changes that would be overwritten by checkout, proposes to perform a "smart checkout" which means stashing local
 * changes, checking out, and then unstashing the changes back (possibly with showing the conflict resolving dialog). 
 *
 *  @author Kirill Likhodedov
 */
class GitCheckoutOperation extends GitBranchOperation {

  private static final Logger LOG = Logger.getInstance(GitCheckoutOperation.class);

  @NotNull private final String myStartPointReference;
  @Nullable private final String myNewBranch;
  @NotNull private final String myPreviousBranch;

  GitCheckoutOperation(@NotNull Project project, @NotNull Collection<GitRepository> repositories,
                              @NotNull String startPointReference, @Nullable String newBranch, @NotNull String previousBranch, 
                              @NotNull ProgressIndicator indicator) {
    super(project, repositories, indicator);
    myStartPointReference = startPointReference;
    myNewBranch = newBranch;
    myPreviousBranch = previousBranch;
  }
  
  @Override
  protected void execute() {
    boolean fatalErrorHappened = false;
    while (hasMoreRepositories() && !fatalErrorHappened) {
      final GitRepository repository = next();

      VirtualFile root = repository.getRoot();
      GitMessageWithFilesDetector localChangesOverwrittenByCheckout = new GitMessageWithFilesDetector(LOCAL_CHANGES_OVERWRITTEN_BY_CHECKOUT, root);
      GitSimpleEventDetector unmergedFiles = new GitSimpleEventDetector(GitSimpleEventDetector.Event.UNMERGED);
      GitMessageWithFilesDetector untrackedOverwrittenByCheckout = new GitMessageWithFilesDetector(UNTRACKED_FILES_OVERWRITTEN_BY, root);

      GitCommandResult result = Git.checkout(repository, myStartPointReference, myNewBranch, false,
                                             localChangesOverwrittenByCheckout, unmergedFiles, untrackedOverwrittenByCheckout);
      if (result.success()) {
        refresh(repository);
        markSuccessful(repository);
      }
      else if (unmergedFiles.hasHappened()) {
        fatalUnmergedFilesError();
        fatalErrorHappened = true;
      }
      else if (localChangesOverwrittenByCheckout.wasMessageDetected()) {
        boolean smartCheckoutSucceeded = smartCheckoutOrNotify(repository, localChangesOverwrittenByCheckout);
        if (!smartCheckoutSucceeded) {
          fatalErrorHappened = true;
        }
      }
      else if (untrackedOverwrittenByCheckout.wasMessageDetected()) {
        fatalUntrackedFilesError(untrackedOverwrittenByCheckout.getFiles());
        fatalErrorHappened = true;
      }
      else {
        fatalError(getCommonErrorTitle(), result.getErrorOutputAsJoinedString());
        fatalErrorHappened = true;
      }
    }

    if (!fatalErrorHappened) {
      notifySuccess();
    }
  }

  private boolean smartCheckoutOrNotify(@NotNull GitRepository repository, 
                                        @NotNull GitMessageWithFilesDetector localChangesOverwrittenByCheckout) {
    // get changes overwritten by checkout from the error message captured from Git
    List<Change> affectedChanges = GitUtil.convertPathsToChanges(repository, localChangesOverwrittenByCheckout.getRelativeFilePaths(), true);
    // get all other conflicting changes
    // get changes in all other repositories (except those which already have succeeded) to avoid multiple dialogs proposing smart checkout
    Map<GitRepository, List<Change>> conflictingChangesInRepositories =
      collectLocalChangesConflictingWithBranch(myProject, getRemainingRepositoriesExceptGiven(repository), myPreviousBranch, myStartPointReference);

    Set<GitRepository> otherProblematicRepositories = conflictingChangesInRepositories.keySet();
    List<GitRepository> allConflictingRepositories = new ArrayList<GitRepository>(otherProblematicRepositories);
    allConflictingRepositories.add(repository);
    for (List<Change> changes : conflictingChangesInRepositories.values()) {
      affectedChanges.addAll(changes);
    }

    int smartCheckoutDecision = GitWouldBeOverwrittenByCheckoutDialog.showAndGetAnswer(myProject, affectedChanges);
    if (smartCheckoutDecision == GitWouldBeOverwrittenByCheckoutDialog.SMART_CHECKOUT) {
      boolean smartCheckedOutSuccessfully = smartCheckout(allConflictingRepositories, myStartPointReference, myNewBranch, getIndicator());
      if (smartCheckedOutSuccessfully) {
        GitRepository[] otherRepositories = ArrayUtil.toObjectArray(otherProblematicRepositories, GitRepository.class);

        markSuccessful(repository);
        markSuccessful(otherRepositories);
        refresh(repository);
        refresh(otherRepositories);
        return true;
      }
      else {
        // notification is handled in smartCheckout()
        return false;
      }
    }
    else if (smartCheckoutDecision == GitWouldBeOverwrittenByCheckoutDialog.FORCE_CHECKOUT_EXIT_CODE) {
      return checkoutOrNotify(allConflictingRepositories, myStartPointReference, myNewBranch, true);
    }
    else {
      fatalLocalChangesError();
      return false;
    }
  }

  private void fatalLocalChangesError() {
    String title = "Couldn't checkout " + myStartPointReference;
    String message = "Local changes would be overwritten by checkout.<br/>Stash or commit them before checking out a branch.<br/>";
    if (wereSuccessful()) {
      showFatalErrorDialogWithRollback(title, message);
    }
    else {
      showFatalNotification(title, message);
    }
  }

  @NotNull
  @Override
  protected String getRollbackProposal() {
    return "However checkout has succeeded for the following " + repositories() + ":<br/>" +
           successfulRepositoriesJoined() +
           "<br/>You may rollback (checkout back to " + myPreviousBranch + ") not to let branches diverge.";
  }

  @NotNull
  @Override
  protected String getOperationName() {
    return "checkout";
  }

  @Override
  protected void rollback() {
    GitCompoundResult checkoutResult = new GitCompoundResult(myProject);
    GitCompoundResult deleteResult = new GitCompoundResult(myProject);
    for (GitRepository repository : getSuccessfulRepositories()) {
      GitCommandResult result = Git.checkout(repository, myPreviousBranch, null, true);
      checkoutResult.append(repository, result);
      if (result.success() && myNewBranch != null) {
        /*
          force delete is needed, because we create new branch from branch other that the current one
          e.g. being on master create newBranch from feature,
          then rollback => newBranch is not fully merged to master (although it is obviously fully merged to feature).
         */
        deleteResult.append(repository, Git.branchDelete(repository, myNewBranch, true));
      }
      refresh(repository);
    }
    if (!checkoutResult.totalSuccess() || !deleteResult.totalSuccess()) {
      StringBuilder message = new StringBuilder();
      if (!checkoutResult.totalSuccess()) {
        message.append("Errors during checking out ").append(myPreviousBranch).append(": ");
        message.append(checkoutResult.getErrorOutputWithReposIndication());
      }
      if (!deleteResult.totalSuccess()) {
        message.append("Errors during deleting ").append(code(myNewBranch)).append(": ");
        message.append(deleteResult.getErrorOutputWithReposIndication());
      }
      GitUIUtil.notify(GitVcs.IMPORTANT_ERROR_NOTIFICATION, myProject, "Error during rollback",
                       message.toString(), NotificationType.ERROR, null);
    }
  }

  @NotNull
  private String getCommonErrorTitle() {
    return "Couldn't checkout " + myStartPointReference;
  }

  @NotNull
  @Override
  public String getSuccessMessage() {
    if (myNewBranch == null) {
      return String.format("Checked out <b><code>%s</code></b>", myStartPointReference);
    }
    return String.format("Checked out new branch <b><code>%s</code></b> from <b><code>%s</code></b>", myNewBranch, myStartPointReference);
  }

  // stash - checkout - unstash
  private boolean smartCheckout(@NotNull final List<GitRepository> repositories, @NotNull final String reference, @Nullable final String newBranch, @NotNull ProgressIndicator indicator) {

    final AtomicBoolean result = new AtomicBoolean();
    GitPreservingProcess preservingProcess = new GitPreservingProcess(myProject, repositories, "checkout", reference, indicator, new Runnable() {
      @Override
      public void run() {
        result.set(checkoutOrNotify(repositories, reference, newBranch, false));
      }
    });
    preservingProcess.execute();
    return result.get();
  }

  /**
   * Checks out or shows an error message.
   */
  private boolean checkoutOrNotify(@NotNull List<GitRepository> repositories, 
                                   @NotNull String reference, @Nullable String newBranch, boolean force) {
    GitCompoundResult compoundResult = new GitCompoundResult(myProject);
    for (GitRepository repository : repositories) {
      compoundResult.append(repository, Git.checkout(repository, reference, newBranch, force));
    }
    if (compoundResult.totalSuccess()) {
      return true;
    }
    notifyError("Couldn't checkout " + reference, compoundResult.getErrorOutputWithReposIndication());
    return false;
  }

  private static void refresh(GitRepository... repositories) {
    for (GitRepository repository : repositories) {
      refreshRoot(repository);
      // repository state will be auto-updated with this VFS refresh => no need to call GitRepository#update().
    }
  }
}
