package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

public class UIFiveEntryViewer extends JFrame {
    private JTextPane contentPane;
    private JTextField subjectField;
    private JTextField timestampField;
    private Runnable onReturn;

    public UIFiveEntryViewer(String entryFileName, Runnable onReturn) {

        this.onReturn = onReturn;

        initializeFrame();
        initializeComponents();
        buildLayout();
        loadEntry(entryFileName);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeFrame() {

        setTitle("JEntries");
        setSize(800, 700);
        setMinimumSize(new Dimension(800, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) {setIconImage(appIcon.getImage());}
    }

    private void initializeComponents() {
        subjectField = new JTextField();
        subjectField.setEditable(false);
        subjectField.setFocusable(false);
        subjectField.setHighlighter(null);
        subjectField.setBorder(BorderFactory.createTitledBorder("SUBJECT"));
        subjectField.getFont().deriveFont(Font.BOLD, 50f);

        timestampField = new JTextField();
        timestampField.setEditable(false);
        timestampField.setFocusable(false);
        timestampField.setHighlighter(null);
        timestampField.setBorder(BorderFactory.createTitledBorder("TIMESTAMP"));
        timestampField.getFont().deriveFont(Font.PLAIN, 50f);

        contentPane = new JTextPane();
        contentPane.setEditable(false);
        contentPane.setMargin(new Insets(10, 10, 10, 10));
    }

    private void buildLayout() {

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        subjectField.setAlignmentX(Component.LEFT_ALIGNMENT);
        timestampField.setAlignmentX(Component.LEFT_ALIGNMENT);

        topPanel.add(subjectField);

        topPanel.add(Box.createVerticalStrut(10));

        topPanel.add(timestampField);
        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBorder(new LineBorder(Color.BLACK, 1));
        JButton backButton = new JButton("<");

        backButton.addActionListener(e -> {
            dispose();
            if (onReturn != null) {
                onReturn.run();
            }
        });

        JButton exportPDFButton = new JButton("EXPORT TO PDF");

        // FUNCTIONALITY LEFT BLANK
        exportPDFButton.addActionListener(e -> {

        });

        JButton printButton = new JButton("SEND TO OTHER USERS");

        printButton.addActionListener(e -> {

        });

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        leftPanel.add(backButton);
        rightPanel.add(exportPDFButton);
        rightPanel.add(Box.createHorizontalStrut(20));
        rightPanel.add(printButton);
        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void loadEntry(String entryFileName) {

        File file = new File("./Documents/" + entryFileName + ".xml");

        try {

            String xml = Files.readString(file.toPath());
            String subject = extract(xml, "<subject>", "</subject>");
            String created = extract(xml, "<created>", "</created>");
            String lastEdited = extract(xml, "<lastEdited>", "</lastEdited>");

            int start = xml.indexOf("<content");
            int cdStart = xml.indexOf("<![CDATA[", start) + 9;
            int cdEnd = xml.indexOf("]]>", cdStart);
            String rtf = xml.substring(cdStart, cdEnd);

            RTFEditorKit kit = new RTFEditorKit();
            StyledDocument doc = new DefaultStyledDocument();
            kit.read(new ByteArrayInputStream(rtf.getBytes()), doc, 0);

            subjectField.setText("   " + subject);
            timestampField.setText("   Created: " + created + "   |   " + "Last Edited: " + lastEdited);
            contentPane.setDocument(doc);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading entry:\n" + e.getMessage());
        }
    }

    private String extract(String xml, String startTag, String endTag) {

        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);

        if (start == -1 || end == -1) {
            return "Unknown";
        }

        return xml.substring(start + startTag.length(), end);
    }

    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);

        if (resource != null) {
            return new ImageIcon(resource);
        }

        System.out.println("Icon not found: " + path);
        return null;
    }
}