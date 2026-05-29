package app;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;

public class XMLUserStorage {

    //Path to users.xml, placed in the project's working directory
    private static final String FILE_NAME = System.getProperty("user.dir") + File.separator + "users.xml";

    // Creates users.xml with an empty <users> root on first launch if it doesn't exist yet. Called once from UIOneUserLogin.main before anything else runs.
    public static void initializeFile() {
        try {
            File file = new File(FILE_NAME);
            if (!file.exists()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.newDocument();
                Element rootElement = doc.createElement("users");
                doc.appendChild(rootElement);
                saveDocument(doc);
                System.out.println("XML file created.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Makes the local folder at users/<username>/ when a new account is registered. Returns the absolute path so it can be stored in users.xml and used later.
    public static String createUserFolder(String username) {
        String projectPath = System.getProperty("user.dir");
        String usersFolderPath = projectPath + File.separator + "users";
        File usersFolder = new File(usersFolderPath);
        if (!usersFolder.exists()) usersFolder.mkdirs();

        String userFolderPath = usersFolderPath + File.separator + username;
        File userFolder = new File(userFolderPath);
        if (!userFolder.exists()) {
            if (userFolder.mkdirs()) System.out.println("User folder created.");
        }
        return userFolderPath;
    }


    // Registers a new user in users.xml. Rejects duplicate usernames (case-insensitive).
    public static boolean saveUser(String username, String hashedPassword, String folderPath, String question, String answer) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);

            // Reject the registration if this username is already taken
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element element = (Element) userList.item(i);
                String existingUsername = element.getElementsByTagName("username").item(0).getTextContent();
                if (existingUsername.equalsIgnoreCase(username)) {
                    System.out.println("User already existed.");
                    return false;
                }
            }

            // Build the new <user> element and append it to the root
            Element root = doc.getDocumentElement();
            Element user = doc.createElement("user");

            Element xmlQuestion = doc.createElement("securityQuestion");
            xmlQuestion.appendChild(doc.createTextNode(question));
            Element xmlAnswer = doc.createElement("securityAnswer");
            xmlAnswer.appendChild(doc.createTextNode(answer));
            Element xmlUsername = doc.createElement("username");
            xmlUsername.appendChild(doc.createTextNode(username));
            Element xmlPassword = doc.createElement("password");
            xmlPassword.appendChild(doc.createTextNode(hashedPassword));
            Element xmlFolder = doc.createElement("folderPath");
            xmlFolder.appendChild(doc.createTextNode(folderPath));

            user.appendChild(xmlQuestion);
            user.appendChild(xmlAnswer);
            user.appendChild(xmlUsername);
            user.appendChild(xmlPassword);
            user.appendChild(xmlFolder);
            root.appendChild(user);
            saveDocument(doc);

            // Sync the updated registry to Laktev's GDrive
            DriveSync.pushUsersXml();

            System.out.println("User saved.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Checks username + hashed password against users.xml.
    // Returns the user's local folder path on success, or null if credentials don't match.
    public static String verifyLogin(String username, String hashedPassword) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Node node = userList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String storedUsername = element.getElementsByTagName("username").item(0).getTextContent();
                    String storedPassword = element.getElementsByTagName("password").item(0).getTextContent();
                    if (storedUsername.equals(username) && storedPassword.equals(hashedPassword)) {
                        return element.getElementsByTagName("folderPath").item(0).getTextContent();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Recursively deletes a local folder and everything inside it.
    // Used by deleteUser() to clean up the user's entry files on disk.
    private static void deleteFolder(File folder) {
        if (folder == null || !folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
            }
        }
        folder.delete();
    }

    // Removes a user from users.xml, wipes their local folder, and mirrors both Laktev's GDrive.
    // Called from UIAddiUserAccountPanel when the user confirms account deletion.
    public static boolean deleteUser(String username) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element user = (Element) userList.item(i);
                String storedUsername = user.getElementsByTagName("username").item(0).getTextContent();
                if (storedUsername.equalsIgnoreCase(username)) {
                    String folderPath = user.getElementsByTagName("folderPath").item(0).getTextContent();
                    deleteFolder(new File(folderPath));
                    user.getParentNode().removeChild(user);
                    saveDocument(doc);

                    // Remove the Laktev's GDrive folder and push the updated registry
                    DriveSync.deleteUserFolder(username);
                    DriveSync.pushUsersXml();

                    System.out.println("User deleted successfully.");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Writes login time, logout time, and/or last action to the user's record.
    // Pass null for any field you don't want to update — only non-null values are written.
    public static void logActivity(String username, String loginTime,
                                   String logoutTime, String lastAction) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element user = (Element) userList.item(i);
                String storedUsername = user.getElementsByTagName("username").item(0).getTextContent();
                if (storedUsername.equals(username)) {
                    if (loginTime != null)  setOrCreate(doc, user, "lastLogin",  loginTime);
                    if (logoutTime != null) setOrCreate(doc, user, "lastLogout", logoutTime);
                    if (lastAction != null) setOrCreate(doc, user, "lastAction", lastAction);
                    saveDocument(doc);

                    // Sync the updated activity timestamps to Laktev's GDrive
                    DriveSync.pushUsersXml();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Changes a user's username and/or password. Rejects if the new username is already taken.
    public static boolean updateUser(String oldUsername, String newUsername, String newPasswordHash) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element user = (Element) userList.item(i);
                String storedUsername = user.getElementsByTagName("username").item(0).getTextContent();
                if (storedUsername.equalsIgnoreCase(oldUsername)) {

                    // Check the new username isn't already claimed by someone else
                    if (!oldUsername.equalsIgnoreCase(newUsername)) {
                        for (int j = 0; j < userList.getLength(); j++) {
                            Element other = (Element) userList.item(j);
                            String existing = other.getElementsByTagName("username").item(0).getTextContent();
                            if (existing.equalsIgnoreCase(newUsername)) {
                                System.out.println("Username already taken.");
                                return false;
                            }
                        }
                    }

                    user.getElementsByTagName("username").item(0).setTextContent(newUsername);
                    user.getElementsByTagName("password").item(0).setTextContent(newPasswordHash);

                    // Rename the local entry folder to match the new username
                    NodeList folderNode = user.getElementsByTagName("folderPath");
                    if (folderNode.getLength() > 0) {
                        String oldPath = folderNode.item(0).getTextContent();
                        File oldFolder = new File(oldPath);
                        File newFolder = new File(oldFolder.getParent(), newUsername);
                        if (oldFolder.exists()) {
                            boolean renamed = oldFolder.renameTo(newFolder);
                            if (renamed) folderNode.item(0).setTextContent(newFolder.getAbsolutePath());
                            else System.out.println("Failed to rename folder.");
                        }
                    }

                    saveDocument(doc);

                    // Mirror the rename and updated registry to Laktev's GDrive
                    DriveSync.renameUserFolder(oldUsername, newUsername);
                    DriveSync.pushUsersXml();

                    System.out.println("User updated successfully.");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Used by UIAddiUserAccountPanel to populate the activity section.
    public static String[] getActivity(String username) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element user = (Element) userList.item(i);
                String storedUsername = user.getElementsByTagName("username").item(0).getTextContent();
                if (storedUsername.equals(username)) {
                    return new String[]{
                            getTagText(user, "lastLogin"),
                            getTagText(user, "lastLogout"),
                            getTagText(user, "lastAction")
                    };
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[]{null, null, null};
    }

    // Looks up and returns the stored folder path for a given username.
    // Used after a username rename to refresh the live session path in UIOneUserLogin.
    public static String getFolderPath(String username) {
        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element user = (Element) userList.item(i);
                String storedUsername = user.getElementsByTagName("username").item(0).getTextContent();
                if (storedUsername.equals(username)) {
                    return user.getElementsByTagName("folderPath").item(0).getTextContent();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // Returns a list of every registered username from users.xml.
    // Used by the share dialog in UIFiveEntryViewer so it doesn't rely on the local users/ folder.
    public static java.util.List<String> getAllUsernames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        try {
            File file = new File(FILE_NAME);
            if (!file.exists()) return names;
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element user = (Element) userList.item(i);
                NodeList un = user.getElementsByTagName("username");
                if (un.getLength() > 0) names.add(un.item(0).getTextContent().trim());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return names;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static void setOrCreate(Document doc, Element parent, String tag, String value) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() > 0) {
            nodes.item(0).setTextContent(value);
        } else {
            Element el = doc.createElement(tag);
            el.appendChild(doc.createTextNode(value));
            parent.appendChild(el);
        }
    }

    // Reads the text content of a child tag. Returns null if missing or blank.
    private static String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent().trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    // Serializes the in-memory DOM back to users.xml with indentation. Every write method calls this — it's the single point that touches the file on disk.
    private static void saveDocument(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(FILE_NAME));
        transformer.transform(source, result);
    }
}