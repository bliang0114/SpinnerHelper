package com.bol.spinner.ui;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import com.bol.spinner.auth.Util;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.StartupUiUtil;
import enobrowser.WhereClause;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MQL_Box extends JFrame {
    Context ctx;
    String command;
    String find = "";
    boolean execute;
    boolean multiLineEdit;
    int numberOfLines;
    ArrayList matches;
    private Document doc;
    private UndoManager undoManager = new UndoManager();
    private UndoHandler undoHandler = new UndoHandler();
    private UndoAction undoAction = null;
    private RedoAction redoAction = null;
    private static boolean windowLoad = false;
    KeyListener kl = new KeyListener() {
        public void keyPressed(KeyEvent evt) {
            if (evt.getKeyCode() == 70 && evt.isControlDown()) {
                MQL_Box.this.txtFind.requestFocus();
            }

            if (evt.getKeyCode() == 10 && (!MQL_Box.this.multiLineEdit || evt.isAltDown())) {
                MQL_Box.this.btnExecuteActionPerformed((ActionEvent)null);
                evt.consume();
            }

            if (evt.getKeyCode() == 9) {
                int m = MQL_Box.this.txtMQLCommand.getCaretPosition();
                int n;
                if ((n = MQL_Box.this.txtMQLCommand.getText().indexOf("???", m)) > 0) {
                    MQL_Box.this.txtMQLCommand.setSelectionStart(n);
                    MQL_Box.this.txtMQLCommand.setSelectionEnd(n + 3);
                } else if ((n = MQL_Box.this.txtMQLCommand.getText().indexOf("???")) > 0) {
                    MQL_Box.this.txtMQLCommand.setSelectionStart(n);
                    MQL_Box.this.txtMQLCommand.setSelectionEnd(n + 3);
                }

                evt.consume();
            }

        }

        public void keyReleased(KeyEvent evt) {
        }

        public void keyTyped(KeyEvent evt) {
        }
    };
    Action MQLAction = new AbstractAction() {
        public void actionPerformed(ActionEvent evt) {
            boolean concat = false;
            String t = MQL_Box.this.txtMQLCommand.getText();
            String s = evt.getActionCommand();
            if (s.equalsIgnoreCase("Where Builder")) {
                WhereClause wc = new WhereClause(MQL_Box.this.ctx, (String)null, (String)null);
                wc.setVisible(true);
                s = wc.where;
                if (!s.isEmpty()) {
                    s = "... where \"" + s + "\"";
                }

                wc.dispose();
            }

            if (s.startsWith("...")) {
                concat = true;
                s = s.substring(3);
            }

            if (t.endsWith("...")) {
                t = t.substring(0, t.length() - 3);
                concat = true;
            }

            if (concat) {
                s = t + s;
            }

            MQL_Box.this.txtMQLCommand.setText(s);
            int n;
            if ((n = s.indexOf("???")) > 0) {
                MQL_Box.this.txtMQLCommand.setSelectionStart(n);
                MQL_Box.this.txtMQLCommand.setSelectionEnd(n + 3);
            }

        }
    };
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
    private RTextScrollPane rTextScrollPane1;
    private RTextScrollPane rTextScrollPane2;
    private JRadioButton rbDown;
    private JRadioButton rbUp;
    private JTextField txtFind;
    private RSyntaxTextArea txtMQLCommand;
    private RSyntaxTextArea txtMQLResult;

    public MQL_Box(Context ctx, String command, int markA, int markE, boolean execute) {
       /* try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Util.handleException(ex);
        }*/
        windowLoad = true;
        this.ctx = ctx;
        this.command = command;
        this.execute = execute;
        this.initComponents();
        this.add(this.mnuDispose);
        this.setLocationRelativeTo((Component)null);
//        this.setIconImage(Toolkit.getDefaultToolkit().getImage(MainWindow.class.getResource("resources/matrix.gif")));
        String var10001 = this.getTitle();
        this.setTitle(var10001 + " Server ");
        this.setCursor(new Cursor(3));
        this.createMenus();
        this.multiLineEdit = false;
        this.mnuMultiLineMQL.setSelected(this.multiLineEdit);
        this.numberOfLines = 4;
        this.txtMQLResult.setPopupMenu((JPopupMenu)null);
        AbstractTokenMakerFactory defaultInstance = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        defaultInstance.putMapping("text/MQL", "com.bol.spinner.ui.MQLTokenMaker");
        this.txtMQLCommand.setSyntaxEditingStyle("text/MQL");
        this.txtMQLCommand.setHighlightCurrentLine(false);
        this.txtMQLCommand.setLineWrap(true);
        this.txtMQLCommand.setRows(this.numberOfLines);
        SyntaxScheme scheme = this.txtMQLCommand.getSyntaxScheme();
        scheme.getStyle(13).foreground = scheme.getStyle(20).foreground;
        this.rTextScrollPane1.setLineNumbersEnabled(this.multiLineEdit);
        this.rTextScrollPane2.setLineNumbersEnabled(true);
        if (command != null && !command.isEmpty()) {
            this.txtMQLCommand.setText(command);
//            ctx = MainWindow.currentServer.checkContext();
            if (execute && ctx != null) {
                this.btnExecuteActionPerformed((ActionEvent)null);
            } else if (markA < markE) {
                this.txtMQLCommand.setSelectionStart(markA);
                this.txtMQLCommand.setSelectionEnd(markE);
            }
        }

        this.txtMQLCommand.addKeyListener(this.kl);
        this.setCursor(new Cursor(0));
        this.doc = this.txtMQLCommand.getDocument();
        this.doc.addUndoableEditListener(this.undoHandler);
        KeyStroke undoKeystroke = KeyStroke.getKeyStroke(90, 2);
        KeyStroke redoKeystroke = KeyStroke.getKeyStroke(89, 2);
        this.undoAction = new UndoAction();
        this.txtMQLCommand.getInputMap().put(undoKeystroke, "undoKeystroke");
        this.txtMQLCommand.getActionMap().put("undoKeystroke", this.undoAction);
        this.redoAction = new RedoAction();
        this.txtMQLCommand.getInputMap().put(redoKeystroke, "redoKeystroke");
        this.txtMQLCommand.getActionMap().put("redoKeystroke", this.redoAction);
        this.txtMQLCommand.requestFocus();

        /*for(int i = 0; i < 50; ++i) {
            Object[] var13 = new Object[]{i};
            String k = "MQLHistory." + String.format("%02d", var13);
            if (MainWindow.settings.containsKey(k)) {
                String v = MainWindow.settings.get(k).toString();
                if (!v.isEmpty()) {
                    this.cmbMQLCommand.addItem(v);
                }
            }
        }*/

        windowLoad = false;
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                this.closeMQLWindow();
            }

            private void closeMQLWindow() {
                int size = MQL_Box.this.cmbMQLCommand.getItemCount();

                /*for(int i = 0; i < 50; ++i) {
                    Object[] var10001 = new Object[]{i};
                    String k = "MQLHistory." + String.format("%02d", var10001);
                    if (i < size && MQL_View.this.mnuSaveMQLHistory.isSelected()) {
                        MainWindow.settings.put(k, MQL_View.this.cmbMQLCommand.getItemAt(i).toString());
                    } else {
                        MainWindow.settings.put(k, "");
                    }
                }*/

                MQL_Box.this.dispose();
            }
        });
    }

    private void createMenus() {
        String s;
        /*for(int n = 1; (s = MainWindow.settings.getProperty("MQL" + String.valueOf(n))) != null; ++n) {
            JMenu menu = new JMenu(s);
            this.menuBar.add(menu);
            int i = 1;

            while(true) {
                Properties var10000 = MainWindow.settings;
                String var10001 = String.valueOf(n);
                if ((s = var10000.getProperty("MQL" + var10001 + String.valueOf(i))) == null) {
                    break;
                }

                if (Pattern.matches("-+", s)) {
                    menu.add(new JSeparator());
                } else {
                    JMenuItem menuitem = menu.add(this.MQLAction);
                    menuitem.setText(s);
                }

                ++i;
            }
        }*/

    }

    private void initComponents() {
        this.mnuDispose = new JMenuItem();
        this.buttonGroup1 = new ButtonGroup();
        this.jPanel1 = new JPanel();
        this.lblMQLCommand = new JLabel();
        this.lblMQLResult = new JLabel();
        this.cmbMQLCommand = new JComboBox();
        this.btnClear = new JButton();
        this.btnClearAll = new JButton();
        this.btnToClipboard = new JButton();
        this.btnSelectionToClipboard = new JButton();
        this.btnExecute = new JButton();
        this.jLabel1 = new JLabel();
        this.txtFind = new JTextField();
        this.chkMatchCase = new JCheckBox();
        this.chkWholeWord = new JCheckBox();
        this.rbUp = new JRadioButton();
        this.rbDown = new JRadioButton();
        this.btnFind = new JButton();
        this.btnBack = new JButton();
        this.btnForward = new JButton();
        this.jLabel2 = new JLabel();
        this.chkHistory = new JCheckBox();
        this.btnSelectFile = new JButton();
        this.jSplitPane1 = new JSplitPane();
        this.rTextScrollPane1 = new RTextScrollPane();
        this.txtMQLCommand = new RSyntaxTextArea();
        this.txtMQLResult = new RSyntaxTextArea();
        if (StartupUiUtil.INSTANCE.isDarkTheme()) {
            txtMQLCommand.setBackground(Color.decode("#2B2D30"));
            txtMQLCommand.setForeground(Color.WHITE);
            txtMQLResult.setBackground(Color.decode("#2B2D30"));
            txtMQLResult.setForeground(Color.WHITE);
        } else {
            txtMQLCommand.setBackground(JBColor.WHITE);
            txtMQLCommand.setForeground(JBColor.BLACK);
            txtMQLResult.setBackground(JBColor.WHITE);
            txtMQLResult.setForeground(JBColor.BLACK);
        }
        this.rTextScrollPane2 = new RTextScrollPane();
        this.menuBar = new JMenuBar();
        this.mnuMQL = new JMenu();
        this.mnuNewMQL = new JMenuItem();
        this.mnuMultiLineMQL = new JCheckBoxMenuItem();
        this.mnuSaveMQLHistory = new JCheckBoxMenuItem();
        this.mnuDispose.setAccelerator(KeyStroke.getKeyStroke(27, 0));
        this.mnuDispose.setText("jMenuItem1");
        this.mnuDispose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.mnuDisposeActionPerformed(evt);
            }
        });
        this.setDefaultCloseOperation(2);
        this.setTitle("MQL");
        this.lblMQLCommand.setText("MQL Command");
        this.lblMQLResult.setText("MQL Result");
        this.cmbMQLCommand.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                MQL_Box.this.cmbMQLCommandItemStateChanged(evt);
            }
        });
        this.btnClear.setText("Clear");
        this.btnClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnClearActionPerformed(evt);
            }
        });
        this.btnClearAll.setText("Clear All");
        this.btnClearAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnClearAllActionPerformed(evt);
            }
        });
        this.btnToClipboard.setText("To Clipboard");
        this.btnToClipboard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnToClipboardActionPerformed(evt);
            }
        });
        this.btnSelectionToClipboard.setText("Selection to Clipboard");
        this.btnSelectionToClipboard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnSelectionToClipboardActionPerformed(evt);
            }
        });
        this.btnExecute.setText("Execute");
        this.btnExecute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnExecuteActionPerformed(evt);
            }
        });
        this.jLabel1.setText("Find Text");
        this.txtFind.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                MQL_Box.this.txtFindKeyPressed(evt);
            }

            public void keyReleased(KeyEvent evt) {
                MQL_Box.this.txtFindKeyReleased(evt);
            }
        });
        this.chkMatchCase.setText("Match Case");
        this.chkMatchCase.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.chkMatchCaseActionPerformed(evt);
            }
        });
        this.chkWholeWord.setText("Whole Word");
        this.chkWholeWord.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.chkWholeWordActionPerformed(evt);
            }
        });
        this.buttonGroup1.add(this.rbUp);
        this.rbUp.setText("Up");
        this.buttonGroup1.add(this.rbDown);
        this.rbDown.setSelected(true);
        this.rbDown.setText("Down");
        this.btnFind.setText("Find");
        this.btnFind.setEnabled(false);
        this.btnFind.setMaximumSize(new Dimension(71, 23));
        this.btnFind.setMinimumSize(new Dimension(71, 23));
        this.btnFind.setPreferredSize(new Dimension(71, 23));
        this.btnFind.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnFindActionPerformed(evt);
            }
        });
        this.btnBack.setIcon(new ImageIcon(this.getClass().getResource("/enobrowser/resources/left.gif")));
        this.btnBack.setEnabled(false);
        this.btnBack.setMaximumSize(new Dimension(49, 23));
        this.btnBack.setMinimumSize(new Dimension(49, 23));
        this.btnBack.setPreferredSize(new Dimension(49, 23));
        this.btnBack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnBackActionPerformed(evt);
            }
        });
        this.btnForward.setIcon(new ImageIcon(this.getClass().getResource("/enobrowser/resources/right.gif")));
        this.btnForward.setEnabled(false);
        this.btnForward.setMaximumSize(new Dimension(49, 23));
        this.btnForward.setMinimumSize(new Dimension(49, 23));
        this.btnForward.setPreferredSize(new Dimension(49, 23));
        this.btnForward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnForwardActionPerformed(evt);
            }
        });
        this.jLabel2.setText("MQL History");
        this.chkHistory.setSelected(true);
        this.chkHistory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.chkHistoryActionPerformed(evt);
            }
        });
        this.btnSelectFile.setText("...");
        this.btnSelectFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.btnSelectFileActionPerformed(evt);
            }
        });
        this.jSplitPane1.setOrientation(0);
        this.txtMQLCommand.setColumns(20);
        this.txtMQLCommand.setRows(4);
        this.txtMQLCommand.setFont(new Font("Monospaced", 0, 13));
        this.rTextScrollPane1.setViewportView(this.txtMQLCommand);
        this.jSplitPane1.setTopComponent(this.rTextScrollPane1);
        this.txtMQLResult.setEditable(false);
        this.txtMQLResult.setColumns(20);
        this.txtMQLResult.setRows(5);
        this.txtMQLResult.setFont(new Font("Monospaced", 0, 13));
        this.txtMQLResult.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                MQL_Box.this.txtMQLResultKeyPressed(evt);
            }
        });
        this.rTextScrollPane2.setViewportView(this.txtMQLResult);
        this.jSplitPane1.setBottomComponent(this.rTextScrollPane2);
        GroupLayout jPanel1Layout = new GroupLayout(this.jPanel1);
        this.jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING, false).addComponent(this.lblMQLCommand, -1, -1, 32767).addComponent(this.jLabel1).addComponent(this.btnClear, -1, -1, 32767).addComponent(this.btnClearAll, -1, -1, 32767).addComponent(this.btnToClipboard, -1, -1, 32767).addComponent(this.btnSelectionToClipboard, -1, -1, 32767)).addGroup(jPanel1Layout.createParallelGroup(Alignment.TRAILING).addComponent(this.btnSelectFile, -2, 21, -2).addGroup(jPanel1Layout.createSequentialGroup().addComponent(this.jLabel2, -2, 80, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.chkHistory))).addComponent(this.lblMQLResult, Alignment.TRAILING, -2, 104, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(this.txtFind, -1, 556, 32767).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.chkMatchCase).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.chkWholeWord).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.rbUp).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.rbDown).addGap(18, 18, 18).addComponent(this.btnFind, -2, 71, -2)).addGroup(jPanel1Layout.createSequentialGroup().addComponent(this.cmbMQLCommand, 0, -1, 32767).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnBack, -2, 20, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnForward, -2, 20, -2).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.btnExecute, -2, 76, -2)).addComponent(this.jSplitPane1)).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createParallelGroup(Alignment.TRAILING).addGroup(Alignment.LEADING, jPanel1Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.btnExecute).addComponent(this.jLabel2).addComponent(this.chkHistory)).addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addComponent(this.btnBack, -2, -1, -2).addComponent(this.btnForward, -2, -1, -2))).addComponent(this.cmbMQLCommand, Alignment.TRAILING, -2, 23, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(this.jSplitPane1, -1, 685, 32767).addPreferredGap(ComponentPlacement.RELATED)).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.lblMQLCommand).addComponent(this.btnSelectFile, -2, 15, -2)).addGap(70, 70, 70).addComponent(this.btnClear).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.btnClearAll).addGap(18, 18, 18).addComponent(this.btnToClipboard).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.btnSelectionToClipboard).addPreferredGap(ComponentPlacement.RELATED, -1, 32767).addComponent(this.lblMQLResult).addGap(14, 14, 14))).addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING, jPanel1Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.chkMatchCase).addComponent(this.chkWholeWord).addComponent(this.rbUp).addComponent(this.rbDown).addComponent(this.btnFind, -2, -1, -2)).addGroup(Alignment.TRAILING, jPanel1Layout.createParallelGroup(Alignment.BASELINE).addComponent(this.jLabel1).addComponent(this.txtFind, -2, -1, -2))).addContainerGap()));
        this.mnuMQL.setText("MQL");
        this.mnuMQL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.mnuMQLActionPerformed(evt);
            }
        });
        this.mnuNewMQL.setText("New MQL Box");
        this.mnuNewMQL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.mnuNewMQLActionPerformed(evt);
            }
        });
        this.mnuMQL.add(this.mnuNewMQL);
        this.mnuMultiLineMQL.setText("Multiline MQL");
        this.mnuMultiLineMQL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.mnuMultiLineMQLActionPerformed(evt);
            }
        });
        this.mnuMQL.add(this.mnuMultiLineMQL);
        this.mnuSaveMQLHistory.setSelected(true);
        this.mnuSaveMQLHistory.setText("Save MQL history");
        this.mnuSaveMQLHistory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MQL_Box.this.mnuSaveMQLHistoryActionPerformed(evt);
            }
        });
        this.mnuMQL.add(this.mnuSaveMQLHistory);
        this.menuBar.add(this.mnuMQL);
        this.setJMenuBar(this.menuBar);
        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.jPanel1, -1, -1, 32767));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.jPanel1, -1, -1, 32767));
        this.pack();
    }

    private void mnuNewMQLActionPerformed(ActionEvent evt) {
        main(this.ctx, "", 0, 0, false);
    }

    private void btnClearActionPerformed(ActionEvent evt) {
        this.txtMQLCommand.setText("");
        this.txtMQLResult.setText("");
        this.txtMQLCommand.requestFocus();
    }

    private void btnClearAllActionPerformed(ActionEvent evt) {
        this.cmbMQLCommand.removeAllItems();
        this.btnClearActionPerformed(evt);
        this.btnBack.setEnabled(false);
        this.btnForward.setEnabled(false);
    }

    private void btnToClipboardActionPerformed(ActionEvent evt) {
        this.txtMQLResult.selectAll();
        String var10000 = this.txtMQLCommand.getText();
        String selection = var10000 + "\n\n" + this.txtMQLResult.getText();
        StringSelection data = new StringSelection(selection);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
        this.txtMQLResult.select(0, 0);
        this.txtMQLCommand.requestFocus();
    }

    private void btnExecuteActionPerformed(ActionEvent evt) {
//        this.ctx = MainWindow.currentServer.checkContext();
        this.setCursor(new Cursor(3));

        try {
            MQLCommand mql = new MQLCommand();
            if (mql.executeCommand(this.ctx, this.txtMQLCommand.getText(), true)) {
                String result = mql.getResult();
                if (result.endsWith("\n")) {
                    result = result.substring(0, result.length() - 1);
                }

                if (result.isEmpty()) {
                    result = "Command executed successfully";
                }

                this.txtMQLResult.setText(result);
                if (this.chkHistory.isSelected()) {
                    boolean found = false;

                    for(int i = 0; i < this.cmbMQLCommand.getItemCount(); ++i) {
                        found = found || this.cmbMQLCommand.getItemAt(i).toString().equalsIgnoreCase(this.txtMQLCommand.getText());
                    }

                    if (!found) {
                        this.cmbMQLCommand.addItem(this.txtMQLCommand.getText());
                        this.cmbMQLCommand.setSelectedIndex(this.cmbMQLCommand.getItemCount() - 1);
                    }
                }

                this.btnBack.setEnabled(this.cmbMQLCommand.getItemCount() > 1);
            } else {
                String error = mql.getError();
                this.txtMQLResult.setText(error);
            }
        } catch (MatrixException ex) {
            Util.handleMatrixException(ex);
        }

        this.txtMQLResult.setSelectionStart(0);
        this.txtMQLResult.setSelectionEnd(0);
        this.txtMQLCommand.requestFocus();
        this.setCursor(new Cursor(0));
    }

    private void mnuDisposeActionPerformed(ActionEvent evt) {
        this.dispose();
    }

    private void txtFindKeyReleased(KeyEvent evt) {
        this.find = this.txtFind.getText();
        this.find();
    }

    private void chkMatchCaseActionPerformed(ActionEvent evt) {
        this.find();
    }

    private void chkWholeWordActionPerformed(ActionEvent evt) {
        this.find();
    }

    private void btnFindActionPerformed(ActionEvent evt) {
        if (this.rbDown.isSelected()) {
            this.findNext();
        } else {
            this.findPrevious();
        }

        this.centerLineInScrollPane(this.txtMQLResult);
    }

    private void btnBackActionPerformed(ActionEvent evt) {
        int currentIndex = this.cmbMQLCommand.getSelectedIndex();
        if (currentIndex >= 0) {
            --currentIndex;
            this.cmbMQLCommand.setSelectedIndex(currentIndex);
            this.btnBack.setEnabled(currentIndex > 0);
            this.btnForward.setEnabled(currentIndex < this.cmbMQLCommand.getItemCount() - 1);
            this.txtMQLCommand.requestFocus();
        }
    }

    private void btnForwardActionPerformed(ActionEvent evt) {
        int currentIndex = this.cmbMQLCommand.getSelectedIndex();
        if (currentIndex >= 0) {
            ++currentIndex;
            this.cmbMQLCommand.setSelectedIndex(currentIndex);
            this.btnBack.setEnabled(currentIndex > 0);
            this.btnForward.setEnabled(currentIndex < this.cmbMQLCommand.getItemCount() - 1);
            this.txtMQLCommand.requestFocus();
        }
    }

    private void cmbMQLCommandItemStateChanged(ItemEvent evt) {
        if (evt.getStateChange() == 1 && !windowLoad) {
            this.txtMQLCommand.setText(this.cmbMQLCommand.getSelectedItem().toString());
            int currentIndex = this.cmbMQLCommand.getSelectedIndex();
            this.btnBack.setEnabled(currentIndex > 0);
            this.btnForward.setEnabled(currentIndex < this.cmbMQLCommand.getItemCount() - 1);
        }
    }

    private void btnSelectFileActionPerformed(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Please select the script file");
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".txt") || f.getName().endsWith(".mql") || f.getName().endsWith(".tcl");
            }

            public String getDescription() {
                return "Enovia Scripts(*.txt|*.mql|*.tcl)";
            }
        });
        int rc = fc.showOpenDialog((Component)null);
        if (rc == 0) {
            File file = fc.getSelectedFile();
            String content = "";

            try {
                content = (new Scanner(file)).useDelimiter("\\Z").next();
            } catch (FileNotFoundException ex) {
                Util.handleException(ex);
            }

            this.multiLineEdit = true;
            this.rTextScrollPane1.setLineNumbersEnabled(this.multiLineEdit);
            this.txtMQLCommand.setText(content);
        }
    }

    private void txtMQLResultKeyPressed(KeyEvent evt) {
        if (evt.isControlDown() && evt.getKeyCode() == 70) {
            this.txtFind.requestFocus();
        }

        if (evt.getKeyCode() == 114 || evt.getKeyCode() == 10) {
            this.btnFindActionPerformed((ActionEvent)null);
        }

    }

    private void txtFindKeyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == 10) {
            this.btnFindActionPerformed((ActionEvent)null);
        }

    }

    private void mnuMultiLineMQLActionPerformed(ActionEvent evt) {
        this.multiLineEdit = this.mnuMultiLineMQL.isSelected();
        this.rTextScrollPane1.setLineNumbersEnabled(this.multiLineEdit);
    }

    private void mnuMQLActionPerformed(ActionEvent evt) {
    }

    private void btnSelectionToClipboardActionPerformed(ActionEvent evt) {
        String selection = this.txtMQLResult.getSelectedText();
        StringSelection data = new StringSelection(selection);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
        this.txtMQLResult.select(0, 0);
        this.txtMQLCommand.requestFocus();
    }

    private void chkHistoryActionPerformed(ActionEvent evt) {
    }

    private void mnuSaveMQLHistoryActionPerformed(ActionEvent evt) {
//        MainWindow.settings.put("MQLHistorySave", String.valueOf(this.mnuSaveMQLHistory.isSelected()));
    }

    void find() {
        if (!this.find.isEmpty()) {
            this.findMatches(this.txtMQLResult, this.find);
        }

        this.removeHighlights(this.txtMQLResult);
        if (!this.find.isEmpty()) {
            this.addHighlight(this.txtMQLResult, this.find);
        }

    }

    void findMatches(JTextComponent textComp, String patternOrig) {
        this.matches = new ArrayList();

        try {
            Document doc = textComp.getDocument();
            String text;
            String pattern;
            if (this.chkMatchCase.isSelected()) {
                text = doc.getText(0, doc.getLength());
                pattern = patternOrig;
            } else {
                text = doc.getText(0, doc.getLength()).toLowerCase();
                pattern = patternOrig.toLowerCase();
            }

            if (this.chkWholeWord.isSelected()) {
                pattern = "^" + pattern + "$|^" + pattern + "[\\s\\p{Punct}]|[\\s\\p{Punct}]" + pattern + "$|[\\s\\p{Punct}]" + pattern + "[\\s\\p{Punct}]";
            }

            Matcher matcher = Pattern.compile(pattern, 8).matcher(text);

            while(matcher.find()) {
                if (this.chkWholeWord.isSelected() && !matcher.group().equals(patternOrig)) {
                    if (matcher.group().startsWith(patternOrig)) {
                        this.matches.add(new Point(matcher.start(), matcher.end() - 1));
                    } else if (matcher.group().endsWith(patternOrig)) {
                        this.matches.add(new Point(matcher.start() + 1, matcher.end()));
                    } else {
                        this.matches.add(new Point(matcher.start() + 1, matcher.end() - 1));
                    }
                } else {
                    this.matches.add(new Point(matcher.start(), matcher.end()));
                }
            }

            this.btnFind.setEnabled(this.matches.size() > 0);
            if (this.matches.isEmpty()) {
                this.txtFind.setForeground(Color.red);
            } else {
                this.txtFind.setForeground(Color.black);
            }
        } catch (BadLocationException ex) {
            Util.handleException(ex);
        }

    }

    void findNext() {
        if (this.matches != null && !this.matches.isEmpty()) {
            int pos = this.txtMQLResult.getCaretPosition();

            int i;
            for(i = 0; i < this.matches.size() && ((Point)this.matches.get(i)).x < pos; ++i) {
            }

            int start;
            int end;
            if (i == this.matches.size()) {
                start = ((Point)this.matches.get(0)).x;
                end = ((Point)this.matches.get(0)).y;
            } else {
                start = ((Point)this.matches.get(i)).x;
                end = ((Point)this.matches.get(i)).y;
            }

            this.txtMQLResult.select(start, end);
            this.txtMQLResult.requestFocusInWindow();
        }
    }

    void findPrevious() {
        if (this.matches != null && !this.matches.isEmpty()) {
            int pos = this.txtMQLResult.getCaretPosition();

            int i;
            for(i = this.matches.size() - 1; i >= 0 && ((Point)this.matches.get(i)).y >= pos; --i) {
            }

            int start;
            int end;
            if (i < 0) {
                start = ((Point)this.matches.get(this.matches.size() - 1)).x;
                end = ((Point)this.matches.get(this.matches.size() - 1)).y;
            } else {
                start = ((Point)this.matches.get(i)).x;
                end = ((Point)this.matches.get(i)).y;
            }

            this.txtMQLResult.select(start, end);
            this.txtMQLResult.requestFocusInWindow();
        }
    }

    public void addHighlight(JTextComponent textComp, String pattern) {
        try {
            Highlighter hl = textComp.getHighlighter();

            for(int i = 0; i < this.matches.size(); ++i) {
                hl.addHighlight(((Point)this.matches.get(i)).x, ((Point)this.matches.get(i)).y, new UnderlineHighlighter.UnderlineHighlightPainter(Color.red));
            }
        } catch (BadLocationException ex) {
            Util.handleException(ex);
        }

    }

    public void removeHighlights(JTextComponent textComp) {
        Highlighter hl = textComp.getHighlighter();
        Highlighter.Highlight[] hls = hl.getHighlights();

        for(Highlighter.Highlight hl1 : hls) {
            if (hl1.getPainter() instanceof UnderlineHighlighter.UnderlineHighlightPainter) {
                hl.removeHighlight(hl1);
            }
        }

    }

    public static void main(Context ctx, String command, int markA, int markE, boolean execute) {
        EventQueue.invokeLater(() -> (new MQL_Box(ctx, command, markA, markE, execute)).setVisible(true));
    }

    public void centerLineInScrollPane(JTextComponent component) {
        Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);
        if (container != null) {
            try {
                Rectangle r = component.modelToView(component.getCaretPosition());
                JViewport viewport = (JViewport)container;
                int extentHeight = viewport.getExtentSize().height;
                int viewHeight = viewport.getViewSize().height;
                int y = Math.max(0, r.y - extentHeight / 2);
                y = Math.min(y, viewHeight - extentHeight);
                viewport.setViewPosition(new Point(0, y));
                viewport.repaint();
            } catch (BadLocationException var8) {
            }

        }
    }

    class UndoHandler implements UndoableEditListener {
        public void undoableEditHappened(UndoableEditEvent e) {
            MQL_Box.this.undoManager.addEdit(e.getEdit());
            MQL_Box.this.undoAction.update();
            MQL_Box.this.redoAction.update();
        }
    }

    class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo");
            this.setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                MQL_Box.this.undoManager.undo();
            } catch (CannotUndoException var3) {
            }

            this.update();
            MQL_Box.this.redoAction.update();
        }

        protected void update() {
            if (MQL_Box.this.undoManager.canUndo()) {
                this.setEnabled(true);
                this.putValue("Name", MQL_Box.this.undoManager.getUndoPresentationName());
            } else {
                this.setEnabled(false);
                this.putValue("Name", "Undo");
            }

        }
    }

    class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo");
            this.setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                MQL_Box.this.undoManager.redo();
            } catch (CannotRedoException var3) {
            }

            this.update();
            MQL_Box.this.undoAction.update();
        }

        protected void update() {
            if (MQL_Box.this.undoManager.canRedo()) {
                this.setEnabled(true);
                this.putValue("Name", MQL_Box.this.undoManager.getRedoPresentationName());
            } else {
                this.setEnabled(false);
                this.putValue("Name", "Redo");
            }

        }
    }
}

