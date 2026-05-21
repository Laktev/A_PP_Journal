import java.util.Scanner;
import java.security.NoSuchAlgorithmException;

public class DISPOSABLE {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.println("\n1. Sign Up\n2. Login\n3. Exit");
            System.out.print("Choose an option: ");
            String choice = input.nextLine();

            if (choice.equals("1")) {
                UITwoNewUserSignUp.handleSignUp(input);
            } else if (choice.equals("2")) {
                UIOneUserLogin.handleLogin(input);
            } else if (choice.equals("3")) {
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
        input.close();
    }
}
