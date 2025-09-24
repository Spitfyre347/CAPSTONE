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

    private int[] softIndices = null, hardIndices = null; // Separate storage for new indices once optimization complete

    private int[][] hardBulkyArr;
    private int[][] softBulkyArr;
    private int[] softClauses = null, hardClauses = null;
    private int[] softClauseInds = null, hardClauseInds = null;

    private int[] initialSol = null;
    private int[] FirstInitialSol = null;
    private int[] numbers = null;
    private int[] floatsArr = null;
    private int[] hardVarArr = null;
    private int[] flipCosts = null;

    // Array of clauses
    private String[] clauses = null;

    // Integer trackers (SOME OUTDATED AFTER RUNTIME)
    private int numVariables = 0;
    private int numClauses = 0; // DEPRECATED (based on pre = calculation)
    private int hardCost = -1;

    // Getters for the instance variables
    public int getNumVars() { return numVariables; }

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
    
    private int[] getOuterSoftIndices() { 
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
    private int[] getOuterHardIndices() { 
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
        softIndices = new int[softLoc.length()+1];
        
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

        softIndices[softIndices.length-1] = softLits.length;
        
        return softLits; 
    }

    public int[] getHardLiterals(){
        int[] hardLits;

        // Find locations where soft clauses appear in array
        String hardLoc = getSoftLocs(true);
        
        int size = 0;
        int ind = 0;
        int[] sizes = new int[hardLoc.length()];

        // Will also populate softIndices during this method
        hardIndices = new int[hardLoc.length()+1];
        
        // Calculate size for soft literals array, and work out size of each clause
        for (char c : hardLoc.toCharArray()){   
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

        hardIndices[hardIndices.length-1] = hardLits.length;

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

    public int[] getSoftClauses(){ return softClauses; }
    public int[] getHardClauses(){ return hardClauses; }
    public int[] getSoftClauseIndices(){ return softClauseInds; }
    public int[] getHardClauseIndices(){ return hardClauseInds; }

    public int getHardCost() {
        if (hardCost == -1) {
            throw new IllegalStateException("Hard cost has not been set. Please check the file format.");
        }
        return hardCost;
    }

    public int[] getInitialSol(){
        if (initialSol == null)
            System.out.println("No initial solution found, solver not initialized.");
        return initialSol;
    }

    // Deprecated
    //public int[] getLiterals() { return literals; }

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
        indices = new int[clauses.length+1]; // +1 to store end of last clause
        literals = OptimizeArrayStorage();
        
        // Calculate and populate clause and clause index arrays
        softClauseInds = new int[numVariables+1];
        hardClauseInds = new int[numVariables+1];
        softClauses = new int[getSoftLiterals().length];
        hardClauses = new int[getHardLiterals().length];
        softBulkyArr = PopulateClauseArrays(softClauseInds, softClauses, true);
        hardBulkyArr = PopulateClauseArrays(hardClauseInds, hardClauses, false);
        
        // Initial preprocessing complete, proceed to initial solution calcualtions
        System.out.println("Arrays optimized, proceeding to intial solution");

        initialSol = InitialSolution();
        StopTimer();

        if (debug){
            System.out.println(toString());
        }

        writeToFile("Preprocessing_Output.txt");

        
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
        int clauseCheckCounter = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == 'c' || line.charAt(0) == 'p') continue; // Skip comments, headers and blank lines
            
            if (line.indexOf(" = ") != -1) {
                // If the line contains an exact clause, increment the clause counter
                equalsClauseCounter++;
                clauseCheckCounter++;
            }
            else
                clauseCheckCounter++; // Still a clause
        }

        if (clauseCheckCounter == 0){
            System.err.println("No clauses found in file. Aborting...");
            return false;
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

                    if (clauseCheckCounter != numClauses){
                        System.err.println("Error: Number of clauses specified in header (" + numClauses + ") does not match number of clauses found in file (" + clauseCheckCounter + ").");
                        return false;
                    }

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

        for (int c = 0; c < clauses.length; c++) {
            int start = c * numVariables;
            int end = start + numVariables;

            // Stores start of clause in indices array
            indices[ind++] = idx;

            // Add all nonzero literals in this clause
            for (int i = start; i < end; i++) {
                if (literals[i] != 0) 
                    newliterals[idx++] = literals[i];
            }    
        }
        
        indices[ind] = idx;
        return newliterals;
    }

    private int[][] PopulateClauseArrays(int[] indArr, int[] clauseArr, boolean soft){
        // Compute list of clauses for each variable as well as a corresponding index array
        int[] literals;
        int[] indices;

        if (soft)
        {
            literals = getSoftLiterals();
            indices = getSoftIndices();
        }   
        else 
        {
            literals = getHardLiterals();
            indices = getHardIndices();
        }
        int[][] bulkyArr = new int[numVariables][indices.length-1]; // Initial 2D array to populate all clauses for all vars, which will be flattened later on
            

        // Actual incredible code which looks to be written by an absolute genius
        int ind = 0;
        for (int i = 0; i < literals.length; i++){
            bulkyArr[Math.abs(literals[i])-1][ind] = (ind+1) * (literals[i] / Math.abs(literals[i])); // add clause index, with sign
            if ((ind < indices.length-1) && (i == (indices[ind+1]-1))) // new clause reached
                ind++;
        }
        /*|─────────────────────────────────|
          |🏆  CODE AWARD OF EXCELLENCE 🏆 |
          |─────────────────────────────────|
          | ✨ For writing truly spec- ✨  |
          |          tacular code!          | 
          |─────────────────────────────────|*/


        // now we write logic to flatten this 2d array into a 1d array, and to populate our indices
        
        int pos = 0;
        for (int i = 0; i < bulkyArr.length; i++) {
            indArr[i] = pos;  // mark start index
            for (int j = 0; j < bulkyArr[i].length; j++) {
                if (bulkyArr[i][j] != 0) {
                    clauseArr[pos] = bulkyArr[i][j];
                    pos++;
                }
            }
        }
        indArr[indArr.length-1]= clauseArr.length;

        return bulkyArr;
    }


    // Find initial solution (greedy), satisfying all hard clauses if possible. 
    // Returns assignment as int[numVariables] with values 0 or 1.
    private int[] InitialSolution() {
        initialSol = new int[numVariables];
        int[] softCosts = getSoftCosts();

        // Step 1) Assign variables greedily based on soft clause weights
        int k = 0; // Pointer for specific clauses for a variable
        for (int i = 1; i <= initialSol.length; i++){
            int weightIfTrue=0;
            int weightIfFalse=0;

            while (k < softClauseInds[i]){
                int val = softClauses[k];
                if (val<0)
                    weightIfFalse += softCosts[Math.abs(val)-1];
                else if (val > 0)
                    weightIfTrue += softCosts[Math.abs(val)-1];
                else
                    System.out.println("The paradox has been reached, assemble brothers ⚔️⚔️⚔️");
                
                k++;
            }

            // Set to 1 if true, and -1 if false (makes checking far easier)
            if (weightIfTrue >= weightIfFalse)
                initialSol[i-1] = 1;
            else
                initialSol[i-1] = -1;
            
        }

        FirstInitialSol = initialSol.clone(); // Store first initial solution for reference

        // Step 2) Generate list of unsatisfied hard clauses, and their floats
        
        int[] hardLits = getHardLiterals();
        int[] hardValues = getHardValues();
        String unsatStr = "";
        String floats = "";
        int val = 0;
        int curVar;

        for (int i = 0; i < hardIndices.length-1; i++){
            // Check if i-th clause is satisfied   
            val = 0;         
            for (int j = 0; j < (hardIndices[i+1]-hardIndices[i]); j++){
                // Looping through every variable in the clause, check if it is satisfied.
                curVar = hardLits[hardIndices[i]+j];

                for (int l = 0; l < initialSol.length; l++)
                    if ((l+1)*initialSol[l] == curVar)
                        val++;
            }
            if (val < hardValues[i]){
                unsatStr += i + " ";
                floats += hardValues[i] - val + " "; // Number of literals in clause not satisfied
            }
                
        }
        
        if (unsatStr.length() == 0) // All hard clauses are satisfied
            return initialSol;


        numbers = Arrays.stream((unsatStr.substring(0, unsatStr.length() - 1)).split(" ")) // Remove extra separator at end, then split
                              .mapToInt(Integer::parseInt)
                              .toArray();

        floatsArr = Arrays.stream((floats.substring(0, floats.length() - 1)).split(" ")) // Remove extra separator at end, then split
                              .mapToInt(Integer::parseInt)
                              .toArray();



        // Step 3) For all variables in unsatisfied hard clauses, calculate cost of flipping
        String hardVars = "";
        for (int i : numbers){
            for (int j = 0; j < (hardIndices[i+1]-hardIndices[i]); j++){
                curVar = hardLits[hardIndices[i]+j];
                if (!hardVars.contains(String.valueOf(Math.abs(curVar))))
                    hardVars += String.valueOf(Math.abs(curVar)) + " ";
            }
        }
        hardVarArr = Arrays.stream((hardVars.substring(0, hardVars.length() - 1)).split(" ")) // Remove extra separator at end, then split
                                    .mapToInt(Integer::parseInt)
                                    .toArray();


        flipCosts = new int[hardVarArr.length];
        for (int i = 0; i < hardVarArr.length; i++)
        {
                    
            int unflippedCost = calcCost(initialSol);
            initialSol[hardVarArr[i]-1] *= -1; // Flip variable
            int flippedCost = calcCost(initialSol);
            initialSol[hardVarArr[i]-1] *= -1; // Flip back

            flipCosts[i] = flippedCost - unflippedCost;
        }

        // Step 3) Satisfy all hard clauses by flipping variables with minimal cost increases

        // Sort variables by cost of flipping
        for (int i = 0; i < flipCosts.length - 1; i++) {
            for (int j = i + 1; j < flipCosts.length; j++) {
                if (flipCosts[i] < flipCosts[j]) {
                    int tempVar = flipCosts[i];
                    flipCosts[i] = flipCosts[j];
                    flipCosts[j] = tempVar;

                    // swap corresponding variables
                    tempVar = hardVarArr[i];
                    hardVarArr[i] = hardVarArr[j];
                    hardVarArr[j] = tempVar;
                }
            }
        }

        // Now, flip variables until all hard clauses are satisfied
        boolean done = false;
        int ind = 0;
        while (!done)
        {
            initialSol[hardVarArr[ind++]-1] *= -1; // Flip variable with best break cost

            // Check if all hard clauses are satisfied, and flip next variable if not
            int unsatClauses = 0;
            for (int i = 0; i < hardIndices.length-1; i++){
                // Check if i-th clause is satisfied   
                val = 0;         
                for (int j = 0; j < (hardIndices[i+1]-hardIndices[i]); j++){
                    // Looping through every variable in the clause, check if it is satisfied.
                    curVar = hardLits[hardIndices[i]+j];

                    for (int l = 0; l < initialSol.length; l++)
                        if ((l+1)*initialSol[l] == curVar)
                            val++;
                }
                if (val < hardValues[i]){
                    unsatClauses++;
                }
            }
            if (unsatClauses == 0)
                done = true;
        }
        
        return initialSol; 
    }

    


    // Helper and auxiliary functions
    public int calcCost(int[] assignments){
        // Calculate current assignment soft cost
        int curCost = 0;

        int[] softValues = getSoftValues();
        int[] softCosts = getSoftCosts();
        int curVar;
        int val=0;
        int[] softLits = getSoftLiterals();
        for (int i = 0; i < softIndices.length-1; i++){
            // Check if i-th clause is satisfied         
            val = 0;   
            for (int j = 0; j < (softIndices[i+1]-softIndices[i]); j++){
                // Looping through every variable in the clause, check if it is satisfied.
                curVar = softLits[softIndices[i]+j];
                
                for (int l = 0; l < initialSol.length; l++)
                    if ((l+1)*initialSol[l] == curVar)
                        val++;
                        
            }
            if (val >= softValues[i])
                curCost += softCosts[i];
        }

        return curCost;
    }

    public boolean isAllZeros(int[] arr) {
        for (int val : arr) {
            if (val != 0) {
                return false; // found non-zero
            }
        }
        return true; // all were zeros
    }

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
                "\nnumClauses = " + clauses.length +
                "\nhardCost = " + hardCost +
                "\ncosts = " + Arrays.toString(costs) +
                "\nliterals = " + Arrays.toString(literals) +
                "\nvalues = " + Arrays.toString(values) +
                "\nindices = " + Arrays.toString(indices) +
                "\n(clauses = " + Arrays.toString(clauses)+ ")" +
                "\n\nSoft Costs: " + Arrays.toString(getSoftCosts()) + 
                "\nSoft Values: " + Arrays.toString(getSoftValues()) +
                "\nHard Values: " + Arrays.toString(getHardValues()) +
                "\n(Outer Soft Indices: " + Arrays.toString(getOuterSoftIndices()) + ")" +
                "\n(Outer Hard Indices: " + Arrays.toString(getOuterHardIndices()) + ")" +
                "\nSoft Literals: " + Arrays.toString(getSoftLiterals()) +
                "\nHard Literals: " + Arrays.toString(getHardLiterals()) +
                "\nSoft Indices: " + Arrays.toString(getSoftIndices()) +
                "\nHard Indices: " + Arrays.toString(getHardIndices()) +
                "\n\nRaw Soft Clauses Array: " + Arrays.deepToString(softBulkyArr) +
                "\nSoft Clauses by Var: " + Arrays.toString(getSoftClauses()) +
                "\nSoft Clause Indices by Var: " + Arrays.toString(getSoftClauseIndices()) +
                "\nRaw Hard Clauses Array: " + Arrays.deepToString(hardBulkyArr) +
                "\nHard Clauses by Var: " + Arrays.toString(getHardClauses()) +
                "\nHard Clause Indices by Var: " + Arrays.toString(getHardClauseIndices()) +
                "\n\nFirst Initial Soln: " + Arrays.toString(FirstInitialSol) +
                "\nFirst Soft Cost: " + calcCost(FirstInitialSol) +
                "\nUnsatisfied Hard Clauses: " + Arrays.toString(numbers) +
                "\nUnsatisfied Floats: " + Arrays.toString(floatsArr) +
                "\nVariables in unsatisfied hard clauses: " + Arrays.toString(hardVarArr) +
                "\nCost of flipping each variable: " + Arrays.toString(flipCosts) +
                "\nInitial Soln: " + Arrays.toString(initialSol) +
                "\nSoft Cost: " + calcCost(initialSol) +
                "\n\nElapsed time: " + (endTime - startTime) + " ms";
    }

    private void StartTimer(){
        startTime = System.currentTimeMillis();
    }

    private void StopTimer(){
        endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + elapsedTime + " ms");
    }
    
    public void writeToFile(String path){
        FileWriter writer = null;
        try {
            writer = new FileWriter(path);
            writer.write(toString());
            writer.close(); // Always close the writer

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // Main method for quick testing
    public static void main(String[] args) {
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("test2.txt", false);
    }

}