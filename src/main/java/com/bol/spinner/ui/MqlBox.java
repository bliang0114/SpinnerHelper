package com.bol.spinner.ui;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

import javax.swing.*;
import java.awt.*;

public class MqlBox {
    private JPanel mainPanel;
    private JTextField findTextField;
    private JButton findButton;
    private JButton executeBtn;
    private RSyntaxTextArea mqlCommandArea;
    private RSyntaxTextArea mqlResultArea;

    public MqlBox() {
        mainPanel.setPreferredSize(new Dimension(800, 600));
        AbstractTokenMakerFactory defaultInstance = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        defaultInstance.putMapping("text/MQL", "com.bol.spinner.ui.MQLTokenMaker");
        mqlCommandArea.setSyntaxEditingStyle("text/MQL");
        mqlCommandArea.setHighlightCurrentLine(false);
        mqlCommandArea.setLineWrap(true);
        mqlCommandArea.setRows(4);
        mqlResultArea.setEditable(false);
        mqlResultArea.setColumns(20);
        mqlResultArea.setRows(5);
        mqlResultArea.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
