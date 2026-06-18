package cn.github.spinner.task;

import cn.github.spinner.context.UserInput;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TrackedBackgroundTask extends Task.Backgroundable {
    protected TrackedBackgroundTask(@Nullable Project project, @NotNull String title) {
        super(project, title);
    }

    protected TrackedBackgroundTask(@Nullable Project project, @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    @Override
    public final void run(@NotNull ProgressIndicator indicator) {
        Project project = myProject;
        if (project != null) {
            UserInput.getInstance().backgroundTaskStarted(project);
        }
        try {
            runTracked(indicator);
        } finally {
            if (project != null) {
                UserInput.getInstance().backgroundTaskFinished(project);
            }
        }
    }

    protected abstract void runTracked(@NotNull ProgressIndicator indicator);
}
