package com.bol.spinner.ui;

import com.bol.spinner.auth.SpinnerToken;
import com.intellij.openapi.ui.ComboBox;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

public class MQL extends JFrame {

    String command, find = "";
    boolean execute, multiLineEdit;
    int numberOfLines;
    ArrayList matches;
    private Document doc;
    private UndoManager undoManager = new UndoManager();
    private UndoHandler undoHandler = new UndoHandler();
    private UndoAction undoAction = null;
    private RedoAction redoAction = null;
    private static boolean windowLoad = false;

    public MQL() {
        windowLoad = true;
        this.command = "";
        this.execute = false;
        initComponents();
        this.add(mnuDispose);
        setLocationRelativeTo(null);
//        setIconImage(Toolkit.getDefaultToolkit().getImage(MainWindow.class.getResource("resources/matrix.gif")));
        setTitle(getTitle() + " Server ''");
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        multiLineEdit = false;
        mnuMultiLineMQL.setSelected(multiLineEdit);
        numberOfLines = 4;
        txtMQLResult.setPopupMenu(null);
        setSyntaxHighlightTheme(txtMQLCommand,"idea");
        txtMQLCommand.setSyntaxEditingStyle("text/MQL");
        txtMQLCommand.setHighlightCurrentLine(false);
        txtMQLCommand.setLineWrap(true);
        txtMQLCommand.setRows(numberOfLines);
        var scheme = txtMQLCommand.getSyntaxScheme();
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = scheme.getStyle(Token.IDENTIFIER).foreground;
        rTextScrollPane1.setLineNumbersEnabled(multiLineEdit);
        rTextScrollPane2.setLineNumbersEnabled(true);
        if (command != null && !command.isEmpty()) {
            txtMQLCommand.setText(command);
            if (execute && SpinnerToken.context != null) {
                btnExecuteActionPerformed(null);
            }
        }
        txtMQLCommand.addKeyListener(kl);
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        /* For Undo Redo */
        doc = txtMQLCommand.getDocument();
        doc.addUndoableEditListener(undoHandler);
        var undoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
        var redoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK);
        undoAction = new UndoAction();
        txtMQLCommand.getInputMap().put(undoKeystroke, "undoKeystroke");
        txtMQLCommand.getActionMap().put("undoKeystroke", undoAction);
        redoAction = new RedoAction();
        txtMQLCommand.getInputMap().put(redoKeystroke, "redoKeystroke");
        txtMQLCommand.getActionMap().put("redoKeystroke", redoAction);
        txtMQLCommand.requestFocus();

        // * load MQL history *
        String k, v;

        windowLoad = false;

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent winEvt) {
                closeMQLWindow();
            }

            private void closeMQLWindow() {

                // * store commands *
                String k, v;
                int size = cmbMQLCommand.getItemCount();

                dispose();

            }
        });


    }

    KeyListener kl = new KeyListener() {
        @Override
        public void keyPressed(KeyEvent evt) {
            int n, m;
            if (evt.getKeyCode() == 70 && evt.isControlDown()) // Ctrl-f
            {
                txtFind.requestFocus();
            }
            if (evt.getKeyCode() == KeyEvent.VK_ENTER && (!multiLineEdit || evt.isAltDown())) {
                btnExecuteActionPerformed(null);
                evt.consume();
            }
            if (evt.getKeyCode() == KeyEvent.VK_TAB) {
                m = txtMQLCommand.getCaretPosition();
                if ((n = txtMQLCommand.getText().indexOf("???", m)) > 0) {
                    txtMQLCommand.setSelectionStart(n);
                    txtMQLCommand.setSelectionEnd(n + 3);
                } else if ((n = txtMQLCommand.getText().indexOf("???")) > 0) {
                    txtMQLCommand.setSelectionStart(n);
                    txtMQLCommand.setSelectionEnd(n + 3);
                }
                evt.consume();
            }
        }

        @Override
        public void keyReleased(KeyEvent evt) {
        }

        @Override
        public void keyTyped(KeyEvent evt) {
        }
    };

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mnuDispose = new JMenuItem();
        buttonGroup1 = new ButtonGroup();
        jPanel1 = new JPanel();
        lblMQLCommand = new JLabel();
        lblMQLResult = new JLabel();
        cmbMQLCommand = new ComboBox();
        btnClear = new JButton();
        btnClearAll = new JButton();
        btnToClipboard = new JButton();
        btnSelectionToClipboard = new JButton();
        btnExecute = new JButton();
        jLabel1 = new JLabel();
        txtFind = new JTextField();
        chkMatchCase = new JCheckBox();
        chkWholeWord = new JCheckBox();
        rbUp = new JRadioButton();
        rbDown = new JRadioButton();
        btnFind = new JButton();
        btnBack = new JButton();
        btnForward = new JButton();
        jLabel2 = new JLabel();
        chkHistory = new JCheckBox();
        btnSelectFile = new JButton();
        jSplitPane1 = new JSplitPane();
        rTextScrollPane1 = new org.fife.ui.rtextarea.RTextScrollPane();
        txtMQLCommand = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        rTextScrollPane2 = new org.fife.ui.rtextarea.RTextScrollPane();
        txtMQLResult = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        menuBar = new JMenuBar();
        mnuMQL = new JMenu();
        mnuNewMQL = new JMenuItem();
        mnuMultiLineMQL = new JCheckBoxMenuItem();
        mnuSaveMQLHistory = new JCheckBoxMenuItem();

        mnuDispose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        mnuDispose.setText("jMenuItem1");
        mnuDispose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuDisposeActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MQL");

        lblMQLCommand.setText("MQL Command");

        lblMQLResult.setText("MQL Result");

        cmbMQLCommand.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                cmbMQLCommandItemStateChanged(evt);
            }
        });

        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        btnClearAll.setText("Clear All");
        btnClearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnClearAllActionPerformed(evt);
            }
        });

        btnToClipboard.setText("To Clipboard");
        btnToClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnToClipboardActionPerformed(evt);
            }
        });

        btnSelectionToClipboard.setText("Selection to Clipboard");
        btnSelectionToClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSelectionToClipboardActionPerformed(evt);
            }
        });

        btnExecute.setText("Execute");
        btnExecute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnExecuteActionPerformed(evt);
            }
        });

        jLabel1.setText("Find Text");

        txtFind.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                txtFindKeyPressed(evt);
            }
            public void keyReleased(KeyEvent evt) {
                txtFindKeyReleased(evt);
            }
        });

        chkMatchCase.setText("Match Case");
        chkMatchCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                chkMatchCaseActionPerformed(evt);
            }
        });

        chkWholeWord.setText("Whole Word");
        chkWholeWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                chkWholeWordActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbUp);
        rbUp.setText("Up");

        buttonGroup1.add(rbDown);
        rbDown.setSelected(true);
        rbDown.setText("Down");

        btnFind.setText("Find");
        btnFind.setEnabled(false);
        btnFind.setMaximumSize(new java.awt.Dimension(71, 23));
        btnFind.setMinimumSize(new java.awt.Dimension(71, 23));
        btnFind.setPreferredSize(new java.awt.Dimension(71, 23));
        btnFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnFindActionPerformed(evt);
            }
        });

        btnBack.setIcon(new ImageIcon(getClass().getResource("/images/left.gif"))); // NOI18N
        btnBack.setEnabled(false);
        btnBack.setMaximumSize(new java.awt.Dimension(49, 23));
        btnBack.setMinimumSize(new java.awt.Dimension(49, 23));
        btnBack.setPreferredSize(new java.awt.Dimension(49, 23));
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        btnForward.setIcon(new ImageIcon(getClass().getResource("/images/right.gif"))); // NOI18N
        btnForward.setEnabled(false);
        btnForward.setMaximumSize(new java.awt.Dimension(49, 23));
        btnForward.setMinimumSize(new java.awt.Dimension(49, 23));
        btnForward.setPreferredSize(new java.awt.Dimension(49, 23));
        btnForward.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnForwardActionPerformed(evt);
            }
        });

        jLabel2.setText("MQL History");

        chkHistory.setSelected(true);


        btnSelectFile.setText("...");
        btnSelectFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnSelectFileActionPerformed(evt);
            }
        });

        jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);

        txtMQLCommand.setColumns(20);
        txtMQLCommand.setRows(4);
        txtMQLCommand.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        rTextScrollPane1.setViewportView(txtMQLCommand);

        jSplitPane1.setTopComponent(rTextScrollPane1);

        txtMQLResult.setEditable(false);
        txtMQLResult.setColumns(20);
        txtMQLResult.setRows(5);
        txtMQLResult.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        txtMQLResult.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                txtMQLResultKeyPressed(evt);
            }
        });
        rTextScrollPane2.setViewportView(txtMQLResult);

        jSplitPane1.setBottomComponent(rTextScrollPane2);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(lblMQLCommand, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addComponent(btnClear, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnClearAll, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnToClipboard, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnSelectionToClipboard, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(btnSelectFile, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(chkHistory)))
                    .addComponent(lblMQLResult, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 104, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(txtFind, GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkMatchCase)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkWholeWord)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rbUp)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rbDown)
                        .addGap(18, 18, 18)
                        .addComponent(btnFind, GroupLayout.PREFERRED_SIZE, 71, GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(cmbMQLCommand, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBack, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnForward, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnExecute, GroupLayout.PREFERRED_SIZE, 76, GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSplitPane1))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addGroup(GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(btnExecute)
                            .addComponent(jLabel2)
                            .addComponent(chkHistory))
                        .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(btnBack, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnForward, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addComponent(cmbMQLCommand, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(lblMQLCommand)
                            .addComponent(btnSelectFile, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE))
                        .addGap(70, 70, 70)
                        .addComponent(btnClear)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClearAll)
                        .addGap(18, 18, 18)
                        .addComponent(btnToClipboard)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSelectionToClipboard)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblMQLResult)
                        .addGap(14, 14, 14)))
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(chkMatchCase)
                        .addComponent(chkWholeWord)
                        .addComponent(rbUp)
                        .addComponent(rbDown)
                        .addComponent(btnFind, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(txtFind, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        mnuMQL.setText("MQL");
        mnuMQL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuMQLActionPerformed(evt);
            }
        });

        mnuNewMQL.setText("New MQL Box");
        mnuNewMQL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuNewMQLActionPerformed(evt);
            }
        });
        mnuMQL.add(mnuNewMQL);

        mnuMultiLineMQL.setText("Multiline MQL");
        mnuMultiLineMQL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuMultiLineMQLActionPerformed(evt);
            }
        });
        mnuMQL.add(mnuMultiLineMQL);

        mnuSaveMQLHistory.setSelected(true);
        mnuSaveMQLHistory.setText("Save MQL history");

        mnuMQL.add(mnuSaveMQLHistory);

        menuBar.add(mnuMQL);

        setJMenuBar(menuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mnuNewMQLActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuNewMQLActionPerformed
        new MQL().main();
}//GEN-LAST:event_mnuNewMQLActionPerformed

    private void btnClearActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        txtMQLCommand.setText("");
        txtMQLResult.setText("");
        txtMQLCommand.requestFocus();
}//GEN-LAST:event_btnClearActionPerformed

    private void btnClearAllActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnClearAllActionPerformed
        cmbMQLCommand.removeAllItems();
        btnClearActionPerformed(evt);
        btnBack.setEnabled(false);
        btnForward.setEnabled(false);
}//GEN-LAST:event_btnClearAllActionPerformed

    private void btnToClipboardActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnToClipboardActionPerformed
        txtMQLResult.selectAll();
        var selection = txtMQLCommand.getText() + "\n\n" + txtMQLResult.getText();
        var data = new StringSelection(selection);
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
        txtMQLResult.select(0, 0);
        txtMQLCommand.requestFocus();
}//GEN-LAST:event_btnToClipboardActionPerformed

    private void btnExecuteActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnExecuteActionPerformed
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            var mql = new MQLCommand();
            if (mql.executeCommand(SpinnerToken.context, txtMQLCommand.getText(), true)) {
                var result = mql.getResult();
                if (result.endsWith("\n")) {
                    result = result.substring(0, result.length() - 1);
                }
                if (result.isEmpty()) {
                    result = "Command executed successfully";
                }
                txtMQLResult.setText(result);
                if (chkHistory.isSelected()) {
                    var found = false;
                    for (var i = 0; i < cmbMQLCommand.getItemCount(); i++) {
                        found = found || cmbMQLCommand.getItemAt(i).toString().equalsIgnoreCase(txtMQLCommand.getText());
                    }
                    if (!found) {
                        cmbMQLCommand.addItem(txtMQLCommand.getText());
                        cmbMQLCommand.setSelectedIndex(cmbMQLCommand.getItemCount() - 1);
                    }
                }
                btnBack.setEnabled(cmbMQLCommand.getItemCount() > 1);
            } else {
                var error = mql.getError();
                txtMQLResult.setText(error);
            }
        } catch (MatrixException ex) {
            handleMatrixException(ex);
        }
        txtMQLResult.setSelectionStart(0);
        txtMQLResult.setSelectionEnd(0);
        txtMQLCommand.requestFocus();
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_btnExecuteActionPerformed

    private void mnuDisposeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuDisposeActionPerformed
        dispose();
    }//GEN-LAST:event_mnuDisposeActionPerformed

    private void txtFindKeyReleased(KeyEvent evt) {//GEN-FIRST:event_txtFindKeyReleased
        find = txtFind.getText();
        find();
    }//GEN-LAST:event_txtFindKeyReleased

    private void chkMatchCaseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chkMatchCaseActionPerformed
        find();
    }//GEN-LAST:event_chkMatchCaseActionPerformed

    private void chkWholeWordActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chkWholeWordActionPerformed
        find();
    }//GEN-LAST:event_chkWholeWordActionPerformed

    private void btnFindActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnFindActionPerformed
        if (rbDown.isSelected()) {
            findNext();
        } else {
            findPrevious();
        }
        centerLineInScrollPane(txtMQLResult);
    }//GEN-LAST:event_btnFindActionPerformed

    private void btnBackActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        var currentIndex = cmbMQLCommand.getSelectedIndex();
        if (currentIndex < 0) {
            return;
        }
        cmbMQLCommand.setSelectedIndex(--currentIndex);
        btnBack.setEnabled(currentIndex > 0);
        btnForward.setEnabled(currentIndex < cmbMQLCommand.getItemCount() - 1);
        txtMQLCommand.requestFocus();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnForwardActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnForwardActionPerformed
        var currentIndex = cmbMQLCommand.getSelectedIndex();
        if (currentIndex < 0) {
            return;
        }
        cmbMQLCommand.setSelectedIndex(++currentIndex);
        btnBack.setEnabled(currentIndex > 0);
        btnForward.setEnabled(currentIndex < cmbMQLCommand.getItemCount() - 1);
        txtMQLCommand.requestFocus();
    }//GEN-LAST:event_btnForwardActionPerformed

    private void cmbMQLCommandItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_cmbMQLCommandItemStateChanged
        if (evt.getStateChange() != ItemEvent.SELECTED || windowLoad) {
            return;
        }
        txtMQLCommand.setText(cmbMQLCommand.getSelectedItem().toString());
        var currentIndex = cmbMQLCommand.getSelectedIndex();
        btnBack.setEnabled(currentIndex > 0);
        btnForward.setEnabled(currentIndex < cmbMQLCommand.getItemCount() - 1);
    }//GEN-LAST:event_cmbMQLCommandItemStateChanged

    private void btnSelectFileActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSelectFileActionPerformed
        var fc = new JFileChooser();
        fc.setDialogTitle("Please select the script file");
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".txt") || f.getName().endsWith(".mql") || f.getName().endsWith(".tcl");
            }

            @Override
            public String getDescription() {
                return "Enovia Scripts(*.txt|*.mql|*.tcl)";
            }
        });
        var rc = fc.showOpenDialog(null);
        if (rc != JFileChooser.APPROVE_OPTION) {
            return;
        }
        var file = fc.getSelectedFile();
        var content = "";
        try {
            content = new Scanner(file).useDelimiter("\\Z").next();
        } catch (FileNotFoundException ex) {
            handleException(ex);
        }
        multiLineEdit = true;
        rTextScrollPane1.setLineNumbersEnabled(multiLineEdit);
        txtMQLCommand.setText(content);
    }//GEN-LAST:event_btnSelectFileActionPerformed

    private void txtMQLResultKeyPressed(KeyEvent evt) {//GEN-FIRST:event_txtMQLResultKeyPressed
        if (evt.isControlDown() && evt.getKeyCode() == 70) // Ctrl-f
        {
            txtFind.requestFocus();
        }
        if (evt.getKeyCode() == 114 || evt.getKeyCode() == 10) // F3 or ENTER
            btnFindActionPerformed(null);
    }//GEN-LAST:event_txtMQLResultKeyPressed

    private void txtFindKeyPressed(KeyEvent evt) {//GEN-FIRST:event_txtFindKeyPressed
        if (evt.getKeyCode() == 10) // ENTER
            btnFindActionPerformed(null);
    }//GEN-LAST:event_txtFindKeyPressed

    private void mnuMultiLineMQLActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuMultiLineMQLActionPerformed

        multiLineEdit = mnuMultiLineMQL.isSelected();
        rTextScrollPane1.setLineNumbersEnabled(multiLineEdit);
    }//GEN-LAST:event_mnuMultiLineMQLActionPerformed

    private void mnuMQLActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuMQLActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mnuMQLActionPerformed

    private void btnSelectionToClipboardActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSelectionToClipboardActionPerformed

        var selection = txtMQLResult.getSelectedText();
        var data = new StringSelection(selection);
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
        txtMQLResult.select(0, 0);
        txtMQLCommand.requestFocus();
    }//GEN-LAST:event_btnSelectionToClipboardActionPerformed

    void find() {
        if (!find.isEmpty()) {
            findMatches(txtMQLResult, find);
        }
        removeHighlights(txtMQLResult);
        if (!find.isEmpty()) {
            addHighlight(txtMQLResult, find);
        }
    }

    void findMatches(JTextComponent textComp, String patternOrig) {
        matches = new ArrayList();
        try {
            var doc = textComp.getDocument();
            String text, pattern;
            if (chkMatchCase.isSelected()) {
                text = doc.getText(0, doc.getLength());
                pattern = patternOrig;
            } else {
                text = doc.getText(0, doc.getLength()).toLowerCase();
                pattern = patternOrig.toLowerCase();
            }
            if (chkWholeWord.isSelected()) {
                pattern = "^" + pattern + "$|^" + pattern + "[\\s\\p{Punct}]|[\\s\\p{Punct}]" + pattern + "$|[\\s\\p{Punct}]" + pattern + "[\\s\\p{Punct}]";
            }
            var matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text);
            while (matcher.find()) {
                if (!chkWholeWord.isSelected() || matcher.group().equals(patternOrig)) {
                    matches.add(new Point(matcher.start(), matcher.end()));
                } else if (matcher.group().startsWith(patternOrig)) {
                    matches.add(new Point(matcher.start(), matcher.end() - 1));
                } else if (matcher.group().endsWith(patternOrig)) {
                    matches.add(new Point(matcher.start() + 1, matcher.end()));
                } else {
                    matches.add(new Point(matcher.start() + 1, matcher.end() - 1));
                }
            }
            btnFind.setEnabled(matches.size() > 0);
            if (matches.isEmpty()) {
                txtFind.setForeground(Color.red);
            } else {
                txtFind.setForeground(Color.black);
            }
        } catch (BadLocationException ex) {
            handleException(ex);
        }
    }

    void findNext() {
        if (matches == null || matches.isEmpty()) {
            return;
        }
        int start, end;
        var pos = txtMQLResult.getCaretPosition();
        int i;
        for (i = 0; i < matches.size(); i++) {
            if (((Point) matches.get(i)).x >= pos) {
                break;
            }
        }
        if (i == matches.size()) {
            start = ((Point) matches.get(0)).x;
            end = ((Point) matches.get(0)).y;
        } else {
            start = ((Point) matches.get(i)).x;
            end = ((Point) matches.get(i)).y;
        }
        txtMQLResult.select(start, end);
        txtMQLResult.requestFocusInWindow();
    }

    void findPrevious() {
        if (matches == null || matches.isEmpty()) {
            return;
        }
        int start, end;
        var pos = txtMQLResult.getCaretPosition();
        int i;
        for (i = matches.size() - 1; i >= 0; i--) {
            if (((Point) matches.get(i)).y < pos) {
                break;
            }
        }
        if (i < 0) {
            start = ((Point) matches.get(matches.size() - 1)).x;
            end = ((Point) matches.get(matches.size() - 1)).y;
        } else {
            start = ((Point) matches.get(i)).x;
            end = ((Point) matches.get(i)).y;
        }
        txtMQLResult.select(start, end);
        txtMQLResult.requestFocusInWindow();
    }

    public void addHighlight(JTextComponent textComp, String pattern) {
        try {
            var hl = textComp.getHighlighter();
            for (var i = 0; i < matches.size(); i++) {
                hl.addHighlight(((Point) matches.get(i)).x, ((Point) matches.get(i)).y, new UnderlineHighlighter.UnderlineHighlightPainter(Color.red));
            }
        } catch (BadLocationException ex) {
            handleException(ex);
        }
    }

    public void removeHighlights(JTextComponent textComp) {
        var hl = textComp.getHighlighter();
        var hls = hl.getHighlights();
        for (var hl1 : hls) {
            if (hl1.getPainter() instanceof UnderlineHighlighter.UnderlineHighlightPainter) {
                hl.removeHighlight(hl1);
            }
        }
    }

    public void main() {
        java.awt.EventQueue.invokeLater(() -> {
            new MQL().setVisible(true);
        });
    }

    public void centerLineInScrollPane(JTextComponent component) {
        var container = SwingUtilities.getAncestorOfClass(JViewport.class, component);
        if (container == null) {
            return;
        }
        try {
            var r = component.modelToView(component.getCaretPosition());
            var viewport = (JViewport) container;
            var extentHeight = viewport.getExtentSize().height;
            var viewHeight = viewport.getViewSize().height;
            var y = Math.max(0, r.y - (extentHeight / 2));
            y = Math.min(y, viewHeight - extentHeight);
            viewport.setViewPosition(new Point(0, y));
            viewport.repaint();
        } catch (BadLocationException ble) {
        }
    }

    class UndoHandler implements UndoableEditListener {

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            undoManager.addEdit(e.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }

    class UndoAction extends AbstractAction {

        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.undo();
            } catch (CannotUndoException ex) {
            }
            update();
            redoAction.update();
        }

        protected void update() {
            if (undoManager.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.redo();
            } catch (CannotRedoException ex) {
            }
            update();
            undoAction.update();
        }

        protected void update() {
            if (undoManager.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnBack;
    private JButton btnClear;
    private JButton btnClearAll;
    private JButton btnExecute;
    private JButton btnFind;
    private JButton btnForward;
    private JButton btnSelectFile;
    private JButton btnSelectionToClipboard;
    private JButton btnToClipboard;
    private ButtonGroup buttonGroup1;
    private JCheckBox chkHistory;
    private JCheckBox chkMatchCase;
    private JCheckBox chkWholeWord;
    private JComboBox cmbMQLCommand;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JPanel jPanel1;
    private JSplitPane jSplitPane1;
    private JLabel lblMQLCommand;
    private JLabel lblMQLResult;
    private JMenuBar menuBar;
    private JMenuItem mnuDispose;
    private JMenu mnuMQL;
    private JCheckBoxMenuItem mnuMultiLineMQL;
    private JMenuItem mnuNewMQL;
    private JCheckBoxMenuItem mnuSaveMQLHistory;
    private org.fife.ui.rtextarea.RTextScrollPane rTextScrollPane1;
    private org.fife.ui.rtextarea.RTextScrollPane rTextScrollPane2;
    private JRadioButton rbDown;
    private JRadioButton rbUp;
    private JTextField txtFind;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea txtMQLCommand;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea txtMQLResult;
    // End of variables declaration//GEN-END:variables

    public void setSyntaxHighlightTheme(RSyntaxTextArea text, String themeName) {
        if (!themeName.equals("default")) {
            var in = this.getClass().getResourceAsStream("/config/" + themeName + ".xml");
            try {
                var theme = Theme.load(in);
                theme.apply(text);
            } catch (IOException ex) {
            }
        }
    }

    public void handleException(Exception ex) {
        var msg = ex.getLocalizedMessage();
        displayError(msg);
    }

    public void displayError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void handleMatrixException(Exception ex) {
        var msg = ex.getLocalizedMessage();
        if (msg == null)
            msg = "null";
        if (msg.contains("XML: Expected") || msg.contains("Exception: Expected") || msg.contains("XML: -1")) {
            msg = "Enovia reported the following error: " + msg
                    + "\nThis can be due to wrong library versions."
                    + "\nTake these 5 files"
                    + "\n• eMatrixServletRMI.jar"
                    + "\n• enoviaKernel.jar"
                    + "\n• FcsClient.jar"
                    + "\n• m1jsystem.jar"
                    + "\n• mx_jdom_1.0.jar"
                    + "\nfrom your Enovia server and copy them into the EnoBrowser\\lib folder."
                    + "\nYou find them in C:\\Tomcat...\\...\\WEB-INF\\lib on your Enovia server.";
        }
        displayError(msg);
    }
}
