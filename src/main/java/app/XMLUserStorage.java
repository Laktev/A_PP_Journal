package app;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;

public class XMLUserStorage {

    private static final String FILE_NAME = System.getProperty("user.dir") + File.separator + "users.xml";



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


    public static String createUserFolder(String username) {

        String projectPath = System.getProperty("user.dir");
        String usersFolderPath = projectPath + File.separator + "users";
        File usersFolder = new File(usersFolderPath);

        if (!usersFolder.exists()) {
            usersFolder.mkdirs();
        }

        String userFolderPath = usersFolderPath + File.separator + username;
        File userFolder = new File(userFolderPath);

        if (!userFolder.exists()) {

            if (userFolder.mkdirs()) {
                System.out.println("User folder created.");
            }
        }

        return userFolderPath;
    }

    public static boolean saveUser(String username, String hashedPassword, String folderPath) {

        try {
            File file = new File(FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);

            NodeList userList = doc.getElementsByTagName("user");

            for (int i = 0; i < userList.getLength(); i++) {

                Element element = (Element) userList.item(i);
                String existingUsername = element.getElementsByTagName("username").item(0).getTextContent();

                if (existingUsername.equalsIgnoreCase(username)) {
                    System.out.println("User already existed.");
                    return false;
                }
            }

            Element root = doc.getDocumentElement();
            Element user = doc.createElement("user");
            Element xmlUsername = doc.createElement("username");
            xmlUsername.appendChild(doc.createTextNode(username));
            Element xmlPassword = doc.createElement("password");
            xmlPassword.appendChild(doc.createTextNode(hashedPassword));
            Element xmlFolder = doc.createElement("folderPath");
            xmlFolder.appendChild(doc.createTextNode(folderPath));

            user.appendChild(xmlUsername);
            user.appendChild(xmlPassword);
            user.appendChild(xmlFolder);
            root.appendChild(user);
            saveDocument(doc);

            System.out.println("User saved.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

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


    private static void saveDocument(Document doc)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(FILE_NAME));
        transformer.transform(source, result);
    }
}