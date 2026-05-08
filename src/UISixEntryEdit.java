import javax.swing.*;
import java.io.*;
import java.awt.*;

public class UISixEntryEdit {

    JFrame frame;
    JTextField subjectField;
    JTextArea entryArea;

    public UISixEntryEdit() {

        // FRAME
        frame = new JFrame("JEntries");
        frame.setSize(700, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);

        // PROGRAM IMAGE
        ImageIcon image = new ImageIcon("JEntriesIcon.png");
        frame.setIconImage(image.getImage());

        // MAIN PANEL
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // SUBJECT LABEL & FIELD
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        JLabel subjectLabel = new JLabel("SUBJECT:");
        topPanel.add(subjectLabel, BorderLayout.WEST);

        subjectField = new JTextField();
        subjectField.setPreferredSize(new Dimension(200, 35));
        topPanel.add(subjectField, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        // ENTRY TEXT FIELD
        entryArea = new JTextArea();
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(entryArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // BUTTON & BUTTON FUNCTION
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("Add Entry");
        bottomPanel.add(saveButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        saveButton.addActionListener(e -> saveEntry());

        frame.add(panel);
        frame.setVisible(true);
    }

    // CREATE DOCUMENTS FOLDER
    public void createFolder() {
        File dir = new File("Documents");

        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // SAVE ENTRY TO XML
    public void saveEntry() {

        String subject = subjectField.getText();
        String entryText = entryArea.getText();

        createFolder();

        String fileName = "Documents/" + subject + ".xml";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            // XML CONTENT
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<entry>\n");

            writer.write("    <subject>" + subject + "</subject>\n");
            writer.write("    <text>" + entryText + "</text>\n");

            writer.write("</entry>");

            JOptionPane.showMessageDialog(frame,
                    "Entry Successfully Saved!");

            // CLEAR FIELDS
            subjectField.setText("");
            entryArea.setText("");

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(frame,
                    "Error Saving File!");
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new UISixEntryEdit());
    }
}