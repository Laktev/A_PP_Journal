package app;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UIOneUserLogin extends JFrame {

    public static String currentUserFolderPath;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public UIOneUserLogin() {
        initializeUI();
    }

    private void initializeUI() {
        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) {setIconImage(appIcon.getImage());}

        setTitle("Login");
        setTitle("JEntries - Log In");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel form = new JPanel(new GridLayout(2, 2, 20, 20));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        form.add(new JLabel("Username:"));
        usernameField = new JTextField();
        form.add(usernameField);

        form.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        form.add(passwordField);

        JButton loginButton = new JButton("LOGIN");
        JButton signupButton = new JButton("SIGN UP");

        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(signupButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> handleLogin());
        signupButton.addActionListener(e -> new UITwoNewUserSignUp());

        setVisible(true);
    }

    private void handleLogin() {

        try {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            String hashedPassword = encrypt(password);
            String folderPath = XMLUserStorage.verifyLogin(username, hashedPassword);

            if (folderPath != null) {
                currentUserFolderPath = folderPath;
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

    private String encrypt(String input) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
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
        XMLUserStorage.initializeFile();
        SwingUtilities.invokeLater(UIOneUserLogin::new);
    }
}