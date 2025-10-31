package com.bol.spinner.editor.spinner;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class SpinnerCommandComponent extends AbstractSpinnerViewComponent {

    public SpinnerCommandComponent(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        super(project, virtualFile);
    }
}
