package cn.github.spinner.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MQLFileSaveAsDialog extends DialogWrapper {
    @Getter
    private JBTextField nameTextField;
    @Getter
    private ExtendableTextField dirTextField;

    public MQLFileSaveAsDialog(Project project) {
        super(true);
        nameTextField = new JBTextField("New File");
        ExtendableTextComponent.Extension extension =
                ExtendableTextComponent.Extension.create(
                        AllIcons.General.OpenDisk,
                        AllIcons.General.OpenDiskHover,
                        "Select Directory",
                        showSaveFileDialog(project)
                );
        dirTextField = new ExtendableTextField();
        dirTextField.addExtension(extension);
        nameTextField.setPreferredSize(JBUI.size(200, 35));
        setTitle("Save As");
        setOKButtonText("Save");
        setSize(400, 200);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder().addLabeledComponent("FileName", nameTextField)
                .addLabeledComponent("Directory", dirTextField)
                .getPanel();
    }

    public Runnable showSaveFileDialog(Project project) {
        return () -> {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false)
                    .withTitle("Save As ")
                    .withDescription("Save MQL Script")
                    .withShowHiddenFiles(true);
            VirtualFile[] selectedFiles = FileChooserFactory.getInstance()
                    .createFileChooser(descriptor, project, null)
                    .choose(project, new LightVirtualFile());
            if (selectedFiles.length > 0) {
                dirTextField.setText(selectedFiles[0].getPath());
            }
        };
    }
}
