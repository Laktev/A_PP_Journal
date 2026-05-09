package app;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayOutputStream;
//import java.io.ByteArrayInputStream; to be used for reloading a xml file
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UISixEntryEdit {

    private JFrame frame;
    private JTextField subjectField;
    private JTextPane entryPane;
    private boolean bulletMode = false;
    private boolean hangingMode = false;

    public UISixEntryEdit() {

        frame = new JFrame("JEntries");
        frame.setSize(800, 700);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");

        if (appIcon != null) frame.setIconImage(appIcon.getImage());

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JLabel subjectLabel = new JLabel("SUBJECT:");

        subjectField = new JTextField();
        subjectField.setPreferredSize(new Dimension(200, 40));

        topPanel.add(subjectLabel, BorderLayout.WEST);
        topPanel.add(subjectField, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        entryPane = new JTextPane();
        JScrollPane scrollPane = new JScrollPane(entryPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        setupBulletEnterBehavior();

        JPanel bottomContainer = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton boldBtn = createIconButton("icon/JEntriesToolsBoldIcon.png", "B");
        JButton italicBtn = createIconButton("icon/JEntriesToolsItalicIcon.png", "I");
        JButton underlineBtn = createIconButton("icon/JEntriesToolsUnderlineIcon.png", "U");
        JButton strikeBtn = createIconButton("icon/JEntriesToolsStrikethroughIcon.png", "S");
        JButton bulletBtn = createIconButton("icon/JEntriesToolsBulletListIcon.png", "•");
        JButton indentBtn = createIconButton("icon/JEntriesToolsIndentFirstLineIcon.png", ">");
        JButton hangingIndentBtn = createIconButton("icon/JEntriesToolsIndentHangingLineIcon.png", "HI");

        toolBar.add(boldBtn);
        toolBar.add(italicBtn);
        toolBar.add(underlineBtn);
        toolBar.add(strikeBtn);
        toolBar.add(bulletBtn);
        toolBar.add(indentBtn);
        toolBar.add(hangingIndentBtn);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("SAVE");
        buttonPanel.add(saveButton);

        bottomContainer.add(toolBar, BorderLayout.NORTH);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(bottomContainer, BorderLayout.SOUTH);

        boldBtn.addActionListener(e -> toggleStyle("bold"));
        italicBtn.addActionListener(e -> toggleStyle("italic"));
        underlineBtn.addActionListener(e -> toggleStyle("underline"));
        strikeBtn.addActionListener(e -> toggleStyle("strike"));

        bulletBtn.addActionListener(e -> {

            bulletMode = !bulletMode;

            try {

                int pos = entryPane.getCaretPosition();

                if (bulletMode) {
                    entryPane.getDocument().insertString(pos, "• ", null);
                } else {

                    String text = entryPane.getText();
                    int lineStart = javax.swing.text.Utilities.getRowStart(entryPane, pos);

                    if (text.startsWith("• ", lineStart)) {
                        entryPane.getDocument().remove(lineStart, 2);
                    }
                }

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });

        indentBtn.addActionListener(e -> addIndent());
        hangingIndentBtn.addActionListener(e -> toggleHangingIndent());

        saveButton.addActionListener(e -> {

            int result = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to save your entry?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                saveEntry();
            }
        });

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ================= FIX ADDED HERE =================
    private JButton createIconButton(String iconPath, String fallbackText) {

        ImageIcon icon = loadIcon(iconPath);

        if (icon != null) {
            return new JButton(icon);
        }

        return new JButton(fallbackText);
    }

    // ================= ICON LOADER =================
    private ImageIcon loadIcon(String path) {

        java.net.URL resource = getClass().getClassLoader().getResource(path);

        if (resource != null) return new ImageIcon(resource);

        System.out.println("Icon not found: " + path);

        return null;
    }

    // ================= ENTER HANDLING =================
    private void setupBulletEnterBehavior() {

        InputMap inputMap = entryPane.getInputMap();
        ActionMap actionMap = entryPane.getActionMap();

        KeyStroke enter = KeyStroke.getKeyStroke("ENTER");

        inputMap.put(enter, "insert-break");

        actionMap.put("insert-break", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                try {

                    int pos = entryPane.getCaretPosition();

                    if (bulletMode) {
                        entryPane.getDocument().insertString(pos, "\n• ", null);
                    } else {
                        entryPane.getDocument().insertString(pos, "\n", null);
                    }

                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // ================= STYLE =================
    private void toggleStyle(String type) {

        int start = entryPane.getSelectionStart();
        int end = entryPane.getSelectionEnd();

        if (start == end) return;

        StyledDocument doc = entryPane.getStyledDocument();
        Element element = doc.getCharacterElement(start);
        AttributeSet as = element.getAttributes();

        boolean active;
        SimpleAttributeSet style = new SimpleAttributeSet();

        switch (type) {

            case "bold":
                active = StyleConstants.isBold(as);
                StyleConstants.setBold(style, !active);
                break;

            case "italic":
                active = StyleConstants.isItalic(as);
                StyleConstants.setItalic(style, !active);
                break;

            case "underline":
                active = StyleConstants.isUnderline(as);
                StyleConstants.setUnderline(style, !active);
                break;

            case "strike":
                active = StyleConstants.isStrikeThrough(as);
                StyleConstants.setStrikeThrough(style, !active);
                break;
        }

        doc.setCharacterAttributes(start, end - start, style, false);
    }

    private void addIndent() {
        try {
            entryPane.getDocument().insertString(
                    entryPane.getCaretPosition(),
                    "     ",
                    null
            );
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void toggleHangingIndent() {

        StyledDocument doc = entryPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();

        if (!hangingMode) {
            StyleConstants.setLeftIndent(attrs, 20f);
            StyleConstants.setFirstLineIndent(attrs, -20f);
            hangingMode = true;
        } else {
            StyleConstants.setLeftIndent(attrs, 0f);
            StyleConstants.setFirstLineIndent(attrs, 0f);
            hangingMode = false;
        }

        doc.setParagraphAttributes(
                entryPane.getSelectionStart(),
                entryPane.getSelectionEnd() - entryPane.getSelectionStart(),
                attrs,
                false
        );
    }

    public void saveEntry() {

        String subject = subjectField.getText().trim();

        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Subject cannot be empty!");
            return;
        }

        File dir = new File("Documents");
        if (!dir.exists()) dir.mkdirs();

        String fileName = "Documents/" + subject + ".xml";

        try {

            // ================= TIMESTAMP =================
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String timestamp = now.format(formatter);

            // ================= CONVERT DOCUMENT TO RTF =================
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RTFEditorKit rtfKit = new RTFEditorKit();

            rtfKit.write(out, entryPane.getDocument(), 0, entryPane.getDocument().getLength());

            String rtfContent = out.toString("UTF-8");

            // ================= WRITE XML =================
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<entry>\n");

            writer.write("    <subject>" + escapeXML(subject) + "</subject>\n");
            writer.write("    <created>" + timestamp + "</created>\n");
            writer.write("    <lastEdited>" + timestamp + "</lastEdited>\n");

            writer.write("    <content format=\"rtf\"><![CDATA[\n");
            writer.write(rtfContent);
            writer.write("\n]]></content>\n");

            writer.write("</entry>");

            writer.close();

            JOptionPane.showMessageDialog(frame, "Entry Successfully Saved!");

            subjectField.setText("");
            entryPane.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error Saving File!");
        }
    }

    private String escapeXML(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UISixEntryEdit::new);
    }
}