package com.bol.spinner.ui;

import matrix.db.Context;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

import javax.swing.*;
import java.awt.*;

public class MqlBox extends JFrame{
    private JPanel mainPanel;
    private JTextField findTextField;
    private JButton findButton;
    private JButton executeBtn;
    private RSyntaxTextArea mqlCommandArea;
    private RSyntaxTextArea mqlResultArea;

    private void createUIComponents() {
        AbstractTokenMakerFactory defaultInstance = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        defaultInstance.putMapping("text/MQL", "com.bol.spinner.ui.MQLTokenMaker");
        mqlCommandArea.setSyntaxEditingStyle("text/MQL");
        mqlCommandArea.setHighlightCurrentLine(false);
        mqlCommandArea.setLineWrap(true);
        mqlCommandArea.setRows(4);
    }

    public static void main(Context ctx, String command, int markA, int markE, boolean execute) {
        EventQueue.invokeLater(() -> (new MqlBox()).setVisible(true));
    }
}
