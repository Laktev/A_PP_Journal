package app;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*FUNCTION:
sign-up screen of the app — where new users create their account.
collects username, password, security question, and answer.
then delegates account creation to XMLUserStorage after hashing password (secure muna bago store).
*/
public class UITwoNewUserSignUp extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField questionField;
    private JTextField answerField;

    public UITwoNewUserSignUp() {
        initializeUI();
    }

    /*FUNCTION:
    builds the sign-up UI form — 4 inputs (username, password, security question, answer).
    username is limited to 15 characters (para controlled, di sabog input).
    */
    private void initializeUI() {
        ImageIcon appIcon = loadIcon("JEntriesIcon.png");
        if (appIcon != null) { setIconImage(appIcon.getImage()); }

        setTitle("jEntries - Sign Up");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        form.add(new JLabel("Username:"));
        usernameField = new JTextField();

        /*FUNCTION:
        enforces 15-character limit at document level so input is blocked in real-time.
        di na need mag-error after submit, auto-stop na siya while typing.
        */
        usernameField.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (str == null) return;
                if ((getLength() + str.length()) <= 15) {
                    super.insertString(offs, str, a);
                }
            }
        });
        form.add(usernameField);

        form.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        form.add(passwordField);

        form.add(new JLabel("Security Question:"));
        questionField = new JTextField();
        form.add(questionField);

        form.add(new JLabel("Answer:"));
        answerField = new JTextField();
        form.add(answerField);

        JButton createButton = new JButton("CREATE ACCOUNT");

        JPanel bottom = new JPanel();
        bottom.add(createButton);
        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        createButton.addActionListener(e -> handleSignUp());
        setVisible(true);
    }

    /*FUNCTION:
    validates user input, hashes password, creates user folder, then saves account to users.xml.
    also handles duplicate username case (reset fields para retry lang).
    */
    private void handleSignUp() {
        try {
            String username = usernameField.getText().trim();

            // extra safety check in case may maka-bypass sa input limit (just in case lang)
            if (username.length() > 15) {
                JOptionPane.showMessageDialog(this, "Username character limit exceeded. Please use 15 characters or less.");
                return;
            }

            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty!");
                return;
            }

            String question = questionField.getText().trim();
            String answer   = answerField.getText().trim();
            if (question.isEmpty() || answer.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Security question and answer are required!");
                return;
            }

            String hashedPassword = encrypt(password);
            String folderPath     = XMLUserStorage.createUserFolder(username);
            boolean success       = XMLUserStorage.saveUser(username, hashedPassword, folderPath, question, answer);

            if (success) {
                JOptionPane.showMessageDialog(this, "Account created successfully!");
                dispose();
            } else {
                // user already exists — reset fields para fresh try
                JOptionPane.showMessageDialog(this, "User already exists. Please try again!");
                usernameField.setText("");
                passwordField.setText("");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /*FUNCTION:
    SHA-256 password hashing — converts plain password into secure hash string.
    same logic used across login + account update screens para consistent comparison.
    */
    private String encrypt(String input) throws NoSuchAlgorithmException {
        MessageDigest md  = MessageDigest.getInstance("SHA-256");
        byte[] digest     = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1, digest);
        return bigInt.toString(16);
    }

    /*FUNCTION:
    loads icon from resources folder.
    if missing, app won’t crash — just logs warning (safe fallback behavior).
    */
    private ImageIcon loadIcon(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) {
            return new ImageIcon(resource);
        }
        System.out.println("Icon not found: " + path);
        return null;
    }
}