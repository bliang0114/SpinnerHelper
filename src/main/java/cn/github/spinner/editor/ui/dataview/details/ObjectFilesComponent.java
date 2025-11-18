package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.util.MQLUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.collection.CollUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ObjectFilesComponent extends AbstractObjectDetailsTableComponent {
    private static final Logger logger = Logger.getInstance(ObjectFilesComponent.class);
    private volatile boolean isLoading = false;

    public ObjectFilesComponent(Project project, String id) {
        super(project, id);
    }

    @Override
    protected String[] headers() {
        return new String[]{"ObjectId", "Name", "Format", "Size"};
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{300, 280, 280, 300};
    }

    @Override
    protected Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class};
    }

    @Override
    protected String componentId() {
        return ObjectFilesComponent.class.getSimpleName();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getEmptyText().setText("Loading bus files...");
        table.setEnabled(true);
        table.setFocusable(true);
        table.setRowSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
    }

    @Override
    protected void setupListener() {
        super.setupListener();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isLoading) {
                    UIUtil.showNotification(project,"提示", "文件正在下载中，请稍候...");
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = table.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0 || modelRowIndex >= tableModel.getRowCount()) return;
                        String documentId = String.valueOf(tableModel.getValueAt(modelRowIndex, 0));
                        String fileName = String.valueOf(tableModel.getValueAt(modelRowIndex, 1));
                        String format = String.valueOf(tableModel.getValueAt(modelRowIndex, 2));
                        new Task.Backgroundable(project, "Downloading File: " + fileName, false) {
                            private File downloadedFile;
                            private String errorMsg;

                            @Override
                            public void onSuccess() {
                                isLoading = false;
                                table.setEnabled(true);
                                table.setCursor(Cursor.getDefaultCursor());
                                if (errorMsg != null) {
                                    return;
                                }
                                if (downloadedFile != null && downloadedFile.exists()) {
                                    try {
                                        Desktop.getDesktop().open(downloadedFile);
                                        UIUtil.showNotification(project, "提示","文件已保存至：" + downloadedFile.getAbsolutePath());
                                    } catch (Exception ex) {
                                        String msg = "文件下载成功，但打开失败：" + ex.getMessage();
                                        logger.error(msg, ex);
                                    }
                                }
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable t) {
                                isLoading = false;
                                table.setEnabled(true);
                                table.setCursor(Cursor.getDefaultCursor());
                                errorMsg = "下载异常：" + t.getMessage();
                                logger.error(errorMsg, t);
                            }

                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                try {
                                    isLoading = true;
                                    SwingUtilities.invokeLater(() -> {
                                        table.setEnabled(false);
                                        table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                    });
                                    indicator.setText("正在下载：" + fileName);
                                    indicator.setIndeterminate(true);
                                    MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
                                    if (connection == null) {
                                        throw new IllegalStateException("未获取到有效连接，请检查配置");
                                    }
                                    downloadedFile = connection.downloadBusinessAttachment(
                                            documentId, fileName, format
                                    );

                                } catch (Exception e) {
                                    errorMsg = e.getMessage();
                                    throw new RuntimeException(errorMsg, e);
                                } finally {
                                    indicator.setIndeterminate(false);
                                }
                            }
                        }.queue();
                    }
                }
            }
        });
    }

    @Override
    protected void loadData() {
        if (isLoading) return;

        new Task.Backgroundable(project, "Loading bus files", false) {
            private List<String[]> loadedData;
            private String errorMessage;

            @Override
            public void onSuccess() {
                isLoading = false;
                tableModel.setRowCount(0);

                if (errorMessage != null) {
                    table.getEmptyText().setText(errorMessage);
                    return;
                }
                if (CollUtil.isNotEmpty(loadedData)) {
                    SwingUtilities.invokeLater(() -> {
                        for (String[] row : loadedData) {
                            tableModel.addRow(row);
                        }
                        table.getEmptyText().setText("No bus connections found");
                    });
                } else {
                    table.getEmptyText().setText("No bus connections found");
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                isLoading = false;
                errorMessage = "Error loading bus connections: " + t.getMessage();
                SwingUtilities.invokeLater(() -> table.getEmptyText().setText(errorMessage));
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                isLoading = true;
                loadedData = new ArrayList<>();
                errorMessage = null;

                try {
                    indicator.setText("Fetching bus connections...");
                    indicator.setIndeterminate(true);
                    getBusFiles(id, loadedData);
                } catch (MQLException e) {
                    errorMessage = "Error: print " + id + " error. " + e.getMessage();
                } catch (Exception e) {
                    errorMessage = "Unexpected error: " + e.getMessage();
                } finally {
                    indicator.setIndeterminate(false);
                }
            }
        }.queue();
    }

    private  void getBusFiles(String id, List<String[]> tableData) throws MQLException {
        final long KB = 1024, MB = KB * 1024, GB = MB * 1024, TB = GB * 1024;
        var result = MQLUtil.execute(project, "print bus " + id + " select format.file.format format.file.name format.file.size dump");
        if (result.equals(",,")) {
            return;
        }
        var a = result.split(",");
        var n = a.length / 3;
        for (var i = 0; i < n; i++) {
            String format = "Error file format";
            String name = "Error file name";
            String size;
            String sSize = "Error file size 2";
            try {
                format = a[i];
                name = a[n + i];
                size = a[2 * n + i];
                double dSize = Double.parseDouble(size);
                if (dSize > TB) {
                    sSize = String.format("%.1f TB", dSize / TB);
                } else if (dSize > GB) {
                    sSize = String.format("%.1f GB", dSize / GB);
                } else if (dSize > MB) {
                    sSize = String.format("%.1f MB", dSize / MB);
                } else if (dSize > KB) {
                    sSize = String.format("%.1f KB", dSize / KB);
                } else {
                    sSize = String.format("%s B", size);
                }
            } catch (Exception e) {
                logger.error("Error in determination of the file: ", e);
            } finally {
                tableData.add(new String[]{id,name,format,sSize});
            }
        }
    }


}