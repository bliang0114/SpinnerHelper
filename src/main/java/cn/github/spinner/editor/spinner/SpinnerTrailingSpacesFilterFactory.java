package cn.github.spinner.editor.spinner;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.StripTrailingSpacesFilter;
import com.intellij.openapi.editor.StripTrailingSpacesFilterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** In textual Spinner .xls files, trailing tabs are empty columns, not whitespace to clean up. */
public final class SpinnerTrailingSpacesFilterFactory extends StripTrailingSpacesFilterFactory {
    @Override
    public @NotNull StripTrailingSpacesFilter createFilter(@Nullable Project project, @NotNull Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file != null && "xls".equals(file.getExtension())
                ? StripTrailingSpacesFilter.NOT_ALLOWED : StripTrailingSpacesFilter.ALL_LINES;
    }
}
