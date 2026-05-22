package app;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class UISixEntryEdit extends JFrame {

    //FUNCTION: stores the app UI parts and editor state.

    private JTextField subjectField;
    private JTextPane entryPane;
    private JButton boldBtn;
    private JButton italicBtn;
    private JButton underlineBtn;
    private JButton strikeBtn;
    private JButton bulletBtn;
    private JButton indentBtn;
    private JButton hangingIndentBtn;
    private JButton saveButton;
    private JButton discardButton;
    private boolean bulletMode = false;
    private boolean hangingMode = false;
    private Runnable onReturn;
    private String originalSubject;
    private String userFolderPath;

    //FUNCTION: starts the whole editor setup.
    public UISixEntryEdit(String subject, String userFolderPath, Runnable onReturn) {
        this.onReturn = onReturn;
        this.originalSubject = subject;
        this.userFolderPath = userFolderPath;
        initializeFrame();
        initializeComponents();
        buildLayout();
        registerListeners();
        setupBulletEnterBehavior();
        loadExistingEntry();
        setLocationRelativeTo(null);
        setVisible(true);
    }



    //FUNCTION: sets up the main app window and basic frame settings
    private void initializeFrame() {

        setTitle("JEntries");

        setSize(800, 700);
        setMinimumSize(new Dimension(800, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");

        if (appIcon != null) setIconImage(appIcon.getImage());
    }

    //FUNCTION: creates all buttons, text fields, and editor components

    private void initializeComponents() {

        subjectField = new JTextField();
        subjectField.setPreferredSize(new Dimension(200, 40));

        entryPane = new JTextPane();

        boldBtn = createIconButton("icon/JEntriesToolsBoldIcon.png", "B");
        italicBtn = createIconButton("icon/JEntriesToolsItalicIcon.png", "I");
        underlineBtn = createIconButton("icon/JEntriesToolsUnderlineIcon.png", "U");
        strikeBtn = createIconButton("icon/JEntriesToolsStrikethroughIcon.png", "S");
        bulletBtn = createIconButton("icon/JEntriesToolsBulletListIcon.png", "•");
        indentBtn = createIconButton("icon/JEntriesToolsIndentFirstLineIcon.png", ">");
        hangingIndentBtn = createIconButton("icon/JEntriesToolsIndentHangingLineIcon.png", "HI");

        saveButton = new JButton("SAVE");
        discardButton = new JButton("DISCARD");
    }

    //FUNCTION: [LAYOUT] organizes the UI layout so everything looks clean and not cooked af

    private void buildLayout() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        //FUNCTION: [TOP PANEL] for entering the subject/title
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JLabel subjectLabel = new JLabel("SUBJECT:");

        topPanel.add(subjectLabel, BorderLayout.WEST);
        topPanel.add(subjectField, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        //FUNCTION: [CENTER PANEL] main typing area where the entry content goes
        JScrollPane scrollPane = new JScrollPane(entryPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        //FUNCTION: [TOOLBAR] toolbar (below the entryPane) for text styling and formatting controls
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(boldBtn);
        toolBar.add(italicBtn);
        toolBar.add(underlineBtn);
        toolBar.add(strikeBtn);
        toolBar.add(bulletBtn);
        toolBar.add(indentBtn);
        toolBar.add(hangingIndentBtn);

        //FUNCTION: [SAVE & DISCARD BUTTONS PANEL] panel that holds the save & discard buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(discardButton);
        buttonPanel.add(saveButton);

        //FUNCTION: [BOTTOM] combines the toolbar and save section together
        JPanel bottomContainer = new JPanel(new BorderLayout());

        bottomContainer.add(toolBar, BorderLayout.NORTH);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(bottomContainer, BorderLayout.SOUTH);
        add(panel);
    }

    //FUNCTION: [BUTTON LISTENERS] connects buttons to actions, basically what makes the UI actually do shit
    private void registerListeners() {

        boldBtn.addActionListener(e -> toggleStyle("bold"));
        italicBtn.addActionListener(e -> toggleStyle("italic"));
        underlineBtn.addActionListener(e -> toggleStyle("underline"));
        strikeBtn.addActionListener(e -> toggleStyle("strike"));
        bulletBtn.addActionListener(e -> toggleBulletMode());
        indentBtn.addActionListener(e -> addIndent());
        hangingIndentBtn.addActionListener(e -> toggleHangingIndent());
        saveButton.addActionListener(e -> confirmAndSave());
        discardButton.addActionListener(e -> confirmDiscard());
    }

    //FUNCTION: asks the user before saving so no accidental saves happen
    private void confirmAndSave() {
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to save your entry?", "Confirm Save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) saveEntry();

    }
    //FUNCTION: asks the user before discarding so no accidental discard happen
    private void confirmDiscard() {

        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to discard this entry?", "Confirm Discard", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            subjectField.setText("");
            entryPane.setText("");
            dispose();
            if (onReturn != null) {onReturn.run();}
        }
    }

    //----- BULLET LIST -----
    //FUNCTION: [BULLET LIST TOGGLE] toggles bullet list mode on or off
    private void toggleBulletMode() {

        bulletMode = !bulletMode;
        try {

            int pos = entryPane.getCaretPosition();

            if (bulletMode) {
                entryPane.getDocument().insertString(pos, "• ", null);
            } else {

                String text = entryPane.getText();
                int lineStart = Utilities.getRowStart(entryPane, pos);

                if (text.startsWith("• ", lineStart)) {
                    entryPane.getDocument().remove(lineStart, 2);
                }
            }

        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    //FUNCTION: [BULLET LIST ENTER BEHAVIOR] when user presses ENTER, it continues the bullet points automatically when bullet mode is active
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

    //FUNCTION: handles bold, italic, underline, and strikethrough formatting
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

    //FUNCTION: [FIRST LINE INDENT] adds spacing/indent into the text manually
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

    //FUNCTION: [HANGING LINE INDENT] applies hanging indent formatting to selected paragraphs
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

    //FUNCTION: saves the entry as XML with RTF formatting included. also some save system stuff
    public void saveEntry() {

        String subject = subjectField.getText().trim();

        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subject cannot be empty!");
            return;
        }

        String createdTimestamp = null;

        // check existing file first
        File existingFile = new File(userFolderPath, originalSubject + ".xml");

        if (existingFile.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(existingFile);

                createdTimestamp = doc.getElementsByTagName("created")
                        .item(0)
                        .getTextContent();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //FUNCTION: gets the current date and time for save tracking
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy (hh:mm a)");
        String lastEdited = now.format(formatter);

        if (createdTimestamp == null) {
            createdTimestamp = lastEdited; // first-time save fallback
        }

        //File & Folder path names
        File dir = new File(userFolderPath);
        if (!dir.exists()) dir.mkdirs();
        String fileName = userFolderPath + File.separator + subject + ".xml";

        try {
            //FUNCTION: converts the styled document into RTF format text
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RTFEditorKit rtfKit = new RTFEditorKit();

            rtfKit.write(out, entryPane.getDocument(), 0, entryPane.getDocument().getLength());

            String rtfContent = out.toString("UTF-8");

            //FUNCTION: writes all the entry data into the XML save file
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<entry>\n");
            writer.write("    <subject>" + escapeXML(subject) + "</subject>\n");
            writer.write("    <created>" + createdTimestamp  + "</created>\n");
            writer.write("    <lastEdited>" + lastEdited  + "</lastEdited>\n");
            writer.write("    <content format=\"rtf\"><![CDATA[\n");
            writer.write(rtfContent);
            writer.write("\n]]></content>\n");
            writer.write("</entry>");
            writer.close();

            JOptionPane.showMessageDialog(this, "Entry Successfully Saved!");

            subjectField.setText("");
            entryPane.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error Saving File!");
        }

        dispose();
        if (onReturn != null) {onReturn.run();}
    }

    //FUNCTION: prevents XML symbols from breaking the save file
    private String escapeXML(String text) {

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    //FUNCTION: creates toolbar buttons with icons, fallback to text if icons somehow fail
    private JButton createIconButton(String iconPath, String fallbackText) {

        ImageIcon icon = loadIcon(iconPath);

        if (icon != null) return new JButton(icon);
        return new JButton(fallbackText);
    }

    //FUNCTION: loads image icons from the resources folder
    private ImageIcon loadIcon(String path) {

        java.net.URL resource = getClass().getClassLoader().getResource(path);

        if (resource != null) {
            return new ImageIcon(resource);
        }

        System.out.println("Icon not found: " + path);
        return null;
    }

    private void loadExistingEntry() {
        File file = new File(userFolderPath,originalSubject + ".xml");

        if (!file.exists()) return;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            String subject = doc.getElementsByTagName("subject").item(0).getTextContent();
            String content = doc.getElementsByTagName("content").item(0).getTextContent();

            subjectField.setText(subject);

            // IMPORTANT: reuse existing JTextPane (do NOT recreate it)
            RTFEditorKit rtfKit = new RTFEditorKit();
            entryPane.setEditorKit(rtfKit);

            ByteArrayInputStream in =
                    new ByteArrayInputStream(content.getBytes("UTF-8"));

            entryPane.setDocument(entryPane.getStyledDocument()); // reset safely
            rtfKit.read(in, entryPane.getDocument(), 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FUNCTION: safely closes the editor window and clears the frame
    @Override
    public void dispose() {
        super.dispose();
    }


}