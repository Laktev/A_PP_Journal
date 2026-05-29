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
    shares entry to another user's folder by copying XML file and injecting/updating <sharedBy>.
    replaces existing tag if already shared (para clean lang, no duplicates).
    */
    private void shareEntryToUser(String targetUser) {
        try {
            File source      = new File(userFolderPath, entryFileName + ".xml");
            File targetFolder = new File("users/" + targetUser);
            String xml       = Files.readString(source.toPath());

            if (xml.contains("<sharedBy>")) {
                xml = xml.replaceAll("<sharedBy>.*?</sharedBy>", "<sharedBy>" + currentUsername + "</sharedBy>");
            } else {
                xml = xml.replace("</entry>", "<sharedBy>" + currentUsername + "</sharedBy>\n</entry>");
            }

            File destination = new File(targetFolder, entryFileName + ".xml");
            Files.writeString(destination.toPath(), xml);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        share entry dialog — shows list of users and allows sending entry to selected account
        or broadcasting to all users except current user.
        */
        shareButton.addActionListener(e -> {
            JDialog dialog = new JDialog(this, "Share Entry", true);
            JPanel panel   = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            File usersFolder   = new File("users");
            Dimension buttonSize = new Dimension(200, 35);

            JLabel titleLabel = new JLabel("Who do you want to send this to?");
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
            panel.add(titleLabel);

            File[] users = usersFolder.listFiles(File::isDirectory);

            if (users != null) {
                for (File user : users) {
                    String userName = user.getName();
                    if (userName.equals(currentUsername)) continue;

                    JButton userButton = new JButton(userName);
                    userButton.setMaximumSize(buttonSize);
                    userButton.setPreferredSize(buttonSize);
                    userButton.setMinimumSize(buttonSize);
                    userButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                    userButton.addActionListener(x -> {
                        shareEntryToUser(userName);
                        dialog.dispose();
                    });

                    panel.add(userButton);
                    panel.add(Box.createVerticalStrut(6));
                }
                panel.add(Box.createVerticalStrut(10));
            }

            JButton allButton = new JButton("SHARE TO ALL");
            allButton.setMaximumSize(buttonSize);
            allButton.setPreferredSize(buttonSize);
            allButton.setMinimumSize(buttonSize);
            allButton.setAlignmentX(Component.CENTER_ALIGNMENT);

            allButton.addActionListener(x -> {
                if (users != null) {
                    for (File user : users) {
                        String name = user.getName();
                        if (!name.equals(currentUsername)) {
                            shareEntryToUser(name);
                        }
                    }
                }
                dialog.dispose();
            });

            panel.add(allButton);
            dialog.add(new JScrollPane(panel));
            dialog.setSize(300, 200);
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