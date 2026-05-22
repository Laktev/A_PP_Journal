package app;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UITwoNewUserSignUp extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public UITwoNewUserSignUp() {
        initializeUI();
    }

    private void initializeUI() {
        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) {setIconImage(appIcon.getImage());}

        setTitle("Login");
        setTitle("JEntries - Sign Up");
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

        JButton createButton = new JButton("CREATE ACCOUNT");

        JPanel bottom = new JPanel();
        bottom.add(createButton);
        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        createButton.addActionListener(e -> handleSignUp());
        setVisible(true);
    }

    private void handleSignUp() {
        try {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty!");
                return;
            }

            String hashedPassword = encrypt(password);
            String folderPath = XMLUserStorage.createUserFolder(username);
            boolean success = XMLUserStorage.saveUser(username, hashedPassword, folderPath);

            if (success) {JOptionPane.showMessageDialog(this, "Account created successfully!");
                dispose();
            } else {

                JOptionPane.showMessageDialog(this, "User already exists. Try another username."
                );

                usernameField.setText("");
                passwordField.setText("");
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
}