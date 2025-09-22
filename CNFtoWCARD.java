import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class CNFtoWCARD {
    public static void main(String[] args){
        System.out.println("Please write the name of the file you want to convert to WCARD format (excluding extension):");
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String fileName = scanner.nextLine();
        scanner.close();

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(fileName + ".cnf")); // read .cnf file
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

        String[] converted = cnftowcard(lines.toArray(new String[0]));

        // Save output to .wcard file
        try {
            Files.write(Paths.get(fileName + ".wcard"),
                        String.join(System.lineSeparator(), converted).getBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Conversion complete. Saved as " + fileName + ".wcard");
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static String[] cnftowcard(String[] lines){
        String hardcost = "100"; // Default hard cost for WCard format
        for (int i = 0; i < lines.length; i++){
            if (lines[i].isEmpty()) continue; // skip blank lines
            if (lines[i].charAt(0) == 'p'){
                lines[i] = lines[i] + " " + hardcost; // Append hard cost to header
            }
            else if (lines[i].charAt(0) == 'c') {
                // Keep comments or skip them
                // lines[i] = "";  // uncomment if you want to remove comments entirely
                continue;
            } else {
                lines[i] = hardcost + " " + lines[i]; // Prepend hard cost to each clause
            }
        }
        return lines;
    }
}