package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

/*FUNCTION:
read-only entry viewer screen — shows a single journal entry (subject, timestamps, and RTF content).
also handles sharing of entries to other users via file copy + <sharedBy> tagging.
*/
public class UIFiveEntryViewer extends JFrame {

    private JTextPane contentPane;
    private JTextField subjectField;
    private JTextField timestampField;
    private Runnable onReturn;
    private String userFolderPath;
    private String entryFileName;
    private String currentUsername;

    public UIFiveEntryViewer(String entryFileName, String userFolderPath, String currentUsername, Runnable onReturn) {
        this.entryFileName   = entryFileName;
        this.currentUsername = currentUsername;
        this.onReturn        = onReturn;
        this.userFolderPath  = userFolderPath;

        initializeFrame();
        initializeComponents();
        buildLayout();
        loadEntry(entryFileName);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /*FUNCTION:
    sets up frame basics — title, size, resize behavior, and app icon.
    title gets updated later once entry is loaded.
    */
    private void initializeFrame() {
        setTitle("JEntries");
        setSize(800, 700);
        setMinimumSize(new Dimension(800, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) { setIconImage(appIcon.getImage()); }
    }

    /*FUNCTION:
    initializes UI components — subject, timestamp, and content pane (all read-only).
    */
    private void initializeComponents() {
        subjectField = new JTextField();
        subjectField.setEditable(false);
        subjectField.setFocusable(false);
        subjectField.setHighlighter(null);
        subjectField.setBorder(BorderFactory.createTitledBorder("SUBJECT"));

        timestampField = new JTextField();
        timestampField.setEditable(false);
        timestampField.setFocusable(false);
        timestampField.setHighlighter(null);
        timestampField.setBorder(BorderFactory.createTitledBorder("TIMESTAMP"));

        contentPane = new JTextPane();
        contentPane.setEditable(false);
        contentPane.setMargin(new Insets(10, 10, 10, 10));
    }

    /*FUNCTION:
    shares entry to another user by uploading the XML (with <sharedBy> tag) directly to
    their Google Drive folder via DriveSync. No local folder needed on the sender's machine —
    this works cross-computer because it goes through Drive, not the local filesystem.
    */
    private void shareEntryToUser(String targetUser) {
        DriveSync.shareEntry(currentUsername, targetUser, entryFileName, userFolderPath);
    }

    /*FUNCTION:
    builds UI layout — top (subject/timestamp), center (RTF content), bottom (back + share buttons).
    */
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

        /*FUNCTION:
        back button — closes viewer and returns to previous entry list screen.
        */
        backButton.addActionListener(e -> {
            dispose();
            if (onReturn != null) { onReturn.run(); }
        });

        JButton shareButton = new JButton("SEND TO OTHER USERS");

        /*FUNCTION:
        share entry dialog — shows list of all registered users (from users.xml, not local disk)
        and allows sending entry to a selected account or broadcasting to all users except current user.
        Works cross-computer because the user list comes from Drive-synced users.xml.
        */
        shareButton.addActionListener(e -> {
            // Refresh users.xml from Drive before reading so the list is always up to date
            DriveSync.initAndPullUsersXml();

            JDialog dialog = new JDialog(this, "Share Entry", true);
            JPanel panel   = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            Dimension buttonSize = new Dimension(200, 35);

            JLabel titleLabel = new JLabel("Who do you want to send this to?");
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
            panel.add(titleLabel);

            // Pull user list from users.xml (Drive-synced) — not the local filesystem
            java.util.List<String> allUsers = XMLUserStorage.getAllUsernames();
            java.util.List<String> otherUsers = new java.util.ArrayList<>();
            for (String name : allUsers) {
                if (!name.equalsIgnoreCase(currentUsername)) otherUsers.add(name);
            }

            if (otherUsers.isEmpty()) {
                JLabel noUsers = new JLabel("No other users found.");
                noUsers.setAlignmentX(Component.CENTER_ALIGNMENT);
                noUsers.setBorder(new EmptyBorder(6, 0, 6, 0));
                panel.add(noUsers);
            } else {
                for (String userName : otherUsers) {
                    JButton userButton = new JButton(userName);
                    userButton.setMaximumSize(buttonSize);
                    userButton.setPreferredSize(buttonSize);
                    userButton.setMinimumSize(buttonSize);
                    userButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                    userButton.addActionListener(x -> {
                        shareEntryToUser(userName);
                        JOptionPane.showMessageDialog(dialog,
                                "Entry sent to " + userName + ".\nThey will see it on next login.");
                        dialog.dispose();
                    });

                    panel.add(userButton);
                    panel.add(Box.createVerticalStrut(6));
                }
                panel.add(Box.createVerticalStrut(10));

                JButton allButton = new JButton("SHARE TO ALL");
                allButton.setMaximumSize(buttonSize);
                allButton.setPreferredSize(buttonSize);
                allButton.setMinimumSize(buttonSize);
                allButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                allButton.addActionListener(x -> {
                    for (String name : otherUsers) shareEntryToUser(name);
                    JOptionPane.showMessageDialog(dialog,
                            "Entry sent to all users.\nThey will see it on next login.");
                    dialog.dispose();
                });

                panel.add(allButton);
            }

            dialog.add(new JScrollPane(panel));
            dialog.setSize(300, 220);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel leftPanel   = new JPanel(new FlowLayout(FlowLayout.LEFT,  0, 0));
        JPanel rightPanel  = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        leftPanel.add(backButton);
        rightPanel.add(Box.createHorizontalStrut(20));
        rightPanel.add(shareButton);

        buttonPanel.add(leftPanel,  BorderLayout.WEST);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        mainPanel.add(topPanel,    BorderLayout.NORTH);
        mainPanel.add(scrollPane,  BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    /*FUNCTION:
    loads entry XML, extracts subject/timestamps/content, then renders RTF into text pane.
    also updates window title based on entry subject.
    */
    private void loadEntry(String entryFileName) {
        File file = new File(userFolderPath, entryFileName + ".xml");

        try {
            String xml      = Files.readString(file.toPath());
            String subject  = extract(xml, "<subject>",    "</subject>");
            String created  = extract(xml, "<created>",    "</created>");
            String lastEdited = extract(xml, "<lastEdited>", "</lastEdited>");

            setTitle("JEntries - " + subject);

            int start   = xml.indexOf("<content");
            int cdStart = xml.indexOf("<![CDATA[", start) + 9;
            int cdEnd   = xml.indexOf("]]>", cdStart);
            String rtf  = xml.substring(cdStart, cdEnd);

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

    /*FUNCTION:
    extracts value between XML tags using simple string parsing.
    returns "Unknown" if tags are missing instead of crashing.
    */
    private String extract(String xml, String startTag, String endTag) {
        int start = xml.indexOf(startTag);
        int end   = xml.indexOf(endTag);
        if (start == -1 || end == -1) { return "Unknown"; }
        return xml.substring(start + startTag.length(), end);
    }

    /*FUNCTION:
    loads image icon from classpath resources.
    */
    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) { return new ImageIcon(resource); }
        System.out.println("Icon not found: " + path);
        return null;
    }
}