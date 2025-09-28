import java.util.BitSet;
import java.util.Random;

public class solver4 {

    // Max runtime
    private final static int T = 100000;
    private static long startTime, endTime;
    static double scaling = 1;

    // Variables
    static BitSet vars; // BitSet to keep track of boolean variables
    static int numVars; // number of literals
    static int numSoft; // number of soft clauses
    static int numHard; // number of hard clauses
    static int hardCost; // cost of hard clause

    // Integer arrays
    static int[] softIndices; // indices used to locate literals in a specific soft clause in softLiterals 
    static int[] softLiterals; // soft clauses expressed ito their literals
    static int[] hardIndices; // indices used to locate literals in a specific hard clause in hardLiterals 
    static int[] hardLiterals; // hard clauses expressed ito their literals
    static int[] softCosts; // soft clause costs
    static int[] softFloats; // floats for each soft clause
    static int[] hardFloats; // floats for each hard clause
    static int[] hardUnsat; // unSAT hard clauses
    static int[] softUnsat; // unSAT soft clauses
    static int unsat_end; // keep track of end of unsat array
    static int unsat_remove; // element to be removed next
    static int[] unsat_indices; // lookup table to find index of unsat clause
    static double[] dynamicCosts; // weighted soft clause costs
    static int[] softValues; // k-values for soft clauses
    static int[] hardValues; // k-values for hard clauses
    static int[] softClauseIndices; // indices to locate soft clauses containing a specific variable
    static int[] softClauses; // variables expressed ito the soft clauses they are found in
    static int[] hardClauseIndices; // indices to locate hard clauses containing a specific variable
    static int[] hardClauses; // variables expressed ito the hard clauses they are found in

    // Tracking stats
    static long bestCost = Long.MAX_VALUE;
    static BitSet bestAssignment = new BitSet(numVars);
    static long curTotalCost;

    //////////////////
    // MAIN METHODS //
    //////////////////

    public static void setup()
    {
        // Read in wcard file 
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("sample.wcard", false); 

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
        numSoft = softValues.length;
        numHard = hardValues.length;
        softFloats = new int[numSoft];
        hardFloats = new int[numHard];
        unsat_indices = new int[numSoft];

        // Create Boolean variables
        vars = new BitSet(numVars);

        // Get initial boolean assignment from preprocessing
        int[] inital_sol = new int[numVars];
        inital_sol = reader.getInitialSol();
        for (int k=0; k < numVars; k++)
        {
            if (inital_sol[k]==1) {vars.set(k);}
        }

    }

    public static void initialize_solver()
    {
        // Calculate float of hard clauses
        for (int c=1; c <= numHard; c++)
        {
            hardFloats[c-1] = checkFloat(c, true);
        }

        // Calculate:
        // 1. total cost of initial state
        // 2. float for each clause
        // 3. SAT for each clause
        curTotalCost = 0;
        softUnsat = new int[numSoft];
        unsat_end = 0;
        unsat_remove = 0;
        boolean sat;
        for (int c=1; c <= numSoft; c++)
        {
            softFloats[c-1] = checkFloat(c, false);
            sat = checkSATFloat(c, false); // if current clause is unSAT, add to array
            if (!sat) 
            {
                softUnsat[unsat_end++] = c; // add to unsat array
                unsat_indices[c-1] = unsat_end-1; // update lookup
                curTotalCost += softCosts[c-1]; // Add to total cost if not SAT
            }
            else {unsat_indices[c-1]=-1;}
        }

        // Ouput initial cost
        System.out.println("Initial cost: "+String.valueOf(curTotalCost));
    }

    public static void solve()
    {
        // VARIABLE INITIALISATION
        Random random = new Random();

        // Set up make and break scores for each variable
        long[] makeScores = new long[numVars];
        long[] breakScores = new long[numVars];

        // Temp variables
        int t = 0, v = 0, c = 0;
        int lastImproved = 0;
        int curClause = -1;
        int start, end;
        boolean skip = false;
        int sign = 1, vsign = 1;
        int removed;

        // To keep track of best score
        long bestScore = 0;
        int bestFlip = -1;

        // Parameters
        final double RANDOM_CHANCE = 0.01;

        // MAIN LOOP
        while (true)
        {
            // Algorithm:
            // update floats - ONLY AFFECTED CLAUSES
            // calculate cost and update best assignment - ONLY AFFECTED CLAUSES
            // pick unsat soft clause, with prob weighted by cost
            // calculate break and make costs for variables in clause
            // outlaw any flips that violate hard clauses
            // flip:
            // a) variable with highest score: make - break
            // b) with small chance, random flip            

            if (bestFlip != -1)
            {
                // Update floats - ONLY AFFECTED CLAUSES
                // Calculate float of hard clauses
                for (int i = hardClauseIndices[bestFlip-1]; i < hardClauseIndices[bestFlip-1+1]; i++)
                {
                    sign = (hardClauses[i] < 0) ? -1 : 1; // check sign of literal
                    c = hardClauses[i]*sign;
                    hardFloats[c-1] = checkFloat(c, true);
                }
                // Calculate float of soft clauses
                for (int i = softClauseIndices[bestFlip-1]; i < softClauseIndices[bestFlip-1+1]; i++)
                {
                    sign = (softClauses[i] < 0) ? -1 : 1;
                    c = softClauses[i]*sign; // get |clause|
                    softFloats[c-1] = checkFloat(c, false);

                    // Also update unsat array and total cost
                    if (softFloats[c-1] >= 0 && unsat_indices[c-1] != -1) // i.e. if SAT and in unsat (we need to remove from unsat)
                    {
                        // NOTE: This is why the unsat clause selection is APPROXIMATE LRU. 
                        // When random soft clauses that get satisfied by flips need to be removed, 
                        // the last elements get moved to essentially random positions, slightly messing up the order. 
                        // It is by far the most performant way to handle this, 
                        // with only a slight loss due to the LRU no longer being exact.
                        // Plus: as we have been told, a bit of randomness is always good ;)

                        // remove if was unSAT and is now SAT                      
                        softUnsat[unsat_indices[c-1]] = softUnsat[unsat_end-1]; // Move element at the end of the array to removed element (c)
                        unsat_indices[softUnsat[unsat_end-1]-1] = unsat_indices[c-1]; // update lookup
                        unsat_indices[c-1] = -1;
                        unsat_end--; // decrease "end point"

                        curTotalCost -= softCosts[c-1];
                    }
                    else if (softFloats[c-1] < 0 && unsat_indices[c-1] == -1) // if clause is unSAT and not in unsat, we must add it
                    {
                        // add if was SAT and is now unSAT
                        softUnsat[unsat_end++] = c;

                        // update lookup
                        unsat_indices[c-1] = unsat_end-1;

                        curTotalCost += softCosts[c-1];
                    }
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
                lastImproved = t;
                bestAssignment = (BitSet)vars.clone();
                System.out.println("New Best Cost: " + Long.toString(bestCost));
            }

            t++;

            if (t > T) {System.out.println("Max time reached"); break;} // if time is up, end run
            //if (t-lastImproved>1000) {System.out.println("No improvement, timeout at t = "+String.valueOf(t)); break;} // if no improvements have been made in a while, break

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
                //curClause = pickClause(softUnsat, random);
                
                // Pick unsat clause using approx LRU
                removed = unsat_remove;
                unsat_remove = (unsat_remove+1) % unsat_end; // cycle through unsat array
                curClause = softUnsat[removed];
                
                // Move last element of softUnsat to position of element just removed 
                softUnsat[removed] = softUnsat[unsat_end-1];
                unsat_indices[softUnsat[unsat_end-1]-1] = removed; // update index 

                // Move selected clause to end
                softUnsat[unsat_end-1] = curClause;    
                unsat_indices[curClause-1] = unsat_end-1;            

                // Get literals involved in selected clause:
                start = softIndices[curClause-1];
                end = softIndices[curClause-1+1];

                // Reset heuristic variables
                bestScore = Long.MIN_VALUE;
                bestFlip = -1;

                // Calculate scores for each literal in selected clause
                for (int i = start; i < end; i++)
                {
                    v = softLiterals[i];
                    vsign = (v<0) ? -1 : 1;
                    v = vsign*v; // get |v|

                    // Initialise make and break scores for index i of v (index runs from 0 to num literals in clause)
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
        }
    }

    public static void output()
    {
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
    
    public static void main(String[] args) 
    {
        setup();
        initialize_solver();
        StartTimer();
        solve();
        StopTimer();
        output();        
    }

    ////////////////////
    // HELPER METHODS //
    ////////////////////

    public static int pickClause(int[] unsat, Random random)
    {
        // Use the Acceptance-Rejection algorithm to sample from the weighted distribution of unSAT clauses
        double totalCost = 0;
        double weight;
        double[] costs = new double[unsat.length];
        for (int c=0; c < unsat.length; c++)
        {
            weight = scaling * dynamicCosts[unsat[c]-1];
            costs[c] = (weight > 1) ? weight * softCosts[unsat[c]-1] : softCosts[unsat[c]-1]; // get relevant weighted costs (min 1*cost)
            totalCost += costs[c]; // calculate sum of costs to normalize prob.
        }

        int k = 0;
        int unif = 0;
        while (true) 
        {
            k++;
            unif = random.nextInt(unsat.length); // Uniformly draw from clauses
            
            // Accept with prob cost/total_cost, weighted by a quadratic counter to ensure it doesn't run too long (max. 1000)
            if (random.nextDouble() < ( (double) costs[unif] / totalCost + 0.000001*k*k)) {return unsat[unif];}
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

    private static void StartTimer(){
        startTime = System.currentTimeMillis();
    }

    private static void StopTimer(){
        endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + elapsedTime + " ms");
    }
}
