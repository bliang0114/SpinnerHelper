package cn.github.spinner.ui;

import net.sourceforge.plantuml.SourceStringReader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantUMLDiagramViewer extends JFrame {
    private JLabel imageLabel;
    private JScrollPane imageScrollPane;
    private BufferedImage currentImage;
    private BufferedImage originalImage;
    private String currentUMLSource;
    private double scale = 1.0;
    private Point dragStartPoint = new Point(0, 0);
    private Point viewStartPosition = new Point(0, 0);

    public PlantUMLDiagramViewer() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("PlantUML类图查看器");
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // 生成示例图表
        generateExampleDiagram();
    }

    private void initializeComponents() {
        // 显示图像的标签
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);

        // 图像滚动面板
        imageScrollPane = new JScrollPane(imageLabel);
        imageScrollPane.setPreferredSize(new Dimension(900, 500));
        imageScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        imageScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // 添加鼠标滚轮缩放支持
        imageScrollPane.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                // Ctrl+滚轮进行缩放
                int wheelRotation = e.getWheelRotation();
                if (wheelRotation < 0) {
                    zoomImage(1.1);
                } else {
                    zoomImage(0.9);
                }
                e.consume();
            }
        });

        // 添加鼠标拖拽支持
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
                viewStartPosition = new Point(
                        imageScrollPane.getHorizontalScrollBar().getValue(),
                        imageScrollPane.getVerticalScrollBar().getValue()
                );
                imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                imageLabel.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // 双击复制类名
                if (e.getClickCount() == 2) {
                    copyClassNameAtPosition(e.getPoint());
                }
            }
        });

        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    int deltaX = dragStartPoint.x - e.getX();
                    int deltaY = dragStartPoint.y - e.getY();

                    imageScrollPane.getHorizontalScrollBar().setValue(viewStartPosition.x + deltaX);
                    imageScrollPane.getVerticalScrollBar().setValue(viewStartPosition.y + deltaY);
                }
            }
        });
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 顶部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton generateButton = new JButton("生成示例类图");
        JButton downloadButton = new JButton("下载图表");

        buttonPanel.add(generateButton);
        buttonPanel.add(downloadButton);

        add(buttonPanel, BorderLayout.NORTH);

        // 中部面板：图像显示（最大化显示区域）
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("类图展示 (使用 Ctrl+鼠标滚轮 缩放，鼠标拖拽移动，双击复制类名)"));
        centerPanel.add(imageScrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 设置按钮事件
        generateButton.addActionListener(new GenerateButtonListener());
        downloadButton.addActionListener(new DownloadButtonListener());
    }

    private void setupEventHandlers() {
        // 可以添加其他事件处理
    }

    private class GenerateButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            generateExampleDiagram();
        }
    }

    private class DownloadButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadDiagram();
        }
    }

    private void generateExampleDiagram() {
        // 示例PlantUML类图代码
        String example = "@startuml\n" +
                "left to right direction\n" +
                "class ECN as \"设计缺陷变更单\" <<CUS_ENGChangeAction>>\n" +
                "class PL as \"产品线\" <<Product Line>>\n" +
                "class Item1 as \"点检项\" <<CUS_CAPartProcessItemInstance>>\n" +
                "class Item2 as \"点检项\" <<CUS_CAPartProcessItemInstance>>\n" +
                "class Item3 as \"点检项\" <<CUS_CAPartProcessItemInstance>>\n" +
                "class Item4 as \"点检项\" <<CUS_CAPartProcessItemInstance>>\n" +
                "class CT as \"变更类型\" <<CUS_ChangeTypeInfo>>\n" +
                "class PA as \"变更前\" <<Proposed Activity>>\n" +
                "class RA as \"变更后\" <<Realized Activity>>\n" +
                "class P as \"通知用户\" <<Person>>\n" +
                "class CRMM as \"会议纪要\" <<CUS_ChangeReviewMeetingMinutes>>\n" +
                "class R_Project as \"项目点检\" << (R,#FF7700) CUS_Checklist_Configuration_rel>> {\n" +
                "CUS_ChecklistType=Project\n" +
                "}\n" +
                "class R_Software as \"软件点检\" << (R,#FF7700) CUS_Checklist_Configuration_rel>> {\n" +
                "CUS_ChecklistType=Software\n" +
                "}\n" +
                "class R_Hardware as \"硬件点检\" << (R,#FF7700) CUS_Checklist_Configuration_rel>> {\n" +
                "CUS_ChecklistType=Hardware\n" +
                "}\n" +
                "class R_Structure as \"结构点检\" << (R,#FF7700) CUS_Checklist_Configuration_rel>> {\n" +
                "CUS_ChecklistType=Structure\n" +
                "}\n" +
                "ECN --> R_Project\n" +
                "R_Project --> Item1\n" +
                "ECN --> R_Software \n" +
                "R_Software  --> Item2\n" +
                "ECN --> R_Hardware \n" +
                "R_Hardware --> Item3\n" +
                "ECN --> R_Structure \n" +
                "R_Structure --> Item4\n" +
                "ECN --> PL :CUS_RelationToProudctLine\n" +
                "ECN --> PA :Proposed Activities\n" +
                "ECN --> RA :Realized Activities\n" +
                "ECN --> CT :CUS_DesignChangeAction_Type_Rel\n" +
                "ECN --> P :CUS_DesignChangeAction_Grp_Rel\n" +
                "ECN --> CRMM :CUS_ChangeAction_Doc_Rel\n" +
                "@enduml";

        generateDiagram(example);
    }

    private void generateDiagram(String plantUMLSource) {
        if (plantUMLSource == null || plantUMLSource.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "PlantUML代码为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.currentUMLSource = plantUMLSource;
        this.scale = 1.0; // 重置缩放比例

        try {
            // 使用PlantUML生成图像
            SourceStringReader reader = new SourceStringReader(plantUMLSource);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 生成图像并获取描述
            String desc = reader.generateImage(outputStream);

            // 将输出流转换为图像
            byte[] imageBytes = outputStream.toByteArray();
            originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes)); // 保存原始图像
            currentImage = originalImage; // 当前图像初始化为原始图像

            // 在标签中显示图像
            displayImage(currentImage);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "生成图表时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void displayImage(BufferedImage image) {
        if (image != null) {
            // 应用当前缩放比例，使用原始图像进行高质量缩放
            int scaledWidth = (int) (originalImage.getWidth() * scale);
            int scaledHeight = (int) (originalImage.getHeight() * scale);

            // 使用高质量缩放
            Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            BufferedImage bufferedScaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedScaledImage.createGraphics();

            // 设置高质量渲染提示
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            ImageIcon icon = new ImageIcon(bufferedScaledImage);
            imageLabel.setIcon(icon);
            imageLabel.setText(""); // 清除之前的文本
            imageScrollPane.revalidate();
            imageScrollPane.repaint();
        }
    }

    private void zoomImage(double factor) {
        scale *= factor;
        // 限制缩放范围
        scale = Math.max(0.1, Math.min(scale, 5.0));

        if (originalImage != null) {
            displayImage(originalImage); // 始终使用原始图像进行缩放
        }
    }

    // 复制鼠标位置处的类名
    private void copyClassNameAtPosition(Point point) {
        if (currentUMLSource == null) return;

        try {
            // 由于我们无法直接从图像中识别点击位置对应的类名，
            // 我们提供一个简单的解决方案：显示所有类名供用户选择

            // 提取所有类名
            java.util.List<String> classNames = extractClassNames(currentUMLSource);

            if (classNames.isEmpty()) {
                JOptionPane.showMessageDialog(this, "未找到类名", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 显示选择对话框
            String selectedClass = (String) JOptionPane.showInputDialog(
                    this,
                    "选择要复制的类名:",
                    "复制类名",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    classNames.toArray(),
                    classNames.get(0)
            );

            if (selectedClass != null) {
                // 复制到剪贴板
                copyToClipboard(selectedClass);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "复制类名时出错: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 从PlantUML源码中提取类名
    private java.util.List<String> extractClassNames(String umlSource) {
        java.util.List<String> classNames = new java.util.ArrayList<>();

        // 匹配 class ClassName { 或 class ClassName extends ... {
        Pattern classPattern = Pattern.compile("class\\s+([\\w]+)");
        Matcher matcher = classPattern.matcher(umlSource);

        while (matcher.find()) {
            classNames.add(matcher.group(1));
        }

        return classNames;
    }

    // 复制文本到剪贴板
    private void copyToClipboard(String text) {
        java.awt.datatransfer.StringSelection stringSelection =
                new java.awt.datatransfer.StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(stringSelection, null);
    }

    private void downloadDiagram() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, "没有可下载的图表", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 创建文件选择器
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存类图");
        fileChooser.setSelectedFile(new File("class_diagram"));

        // 设置文件过滤器
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".png") ||
                        f.getName().toLowerCase().endsWith(".uml");
            }

            @Override
            public String getDescription() {
                return "PNG 图像 (*.png) 或 PlantUML 源文件 (*.uml)";
            }
        });

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String fileName = fileToSave.getAbsolutePath();

            // 根据选择的文件类型保存
            if (fileChooser.getFileFilter().getDescription().contains("PNG")) {
                // 如果用户选择了PNG过滤器，确保文件扩展名为.png
                if (!fileName.endsWith(".png") && !fileName.endsWith(".uml")) {
                    fileName += ".png";
                }
            }

            try {
                if (fileName.endsWith(".png")) {
                    // 保存为PNG图像，使用原始图像以保证质量
                    ImageIO.write(originalImage, "png", new File(fileName));
                    JOptionPane.showMessageDialog(this, "类图已保存为PNG: " + fileName,
                            "保存成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // 保存为UML源文件
                    if (!fileName.endsWith(".uml")) {
                        fileName += ".uml";
                    }
                    try (FileWriter writer = new FileWriter(fileName)) {
                        writer.write(currentUMLSource);
                    }
                    JOptionPane.showMessageDialog(this, "PlantUML源码已保存为: " + fileName,
                            "保存成功", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存文件时出错: " + ex.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public String generateClass(String classType){
        return String.format("class %s <<%s>>", classType, classType);
    }

    public String generateRelationshipClass(String relType){
        return String.format("class %s <<(R,#FF7700) %s>>", relType, relType);
    }

    public String generateConnection(String from, String rel, String to){
        String fromRel = String.format("%s --> %s :from", from, rel);
        String toRel = String.format("%s --> %s :to", rel, to);
        return fromRel + "\n" + toRel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PlantUMLDiagramViewer().setVisible(true);
        });
    }


}

