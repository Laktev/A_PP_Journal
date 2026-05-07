import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.text.ParseException;
import java.util.stream.Stream;
import java.nio.file.*;


public class Main {


    public static void createFolder(String input){
        //FUNCTION: Creates the Directory Folder for entries called "Documents"
        File dir = new File("Documents");
        if (!dir.exists()){
            if (!dir.mkdirs()){
                System.out.println("Failed to create directory: Documents");
                System.exit(1);
            }
        }

    }

    public static void main (String [] args){

        //Create instances of neccessary classess

        Scanner input =  new Scanner(System.in);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");

        Data sd = new Data();
        System.out.println("\n---------------------------------------------------------------------");
        System.out.println("|LEGENDS:                                                           |");
        System.out.println("|Type: 'Add Entry'    => To create new entry logs in this terminal. |");
        System.out.println("|Type: 'View Entries' => To view your entries in this terminal.     |");
        System.out.println("---------------------------------------------------------------------");

        System.out.println("\nHello fellow member of the Enclave!");
        System.out.println("What do you want to do today?");

        //--------- PROGRAM PROPER ---------
        String action = input.nextLine();
        //IF: user choose "Add Entry"
        if (action.equalsIgnoreCase("Add Entry")){
            //FUNCTION: User Inputs general information for new entry
            //GENERAL INPUT: Name & full string of name afterwards
            System.out.println("\nFILL THE FOLLOWING INFORMATION:");
            System.out.println("Last Name: ");
            String lastName = input.nextLine();
            System.out.println("Middle  Name:");
            String middleName = input.nextLine();
            System.out.println("First Name:");
            String firstName = input.nextLine();
            String Fullname = sd.fullname(firstName, middleName, lastName);

            //GENERAL INPUT: (For entries) Date
            System.out.println("Date of Entry (MM-DD-YYYY):");
            String dateEntry = input.nextLine();
            Date entryDate;
            try {
                entryDate = sdf.parse(dateEntry);
            }
            catch (ParseException e) {
                System.out.println("Invalid date format. Please use MM-DD-YYYY.");
                return;
            }

            //GENERAL INPUT: (For entries) Subject
            System.out.println("Entry Subject:");
            String entrySubject = input.nextLine();

            //GENERAL INPUT: (For entries) Entry Text itself
            System.out.println("\n\nWrite your Entry:");
            String entryText = input.nextLine();

            //Writing the inputed information onto the file itself
            createFolder(null);
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter("Documents/" + dateEntry + " - " + Fullname + ".xml"))) {

                writer.write("Personnel Name: " + Fullname);
                writer.write("\n---------------------------------------------");
                writer.write("\nSubject: " + entrySubject);
                writer.write("\nDate: " + sdf.format(entryDate));
                writer.write("\n---------------------------------------------");
                writer.write("\n" + entryText);

            } catch (IOException e) {
                System.out.println("An Error occured while writing the file entry.");
            }
        }

        //IF: user choose "View Entries"
        else if (action.equalsIgnoreCase("View Entries")) {
            //FUNCTION: Prints the files inside the Document directory.
            String directoryPath = "./Documents";
            Path dir = Paths.get(directoryPath);

            try(Stream<Path> stream = Files.list(dir)) {
                System.out.println("\nHERE ARE THE ENTRIES STORED IN THIS TERMINAL:");
                System.out.println("----------------------------------------------");

                stream
                        .filter(Files::isRegularFile)
                        .forEach(System.out::println);

            } catch (IOException e) {
                System.err.println("An I/O error occured: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("An unexpected error occured: " + e.getMessage());
            }

            //FUNCTION: User inputs the File's Date & Author's name for it to be used for searching entry file.
            System.out.println("\nEnter Entry Author (Last name, First name, Middle Name):");
            String entryAuthor = input.nextLine();
            System.out.println("\nEnter Entry Date (MM-DD-YYYY):");
            String entryDateMade = input.nextLine();
            File file = new File ("./Documents/" + entryDateMade + " - " + entryAuthor + ".txt");

            //FUNCTION: The following scans the directory folder for the desired entry file.
            try {
                Scanner editor = new Scanner(file);
                while (editor.hasNextLine()) {
                    String line = editor.nextLine();
                    System.out.println(line);
                }
                editor.close();
            }
            catch (FileNotFoundException e) {
                System.out.println("File not found" + e.getMessage());
            }
        }

        else{
            System.out.println("Invalid input: Please select the choices noted in the legends above.");
            System.exit(1);
        }

    }


}
//FUNCTION: This unites the full name of entry author when adding a new entry.
interface iData {
    public String fullname(String firstName, String middleName, String lastName);

}

class Data implements iData {
    @Override
    public String fullname(String firstName, String middleName, String lastName) {
        return lastName + ", " + firstName + " " + middleName;
    }
}