package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.editor.ui.dataview.bean.ProgramsRow;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.MQLUtil;
import cn.github.spinner.util.TriggerQueryUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class ProgramTableComponent extends AbstractDataViewTableComponent<ProgramsRow> implements Disposable {
    private static final String TOOLBAR_ID = "ProgramView Table";

    public ProgramTableComponent(@NotNull Project project, VirtualFile file) {
        super(project, file, new ProgramsRow(), TOOLBAR_ID);
        setupBusinessListener();
        setName(TOOLBAR_ID);
        reloadData();
    }

    private void setupBusinessListener() {
        // 双击行预览 JPO/MQL 代码（与项目内其它表格一致的交互约定）
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2) return;
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                // 过滤/排序后 view 行需先转回 model 行
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (modelRow < 0) return;
                // 第 0 列为 PaginatedTableModel 插入的行号列，程序名在模型第 1 列
                Object value = tableModel.getValueAt(modelRow, 1);
                if (value == null) return;
                String programName = value.toString();
                if (CharSequenceUtil.isBlank(programName)) return;
                if (programName.startsWith("Failed") || programName.startsWith("No ")) return;
                if (programName.equals(SpinnerBundle.message("message.no.programs.found"))) return;
                openProgramInNativeEditor(programName);
            }
        });
    }

    @Override
    protected List<ProgramsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        List<ProgramsRow> programDataList = new ArrayList<>();
        try {
            String allProgram = MQLUtil.execute(project, "list prog select name Originated Modified dump");
            if (CharSequenceUtil.isBlank(allProgram)) {
                programDataList.add(new ProgramsRow(SpinnerBundle.message("message.no.programs.found"), "", ""));
                return programDataList;
            }

            String[] rawRows = allProgram.split("\n");
            for (String rawRow : rawRows) {
                String trimmedRow = rawRow.trim();
                if (CharSequenceUtil.isBlank(trimmedRow)) continue;

                String[] rowColumns = trimmedRow.split(",");
                ProgramsRow item = new ProgramsRow(rowColumns.length > 0 ? rowColumns[0] : "", rowColumns.length > 1 ? rowColumns[1] : "", rowColumns.length > 2 ? rowColumns[2] : "");
                programDataList.add(item);
            }
            programDataList.sort(Comparator.comparing(ProgramsRow::getName));

        } catch (MQLException e) {
            String errorMsg = SpinnerBundle.message("message.programs.load.failed", e.getMessage());
            programDataList.add(new ProgramsRow());
            Messages.showWarningDialog(project, errorMsg, SpinnerBundle.message("dialog.program.list.load.error.title"));
            throw e;
        }
        return programDataList;
    }

    private void openProgramInNativeEditor(String programName) {
        String programType;
        try {
            programType = getProgType(programName);
        } catch (MQLException e) {
            Messages.showErrorDialog(project,
                    SpinnerBundle.message("message.open.failed", e.getMessage()),
                    SpinnerBundle.message("notification.title.error"));
            return;
        }

        if ("MQL".equals(programType)) {
            openRemoteProgramCode(programName, programName, true);
            return;
        }
        if (!"JAVA".equals(programType)) {
            // 非 MQL、非 JPO 的程序：直接展示服务端拉取的源码（纯文本）
            openRemoteProgramCode(programName, programName + ".txt", false);
            return;
        }

        // JPO（Java 程序）：参照 TriggerQueryDialog 的导航逻辑
        // 优先从服务端拉取源码，找不到再回退到工程内索引的 class / 编译产物
        String className = programName + "_mxJPO";
        new TrackedBackgroundTask(project, SpinnerBundle.message("progress.load.trigger.source"), true) {
            private TriggerQueryUtil.ClassLookupResult classLookupResult;
            private String sourceCode = "";
            private boolean remoteSourceLoaded;
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    sourceCode = TriggerQueryUtil.queryProgramCode(project, className);
                    if (CharSequenceUtil.isNotBlank(sourceCode)) {
                        remoteSourceLoaded = true;
                        return;
                    }
                } catch (Throwable throwable) {
                    error = throwable;
                }
                try {
                    classLookupResult = TriggerQueryUtil.findClassTarget(project, className, "");
                } catch (Throwable throwable) {
                    if (error == null) {
                        error = throwable;
                    }
                }
            }

            @Override
            public void onSuccess() {
                if (remoteSourceLoaded) {
                    if (sourceCode.contains("the program is empty")) {
                        Messages.showInfoMessage(project,
                                SpinnerBundle.message("message.program.empty.content"),
                                SpinnerBundle.message("message.empty.program"));
                        return;
                    }
                    LightVirtualFile virtualFile = new LightVirtualFile(programName + ".java");
                    virtualFile.setFileType(JavaFileType.INSTANCE);
                    virtualFile.setContent(this, sourceCode, false);
                    virtualFile.setWritable(false);
                    new OpenFileDescriptor(project, virtualFile, 0, 0).navigate(true);
                    return;
                }
                if (navigateClassTarget(classLookupResult)) {
                    return;
                }
                if (error != null) {
                    UIUtil.showWarningNotification(project,
                            SpinnerBundle.message("notification.title.trigger.query"),
                            SpinnerBundle.message("message.trigger.source.load.failed", programName, error.getMessage()));
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                UIUtil.showWarningNotification(project,
                        SpinnerBundle.message("notification.title.trigger.query"),
                        SpinnerBundle.message("message.trigger.source.load.failed", programName, t.getMessage()));
            }
        }.queue();
    }

    private void openRemoteProgramCode(@NotNull String programName, @NotNull String fileName, boolean mql) {
        try {
            String programCode = MQLUtil.execute(project, "list prog {} select code dump", programName);
            if (programCode.contains("the program is empty")) {
                Messages.showInfoMessage(project,
                        SpinnerBundle.message("message.program.empty.content"),
                        SpinnerBundle.message("message.empty.program"));
                return;
            }
            LightVirtualFile virtualFile = new LightVirtualFile(fileName);
            if (mql) {
                virtualFile.setLanguage(MQLLanguage.INSTANCE);
                virtualFile.setContent(programCode, programCode, true);
                virtualFile.setWritable(true);
            } else {
                virtualFile.setContent(programCode, programCode, false);
                virtualFile.setWritable(false);
            }
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        } catch (MQLException e) {
            Messages.showErrorDialog(project,
                    SpinnerBundle.message("message.open.failed", e.getMessage()),
                    SpinnerBundle.message("notification.title.error"));
        }
    }

    private boolean navigateClassTarget(@Nullable TriggerQueryUtil.ClassLookupResult classLookupResult) {
        if (classLookupResult == null || classLookupResult.isEmpty()) {
            return false;
        }
        SmartPsiElementPointer<PsiElement> elementPointer = classLookupResult.elementPointer();
        if (elementPointer != null) {
            PsiElement element = elementPointer.getElement();
            if (element instanceof Navigatable navigatable && element.isValid()) {
                navigatable.navigate(true);
                return true;
            }
        }
        if (classLookupResult.classPath().isBlank()) {
            return false;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance()
                .refreshAndFindFileByNioFile(Path.of(classLookupResult.classPath()));
        if (virtualFile == null) {
            return false;
        }
        new OpenFileDescriptor(project, virtualFile).navigate(true);
        return true;
    }


    private String getProgType(String programName) throws MQLException {
        String[] typeArray = MQLUtil.execute(project, "list prog {} select ismqlprogram isjavaprogram dump", programName).split(",");
        if (typeArray.length == 2) {
            if (typeArray[0].trim().equalsIgnoreCase("TRUE")) return "MQL";
            if (typeArray[1].trim().equalsIgnoreCase("TRUE")) return "JAVA";
        }
        return "Other";
    }


    @Override
    public void dispose() {
        // 已改为直接导航到工程源码 / 编译 class，不再写临时文件，无需清理
        if (super.executor != null && !super.executor.isShutdown()) {
            super.executor.shutdownNow();
        }
    }
}
