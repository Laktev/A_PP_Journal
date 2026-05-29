package app;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UIAddiUserAccountPanel extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private Image backgroundImage;

    // Validates the new credentials, hashes the password, and calls XMLUserStorage.updateUser().
    // On success: refreshes the live session folder path (important after a username rename), closes this window and the entry list, then returns to the login screen.
    private void handleSave(String oldUsername, UIFourEntryListViewer listViewer) {
        String newUsername = usernameField.getText().trim();
        String newPassword = new String(passwordField.getPassword()).trim();

        if (newUsername.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.");
            return;
        }
        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.");
            return;
        }

        try {
            // Hash before saving — updateUser expects a hash, not plain text
            String hashedPassword = encrypt(newPassword);
            boolean success = XMLUserStorage.updateUser(oldUsername, newUsername, hashedPassword);

            if (success) {
                // Refresh the session path so the entry list points to the renamed folder
                String newFolderPath = XMLUserStorage.getFolderPath(newUsername);
                if (newFolderPath != null) {
                    UIOneUserLogin.currentUserFolderPath = newFolderPath;
                }

                JOptionPane.showMessageDialog(this, "Account updated successfully.");
                dispose();
                if (listViewer != null) listViewer.dispose();
                new UIOneUserLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update account. Username may already be taken.");
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Encryption error. Could not save changes.");
        }
    }

    // SHA-256 password hashing, same implementation as UIOneUserLogin and UITwoNewUserSignUp.
    private String encrypt(String input) throws NoSuchAlgorithmException {
        MessageDigest md  = MessageDigest.getInstance("SHA-256");
        byte[] digest     = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }

    // Builds the full account panel: credentials change section at top, activity log below, and a delete-account button at the bottom. Reads activity data from XMLUserStorage on load.
    public UIAddiUserAccountPanel(String username, UIFourEntryListViewer listViewer) {

        setTitle("jEntries - [" + username + "]");
        setSize(500, 550);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) { setIconImage(appIcon.getImage()); }

        ImageIcon bg = loadIcon("JEntriesUIAccountBackground.png");
        if (bg != null) { backgroundImage = bg.getImage(); }

        // Background panel that paints the image behind everything
        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        bgPanel.setOpaque(true);
        setContentPane(bgPanel);

        // CENTER: credentials + activity sections stacked vertically
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(30, 40, 10, 40));
        centerPanel.setOpaque(false);

        // Credential Section:
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBorder(new CompoundBorder(
                new LineBorder(Color.BLACK, 1),
                new EmptyBorder(0, 0, 0, 0)
        ));
        settingsPanel.setOpaque(false);

        JPanel settingsHeader = new JPanel(new BorderLayout());
        settingsHeader.setBackground(new Color(200, 200, 200));
        settingsHeader.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Color.BLACK),
                new EmptyBorder(8, 25, 8, 25)
        ));
        JLabel settingsTitle = new JLabel("CHANGE CREDENTIALS:");
        settingsHeader.add(settingsTitle, BorderLayout.WEST);

        JPanel settingsFields = new JPanel();
        settingsFields.setLayout(new BoxLayout(settingsFields, BoxLayout.Y_AXIS));
        settingsFields.setOpaque(false);
        settingsFields.setBorder(new EmptyBorder(14, 25, 14, 25));

        usernameField = new JTextField(username);
        usernameField.setBorder(BorderFactory.createTitledBorder("Username"));
        usernameField.setOpaque(false);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setBorder(BorderFactory.createTitledBorder("New Password"));
        passwordField.setOpaque(false);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveButton = new JButton("SAVE CHANGES");
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.addActionListener(e -> handleSave(username, listViewer));

        settingsFields.add(usernameField);
        settingsFields.add(Box.createVerticalStrut(8));
        settingsFields.add(passwordField);
        settingsFields.add(Box.createVerticalStrut(10));
        settingsFields.add(saveButton);

        settingsPanel.add(settingsHeader, BorderLayout.NORTH);
        settingsPanel.add(settingsFields, BorderLayout.CENTER);

        centerPanel.add(settingsPanel);
        centerPanel.add(Box.createVerticalStrut(20));

        // Activity section: read-only fields pulled from users.xml
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBorder(new LineBorder(Color.BLACK, 1));
        activityPanel.setOpaque(false);

        JPanel activityHeader = new JPanel(new BorderLayout());
        activityHeader.setBackground(new Color(200, 200, 200));
        activityHeader.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Color.BLACK),
                new EmptyBorder(8, 25, 8, 25)
        ));
        JLabel activityTitle = new JLabel("ACCOUNT ACTIVITY:");
        activityHeader.add(activityTitle, BorderLayout.WEST);

        JPanel activityRows = new JPanel();
        activityRows.setLayout(new BoxLayout(activityRows, BoxLayout.Y_AXIS));
        activityRows.setOpaque(false);
        activityRows.setBorder(new EmptyBorder(14, 25, 14, 25));

        // Pull the three activity timestamps; fall back to "No record" if any are null
        String[] activity  = XMLUserStorage.getActivity(username);
        String lastLogin   = activity[0] != null ? activity[0] : "No record";
        String lastLogout  = activity[1] != null ? activity[1] : "No record";
        String lastAction  = activity[2] != null ? activity[2] : "No record";

        activityRows.add(makeActivityRow("Last Login",  lastLogin));
        activityRows.add(Box.createVerticalStrut(8));
        activityRows.add(makeActivityRow("Last Logout", lastLogout));
        activityRows.add(Box.createVerticalStrut(8));
        activityRows.add(makeActivityRow("Last Action", lastAction));

        activityPanel.add(activityHeader, BorderLayout.NORTH);
        activityPanel.add(activityRows,   BorderLayout.CENTER);

        centerPanel.add(activityPanel);
        bgPanel.add(centerPanel, BorderLayout.CENTER);

        //BOTTOM: delete account button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(10, 40, 30, 40));
        bottomPanel.setOpaque(false);

        JButton deleteAccount = new JButton("DELETE ACCOUNT");
        deleteAccount.setBackground(new Color(200, 40, 40));
        deleteAccount.setForeground(Color.WHITE);
        deleteAccount.setOpaque(true);
        deleteAccount.setBorderPainted(false);

        // Prompts for confirmation before wiping the account from users.xml, disk, and Laktev's GDrive.
        deleteAccount.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "This will permanently delete your account and all data.\nContinue?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = XMLUserStorage.deleteUser(username);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Account deleted successfully.");
                    dispose();
                    if (listViewer != null) listViewer.dispose();
                    new UIOneUserLogin();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete account.");
                }
            }
        });

        bottomPanel.add(deleteAccount, BorderLayout.CENTER);
        bgPanel.add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }


    // Creates a single labeled, read-only text field for displaying an activity timestamp. Used three times in the constructor for login, logout, and last-action rows.
    private JPanel makeActivityRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setFocusable(false);
        field.setHighlighter(null);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createTitledBorder(label.toUpperCase()));

        row.add(field, BorderLayout.CENTER);
        return row;
    }

    // Classpath image loader — returns null if the file isn't bundled, logs a console message.
    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) { return new ImageIcon(resource); }
        System.out.println("Icon not found: " + path);
        return null;
    }
}