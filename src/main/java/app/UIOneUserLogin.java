import java.util.*;
import java.math.BigInteger;
import java.security.*;


public class UIOneUserLogin {
    public String loginEncryptString(String input) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1,messageDigest);
        return bigInt.toString(16);

    }

    public static void handleLogin (Scanner input) throws NoSuchAlgorithmException {
        UIOneUserLogin loginSecurity = new UIOneUserLogin();
        UITwoNewUserSignUp loginInformation = new UITwoNewUserSignUp();

        System.out.print("Password: ");
        String loginPassword = input.nextLine();
        String hashedLoginSearchKey = loginSecurity.loginEncryptString(loginPassword);

        if (UITwoNewUserSignUp.userMap.containsKey(hashedLoginSearchKey)) {
            System.out.println("Successfully logged-in!");
        } else {
            System.out.println("No matching user found.");
        }


    }
}