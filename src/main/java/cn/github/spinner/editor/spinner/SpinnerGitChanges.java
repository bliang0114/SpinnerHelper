package cn.github.spinner.editor.spinner;

import cn.github.spinner.action.editor.SpinnerDeployAction;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.TreeSet;

final class SpinnerGitChanges {
    static boolean isGitFile(Project project, VirtualFile file) {
        var vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
        return vcs != null && "Git".equalsIgnoreCase(vcs.getName());
    }

    static String changedPayload(String before, String current) throws com.intellij.diff.comparison.DiffTooBigException {
        before = before.replace("\r\n", "\n").replace('\r', '\n');
        current = current.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = current.split("\n", -1);
        int end = lines.length - (current.endsWith("\n") ? 1 : 0);
        var rows = new TreeSet<Integer>();
        if (before.isEmpty() || !before.split("\n", 2)[0].equals(lines[0])) {
            for (int row = 1; row < end; row++) rows.add(row);
        } else {
            for (var change : ComparisonManager.getInstance().compareLines(before, current, ComparisonPolicy.DEFAULT, new EmptyProgressIndicator())) {
                for (int row = Math.max(1, change.getStartLine2()); row < Math.min(end, change.getEndLine2()); row++) rows.add(row);
            }
        }
        if (rows.isEmpty()) return null;
        var payload = new ArrayList<String>();
        payload.add(lines[0]);
        for (int row : rows) payload.add(lines[row]);
        return String.join("\n", payload);
    }

    static void deployChanges(AbstractSpinnerViewComponent view) {
        if (!view.isGitFile()) return;
        var connection = cn.github.spinner.context.UserInput.getInstance().connection.get(view.project);
        if (connection == null) {
            Messages.showWarningDialog(view.project, SpinnerBundle.message("message.connect.required"), SpinnerBundle.message("spinner.deploy.diff"));
            return;
        }
        var document = FileDocumentManager.getInstance().getDocument(view.virtualFile);
        if (document == null) return;
        String current = document.getText();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var change = ChangeListManager.getInstance(view.project).getChange(view.virtualFile);
                var revision = change == null ? null : change.getBeforeRevision();
                String before;
                if (revision != null) before = revision.getContent();
                else if (change != null && change.getType() == com.intellij.openapi.vcs.changes.Change.Type.NEW) before = "";
                else {
                    var vcs = ProjectLevelVcsManager.getInstance(view.project).getVcsFor(view.virtualFile);
                    if (vcs == null || vcs.getDiffProvider() == null) throw new IllegalStateException(SpinnerBundle.message("spinner.git.unavailable"));
                    var provider = vcs.getDiffProvider();
                    var base = provider.getCurrentRevision(view.virtualFile);
                    var content = base == null ? null : provider.createFileContent(base, view.virtualFile);
                    before = base == null ? "" : content == null ? null : content.getContent();
                }
                if (before == null) throw new IllegalStateException(SpinnerBundle.message("spinner.git.unavailable"));
                String payload = changedPayload(before, current);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (view.project.isDisposed() || !view.virtualFile.isValid()) return;
                    if (cn.github.spinner.context.UserInput.getInstance().connection.get(view.project) != connection) {
                        Messages.showWarningDialog(view.project, SpinnerBundle.message("spinner.deploy.environment.changed"), SpinnerBundle.message("spinner.deploy.diff"));
                    } else if (!view.isGitFile() || !document.getText().equals(current)) {
                        Messages.showWarningDialog(view.project, SpinnerBundle.message("spinner.paste.stale"), SpinnerBundle.message("spinner.deploy.diff"));
                    } else if (payload == null) {
                        Messages.showInfoMessage(view.project, SpinnerBundle.message("spinner.deploy.diff.empty"), SpinnerBundle.message("spinner.deploy.diff"));
                    } else SpinnerDeployAction.deploySpinnerContent(view.project, view.virtualFile.getPath(), payload);
                });
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!view.project.isDisposed()) Messages.showErrorDialog(view.project, ex.getMessage(), SpinnerBundle.message("spinner.deploy.diff"));
                });
            }
        });
    }
}
