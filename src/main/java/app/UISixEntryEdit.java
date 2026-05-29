package app;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class UISixEntryEdit extends JFrame {

    private JTextField subjectField;
    private JTextPane entryPane;
    private JButton boldBtn, italicBtn, underlineBtn, strikeBtn;
    private JButton indentBtn, hangingIndentBtn;
    private JButton alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn;
    private JButton undoBtn, redoBtn;
    private JButton saveButton, discardButton;
    private boolean hangingMode = false;
    private Runnable onReturn;
    private String originalSubject;
    private String userFolderPath;
    private String username;
    private Image backgroundImage;

    private UndoManager undoManager = new UndoManager();

    /* Function:
    Wires up everything in order: frame setup, components, layout, event listeners, then load existing data.
    originalSubject is empty string when creating a new entry.
     */
    public UISixEntryEdit(String subject, String userFolderPath, String username, Runnable onReturn) {
        this.onReturn        = onReturn;
        this.originalSubject = subject;
        this.userFolderPath  = userFolderPath;
        this.username        = username;
        initializeFrame();
        initializeComponents();
        buildLayout();
        registerListeners();
        loadExistingEntry();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Function: initializeFrame | UISixEntryEdit
    // Sets window properties and loads the background image.
    // The background is painted via a custom JPanel set as the content pane.
    private void initializeFrame() {
        setTitle("JEntries");
        setSize(800, 700);
        setMinimumSize(new Dimension(800, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) setIconImage(appIcon.getImage());
        ImageIcon bg = loadIcon("JEntriesUIEditBackground.png");
        if (bg != null) backgroundImage = bg.getImage();

        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        bgPanel.setOpaque(false);
        setContentPane(bgPanel);
    }

    /* Function:
    Creates all UI controls. entryPane uses a custom WrapEditorKit so text wraps at the viewport
    edge instead of scrolling horizontally. Undo history is attached to the document here.
    */
    private void initializeComponents() {
        subjectField = new JTextField();
        subjectField.setPreferredSize(new Dimension(200, 40));

        entryPane = new JTextPane() {
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
            @Override public void setSize(Dimension d) {
                if (d.width < getParent().getSize().width) d.width = getParent().getSize().width;
                super.setSize(d);
            }
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = getParent() != null ? getParent().getWidth() : d.width;
                return d;
            }
        };
        entryPane.setEditorKit(new WrapEditorKit());
        entryPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        // Toolbar buttons — load icons from classpath, fall back to text labels if missing
        boldBtn          = createIconButton("icon/JEntriesToolsBoldIcon.png",               "B");
        italicBtn        = createIconButton("icon/JEntriesToolsItalicIcon.png",             "I");
        underlineBtn     = createIconButton("icon/JEntriesToolsUnderlineIcon.png",          "U");
        strikeBtn        = createIconButton("icon/JEntriesToolsStrikethroughIcon.png",      "S");
        indentBtn        = createIconButton("icon/JEntriesToolsIndentFirstLineIcon.png",    ">");
        hangingIndentBtn = createIconButton("icon/JEntriesToolsIndentHangingLineIcon.png",  "HI");
        alignLeftBtn     = createIconButton("icon/JEntriesToolsAlignTextLeftIcon.png",      "L");
        alignCenterBtn   = createIconButton("icon/JEntriesToolsAlignTextCenterIcon.png",    "C");
        alignRightBtn    = createIconButton("icon/JEntriesToolsAlignTextRightIcon.png",     "R");
        alignJustifyBtn  = createIconButton("icon/JEntriesToolsAlignTextJustifiedIcon.png", "J");
        undoBtn          = createIconButton("icon/JEntriesToolsUndoIcon.png",               "↩");
        redoBtn          = createIconButton("icon/JEntriesToolsRedoIcon.png",               "↪");
        saveButton       = new JButton("SAVE");
        discardButton    = new JButton("DISCARD");
    }

    /* Function: buildLayout | UISixEntryEdit
    Lays out the subject field at top, scrollable editor in center,
    and a toolbar + save/discard buttons at the bottom.
    */

    private void buildLayout() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        JLabel subjectLabel = new JLabel("SUBJECT:");
        subjectLabel.setFont(new Font("Arial", Font.BOLD, 12));
        subjectLabel.setForeground(Color.WHITE);
        topPanel.add(subjectLabel,  BorderLayout.WEST);
        topPanel.add(subjectField,  BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(entryPane);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(boldBtn);   toolBar.add(italicBtn); toolBar.add(underlineBtn); toolBar.add(strikeBtn);
        toolBar.add(indentBtn); toolBar.add(hangingIndentBtn);
        toolBar.addSeparator();
        toolBar.add(alignLeftBtn); toolBar.add(alignCenterBtn); toolBar.add(alignRightBtn); toolBar.add(alignJustifyBtn);
        toolBar.addSeparator();
        toolBar.add(undoBtn); toolBar.add(redoBtn);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(discardButton);
        buttonPanel.add(saveButton);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);
        bottomContainer.add(toolBar,      BorderLayout.NORTH);
        bottomContainer.add(buttonPanel,  BorderLayout.SOUTH);
        panel.add(bottomContainer, BorderLayout.SOUTH);

        add(panel);
    }


    // Le wire toolbar buttons to their formatting methods and registers, also used for the Ctrl+Z / Ctrl+Y shortcuts stuff.
    private void registerListeners() {
        boldBtn.addActionListener(e         -> toggleStyle("bold"));
        italicBtn.addActionListener(e       -> toggleStyle("italic"));
        underlineBtn.addActionListener(e    -> toggleStyle("underline"));
        strikeBtn.addActionListener(e       -> toggleStyle("strike"));
        indentBtn.addActionListener(e       -> addIndent());
        hangingIndentBtn.addActionListener(e -> toggleHangingIndent());
        alignLeftBtn.addActionListener(e    -> setAlignment(StyleConstants.ALIGN_LEFT));
        alignCenterBtn.addActionListener(e  -> setAlignment(StyleConstants.ALIGN_CENTER));
        alignRightBtn.addActionListener(e   -> setAlignment(StyleConstants.ALIGN_RIGHT));
        alignJustifyBtn.addActionListener(e -> setAlignment(StyleConstants.ALIGN_JUSTIFIED));
        undoBtn.addActionListener(e -> { if (undoManager.canUndo()) undoManager.undo(); });
        redoBtn.addActionListener(e -> { if (undoManager.canRedo()) undoManager.redo(); });
        saveButton.addActionListener(e    -> confirmAndSave());
        discardButton.addActionListener(e -> confirmDiscard());

        // Keyboard shortcuts for undo/redo
        entryPane.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
        entryPane.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo");
        entryPane.getActionMap().put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (undoManager.canUndo()) undoManager.undo(); }
        });
        entryPane.getActionMap().put("redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (undoManager.canRedo()) undoManager.redo(); }
        });
    }

    // Applies a paragraph alignment to the selected text, or to the current paragraph if nothing is selected.
    private void setAlignment(int alignment) {
        StyledDocument doc = entryPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrs, alignment);
        int start = entryPane.getSelectionStart();
        int end   = entryPane.getSelectionEnd();
        if (start == end) {

            // Nothing selected — apply to the whole paragraph the caret is in
            int pos = entryPane.getCaretPosition();
            Element para = doc.getParagraphElement(pos);
            start = para.getStartOffset();
            end   = para.getEndOffset();
        }
        doc.setParagraphAttributes(start, end - start, attrs, false);
    }


    // Shows a confirmation dialog before calling saveEntry(). The extra step stops accidental saves when the user meant to keep editing.
    private void confirmAndSave() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to save your entry?",
                "Confirm Save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) saveEntry();
    }

    // Prompts before discarding — clears the fields, closes the editor, and returns to the list.
    private void confirmDiscard() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to discard this entry?",
                "Confirm Discard", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            subjectField.setText("");
            entryPane.setText("");
            dispose();
            if (onReturn != null) onReturn.run();
        }
    }

    // Flips bold, italic, underline, or strikethrough on the current selection. Does nothing if no text is selected — applying formatting to a zero-width range has no visible effect.
    private void toggleStyle(String type) {
        int start = entryPane.getSelectionStart();
        int end   = entryPane.getSelectionEnd();
        if (start == end) return;

        StyledDocument doc = entryPane.getStyledDocument();
        AttributeSet as    = doc.getCharacterElement(start).getAttributes();
        SimpleAttributeSet style = new SimpleAttributeSet();
        switch (type) {
            case "bold":      StyleConstants.setBold(style,          !StyleConstants.isBold(as));          break;
            case "italic":    StyleConstants.setItalic(style,        !StyleConstants.isItalic(as));        break;
            case "underline": StyleConstants.setUnderline(style,     !StyleConstants.isUnderline(as));     break;
            case "strike":    StyleConstants.setStrikeThrough(style, !StyleConstants.isStrikeThrough(as)); break;
        }
        doc.setCharacterAttributes(start, end - start, style, false);
    }

    // Inserts a 5-space indent at the caret position. Simple tab substitute.
    private void addIndent() {
        try { entryPane.getDocument().insertString(entryPane.getCaretPosition(), "     ", null); }
        catch (BadLocationException e) { e.printStackTrace(); }
    }


    // Toggles hanging indent on the selected paragraphs: left indent 20pt, first line -20pt. Calling it again resets both back to 0, acting as a toggle.
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
                attrs, false);
    }


    /* validates subject, preserves the original created timestamp,
    serializes the RTF content into an XML file, and pushes it to Laktev's GDrive.
    If the subject changed, the old local file is deleted and Laktev's GDrive is told about the rename.
     */
    public void saveEntry() {
        String subject = subjectField.getText().trim();

        if (subject.length() > 80) {
            JOptionPane.showMessageDialog(this, "Subject character limit exceeded, please limit subject to less than 80 characters");
            return;
        }
        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subject cannot be empty!");
            return;
        }

        // Preserve the original created timestamp so editing doesn't reset it
        String createdTimestamp = null;
        File existingFile = new File(userFolderPath, originalSubject + ".xml");
        if (existingFile.exists()) {
            try {
                String xml = new String(Files.readAllBytes(existingFile.toPath()), "UTF-8");
                createdTimestamp = extract(xml, "<created>", "</created>");
            } catch (Exception e) { e.printStackTrace(); }
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy (hh:mm a)");
        String lastEdited = now.format(formatter);

        // New entry: use the current time as the created timestamp too
        if (createdTimestamp == null || createdTimestamp.isEmpty()) createdTimestamp = lastEdited;

        File dir = new File(userFolderPath);
        if (!dir.exists()) dir.mkdirs();
        String fileName = userFolderPath + File.separator + subject + ".xml";

        try {

            // Serialize the styled document to RTF bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RTFEditorKit rtfKit = new RTFEditorKit();
            rtfKit.write(out, entryPane.getDocument(), 0, entryPane.getDocument().getLength());
            String rtfContent = out.toString("UTF-8");

            // Write the XML wrapper around the RTF CDATA block
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<entry>\n");
            writer.write("    <subject>" + escapeXML(subject) + "</subject>\n");
            writer.write("    <created>" + createdTimestamp + "</created>\n");
            writer.write("    <lastEdited>" + lastEdited + "</lastEdited>\n");
            writer.write("    <content format=\"rtf\"><![CDATA[\n");
            writer.write(rtfContent);
            writer.write("\n]]></content>\n");
            writer.write("</entry>");
            writer.close();

            // If the subject was renamed, delete the old file and tell Laktev's GDrive about the rename
            boolean renamed = !originalSubject.isEmpty() && !originalSubject.equals(subject);
            if (renamed) {
                if (existingFile.exists()) existingFile.delete();
                DriveSync.renameEntry(username, originalSubject, subject);
            }

            // Push the saved (or newly renamed) file to Laktev's GDrive
            DriveSync.pushEntry(username, subject, userFolderPath);
            JOptionPane.showMessageDialog(this, "Entry Successfully Saved!");

            // Log whether this was a new entry or an edit
            String actionLabel = originalSubject.isEmpty() ? "Added entry" : "Edited entry";
            String actionTime  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy (hh:mm a)"));
            if (username != null) {
                XMLUserStorage.logActivity(username, null, null, actionLabel + " \"" + subject + "\" on " + actionTime);
            }

            subjectField.setText("");
            entryPane.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error Saving File!");
            return;
        }

        dispose();
        if (onReturn != null) onReturn.run();
    }


    // Escapes the three characters that would break XML inside element text.
    private String escapeXML(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Tries to load a toolbar button icon from the classpath.
    private JButton createIconButton(String iconPath, String fallbackText) {
        ImageIcon icon = loadIcon(iconPath);
        return icon != null ? new JButton(icon) : new JButton(fallbackText);
    }

    // Classpath image loader — returns null with a console warning if the file is missing.
    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) return new ImageIcon(resource);
        System.out.println("Icon not found: " + path);
        return null;
    }

    // Populates the editor with an existing entry's subject and RTF content.
    private void loadExistingEntry() {
        if (originalSubject == null || originalSubject.isEmpty()) return;
        File file = new File(userFolderPath, originalSubject + ".xml");
        if (!file.exists()) return;
        try {
            String xml     = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            String subject = extract(xml, "<subject>", "</subject>");
            int cdStart    = xml.indexOf("<![CDATA[") + 9;
            int cdEnd      = xml.indexOf("]]>", cdStart);
            String rtf     = xml.substring(cdStart, cdEnd);

            subjectField.setText(subject);
            RTFEditorKit rtfKit = new RTFEditorKit();
            StyledDocument doc  = new DefaultStyledDocument();
            rtfKit.read(new ByteArrayInputStream(rtf.getBytes("UTF-8")), doc, 0);

            // Clear history before attaching the listener — loading isn't an undoable user action
            undoManager.discardAllEdits();
            entryPane.setDocument(doc);
            doc.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Simple tag-based text extractor. Returns empty string if tags aren't found.
    private String extract(String xml, String startTag, String endTag) {
        int start = xml.indexOf(startTag);
        int end   = xml.indexOf(endTag);
        if (start == -1 || end == -1) return "";
        return xml.substring(start + startTag.length(), end);
    }

    @Override public void dispose() { super.dispose(); }
}

// Class: WrapEditorKit | UISixEntryEdit (companion class)
// Custom editor kit that overrides the view factory to prevent horizontal scrolling.
// Text wraps at the viewport width instead of pushing the pane wider.
class WrapEditorKit extends StyledEditorKit {
    ViewFactory defaultFactory = new WrapColumnFactory();
    @Override public ViewFactory getViewFactory() { return defaultFactory; }

    // Class: WrapColumnFactory | WrapEditorKit
    // Maps each document element type to the appropriate view, with a tweaked LabelView
    // that reports 0 minimum width so the text can always shrink to fit the viewport.
    static class WrapColumnFactory implements ViewFactory {
        @Override public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName))
                    return new LabelView(elem) {
                        @Override public float getMinimumSpan(int axis) {
                            return axis == View.X_AXIS ? 0 : super.getMinimumSpan(axis);
                        }
                    };
                else if (kind.equals(AbstractDocument.ParagraphElementName)) return new ParagraphView(elem);
                else if (kind.equals(AbstractDocument.SectionElementName))   return new BoxView(elem, View.Y_AXIS);
                else if (kind.equals(StyleConstants.ComponentElementName))   return new ComponentView(elem);
                else if (kind.equals(StyleConstants.IconElementName))        return new IconView(elem);
            }
            return new LabelView(elem);
        }
    }
}