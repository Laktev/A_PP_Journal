import java.util.*;
import java.math.BigInteger;
import java.security.*;


public class UITwoNewUserSignUp {
    public static HashMap<String, List<String>> userMap = new HashMap<>();
    public String signupEncryptString(String input) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1,messageDigest);
        return bigInt.toString(16);

    }

    public static void handleSignUp (Scanner input) throws NoSuchAlgorithmException {
        UITwoNewUserSignUp signupSecurity = new UITwoNewUserSignUp();


        System.out.print("Username: ");
        String Username = input.nextLine();
        System.out.print("Email Address: ");
        String emailAddress = input.nextLine();
        System.out.print("Password: ");
        String password = input.nextLine();

        userMap.computeIfAbsent(signupSecurity.signupEncryptString(password), k -> new ArrayList<>(List.of(Username, emailAddress)));
        System.out.println("You've successfully been Sign-up!");




    }
}