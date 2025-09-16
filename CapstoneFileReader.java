import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Arrays;

public class CapstoneFileReader {

    private boolean debug = true; // Debug flag to control debug output

    // Instance variables to hold the parsed data

    // These arrays will be populated with the data read from the file
    private int[] costs = null;
    private int[] literals = null;
    private int[] values = null;

    private String[] lines = null; // To hold the lines read from the file

    // Testing array to print out all clauses and their final forms
    private String[] oldClauses = null;
    private String[] clauses = null;

    int numVariables = 0;
    int numClauses = 0;
    int hardCost = -1;

    // Getters for the instance variables
    public int getNumVars() { return numVariables; }
    public int getNumClauses() { return numClauses; }
    public int[] getCosts() { return costs; }
    public int[] getLiterals() { return literals; }
    public int[] getValues() { return values; }

    public int getHardCost() {
        if (hardCost == -1) {
            throw new IllegalStateException("Hard cost has not been set. Please check the file format.");
        }
        return hardCost;
    }

    /**
     * Reads a file and parses its contents into the instance variables.
     * The file is expected to be in a specific format as described in the comments.
     *
     * @param path The path to the file to be read.
     */

    public void InitializeClauses(String path, boolean Debug){

    }

    public void readInFile(String path)
    {
        // Check file exists; break if no file found
        try (BufferedReader bReader = new BufferedReader(new FileReader(path))) {
            // Briefly uses a list object - may need to change
            lines = Files.readAllLines(Paths.get(path)).toArray(new String[0]);
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

        // Initialize variables
        boolean initialised = false; // Flag to check if the header line has been processed
        hardCost = -1; // Hard cost for the clauses, as specified in the header line

        int clauseCounter = 0; // Index for current clause processed
        int newClauseCounter = 0; // Index for actual clauses processed (including parsing)
        String[] lineHolder = null; // Temporary holder for the split line data

        // First, pass through file to determine number of '=' (exact) clauses,
        // and increment a counter to adjust the number of clauses later on
        int exactClauseCounter = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == 'c') continue; // Skip comments and blank lines
            
            if (line.indexOf(" = ") != -1) {
                // If the line contains an exact clause, increment the clause counter
                exactClauseCounter++;
            }
        }

        // Now process file normally.
        for (String line : lines) {
            // On blank lines/comment lines, skip to next iteration
            // Note due to short circuiting, if can be checked in this way
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == 'c') continue;
            
            // The first thing we look for is the header line. 
            if (!initialised){

                // If line begins with p, header line found, so initialize all values
                if(line.charAt(0) == 'p'){
                    lineHolder = line.split("\\s+");

                    // Adjust number of clauses for input parsing later on
                    numClauses = Integer.parseInt(lineHolder[3]);

                    numVariables =Integer.parseInt(lineHolder[2]);
                    hardCost = Integer.parseInt(lineHolder[4]);

                    // Initialize all arrays
                    costs = new int[numClauses + exactClauseCounter];
                    literals = new int[(numClauses + exactClauseCounter) * numVariables];
                    values = new int[numClauses + exactClauseCounter];
                    

                    // Testing array
                    oldClauses = new String[numClauses];
                    clauses = new String[numClauses + exactClauseCounter];

                    initialised = true;
                }

                continue;
            }

            // If code reaches this point, we've found the header and intialized.

            // As such, if multiple header lines are found, stop reading the file (incorrect format)
            if(line.charAt(0) == 'p') throw new IllegalStateException("ERROR: Invalid file format. Cannot have more than one header line.");
         
            lineHolder = line.split("\\s+");
            int numArgs = lineHolder.length;

            // At this point, we expect the line to be a clause. We perform several validation checks.

            // Check the clause is not too short.
            if(numArgs < 3){
                System.out.println("Invalid line - Line too short, missing information");
                System.out.println("Line: " + line);
                return;
            }

            // Ensure clause begins with an integer (cost)
            int num;
            try {
                num = Integer.parseInt(lineHolder[0]);
            } catch (Exception e){
                System.out.println("Invalid line detected - Line does not fit format of clause, comment or header");
                System.out.println("Line: " + line);
                e.printStackTrace();
                return;
            }

            // Ensure cost is positive
            if(num < 0){
                System.out.println("Invalid line detected - A cost may not be negative");
                System.out.println("Line: " + line);
                return;
            }
            
            
            // We now need to write logic for handling the different type of clauses (<=, >=, =)

            // We use a case statement to check against possibilities for the 3 types of clauses
            switch (lineHolder[numArgs-2]) {
                case ">=":
                    // Format is fine as is
                    oldClauses[clauseCounter] = this.arrToStr(lineHolder);
                    clauses[newClauseCounter] = this.arrToStr(lineHolder);

                    populateArrays(lineHolder, newClauseCounter, numVariables);
                    break;
                case "<=":
                    // Convert to >= (standardized format)
                    oldClauses[clauseCounter] = this.arrToStr(lineHolder);
                    String[] conLineHolder = leqtogeq(lineHolder); 
                    clauses[newClauseCounter] = this.arrToStr(conLineHolder);

                    populateArrays(conLineHolder, newClauseCounter, numVariables);
                    break;
                case "=":
                    // If the clause is an exact clause, we need to convert it to two >= clauses
                    oldClauses[clauseCounter] = this.arrToStr(lineHolder);
                    String[] geqLineHolder = eqtogeq(lineHolder, false); 
                    clauses[newClauseCounter] = this.arrToStr(geqLineHolder);
                    populateArrays(geqLineHolder, newClauseCounter, numVariables);

                    newClauseCounter++;
                    String[] leqLineHolder = eqtogeq(lineHolder, true); 
                    clauses[newClauseCounter] = this.arrToStr(leqLineHolder);
                    populateArrays(leqLineHolder, newClauseCounter, numVariables);

                    break;

                default: // We must have that the statement ends in 0 to denote >= 1, or we have an error
                    if (lineHolder[numArgs-1].equals("0")){
                        // We need to artifically extend our arguments array, and then resolve as normal
                        String[] newLineHolder = new String[numArgs+1];

                        int i;
                        for (i=0; i < numArgs-1; i++){
                            newLineHolder[i] = lineHolder[i];
                        }

                        newLineHolder[i] = ">=";
                        newLineHolder[i+1] = "1";

                        oldClauses[clauseCounter] = this.arrToStr(newLineHolder);
                        clauses[newClauseCounter] = this.arrToStr(newLineHolder);
                        populateArrays(newLineHolder, newClauseCounter, numVariables);
                    }
                    else{ // Error: Unexpected clause format
                        System.out.println("Invalid line detected - Clause format not recognized.");
                        System.out.println("Line: " + line);
                        return;
                    }
                    break;
            }
            clauseCounter++;
            newClauseCounter++;
        }

        if (debug) System.out.println(this.toString());

        // At this point, all clauses are obtained, we move to sorting/optimizing
        OptimizeClauses();
    }

    public void OptimizeClauses(){
        // We require, for this void, a nonempty array of clauses

       
    }

    public static void main(String[] args) {
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.readInFile("test.txt");
    }


    private String[] leqtogeq (String[] elements){
        int numEl = elements.length;
        int k = Integer.parseInt(elements[numEl-1]);
        int n = numEl - 3;
        
        elements[numEl-1] = String.valueOf(n-k);
        elements[numEl-2] = ">=";

        for (int i = 1; i < numEl-2; i++){
            elements[i] = String.valueOf(Integer.parseInt(elements[i]) * (-1));
        }
        return elements;
    }

    private String[] eqtogeq (String[] elements, boolean extraConver){
        if (!extraConver) {
            elements[elements.length-2] = ">=";
            return elements;
        }
        else{
            elements[elements.length-2] = "<=";
            return leqtogeq(elements);
        }
    }

    private String arrToStr(String[] in){
        String out = "{ ";
        for (int i = 1; i < in.length; i++)
        {
            out = out + in[i];
            if (i < (in.length-1))
                out = out + " ";
        }
        out += " (cost = " + in[0] + ") }";
            
        return out;
    }

    private void populateArrays(String[] in, int index, int vars){
        if (debug) System.out.println(Arrays.toString(in) + "index " + String.valueOf(index) + " numvars " + String.valueOf(vars));
        int len = in.length;
        costs[index] = Integer.parseInt(in[0]);
        values[index] = Integer.parseInt(in[len-1]);
        for (int i = 1; i < len -2 ; i++){
            literals[index*vars + Math.abs(Integer.parseInt(in[i]))-1] = Integer.parseInt(in[i]);
        }
    }
    
    public String toString(){
        return "CapstoneFileReader - Parsed Input Results \n-----------------------------------\n" +
                "numVariables = " + numVariables +
                "\n\nnumClauses = " + numClauses +
                "\n\nhardCost = " + hardCost +
                "\n\ncosts = " + Arrays.toString(costs) +
                "\n\nliterals = " + Arrays.toString(literals) +
                "\n\nvalues = " + Arrays.toString(values) +
                "\n\noldClauses = " + Arrays.toString(oldClauses) +
                "\n\nclauses = " + Arrays.toString(clauses) +
                "\n}";
    }
}