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
import java.util.*;
import java.util.List;

public class UIFourEntryListViewer extends JFrame {

    private DefaultListModel<EntryData> listModel;
    private JList<EntryData> entryList;
    private JButton editButton;
    private JButton deleteButton;
    private JButton addButton;
    private JButton logoutButton;
    private JComboBox<String> sortBox;
    private boolean sortByLastEdited = true;
    private boolean ascending = true;
    private int hoverIndex = -1;

    public UIFourEntryListViewer() {

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) {setIconImage(appIcon.getImage());}

        setTitle("Entry List Viewer");

        setSize(800, 700);
        setMinimumSize(new Dimension(800, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        initializeUI();
        loadEntries();
        setVisible(true);
    }

    private void initializeUI() {

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(35, 40, 20, 40));
        JLabel helloLabel = new JLabel("HELLO, USER!");
        helloLabel.setFont(helloLabel.getFont().deriveFont(25f));


        logoutButton = new JButton("LOG OUT");
        logoutButton.addActionListener(e -> System.exit(0));

        topPanel.add(helloLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(0, 40, 0, 40));

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new LineBorder(Color.BLACK, 1));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(200, 200, 200));
        Border padding = new EmptyBorder(10, 25, 10, 25);
        Border bottomLine = new MatteBorder(0, 0, 1, 0, Color.BLACK);
        headerPanel.setBorder(new CompoundBorder(bottomLine, padding));

        JLabel subjectLabel = new JLabel("Entry subjects");

        sortBox = new JComboBox<>(new String[] {"Last modified (Newest)", "Last modified (Oldest)", "Created (Newest)", "Created (Oldest)"});
        sortBox.setPreferredSize(new Dimension(190, 30));
        sortBox.setOpaque(false);
        sortBox.setBackground(new Color(200, 200, 200));
        sortBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        sortBox.setFocusable(false);

        sortBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBackground(new Color(200, 200, 200));
                label.setOpaque(true);

                if (isSelected) {
                    label.setBackground(new Color(244, 244, 244));
                }

                return label;
            }
        });

        sortBox.addActionListener(e -> {
            int index = sortBox.getSelectedIndex();

            switch (index) {

                case 0:
                    sortByLastEdited = true;
                    ascending = false;
                    break;

                case 1:
                    sortByLastEdited = true;
                    ascending = true;
                    break;

                case 2:
                    sortByLastEdited = false;
                    ascending = false;
                    break;

                case 3:
                    sortByLastEdited = false;
                    ascending = true;
                    break;
            }

            loadEntries();
        });

        headerPanel.add(subjectLabel, BorderLayout.WEST);
        headerPanel.add(sortBox, BorderLayout.EAST);

        listModel = new DefaultListModel<>();

        entryList = new JList<>(listModel);

        entryList.setCellRenderer(new EntryRenderer());
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.setBackground(Color.WHITE);
        entryList.setFixedCellHeight(35);

        entryList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = entryList.locationToIndex(e.getPoint());

                if (index != hoverIndex) {
                    hoverIndex = index;
                    entryList.repaint();
                }
            }
        });

        entryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                entryList.repaint();
            }
        });


        entryList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() == 2) {

                    openSelectedEntry();
                }
            }

        });

        JScrollPane scrollPane = new JScrollPane(entryList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        tablePanel.add(headerPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(tablePanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(new EmptyBorder(15, 40, 40, 40));

        editButton = createDarkButton("    EDIT    ");
        deleteButton = createDarkButton("    DELETE    ");
        addButton = createDarkButton("    ADD    ");

        editButton.addActionListener(e -> {
            EntryData selected = entryList.getSelectedValue();

            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select an entry to edit!");
                return;
            }

            setVisible(false);
            new app.UISixEntryEdit(selected.subject, () -> {
                setVisible(true);
                loadEntries();
            });
        });

        deleteButton.addActionListener(e -> {
            EntryData selected = entryList.getSelectedValue();

            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select an entry to be deleted!");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Delete \"" + selected.subject + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            File file = new File("Documents/" + selected.subject + ".xml");
            if (file.exists()) {
                if (file.delete()) {
                    JOptionPane.showMessageDialog(this, "Entry deleted successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete file!");
                }
            } else {
                JOptionPane.showMessageDialog(this, "File not found!");
            }

            loadEntries();
        });

        addButton.addActionListener(e -> {
            setVisible(false);
            new app.UISixEntryEdit("", () -> {
                setVisible(true);
                loadEntries();
            });

        });

        bottomPanel.add(editButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(deleteButton);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(addButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createDarkButton(String text) {
        JButton button = new JButton(text);
        return button;
    }

    private void openSelectedEntry() {
        EntryData selected = entryList.getSelectedValue();

        if (selected == null) {
            return;
        }

        setVisible(false);
        new app.UIFiveEntryViewer(selected.subject, () -> {
            setVisible(true);
            loadEntries();
        });
    }

    private void loadEntries() {

        listModel.clear();
        File folder = new File("./Documents");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));

        if (files == null) {return;}

        List<EntryData> entries = new ArrayList<>();
        for (File file : files) {

            try {
                EntryData data = readXMLData(file);
                if (data != null) {
                    entries.add(data);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        entries.sort((a, b) -> {
            Date dateA = sortByLastEdited ? a.lastEdited : a.created;
            Date dateB = sortByLastEdited ? b.lastEdited : b.created;

            if (ascending) {
                return dateA.compareTo(dateB);
            } else {
                return dateB.compareTo(dateA);
            }
        });

        for (EntryData entry : entries) {
            listModel.addElement(entry);
        }
    }

    private EntryData readXMLData(File file) {

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);

            if (document.getElementsByTagName("subject").getLength() == 0
                    || document.getElementsByTagName("created").getLength() == 0
                    || document.getElementsByTagName("lastEdited").getLength() == 0) {
                return null;
            }

            String subject = document.getElementsByTagName("subject").item(0).getTextContent().trim();
            String createdText = document.getElementsByTagName("created").item(0).getTextContent().trim();
            String editedText = document.getElementsByTagName("lastEdited").item(0).getTextContent().trim();
            SimpleDateFormat parser = new SimpleDateFormat("MM-dd-yyyy (hh:mm a)");

            Date createdDate = parser.parse(createdText);
            Date editedDate = parser.parse(editedText);
            return new EntryData(subject, createdDate, editedDate);

        } catch (Exception e) {e.printStackTrace();}
        return null;
    }

    private static class EntryData {

        String subject;
        Date created;
        Date lastEdited;

        EntryData(String subject, Date created, Date lastEdited) {
            this.subject = subject;
            this.created = created;
            this.lastEdited = lastEdited;
        }

        @Override
        public String toString() {
            return subject;
        }
    }
    private String truncate(String text, int maxLength) {
        if (text == null) return "";

        if (text.length() <= maxLength) return text;

        String cut = text.substring(0, maxLength - 3);

        int lastSpace = cut.lastIndexOf(" ");
        if (lastSpace > 20) {
            cut = cut.substring(0, lastSpace);
        }

        return cut + "...";
    }

    private class EntryRenderer extends JPanel
            implements ListCellRenderer<EntryData> {

        private JLabel subjectLabel;
        private JLabel dateLabel;

        public EntryRenderer() {

            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 25, 5, 25));
            subjectLabel = new JLabel();
            dateLabel = new JLabel();

            add(subjectLabel, BorderLayout.WEST);
            add(dateLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends EntryData> list, EntryData value, int index, boolean isSelected, boolean cellHasFocus) {

            subjectLabel.setText(truncate(value.subject, 45));

            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy (hh:mm a)");

            Date shownDate = sortByLastEdited ? value.lastEdited : value.created;
            dateLabel.setText(formatter.format(shownDate));
            dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN));

            boolean isHovered = (index == hoverIndex);
            boolean isActuallySelected = isSelected;

            if (isActuallySelected) {
                setBackground(new Color(70, 130, 255));
                subjectLabel.setForeground(Color.WHITE);
                dateLabel.setForeground(Color.WHITE);
            } else if (isHovered) {
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

    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);

        if (resource != null) {
            return new ImageIcon(resource);
        }

        System.out.println("Icon not found: " + path);
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UIFourEntryListViewer::new);
    }
}