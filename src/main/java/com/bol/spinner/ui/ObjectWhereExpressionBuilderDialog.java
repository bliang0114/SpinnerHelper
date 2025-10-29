package com.bol.spinner.ui;

import com.bol.spinner.config.ObjectWhereExpression;
import com.bol.spinner.config.SpinnerToken;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ObjectWhereExpressionBuilderDialog extends DialogWrapper {
    private final Project project;
    private JBTextField nameField;
    private JBTextField revisionField;
    private JBTextField idField;
    private JBTextField physicalIdField;
    private JBTextField policyField;
    private JBTextField stateField;
    private JBTextField organizationField;
    private JBTextField collaborativeField;

    public ObjectWhereExpressionBuilderDialog(Project project) {
        super(true);
        this.project = project;
        setTitle("Where Expression");
        setSize(600, 400);
        setOKButtonText("OK");
        initComponents();
        setupValue();
        init();
    }

    private void setupValue() {
        ObjectWhereExpression whereExpression = SpinnerToken.getObjectWhereExpression(project);
        if (whereExpression != null) {
            nameField.setText(whereExpression.getName());
            revisionField.setText(whereExpression.getRevision());
            idField.setText(whereExpression.getId());
            physicalIdField.setText(whereExpression.getPhysicalId());
            policyField.setText(whereExpression.getPolicy());
            stateField.setText(whereExpression.getState());
            organizationField.setText(whereExpression.getOrganization());
            collaborativeField.setText(whereExpression.getCollaborative());
        }
    }

    private void initComponents() {
        nameField = new JBTextField("*");
        revisionField = new JBTextField("*");
        idField = new JBTextField();
        physicalIdField = new JBTextField();
        policyField = new JBTextField();
        stateField = new JBTextField();
        organizationField = new JBTextField();
        collaborativeField = new JBTextField();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name = ", nameField)
                .addLabeledComponent("Revision = ", revisionField)
                .addLabeledComponent("ID = ", idField)
                .addLabeledComponent("Physical ID = ", physicalIdField)
                .addLabeledComponent("Policy = ", policyField)
                .addLabeledComponent("State = ", stateField)
                .addLabeledComponent("Organization = ", organizationField)
                .addLabeledComponent("Collaborative Space = ", collaborativeField)
                .getPanel();
    }

    public ObjectWhereExpression getWhereExpression() {
        ObjectWhereExpression whereExpression = new ObjectWhereExpression();
        whereExpression.setName(nameField.getText());
        whereExpression.setRevision(revisionField.getText());
        whereExpression.setId(idField.getText());
        whereExpression.setPhysicalId(physicalIdField.getText());
        whereExpression.setPolicy(policyField.getText());
        whereExpression.setState(stateField.getText());
        whereExpression.setOrganization(organizationField.getText());
        whereExpression.setCollaborative(collaborativeField.getText());
        return  whereExpression;
    }
}
