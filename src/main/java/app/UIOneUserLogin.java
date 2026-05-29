package app;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*FUNCTION:
entry point of the app — main login screen kung asa mag-start ang user.
handles login, signup navigation, password recovery, and auto Drive sync on startup (automatic lang, no stress).
currentUserFolderPath is static so other screens can access it directly without passing params everywhere.
*/
public class UIOneUserLogin extends JFrame {

    /*FUNCTION:
    shared session variable that stores the logged-in user's local entry folder path.
    updated after login and also after username changes (UIAddiUserAccountPanel sync moment).
    */
    public static String currentUserFolderPath;

    private JTextField usernameField;
    private JPasswordField passwordField;

    public UIOneUserLogin() {
        initializeUI();
    }

    /*FUNCTION:
    builds the login UI — banner image, input fields, and action buttons (login / signup / recovery).
    basically layout setup para clean tan-awon ang whole screen, dili sabog vibes.
    */
    private void initializeUI() {
        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        ImageIcon image   = loadIcon("JEntriesWelcome.png");

        if (appIcon != null) setIconImage(appIcon.getImage());

        setTitle("jEntries - Log In");
        setSize(400, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JLabel imageLabel = new JLabel();
        if (image != null) imageLabel.setIcon(image);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(imageLabel, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 40, 5, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 3.0;
        usernameField = new JTextField();
        form.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 3.0;
        passwordField = new JPasswordField();
        form.add(passwordField, gbc);

        JButton loginButton  = new JButton("LOGIN");
        JButton signupButton = new JButton("NEW USER?");
        JButton forgotButton = new JButton("RECOVER");

        Dimension buttonSize = new Dimension(150, 25);
        loginButton.setPreferredSize(new Dimension(306, 25));
        signupButton.setPreferredSize(buttonSize);
        signupButton.setMinimumSize(buttonSize);
        signupButton.setMaximumSize(buttonSize);
        forgotButton.setPreferredSize(buttonSize);
        forgotButton.setMinimumSize(buttonSize);
        forgotButton.setMaximumSize(buttonSize);

        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginPanel.setOpaque(false);
        loginPanel.add(loginButton);

        JPanel bottomRow     = new JPanel(new GridLayout(1, 2, 5, 0));
        JPanel bottomWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomWrapper.setOpaque(false);
        bottomWrapper.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));
        bottomRow.add(signupButton);
        bottomRow.add(forgotButton);
        bottomWrapper.add(bottomRow);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setOpaque(false);
        loginPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(loginPanel);
        buttonsPanel.add(Box.createVerticalStrut(2));
        buttonsPanel.add(bottomWrapper);

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.setOpaque(false);
        southWrapper.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));
        southWrapper.add(buttonsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(southWrapper, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> handleLogin());
        signupButton.addActionListener(e -> new UITwoNewUserSignUp());
        forgotButton.addActionListener(e -> handleForgotPassword());

        setVisible(true);
    }

    /*FUNCTION:
    handles login process — checks username + hashed password against users.xml via XMLUserStorage.
    if success: pulls latest Laktev's GDrive entries, logs login time, then opens entry list screen.
    */
    private void handleLogin() {
        try {
            String username       = usernameField.getText().trim();
            String password       = new String(passwordField.getPassword());
            String hashedPassword = encrypt(password);
            String folderPath     = XMLUserStorage.verifyLogin(username, hashedPassword);

            if (folderPath != null) {
                currentUserFolderPath = folderPath;

                DriveSync.pullUserEntries(username, folderPath);

                String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy (hh:mm a)"));
                XMLUserStorage.logActivity(username, loginTime, null, null);

                JOptionPane.showMessageDialog(this, "Login successful!");
                dispose();
                new UIFourEntryListViewer(username);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /*FUNCTION:
    opens entry list after recovery login.
    same sync behavior as normal login so consistent ang data (no weird mismatch issues).
    */
    private void openAfterRecovery(String username, String folderPath) {
        currentUserFolderPath = folderPath;

        DriveSync.pullUserEntries(username, folderPath);

        dispose();
        new UIFourEntryListViewer(username);
    }

    /*FUNCTION:
    handles password recovery using security question validation.
    reads users.xml directly since read-only check ra ni.
    if correct, user gets logged in and told to update password later in account settings.
    */
    private void handleForgotPassword() {
        String username = JOptionPane.showInputDialog(this, "Enter your username:");
        if (username == null || username.isBlank()) return;

        try {
            File file = new File(System.getProperty("user.dir") + File.separator + "users.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList users = doc.getElementsByTagName("user");

            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String storedUsername = user.getElementsByTagName("username").item(0).getTextContent();

                if (storedUsername.equalsIgnoreCase(username)) {
                    String question    = user.getElementsByTagName("securityQuestion").item(0).getTextContent();
                    String answer      = user.getElementsByTagName("securityAnswer").item(0).getTextContent();
                    String inputAnswer = JOptionPane.showInputDialog(this, question);

                    if (inputAnswer != null && inputAnswer.equals(answer)) {
                        JOptionPane.showMessageDialog(this,
                                "Security verification successful. You are now logged in.\n"
                                        + "Please change your password in Account settings.");

                        String folderPath = user.getElementsByTagName("folderPath").item(0).getTextContent();
                        openAfterRecovery(username, folderPath);
                    } else {
                        JOptionPane.showMessageDialog(this, "Incorrect answer!");
                    }
                    return;
                }
            }

            JOptionPane.showMessageDialog(this, "User not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*FUNCTION:
    This shit uses SHA-256 for password hashing — converts plain text password into secure hex string.
    same logic used across signup + account update para consistent ang authentication.
    */
    private String encrypt(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest    = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }

    /*FUNCTION:
    loads icons from resources folder.
    if missing, app won’t crash — just logs it (lowkey safe fail behavior).
    */
    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) return new ImageIcon(resource);
        System.out.println("Icon not found: " + path);
        return null;
    }

    /*FUNCTION:
    app entry point.
    1. sync users.xml from Laktev's GDrive first (para updated data)
    2. initialize local storage if needed
    3. launch login UI on Swing thread
    */
    public static void main(String[] args) {
        DriveSync.initAndPullUsersXml();
        XMLUserStorage.initializeFile();
        SwingUtilities.invokeLater(UIOneUserLogin::new);
    }
}