import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.FileWriter;
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
    private int[] indices = null;

    private int[] softIndices, hardIndices = null; // Separate storage for new indices once optimization complete

    private int[] initialSol = null;

    // Array of clauses
    private String[] clauses = null;

    // Integer trackers
    private int numVariables = 0;
    private int numClauses = 0;
    private int hardCost = -1;

    // Getters for the instance variables
    public int getNumVars() { return numVariables; }
    public int getNumClauses() { return numClauses; }

    public int[] getSoftCosts() {
        int[] softCosts;

        // Find locations where soft clauses appear in array
        String softInd = getSoftLocs(false);

        softCosts = new int[softInd.length()];
        int ind = 0;

        // Extract soft costs
        for (char c : softInd.toCharArray()){
            softCosts[ind++] = costs[c - '0']; // subtracting '0' gives the integer value of this character
        }

        return softCosts; 
    }
    
    public int[] getSoftValues() { 
        int[] softVals;

        // Find locations where soft clauses appear in array
        String softInd = getSoftLocs(false);

        softVals = new int[softInd.length()];
        int ind = 0;

        // Extract soft values
        for (char c : softInd.toCharArray()){
            softVals[ind++] = values[c - '0']; // for explanation see getSoftCosts()
        }

        return softVals; 
    }
    public int[] getHardValues() { 
        int[] hardVals;

        // Find locations where soft clauses appear in array
        String hardInd = getSoftLocs(true);

        hardVals = new int[hardInd.length()];
        int ind = 0;

        // Extract soft values
        for (char c : hardInd.toCharArray()){
            hardVals[ind++] = values[c - '0']; // for explanation see getSoftCosts()
        }

        return hardVals; 
    }
    
    public int[] getOuterSoftIndices() { 
        int[] softInds;

        // Find locations where soft clauses appear in array
        String softLoc = getSoftLocs(false);

        softInds = new int[softLoc.length()];
        int ind = 0;

        // Extract soft indices
        for (char c : softLoc.toCharArray()){
            softInds[ind++] = indices[c - '0']; // for explanation see getSoftCosts()
        }

        return softInds; 
    }
    public int[] getOuterHardIndices() { 
        int[] hardInds;

        // Find locations where soft clauses appear in array
        String hardLoc = getSoftLocs(true);

        hardInds = new int[hardLoc.length()];
        int ind = 0;

        // Extract hard indices
        for (char c : hardLoc.toCharArray()){
            hardInds[ind++] = indices[c - '0']; // for explanation see getSoftCosts()
        }

        return hardInds; 
    }
    
    public int[] getSoftLiterals(){
        int[] softLits;

        // Find locations where soft clauses appear in array
        String softLoc = getSoftLocs(false);
        
        int size = 0;
        int ind = 0;
        int[] sizes = new int[softLoc.length()];

        // Will also populate softIndices during this method
        softIndices = new int[softLoc.length()];
        
        // Calculate size for soft literals array, and work out size of each clause
        for (char c : softLoc.toCharArray()){   
            sizes[ind] = indices[c - '0' + 1] - indices[c - '0'];
            size += sizes[ind]; // for explanation see getSoftCosts()
            ind++;
        }

        softLits = new int[size];

        ind = 0; // Reuse indice variable
        int outerInd = 0;
        int[] inds = getOuterSoftIndices();

        for (int i : inds){
            softIndices[outerInd] = ind;
            for (int j = 0; j < sizes[outerInd]; j++){
                softLits[ind] = literals[i + j];
                ind++;
            }
            outerInd++;
        }
        return softLits; 
    }

    public int[] getHardLiterals(){
         int[] hardLits;

        // Find locations where soft clauses appear in array
        String softLoc = getSoftLocs(true);
        
        int size = 0;
        int ind = 0;
        int[] sizes = new int[softLoc.length()];

        // Will also populate softIndices during this method
        hardIndices = new int[softLoc.length()];
        
        // Calculate size for soft literals array, and work out size of each clause
        for (char c : softLoc.toCharArray()){   
            sizes[ind] = indices[c - '0' + 1] - indices[c - '0'];
            size += sizes[ind]; // for explanation see getSoftCosts()
            ind++;
        }

        hardLits = new int[size];

        ind = 0; // Reuse indice variable
        int outerInd = 0;
        int[] inds = getOuterHardIndices();

        for (int i : inds){
            hardIndices[outerInd] = ind;
            for (int j = 0; j < sizes[outerInd]; j++){
                hardLits[ind] = literals[i + j];
                ind++;
            }
            outerInd++;
        }
        return hardLits; 
    }

    public int[] getSoftIndices(){
        if (softIndices == null)
            System.out.println("Error: Must cause getSoftLiterals() first before this method");

        return softIndices;
        
    }

    public int[] getHardIndices(){
        if (softIndices == null)
            System.out.println("Error: Must cause getHardLiterals() first before this method");
        
        return hardIndices;


    }

    public int[] getInitialSol(){
        if (initialSol == null)
            System.out.println("No initial solution found, solver not initialized.");
        return initialSol;
    }

    // Deprecated
    //public int[] getLiterals() { return literals; }
    
    public int getHardCost() {
        if (hardCost == -1) {
            throw new IllegalStateException("Hard cost has not been set. Please check the file format.");
        }
        return hardCost;
    }

    public void InitializeClauses(String path, boolean Debug){
        StartTimer();
        debug = Debug;

        // Reads in file
        boolean success = ReadInFile(path);

        if (!success){
            System.err.println("Issue encountered during file read. Aborting...");
            return;
        }

        if (clauses.length == 0){
            System.err.println("File read succesfully, though no clauses found. Aborting...");
            return;
        }

        // File read successfully, proceed to optimization
        System.out.println("Found " + clauses.length + " clauses, beginning preprocessing");
        
        // Sort clauses by length, and remove duplicates
        OptimizeClauses();

        // Trim literals array to reduced EOF size, AND populate indices array
        indices = new int[numClauses+1]; // +1 to store end of last clause
        literals = OptimizeArrayStorage();
        
        
        // Initial preprocessing complete, proceed to initial solution calcualtions
        System.out.println("Arrays optimized, proceeding to intial solution");

        StopTimer();
        if (debug){
            printAll();
        }

        writeToFile("Preprocessing_Output.txt");

        initialSol = InitialSolution();
    }





    // Initial arrayization, file reading and input validation
    private boolean ReadInFile(String path)
    {
        String[] lines = null;
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





    // Remove duplicate clauses, and sort clauses by length
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

        // Allocate new array to remove unused zeros, and populate indices array
        int[] newliterals = new int[literals.length - zeroCount];
        int idx = 0; // pointer for new array
        int ind = 0; // pointer for indices array

        for (int c = 0; c < numClauses; c++) {
            int start = c * numVariables;
            int end = start + numVariables;

            // Stores start of clause in indices array
            indices[ind++] = idx;

            // Add all nonzero literals in this clause
            for (int i = start; i < end; i++) {

                if (literals[i] != 0) {
                    newliterals[idx++] = literals[i];
                }
            }

            indices[ind] = idx-1;
        }

        return newliterals;
    }


    // Find initial solution (greedy), satisfying all hard clauses if possible. 
    // Returns assignment as int[numVariables] with values 0 or 1.
    private int[] InitialSolution() {
        // Arrays for tracking current assignment, and history of flips
        int[] assign = new int[numVariables];
        int[] history = new int[numVariables];

        // Random number generator with fixed seed for reproducibility
        java.util.Random rand = new java.util.Random(12345);

        // To start, set a weight-sensitive greedy initialization
        long[] oneBenefit = new long[numVariables];
        long[] zeroBenefit = new long[numVariables];

        // For every clause (soft only), accumulate benefit
        for (int c = 0; c < numClauses; c++) {
            if (costs[c] == hardCost) 
                continue; // skip hard clauses

            int start = indices[c];
            int end = indices[c + 1];
            int cost = costs[c];

            for (int k = start; k < end; k++) {
                int lit = literals[k];
                int var = Math.abs(lit) - 1; // -1 since benefit arrays are 0-indexed
                if (lit > 0) 
                    oneBenefit[var] += cost;
                else 
                    zeroBenefit[var] += cost;
            }
        }

        // Assign greedily (maximum cost benefit)
        for (int v = 0; v < numVariables; v++) {
            assign[v] = (oneBenefit[v] >= zeroBenefit[v]) ? 1 : 0;
        }

        // Then, repair hard clauses
        final int MAX_ITERS = numVariables * 50;
        for (int iter = 0; iter < MAX_ITERS; iter++) {
            int unsatHard = findUnsatHardClause(assign);
            if (unsatHard == -1) {
                // success
                initialSol = Arrays.copyOf(assign, assign.length);
                return initialSol;
            }

            // Try to fix by flipping a variable in that clause
            int start = indices[unsatHard];
            int end = indices[unsatHard + 1]; // inclusive
            int bestVar = -1;
            int bestNewVal = -1;
            long bestLoss = Long.MAX_VALUE;
            int bestHist = Integer.MAX_VALUE;

            for (int k = start; k <= end; k++) {
                int lit = literals[k];
                int var = Math.abs(lit) - 1;
                int targetVal = (lit > 0) ? 1 : 0;

                if (assign[var] == targetVal) continue; // already set correctly, clause still unsat, need others

                // compute soft weight loss if we flip var
                int oldVal = assign[var];
                assign[var] = targetVal;
                long newSoft = softSatisfiedWeight(assign);
                assign[var] = oldVal;
                long oldSoft = softSatisfiedWeight(assign);
                long loss = oldSoft - newSoft;

                if (loss < bestLoss ||
                    (loss == bestLoss && history[var] < bestHist) ||
                    (loss == bestLoss && history[var] == bestHist && rand.nextBoolean())) {
                    bestLoss = loss;
                    bestVar = var;
                    bestNewVal = targetVal;
                    bestHist = history[var];
                }
            }

            // apply the chosen flip
            if (bestVar != -1) {
                assign[bestVar] = bestNewVal;
                history[bestVar]++;
            } else {
                // if no variable found, flip something random
                int lit = literals[start + rand.nextInt(end - start + 1)];
                bestVar = Math.abs(lit) - 1;
                bestNewVal = (lit > 0) ? 1 : 0;
                assign[bestVar] = bestNewVal;
                history[bestVar]++;
            }
        }

        // If failed, warn but return best effort
        System.err.println("Warning: could not satisfy all hard clauses in InitialSolution()");
        initialSol = Arrays.copyOf(assign, assign.length);
        return initialSol;
    }

    // --- Helper: check if a clause is satisfied ---
    private boolean clauseSatisfied(int c, int[] assign) {
        int start = indices[c];
        int end = indices[c + 1]; // inclusive
        int required = values[c];
        int count = 0;
        for (int k = start; k <= end; k++) {
            int lit = literals[k];
            int var = Math.abs(lit) - 1;
            boolean litTrue = (lit > 0) ? (assign[var] == 1) : (assign[var] == 0);
            if (litTrue) count++;
            if (count >= required) return true;
        }
        return false;
    }

    // --- Helper: find one unsatisfied hard clause (or -1 if all satisfied) ---
    private int findUnsatHardClause(int[] assign) {
        for (int c = 0; c < numClauses; c++) {
            if (costs[c] == hardCost && !clauseSatisfied(c, assign)) {
                return c;
            }
        }
        return -1;
    }

    // --- Helper: total soft weight satisfied by assignment ---
    private long softSatisfiedWeight(int[] assign) {
        long sum = 0;
        for (int c = 0; c < numClauses; c++) {
            if (costs[c] == hardCost) continue;
            if (clauseSatisfied(c, assign)) sum += costs[c];
        }
        return sum;
    }


    // Helper and auxiliary functions
    private String getSoftLocs(boolean invert){
        String softInd = "";

        for (int i = 0; i < costs.length; i++)
            if ((costs[i] == hardCost) == invert)
                softInd += String.valueOf(i);
                
        return softInd;
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
        String out = "";
        for (int i = 1; i < in.length; i++)
        {
            out = out + in[i];
            if (i < (in.length-1))
                out = out + "";
        }
        out += " (" +in[0] + ")";
            
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
        return "CapstoneFileReader - Parsed Input Results \n------------------------------------------\n" +
                "numVariables = " + numVariables +
                "\nnumClauses = " + numClauses +
                "\nhardCost = " + hardCost +
                "\ncosts = " + Arrays.toString(costs) +
                "\nliterals = " + Arrays.toString(literals) +
                "\nvalues = " + Arrays.toString(values) +
                "\nindices = " + Arrays.toString(indices) +
                "\n(clauses = " + Arrays.toString(clauses)+ ")";
    }

    private void StartTimer(){
        startTime = System.currentTimeMillis();
    }

    private void StopTimer(){
        endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + elapsedTime + " ms");
    }

    public void printAll(){
        System.out.println(toString());
        System.out.println("Soft Costs: " + Arrays.toString(getSoftCosts()));
        System.out.println("Soft Values: " + Arrays.toString(getSoftValues()));
        System.out.println("Hard Values: " + Arrays.toString(getHardValues()));
        System.out.println("(Outer Soft Indices: " + Arrays.toString(getOuterSoftIndices()) + ")");
        System.out.println("(Outer Hard Indices: " + Arrays.toString(getOuterHardIndices()) + ")");
        
        System.out.println("Soft Literals: " + Arrays.toString(getSoftLiterals()));
        System.out.println("Hard Literals: " + Arrays.toString(getHardLiterals()));

        System.out.println("Soft Indices: " + Arrays.toString(getSoftIndices()));
        System.out.println("Hard Indices: " + Arrays.toString(getHardIndices()));

        System.out.println("Initial Soln: " + Arrays.toString(getInitialSol()));
    }

    public void writeToFile(String path){
        FileWriter writer = null;
        try {
            writer = new FileWriter(path);
            writer.write(toString());
            writer.write("\n\nSoft Costs: " + Arrays.toString(getSoftCosts()));
            writer.write("\nSoft Values: " + Arrays.toString(getSoftValues()));
            writer.write("\nHard Values: " + Arrays.toString(getHardValues()));
            writer.write("\n(Outer Soft Indices: " + Arrays.toString(getOuterSoftIndices()) + ")");
            writer.write("\n(Outer Hard Indices: " + Arrays.toString(getOuterHardIndices()) + ")");
            
            writer.write("\nSoft Literals: " + Arrays.toString(getSoftLiterals()));
            writer.write("\nHard Literals: " + Arrays.toString(getHardLiterals()));

            writer.write("\nSoft Indices: " + Arrays.toString(getSoftIndices()));
            writer.write("\nHard Indices: " + Arrays.toString(getHardIndices()));
            writer.write("\nInitial Soln: " + Arrays.toString(getInitialSol()));
            writer.write("\n\nElapsed time: " + (endTime - startTime) + " ms");
            writer.close(); // Always close the writer

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // Main method for quick testing
    public static void main(String[] args) {
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("test.txt", true);
    }
}