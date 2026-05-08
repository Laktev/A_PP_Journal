import java.util.Scanner;
import java.math.BigInteger;
import java.security.*;

public class Security {
    public String encryptString(String input) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1,messageDigest);
        return bigInt.toString(16);

    }

    public static void main (String [] args) throws NoSuchAlgorithmException {
        Scanner input = new Scanner(System.in);
        Security security = new Security();

        System.out.print("Input Password: ");
        String password = input.nextLine();

        System.out.println(security.encryptString(password));

    }
}