package app;

import javax.swing.*;
import javax.swing.border.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/*FUNCTION:
main screen after login — displays all user journal entries in a sortable list view.
from here, user can add, edit, delete, open entries, access account settings, or log out.
*/
public class UIFourEntryListViewer extends JFrame {

    private DefaultListModel<EntryData> listModel;
    private JList<EntryData> entryList;
    private JButton editButton, deleteButton, addButton, logoutButton;
    private JComboBox<String> sortBox;
    private boolean sortByLastEdited = true;  // tracks current sort column
    private boolean ascending        = true;  // tracks sort direction
    private int hoverIndex = -1;              // hover row tracking for UI highlight
    private String username;
    private Image backgroundImage;

    public UIFourEntryListViewer(String username) {
        this.username = username;

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) setIconImage(appIcon.getImage());

        ImageIcon bg = loadIcon("JEntriesUIGeneralBackground.png");
        if (bg != null) backgroundImage = bg.getImage();

        setTitle("jEntries");
        setSize(800, 700);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        /*FUNCTION:
        custom background panel — paints image behind entire UI for consistent theme vibe.
        */
        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        bgPanel.setOpaque(true);
        setContentPane(bgPanel);

        initializeUI();
        loadEntries();
        setVisible(true);
    }

    /*FUNCTION:
    builds full UI layout: top bar (greeting + logout/account),
    center (entry list + sorting), bottom (edit/delete/add controls).
    */
    private void initializeUI() {

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(35, 40, 20, 40));
        topPanel.setOpaque(false);

        JLabel helloLabel = new JLabel("Hello, " + username + "!");
        helloLabel.setFont(new Font("Arial Black", Font.PLAIN, 28));
        helloLabel.setForeground(Color.WHITE);
        helloLabel.setFont(helloLabel.getFont().deriveFont(25f));

        logoutButton = new JButton("LOG OUT");

        /*FUNCTION:
        logout action — logs timestamp, closes current window, then reopens login screen.
        basically exit + balik sa login page.
        */
        logoutButton.addActionListener(e -> {
            String logoutTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy (hh:mm a)"));
            XMLUserStorage.logActivity(username, null, logoutTime, null);
            dispose();
            try { UIOneUserLogin.main(new String[]{}); } catch (Exception ex) { ex.printStackTrace(); }
        });

        JButton accountButton = new JButton("ACCOUNT");
        accountButton.addActionListener(e -> new UIAddiUserAccountPanel(username, UIFourEntryListViewer.this));

        topPanel.add(helloLabel, BorderLayout.WEST);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        topRight.setOpaque(false);
        topRight.add(logoutButton);
        topRight.add(accountButton);
        topPanel.add(topRight, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(0, 40, 0, 40));
        centerPanel.setOpaque(false);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new LineBorder(Color.BLACK, 1));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(200, 200, 200));
        headerPanel.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.BLACK),
                new EmptyBorder(10, 25, 10, 25)));

        JLabel subjectLabel = new JLabel("Entry subjects");

        sortBox = new JComboBox<>(new String[]{
                "Last modified (Newest)", "Last modified (Oldest)",
                "Created (Newest)",       "Created (Oldest)"});

        sortBox.setPreferredSize(new Dimension(190, 30));
        sortBox.setOpaque(false);
        sortBox.setBackground(new Color(200, 200, 200));
        sortBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        sortBox.setFocusable(false);

        /*FUNCTION:
        dropdown sort controller — maps selected option to sorting rules then reloads list.
        */
        sortBox.addActionListener(e -> {
            int index = sortBox.getSelectedIndex();
            sortByLastEdited = index < 2;
            ascending = (index == 1 || index == 3);
            loadEntries();
        });

        headerPanel.add(subjectLabel, BorderLayout.WEST);
        headerPanel.add(sortBox,      BorderLayout.EAST);

        listModel = new DefaultListModel<>();
        entryList = new JList<>(listModel);
        entryList.setCellRenderer(new EntryRenderer());
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.setBackground(Color.WHITE);
        entryList.setFixedCellHeight(35);

        /*FUNCTION:
        hover tracking — detects which row mouse is currently on for UI highlight effect.
        */
        entryList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int index = entryList.locationToIndex(e.getPoint());
                if (index != hoverIndex) { hoverIndex = index; entryList.repaint(); }
            }
        });

        entryList.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoverIndex = -1; entryList.repaint(); }

            /*FUNCTION:
            double click entry — opens selected entry in viewer mode.
            */
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedEntry();
            }
        });

        JScrollPane scrollPane = new JScrollPane(entryList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        tablePanel.add(headerPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane,  BorderLayout.CENTER);
        centerPanel.add(tablePanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(new EmptyBorder(15, 40, 40, 40));
        bottomPanel.setOpaque(false);

        editButton   = createDarkButton("    EDIT    ");
        deleteButton = createDarkButton("    DELETE    ");
        addButton    = createDarkButton("    ADD    ");

        /*FUNCTION:
        edit entry — opens editor if selected entry exists and is not shared.
        shared entries are locked (read-only behavior).
        */
        editButton.addActionListener(e -> {
            EntryData selected = entryList.getSelectedValue();
            if (selected == null) { JOptionPane.showMessageDialog(this, "Select an entry to edit!"); return; }
            if (selected.sharedBy != null && !selected.sharedBy.isBlank()) {
                JOptionPane.showMessageDialog(this, "This is a shared entry, you cannot edit it!");
                return;
            }
            setVisible(false);
            new UISixEntryEdit(selected.subject, UIOneUserLogin.currentUserFolderPath, username, () -> {
                setVisible(true); loadEntries();
            });
        });

        /*FUNCTION:
        delete entry — confirms action, deletes local XML file,
        syncs deletion to Laktev's GDrive, then logs activity.
        */
        deleteButton.addActionListener(e -> {
            EntryData selected = entryList.getSelectedValue();
            if (selected == null) { JOptionPane.showMessageDialog(this, "Select an entry to be deleted!"); return; }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete \"" + selected.subject + "\"?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) return;

            File file = new File(UIOneUserLogin.currentUserFolderPath, selected.subject + ".xml");
            if (file.exists()) {
                if (file.delete()) {

                    DriveSync.deleteEntry(username, selected.subject);

                    String actionTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy (hh:mm a)"));
                    XMLUserStorage.logActivity(username, null, null,
                            "Deleted entry \"" + selected.subject + "\" on " + actionTime);

                    JOptionPane.showMessageDialog(this, "Entry deleted successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete file!");
                }
            } else {
                JOptionPane.showMessageDialog(this, "File not found!");
            }
            loadEntries();
        });

        /*FUNCTION:
        add entry — opens editor in blank mode to create new journal entry.
        */
        addButton.addActionListener(e -> {
            setVisible(false);
            new UISixEntryEdit("", UIOneUserLogin.currentUserFolderPath, username, () -> {
                setVisible(true); loadEntries();
            });
        });

        bottomPanel.add(editButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(deleteButton);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(addButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /*FUNCTION:
    creates simple styled button (placeholder for reusable UI styling later).
    */
    private JButton createDarkButton(String text) { return new JButton(text); }

    /*FUNCTION:
    opens selected entry in viewer mode (read-only).
    */
    private void openSelectedEntry() {
        EntryData selected = entryList.getSelectedValue();
        if (selected == null) return;
        setVisible(false);
        new UIFiveEntryViewer(selected.subject, UIOneUserLogin.currentUserFolderPath, username, () -> {
            setVisible(true); loadEntries();
        });
    }

    /*FUNCTION:
    loads all XML entries from user folder, parses them into EntryData,
    then sorts based on selected filter and updates UI list.
    */
    private void loadEntries() {
        listModel.clear();
        File folder = new File(UIOneUserLogin.currentUserFolderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (files == null) return;

        List<EntryData> entries = new ArrayList<>();
        for (File file : files) {
            try {
                EntryData data = readXMLData(file);
                if (data != null) entries.add(data);
            } catch (Exception e) { e.printStackTrace(); }
        }

        entries.sort((a, b) -> {
            Date dateA = sortByLastEdited ? a.lastEdited : a.created;
            Date dateB = sortByLastEdited ? b.lastEdited : b.created;
            return ascending ? dateA.compareTo(dateB) : dateB.compareTo(dateA);
        });

        for (EntryData entry : entries) listModel.addElement(entry);
    }

    /*FUNCTION:
    reads single XML file and converts it into EntryData object.
    skips invalid files silently.
    */
    private EntryData readXMLData(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder        = factory.newDocumentBuilder();
            Document document              = builder.parse(file);

            if (document.getElementsByTagName("subject").getLength()    == 0
                    || document.getElementsByTagName("created").getLength()    == 0
                    || document.getElementsByTagName("lastEdited").getLength() == 0) return null;

            String subject     = document.getElementsByTagName("subject").item(0).getTextContent().trim();
            String createdText = document.getElementsByTagName("created").item(0).getTextContent().trim();
            String editedText  = document.getElementsByTagName("lastEdited").item(0).getTextContent().trim();

            SimpleDateFormat parser   = new SimpleDateFormat("MM-dd-yyyy (hh:mm a)");
            Date createdDate          = parser.parse(createdText);
            Date editedDate           = parser.parse(editedText);

            String sharedBy = "";
            if (document.getElementsByTagName("sharedBy").getLength() > 0)
                sharedBy = document.getElementsByTagName("sharedBy").item(0).getTextContent().trim();

            return new EntryData(subject, sharedBy, createdDate, editedDate);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /*FUNCTION:
    data container for entry list items (subject + timestamps + shared info).
    */
    private static class EntryData {
        String subject, sharedBy;
        Date created, lastEdited;
        EntryData(String subject, String sharedBy, Date created, Date lastEdited) {
            this.subject = subject;
            this.sharedBy = sharedBy;
            this.created = created;
            this.lastEdited = lastEdited;
        }
        @Override public String toString() { return subject; }
    }

    /*FUNCTION:
    trims long entry titles for UI display without breaking layout.
    */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        String cut = text.substring(0, maxLength - 3);
        int lastSpace = cut.lastIndexOf(" ");
        if (lastSpace > 20) cut = cut.substring(0, lastSpace);
        return cut + "...";
    }

    /*FUNCTION:
    custom list renderer — displays entry subject + date, handles hover, selection,
    and marks shared entries in red (slight highlight for visibility).
    */
    private class EntryRenderer extends JPanel implements ListCellRenderer<EntryData> {
        private JLabel subjectLabel = new JLabel();
        private JLabel dateLabel    = new JLabel();

        public EntryRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 25, 5, 25));
            add(subjectLabel, BorderLayout.WEST);
            add(dateLabel,    BorderLayout.EAST);
        }

        @Override public Component getListCellRendererComponent(JList<? extends EntryData> list,
                                                                EntryData value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value.sharedBy != null && !value.sharedBy.isBlank())
                subjectLabel.setText("<html><font color='red'>(Shared by " + value.sharedBy + ") </font>"
                        + truncate(value.subject, 45) + "</html>");
            else
                subjectLabel.setText(truncate(value.subject, 45));

            Date shownDate = sortByLastEdited ? value.lastEdited : value.created;
            dateLabel.setText(new SimpleDateFormat("MM/dd/yyyy (hh:mm a)").format(shownDate));

            if (isSelected) {
                setBackground(new Color(70, 130, 255));
                subjectLabel.setForeground(Color.WHITE);
                dateLabel.setForeground(Color.WHITE);
            } else if (index == hoverIndex) {
                setBackground(new Color(220, 220, 220));
                subjectLabel.setForeground(Color.BLACK);
                dateLabel.setForeground(Color.BLACK);
            } else {
                setBackground(Color.WHITE);
                subjectLabel.setForeground(Color.BLACK);
                dateLabel.setForeground(Color.BLACK);
            }
            return this;
        }
    }

    /*FUNCTION:
    loads image icon from resources folder safely (no crash if missing).
    */
    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) return new ImageIcon(resource);
        System.out.println("Icon not found: " + path);
        return null;
    }
}