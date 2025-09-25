import java.util.BitSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class solver3 {

    // Max runtime
    private final static int T = 10000;

    // Variables
    static BitSet vars; // BitSet to keep track of boolean variables
    static int numVars; // number of literals

    static int numSoft; // number of soft clauses
    static int numHard; // number of hard clauses
    static int hardCost; // cost of hard clause
    static int[] softIndices; // indices used to locate literals in a specific soft clause in softLiterals 
    static int[] softLiterals; // soft clauses expressed ito their literals
    static int[] hardIndices; // indices used to locate literals in a specific hard clause in hardLiterals 
    static int[] hardLiterals; // hard clauses expressed ito their literals
    static int[] softCosts; // soft clause costs
    static int[] softFloats; // floats for each soft clause
    static int[] hardFloats; // floats for each hard clause
    static ArrayList<Integer> unsat; // unSAT soft clauses
    static int[] unsat_arr; // unSAT soft clauses as primitive array
    static int[] dynamicCosts; // weighted soft clause costs
    static int[] softValues; // k-values for soft clauses
    static int[] hardValues; // k-values for hard clauses
    static int[] softClauseIndices; // indices to locate soft clauses containing a specific variable
    static int[] softClauses; // variables expressed ito the soft clauses they are found in
    static int[] hardClauseIndices; // indices to locate hard clauses containing a specific variable
    static int[] hardClauses; // variables expressed ito the hard clauses they are found in
    static int[] affectedSoftClauses; // subset of soft clauses that need to be updated after a flip
    static int[] affectedHardClauses; // subset of hard clauses that need to be updated after a flip

    public static void setup()
    {
        // Read in wcard file 
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("samples/small/test3.wcard", false); 

        // Read variables directly from File Reader
        numVars = reader.getNumVars();

        softIndices = reader.getSoftIndices();
        softLiterals = reader.getSoftLiterals();
        softCosts = reader.getSoftCosts();
        softValues = reader.getSoftValues();
        softClauseIndices = reader.getSoftClauseIndices();
        softClauses = reader.getSoftClauses();

        hardIndices = reader.getHardIndices();
        hardLiterals = reader.getHardLiterals();
        hardValues = reader.getHardValues();
        hardCost = reader.getHardCost();
        hardClauseIndices = reader.getHardClauseIndices();
        hardClauses = reader.getHardClauses();

        // Quick calculations to initialise other variables
        dynamicCosts = softCosts.clone(); // array for dynamic soft costs
        numSoft = softValues.length;
        numHard = hardValues.length;
        softFloats = new int[softValues.length];
        hardFloats = new int[hardValues.length];
        affectedSoftClauses = new int[numSoft];
        affectedHardClauses = new int[numHard];

        // Create Boolean variables
        vars = new BitSet(numVars);

        // Get initial boolean assignment from preprocessing
        // All hard clauses are SAT
        int[] inital_sol = new int[numVars];
        inital_sol = reader.getInitialSol();
        for (int k=0; k < numVars; k++)
        {
            if (inital_sol[k]==1) {vars.set(k);}
        }
    }

    
    public static void main(String[] args) 
    {
        setup();

        Random random = new Random();

        // Calculate float of hard clauses
        for (int c=1; c <= numHard; c++)
        {
            hardFloats[c-1] = checkFloat(c, true);
            if (hardFloats[c-1]<0)
            {
                System.err.println("HARD CLAUSES NOT SATISFIED ON INITIAL ASSIGNMENT");
                System.exit(0);
            }
        }

        // Calculate:
        // 1. total cost of initial state
        // 2. float for each clause
        // 3. SAT for each clause
        long curTotalCost = 0;
        unsat = new ArrayList<>();
        boolean sat;
        for (int c=1; c <= numSoft; c++)
        {
            softFloats[c-1] = checkFloat(c, false);
            sat = checkSATFloat(c, false); // if current clause is unSAT, add to array
            if (!sat) 
            {
                unsat.add(c); // add to unsat arraylist
                curTotalCost += softCosts[c-1]; // Add to total cost if not SAT
            } 
        }

        // DEBUG:
        //ArrayList<Integer> debug_softFloats = new ArrayList<>();
        //IntStream.of(softFloats).forEach(debug_softFloats::add);
        //System.out.println("Soft Floats: " + debug_softFloats.toString());

        //ArrayList<Integer> debug_softCosts = new ArrayList<>();
        //IntStream.of(softCosts).forEach(debug_softCosts::add);
        //System.out.println("Soft Costs: " + debug_softCosts.toString());

        // Ouput initial cost
        System.out.println("Initial cost: "+String.valueOf(curTotalCost));

        long bestCost = Long.MAX_VALUE;
        BitSet bestAssignment = new BitSet(numVars);

        // Set up make and break scores for each variable
        long[] makeScores;
        long[] breakScores;

        // Main loop:
        int t = 0, v = 0, c = 0;
        final double RANDOM_CHANCE = 0.01;
        int curClause = -1;
        int start, end;
        boolean skip = false;
        // For score calculation
        long bestScore = 0;
        int bestFlip = -1;
        int sign = 1, vsign = 1;
        int ascIdx = 0, ahcIdx = 0;
        while (true)
        {
            // Algorithm:
            // update floats - ONLY AFFECTED CLAUSES
            // calculate cost and update best assignment - ONLY AFFECTED CLAUSES
            // pick unsat soft clause, with prob weighted by dynamic cost
            // calculate break and make costs for variables in clause
            // outlaw any flips that violate hard clauses
            // flip:
            // a) variable with highest score: make - break
            // b) with small chance, random flip

            // Update floats - ONLY AFFECTED CLAUSES
            // Calculate float of hard clauses
            for (int i=0; i < numHard; i++)
            {
                c = affectedHardClauses[i];
                if (c==0) {break;} // If we have gone through all affected hard clauses, we are done
                hardFloats[c-1] = checkFloat(c, true);
            }
            // Calculate float of soft clauses
            for (int i=0; i < numSoft; i++)
            {
                c = affectedSoftClauses[i];
                if (c==0) {break;} // If we have gone through all affected soft clauses, we are done
                softFloats[c-1] = checkFloat(c, false);
            }

            // Calculate total cost of state and update unsat array - ONLY AFFECTED CLAUSES
            curTotalCost = 0;
            unsat.clear();
            sat = true;
            for (int cl=1; cl <= numSoft; cl++)
            {
                // if current clause is unSAT, add to array
                sat = checkSATFloat(cl, false);
                if (!sat) 
                {
                    unsat.add(cl);
                    curTotalCost += softCosts[cl-1]; // Add to total cost if not SAT
                }
            }

            // Exit if cost is 0
            if (curTotalCost==0)
            {
                bestCost = curTotalCost;
                bestAssignment = (BitSet)vars.clone();
                System.out.println("Solution with cost 0 found.");
                break;
            }

            // If current assignment is the best so far, save it
            if (curTotalCost < bestCost)
            {
                bestCost = curTotalCost;
                bestAssignment = (BitSet)vars.clone();
                System.out.println("New Best Cost: " + Long.toString(bestCost));
            }

            t++;

            if (t > T) {break;} // if time is up, end run

            // Convert unsat ArrayList into int[]
            unsat_arr = new int[unsat.size()];
            for (int i = 0; i < unsat.size(); i++) {unsat_arr[i] = unsat.get(i);}

            // Reset affected clauses arrays and indices
            affectedSoftClauses = new int[numSoft]; 
            affectedHardClauses = new int[numHard]; 
            ascIdx = 0;
            ahcIdx = 0;

            // Small chance for random flip:
            if (random.nextDouble() < RANDOM_CHANCE)
            {
                skip = false;
                bestFlip = random.nextInt(numVars)+1;
                // Check hard clauses
                for (int i=hardClauseIndices[bestFlip-1]; i < hardClauseIndices[bestFlip-1+1]; i++)
                {
                    sign = (hardClauses[i] < 0) ? -1 : 1; // check sign of literal

                    // If a hard clause will get broken, ensure this variable isn't picked
                    if (hardFloats[hardClauses[i]*sign-1]==0)
                    {
                        if (vars.get(bestFlip-1) && sign==1) {skip = true; break;}
                        else if (!vars.get(bestFlip-1) && sign==-1) {skip = true; break;}
                    }
                }

                if (skip) {continue;}            
            }
            // Otherwise take greedy flip
            else 
            {
                // Pick unsat soft clause with weighted probability:
                curClause = pickClause(unsat_arr, random);

                // Get literals involved in selected clause:
                start = softIndices[curClause-1];
                end = softIndices[curClause-1+1];
                breakScores = new long[end-start];
                makeScores = new long[end-start];

                // Reset heuristic variables
                bestScore = Long.MIN_VALUE;
                bestFlip = -1;

                // Calculate scores for each literal in selected clause
                for (int i = start; i < end; i++)
                {
                    v = softLiterals[i];
                    vsign = (v<0) ? -1 : 1;
                    v = vsign*v; // get |v|

                    // Initialise make and break scores for index i of v
                    breakScores[i-start] = 0;
                    makeScores[i-start] = 0;

                    // Run for soft clauses
                    for (int j=softClauseIndices[v-1]; j < softClauseIndices[v-1+1]; j++) // check each soft clause affected by literal v
                    {
                        sign = (softClauses[j] < 0) ? -1 : 1; // check sign of literal in clause
                        if (softFloats[softClauses[j]*sign-1]==0) // check if break score will increase
                        {
                            if (vars.get(v-1) && sign==1) {breakScores[i-start] = breakScores[i-start] + softCosts[softClauses[j]*sign-1];} // increase breakscore by cost of clause
                            else if (!vars.get(v-1) && sign==-1) {breakScores[i-start] = breakScores[i-start] + softCosts[softClauses[j]*sign-1];}
                        }
                        else if (softFloats[softClauses[j]*sign-1]==-1) // check if make score will increase
                        {
                            if (!vars.get(v-1) && sign==1) {makeScores[i-start] = makeScores[i-start] + softCosts[softClauses[j]*sign-1];} // increase makescore by cost of clause
                            else if (vars.get(v-1) && sign==-1) {makeScores[i-start] = makeScores[i-start] + softCosts[softClauses[j]*sign-1];}
                        }
                    }
                    
                    // Check hard clauses
                    for (int j=hardClauseIndices[v-1]; j < hardClauseIndices[v-1+1]; j++) // check each hard clause affected by literal v
                    {
                        sign = (hardClauses[j] < 0) ? -1 : 1; // check sign of literal in clause
                        // If a hard clause will get broken, ensure this variable isn't picked by making score the min
                        if (hardFloats[hardClauses[j]*sign-1]==0)
                        {
                            if (vars.get(v-1) && sign==1) {breakScores[i-start] = 0; makeScores[i-start]= Long.MIN_VALUE;}
                            else if (!vars.get(v-1) && sign==-1) {breakScores[i-start] = 0; makeScores[i-start]= Long.MIN_VALUE;}
                        }
                    }
                    
                    // Keep track of best score
                    if (makeScores[i-start] - breakScores[i-start] > bestScore) // best is max
                    {
                        bestScore = makeScores[i-start] - breakScores[i-start];
                        bestFlip = v; //heuristic
                    }
                }

                // If all flips break a hard clause, skip to next unsat clause
                if (bestFlip==-1) {continue;}
            }

            // Flip selected variable
            vars.flip(bestFlip-1);

            // Update affected soft clauses:
            for (int i = softClauseIndices[bestFlip-1]; i < softClauseIndices[bestFlip-1+1]; i++)
            {
                sign = (softClauses[i] < 0) ? -1 : 1; // check sign of literal
                affectedSoftClauses[ascIdx++] = softClauses[i]*sign; // add |clausenum| to list
            }

            // Update affected hard clauses:
            for (int i = hardClauseIndices[bestFlip-1]; i < hardClauseIndices[bestFlip-1+1]; i++)
            {
                sign = (hardClauses[i] < 0) ? -1 : 1; // check sign of literal
                affectedHardClauses[ahcIdx++] = hardClauses[i]*sign; // add |clausenum| to list
            }
        }

        System.out.println("Final Best Cost: " + String.valueOf(bestCost));

        // Create string output
        String output = "(";
        for (int k=0; k < numVars-1; k++)
        {
            output = output + ((bestAssignment.get(k)) ? "1" : "0") + ", ";
        }
        output = output + ((bestAssignment.get(numVars-1)) ? "1" : "0") + ")";

        System.out.println("Corresponding Assignment: " + output);
    }

    public static int pickClause(int[] unsat, Random random)
    {
        // Use the Acceptance-Rejection algorithm to sample from the weighted distribution of unSAT clauses
        int totalCost = 0;
        int[] costs = new int[unsat.length];
        for (int c=0; c < unsat.length; c++)
        {
            costs[c] = softCosts[unsat[c]-1]; // get relevant weighted costs
            totalCost += costs[c]; // calculate sum of costs to normalize prob.
        }

        int unif = 0;
        while (true) 
        {
            unif = random.nextInt(unsat.length); // Uniformly draw from clauses
            if (random.nextDouble() < ( (double) costs[unif] / totalCost)) {return unsat[unif];} // Accept with prob cost/total_cost
        }
    }

    public static int checkFloat(int clauseToCheck, boolean hard)
    {
        int sum = 0;
        int r = 0;

        if (hard)
        {
            // Loop over each variable that could occur in clause
            for (int i=hardIndices[clauseToCheck-1]; i < hardIndices[clauseToCheck-1+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (hardLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(hardLiterals[i]-1)) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (hardLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(-1*hardLiterals[i]-1)) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            r = (sum - hardValues[clauseToCheck-1]); //return the float, i.e. sum - cost of clause (as we require sum to be >= cost for clause to be SAT)
        }
        else
        {
            // Loop over each variable that could occur in clause
            for (int i=softIndices[clauseToCheck-1]; i < softIndices[clauseToCheck-1+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (softLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(softLiterals[i]-1)) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (softLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(-1*softLiterals[i]-1)) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            r = (sum - softValues[clauseToCheck-1]); //return the float, i.e. sum - cost of clause (as we require sum to be >= cost for clause to be SAT)
        }

        return r;        
    }

    // This is a quicker checkSAT method that relies on the float of the array
    public static boolean checkSATFloat(int clauseToCheck, boolean hard)
    {
        if (hard) {return (hardFloats[clauseToCheck-1] >= 0);}
        else {return (softFloats[clauseToCheck-1] >= 0);}
    }

    // First principles checkSAT
    public static boolean checkSAT(int clauseToCheck, boolean hard)
    {
        int sum = 0;

        if (hard)
        {
            // Loop over each variable that could occur in clause (all vars)
            for (int i=hardIndices[clauseToCheck-1]; i < hardIndices[clauseToCheck-1+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (hardLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(hardLiterals[i]-1)) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (hardLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(-1*hardLiterals[i]-1)) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            return (sum >= hardValues[clauseToCheck-1]); //return whether the accumulated sum is geq the related value
        }
        else
        {
            // Loop over each variable that could occur in clause (all vars)
            for (int i=softIndices[clauseToCheck-1]; i < softIndices[clauseToCheck-1+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (softLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(softLiterals[i]-1)) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (softLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(-1*softLiterals[i]-1)) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            return (sum >= softValues[clauseToCheck-1]); //return whether the accumulated sum is geq the related value
        }
    }    

    public static int calcCurrentClauseCost(int clause, boolean hard)
    {   
        // If clause is satisfied, cost is 0, otherwise return associated cost
        if (hard) {return (checkSATFloat(clause, true)) ? 0 : hardCost;}
        else {return (checkSATFloat(clause, false)) ? 0 : softCosts[clause-1];}        
    }
}
