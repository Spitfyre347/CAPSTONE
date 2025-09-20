import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Arrays;

public class CapstoneFileReader {

    private boolean debug = true; // Debug flag to control debug output

    // Stopwatch variables
    private long startTime;
    private long endTime;

    // Instance variables to hold the parsed data

    // These arrays will be populated with the data read from the file
    private int[] costs = null;
    private int[] literals = null;
    private int[] values = null;

    private String[] lines = null; // To hold the lines read from the file

    // Array of clauses
    private String[] clauses = null;

    int numVariables = 0;
    int numClauses = 0;
    int hardCost = -1;

    // Getters for the instance variables
    public int getNumVars() { return numVariables; }
    public int getNumClauses() { return numClauses; }
    public int[] getCosts() { return costs; }

    // Deprecated
    //public int[] getLiterals() { return literals; }
    
    public int[] getLiterals(int index){
        if (index < 0 || index >= numClauses) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        
        int zeroes = 0;
        int position = 0;
        if (index != 0){
            for (int i = 0; i < literals.length; i++){
                if (literals[i] == 0)
                {
                    zeroes++;
                    if (zeroes == index)
                    {
                        position = i + 1;
                        break;
                    }
                }
            }    
        }
        
        int size = literals.length - position; // default to fill array
        for (int i = position; i < literals.length; i++){
            if (literals[i] == 0){
                size = i - position;
                break;
            }
        }

        return Arrays.copyOfRange(literals, position, position + size);
        
    }
    
    public int[] getValues() { return values; }
    public int getHardCost() {
        if (hardCost == -1) {
            throw new IllegalStateException("Hard cost has not been set. Please check the file format.");
        }
        return hardCost;
    }

    private void StartTimer(){
        startTime = System.currentTimeMillis();
    }

    private void StopTimer(){
        endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + elapsedTime + " ms");
    }

    public void InitializeClauses(String path, boolean Debug){
        StartTimer();
        debug = Debug;
        boolean success = ReadInFile(path);

        if (!success){
            System.err.println("Issue encountered during file read. Aborting...");
            return;
        }

        if (clauses.length == 0){
            System.err.println("File read succesfully, though no clauses found. Aborting...");
            return;
        }

        System.out.println("Organizing " + clauses.length + " clauses found in order of length");
        
        OptimizeClauses();
        literals = OptimizeArrayStorage();
        System.out.println(toString());
        System.out.println(Arrays.toString(getLiterals(1)));
        System.out.println(Arrays.toString(getLiterals(2)));
        System.out.println(Arrays.toString(getLiterals(3)));
        System.out.println(Arrays.toString(getLiterals(12)));
        StopTimer();
    }

    private boolean ReadInFile(String path)
    {
        // Check file exists; break if no file found
        try (BufferedReader bReader = new BufferedReader(new FileReader(path))) {
            // Briefly uses a list object - may need to change
            lines = Files.readAllLines(Paths.get(path)).toArray(new String[0]);
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }

        // Initialize variables
        boolean initialised = false; // Flag to check if the header line has been processed
        hardCost = -1; // Hard cost for the clauses, as specified in the header line

        int clauseCounter = 0; // Index for actual clauses processed (including parsing)
        String[] lineHolder = null; // Temporary holder for the split line data

        // First, pass through file to determine number of '=' (exact) clauses,
        // and increment a counter to adjust the number of clauses later on
        int equalsClauseCounter = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == 'c') continue; // Skip comments and blank lines
            
            if (line.indexOf(" = ") != -1) {
                // If the line contains an exact clause, increment the clause counter
                equalsClauseCounter++;
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
                    costs = new int[numClauses + equalsClauseCounter];
                    literals = new int[(numClauses + equalsClauseCounter) * numVariables];
                    values = new int[numClauses + equalsClauseCounter];
                    
                    clauses = new String[numClauses + equalsClauseCounter];

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
                return false;
            }

            // Ensure clause begins with an integer (cost)
            int num;
            try {
                num = Integer.parseInt(lineHolder[0]);
            } catch (Exception e){
                System.out.println("Invalid line detected - Line does not fit format of clause, comment or header");
                System.out.println("Line: " + line);
                e.printStackTrace();
                return false;
            }

            // Ensure cost is positive
            if(num < 0){
                System.out.println("Invalid line detected - A cost may not be negative");
                System.out.println("Line: " + line);
                return false;
            }
            
            
            // We now need to write logic for handling the different type of clauses (<=, >=, =)

            // We use a case statement to check against possibilities for the 3 types of clauses
            switch (lineHolder[numArgs-2]) {
                case ">=":
                    // Format is fine as is
                    clauses[clauseCounter] = this.arrToStr(lineHolder);

                    populateArrays(lineHolder, clauseCounter, numVariables);
                    break;
                case "<=":
                    // Convert to >= (standardized format)
                    String[] conLineHolder = leqtogeq(lineHolder); 
                    clauses[clauseCounter] = this.arrToStr(conLineHolder);

                    populateArrays(conLineHolder, clauseCounter, numVariables);
                    break;
                case "=":
                    // If the clause is an exact clause, we need to convert it to two >= clauses
                    String[] geqLineHolder = eqtogeq(lineHolder, false); 
                    clauses[clauseCounter] = this.arrToStr(geqLineHolder);
                    populateArrays(geqLineHolder, clauseCounter, numVariables);

                    clauseCounter++;
                    String[] leqLineHolder = eqtogeq(lineHolder, true); 
                    clauses[clauseCounter] = this.arrToStr(leqLineHolder);
                    populateArrays(leqLineHolder, clauseCounter, numVariables);

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

                        clauses[clauseCounter] = this.arrToStr(newLineHolder);
                        populateArrays(newLineHolder, clauseCounter, numVariables);
                    }
                    else{ // Error: Unexpected clause format
                        System.out.println("Invalid line detected - Clause format not recognized.");
                        System.out.println("Line: " + line);
                        return false;
                    }
                    break;
            }
            clauseCounter++;
        }

        if (debug) System.out.println(this.toString());

        // If this point is reached, execution is successful.
        return true;
    }

    private void OptimizeClauses(){
        // First, clauses are sorted (since this makes the removal of later duplicates faster, O(nlogn) against O(n^2) efficiency).

        // Array for length of each clause created
        int[] clauseLengths = new int[clauses.length];
        int i = 0;
        for (int k = 1; k <= literals.length; k++){
            if (literals[k-1] != 0)
                clauseLengths[i] += 1;

            if (k % numVariables == 0){
                i++;
            }
        }

        // Bubble sorts 'costs', 'literals', 'values' (and 'clauses') arrays
        int n = clauseLengths.length;
        for (i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (clauseLengths[j] > clauseLengths[j + 1]) {
                    // swap clauses
                    String sTemp = clauses[j];
                    clauses[j] = clauses[j+1];
                    clauses[j+1] = sTemp;

                    // swap clause lengths
                    int iTemp = clauseLengths[j];
                    clauseLengths[j] = clauseLengths[j + 1];
                    clauseLengths[j + 1] = iTemp;
                    
                    // swap literals
                    for (int k = 0; k < numVariables; k++) {
                        iTemp = literals[j * numVariables + k];
                        literals[j * numVariables + k] = literals[(j + 1) * numVariables + k];
                        literals[(j + 1) * numVariables + k] = iTemp;
                    }

                    // swap costs
                    iTemp = costs[j];
                    costs[j] = costs[j + 1];
                    costs[j + 1] = iTemp;

                    // swap values
                    iTemp = values[j];
                    values[j] = values[j + 1];
                    values[j + 1] = iTemp;
                }
            }
        }

        // now, duplicates (which are known to be adjacent) are removed
        n = clauses.length;
        int write = 0; // position to write next unique record

        for (int read = 0; read < n; read++) {
            if (read == 0 || !clauses[read].equals(clauses[read - 1])) {
                // Keep this one
                clauses[write] = clauses[read];
                clauseLengths[write] = clauseLengths[read];
                costs[write] = costs[read];
                values[write] = values[read];

                // Copy literals
                for (int k = 0; k < numVariables; k++) {
                    literals[write * numVariables + k] =
                        literals[read * numVariables + k];
                }

                write++;
            }
        }

        // Trim arrays down to new size
        clauses = Arrays.copyOf(clauses, write);
        clauseLengths = Arrays.copyOf(clauseLengths, write);
        costs = Arrays.copyOf(costs, write);
        values = Arrays.copyOf(values, write);
        literals = Arrays.copyOf(literals, write * numVariables);
    }

    // Trim arrays to actual size
    private int[] OptimizeArrayStorage() {
        // Count wasted space in original array
        int zeroCount = 0;
        for (int literal : literals) {
            if (literal == 0) {
                zeroCount++;
            }
        }

        // Allocate new array: remove unused zeros, but add 1 per clause for terminators
        int[] dimacs = new int[literals.length - zeroCount + numClauses];
        int idx = 0; // pointer for dimacs

        for (int c = 0; c < numClauses; c++) {
            int start = c * numVariables;
            int end = start + numVariables;

            // add all nonzero literals in this clause
            for (int i = start; i < end; i++) {
                if (literals[i] != 0) {
                    dimacs[idx++] = literals[i];
                }
            }
            // add clause terminator
            dimacs[idx++] = 0;
        }

        return dimacs;
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
                "\n\nclauses = " + Arrays.toString(clauses) +
                "\n}";
    }

    public static void main(String[] args) {
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("test.txt", false);
    }
}