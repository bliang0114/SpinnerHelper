package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.editor.ui.dataview.bean.ProgramsRow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public class ProgramTableComponent extends AbstractDataViewTableComponent<ProgramsRow, ProgramTableComponent> implements Disposable {
    private final List<File> tempJavaFiles = new ArrayList<>();
    private static final Object[] TABLE_COLUMNS = {"名称", "创建时间", "修改时间"};
    private static final int[] TABLE_COLUMN_WIDTHS = {300, 180, 180};
    private static final String TOOLBAR_ID = "ProgramView.Toolbar";

    public ProgramTableComponent(@NotNull Project project, VirtualFile file) {
        super(project, file, TABLE_COLUMNS, TABLE_COLUMN_WIDTHS, TOOLBAR_ID);
        setupBusinessListener();
        setName(TOOLBAR_ID);
        reloadData();
    }

    private void setupBusinessListener() {
        super.table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = ProgramTableComponent.super.table.getSelectedRow();
            String programName = ProgramTableComponent.super.tableModel.getValueAt(selectedRow, 0).toString();
            if (e.getValueIsAdjusting()) return;
            if (selectedRow == -1) return;
            if (programName.startsWith("Failed") || programName.startsWith("No ")) return;
            openProgramInNativeEditor(programName);
        });
    }

    @Override
    protected void addRow(ProgramsRow rowData) {
        tableModel.addRow(new Object[]{rowData.getName(), rowData.getCreateTime(),rowData.getUpdateTime()});
    }

    @Override
    protected List<ProgramsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        List<ProgramsRow> programDataList = new ArrayList<>();
        try {
            String allProgram = MQLUtil.execute(project, "list prog select name Originated Modified dump");
            if (CharSequenceUtil.isBlank(allProgram)) {
                programDataList.add(new ProgramsRow("No programs found", "", ""));
                return programDataList;
            }

            String[] rawRows = allProgram.split("\n");
            for (String rawRow : rawRows) {
                String trimmedRow = rawRow.trim();
                if (CharSequenceUtil.isBlank(trimmedRow)) continue;

                String[] rowColumns = trimmedRow.split(",");
                ProgramsRow item = new ProgramsRow( rowColumns.length > 0 ? rowColumns[0] : "",rowColumns.length > 1 ? rowColumns[1] : "",rowColumns.length > 2 ? rowColumns[2] : "");
                programDataList.add(item);
            }
            programDataList.sort(Comparator.comparing(ProgramsRow::getName));

        } catch (MQLException e) {
            String errorMsg = "Failed to load programs: " + e.getMessage();
            programDataList.add(new ProgramsRow());
            Messages.showWarningDialog(project, errorMsg, "Program List Load Error");
            throw e;
        }
        return programDataList;
    }

    private void openProgramInNativeEditor(String programName) {
        try {
            String programCode = generateJavaCode(programName);
            String programType = getProgType(programName);
            if (programCode.contains("the program is empty")) {
                Messages.showInfoMessage(project, programCode, "Empty Program");
                return;
            }
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            if ("MQL".equals(programType)) {
                LightVirtualFile mqlVirtualFile = new LightVirtualFile(programName);
                mqlVirtualFile.setLanguage(MQLLanguage.INSTANCE); // 语言关联（业务配置，非UI）
                mqlVirtualFile.setContent(programCode, programCode, true);
                mqlVirtualFile.setWritable(true);

                FileEditorManager.getInstance(project);
                fileEditorManager.openFile(mqlVirtualFile, true);
            } else {
                String fileExt = "JAVA".equals(programType) ? ".java" : ".txt";
                String safeFileName = programName.replaceAll("[^a-zA-Z0-9_.-]", "_") + fileExt;
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "SpinnerPrograms");
                if (!tempDir.exists()) tempDir.mkdirs();

                File tempFile = new File(tempDir, safeFileName);
                tempJavaFiles.add(tempFile);

                try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
                    writer.write(programCode);
                }

                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(tempFile);
                if (virtualFile == null) throw new IOException("Temporary file not found");

                fileEditorManager.openFile(virtualFile, true);
                for (FileEditor editor : fileEditorManager.getAllEditors(virtualFile)) {
                    if (editor instanceof TextEditor) {
                        Editor textEditor = ((TextEditor) editor).getEditor();
                        textEditor.getDocument().setReadOnly(true); // 业务规则：只读
                        break;
                    }
                }
            }
        } catch (IOException | MQLException e) {
            Messages.showErrorDialog(project, "Open failed: " + e.getMessage(), "Error");
        }
    }


    private String generateJavaCode(String programName) throws MQLException {
        String content = MQLUtil.execute(project, "list prog {} select code dump", programName);
        return StringUtils.isEmpty(content.trim()) ? "the program is empty!" : content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
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
        tempJavaFiles.forEach(file -> {
            if (file.exists()) file.delete();
        });
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "SpinnerPrograms");
        if (tempDir.exists() && Objects.requireNonNull(tempDir.listFiles()).length == 0) {
            tempDir.delete();
        }
        tempJavaFiles.clear();
        if (super.executor != null && !super.executor.isShutdown()) {
            super.executor.shutdownNow();
        }
    }
}