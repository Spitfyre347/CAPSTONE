import java.util.BitSet;
import java.util.Random;

public class solver3 {

    // Max runtime
    private final static int T = 10000000;

    // Variables
    static BitSet vars; // BitSet to keep track of boolean variables
    static int numVars; // number of literals

    static int numSoft; // number of soft clauses
    static int numHard; // number of hard clauses
    static int hardCost; // cost of hard clause
    static int[] softIndices; // indices used to locate a specific soft clause in softLiterals 
    static int[] softLiterals; // soft clauses expressed ito their literals
    static int[] hardIndices; // indices used to locate a specific hard clause in hardLiterals 
    static int[] hardLiterals; // hard clauses expressed ito their literals
    static int[] softCosts; // soft clause costs
    static int[] softFloats; // floats for each soft clause
    static int[] hardFloats; // floats for each hard clause
    static int[] unsat; // unSAT soft clauses
    static int[] dynamicCosts; // weighted soft clause costs
    static int[] softValues; // k-values for soft clauses
    static int[] hardValues; // k-values for hard clauses

    public static void setup()
    {
        // Read in wcard file 
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("test.txt", false);

        // Read variables directly from File Reader
        numVars = reader.getNumVars();

        softIndices = reader.getSoftIndices();
        softLiterals = reader.getSoftLiterals();
        softCosts = reader.getSoftCosts();
        softValues = reader.getSoftValues();

        hardIndices = reader.getHardIndices();
        hardLiterals = reader.getHardLiterals();
        hardValues = reader.getHardValues();
        hardCost = reader.getHardCost();

        // Quick calculations to initialise other variables
        dynamicCosts = softCosts.clone(); // array for dynamic soft costs
        numSoft = softValues.length;
        numHard = hardValues.length;

        // Create Boolean variables
        vars = new BitSet(numVars);

        // Get initial boolean assignment from preprocessing
        // All hard clauses are SAT

        // Link variables to clauses
    }

    public int[] returnClause(boolean soft, int index){
        int[] softLits=null, softInds=null;
        int[] hardLits=null, hardInds=null;
        // idk where u assign these, don't think you've pushed to git, but just replace these with ur actual ones.
        
        int[] outArr;
        int size=0;

        if (soft){
            if (index < softInds.length-1)
                size = softInds[index+1] - softInds[index];
            else if (index == softInds.length -1) 
                size = (softLits.length) - softInds[index];
            else
                throw new IndexOutOfBoundsException();
             
            outArr = new int[size];
            for (int i = 0; i < size; i++){
                outArr[i] = softLits[softInds[index]+i];
            }
            return outArr;
        }
        else{ // returning a hard clause
            if (index < hardInds.length-1)
                size = hardInds[index+1] - hardInds[index];
            else if (index == hardInds.length -1) 
                size = (hardLits.length) - hardInds[index];
            else
                throw new IndexOutOfBoundsException();
             
            outArr = new int[size];
            for (int i = 0; i < size; i++){
                outArr[i] = hardLits[hardInds[index]+i];
            }
            return outArr;
        }
    } 
    
    public static void main(String[] args) 
    {
        setup();

        Random random = new Random();

        // Calculate total cost of initial state and check which clauses are SAT
        long curTotalCost = 0;
        unsat = new int[numSoft];
        int index = 0;
        boolean sat = true;
        for (int c=0; c < numSoft; c++)
        {
            // if current clause is unSAT, add to array
            sat = checkSAT(c);
            if (!sat)
            {
                unsat[index] = c; 
                index++;
            }

            // Add to total cost if not SAT
            curTotalCost += (sat) ? 0 : softCosts[c]; 
        }

        System.out.println("Initial cost: "+String.valueOf(curTotalCost));

        long bestCost = Long.MAX_VALUE;
        BitSet bestAssignment = new BitSet(numVars);

        // Set up make and break scores for each variable
        long[] makeScores;
        long[] breakScores;

        // Main loop:
        int t = 0;
        int v = 0;
        final double RANDOM_CHANCE = 0.01;
        int curClause = -1;
        int start, end;
        while (true)
        {
            // Algorithm:
            // calculate cost and update best assignment
            // pick unsat soft clause, with prob weighted by dynamic cost
            // calculate break and make costs for variables in clause
            // outlaw any flips that violate hard clauses
            // flip:
            // a) variable with highest score: make - break
            // b) with small chance, random flip

            // Calculate total cost of state and update unsat array
            curTotalCost = 0;
            unsat = new int[numSoft];
            index = 0;
            sat = true;
            for (int c=0; c < numSoft; c++)
            {
                // if current clause is unSAT, add to array
                sat = checkSAT(c);
                if (!sat)
                {
                    unsat[index] = c; 
                    index++;
                }

                // Add to total cost if not SAT
                curTotalCost += (sat) ? 0 : softCosts[c]; 
            }

            // If current assignment is the best so far, save it
            if (curTotalCost < bestCost)
            {
                bestCost = curTotalCost;
                bestAssignment = (BitSet)vars.clone();
                System.out.println("New Best Cost: " + Long.toString(bestCost));
            }

            // Pick unsat soft clause with weighted probability:
            curClause = pickClause(unsat, random);

            // Get literals involved in selected clause:
            start = softIndices[curClause];
            end = softIndices[curClause+1];
            makeScores = new long[end-start];
            breakScores = new long[end-start];

            // Calculate make and break scores for each literal in selected clause
            for (int i = start; i < end; i++)
            {
                v = softLiterals[i];
                vars.flip(v); // flip v for now and see what breaks
                for (int c = ; c < ; c++)
                {
                    
                }
            }


            t++;

            if (t > T) {break;} // if time is up, end run
        }

        System.out.println("Final Best Cost: " + String.valueOf(bestCost));

        // Create string output
        //String output = "(";
        //for (int k=0; k < numVars-1; k++)
        //{
        //    output = output + ((bestAssignment.get(k)) ? "1" : "0") + ", ";
        //}
        //output = output + ((bestAssignment.get(numVars-1)) ? "1" : "0") + ")";

        //System.out.println("Corresponding Assignment: " + output);
    }

    public static int pickClause(int[] unsat, Random random)
    {
        // Use the Acceptance-Rejection algorithm to sample from the weighted distribution of unSAT clauses
        int totalCost = 0;
        int[] costs = new int[unsat.length];
        for (int c=0; c < unsat.length; c++)
        {
            costs[c] = dynamicCosts[unsat[c]]; // get relevant weighted costs
            totalCost += costs[c]; // calculate sum of costs to normalize prob.
        }

        int unif = 0;
        while (true) 
        {
            unif = random.nextInt(unsat.length); // Uniformly draw from clauses
            if (random.nextDouble() < costs[unif] / totalCost) {return unsat[unif];} // Accept with prob cost/total_cost
        }
    }

    public static int checkFloat(int clauseToCheck, boolean hard)
    {
        int sum = 0;

        if (hard)
        {
            // Loop over each variable that could occur in clause
            for (int i=hardIndices[clauseToCheck]; i < hardIndices[clauseToCheck+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (hardLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(i)) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (hardLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(i)) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            return (sum - hardValues[clauseToCheck]); //return the float, i.e. sum - cost of clause (as we require sum to be >= cost for clause to be SAT)
        }
        else
        {
            // Loop over each variable that could occur in clause
            for (int i=softIndices[clauseToCheck]; i < softIndices[clauseToCheck+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (softLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(i)) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (softLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(i)) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            return (sum - softValues[clauseToCheck]); //return the float, i.e. sum - cost of clause (as we require sum to be >= cost for clause to be SAT)
        }
        
    }

    public static boolean checkSAT(int clauseToCheck, boolean hard)
    {
        int sum = 0;

        if (hard)
        {
            // Loop over each variable that could occur in clause (all vars)
            for (int i=hardIndices[clauseToCheck]; i < hardIndices[clauseToCheck+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (hardLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(hardLiterals[i])) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (hardLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(hardLiterals[i])) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            return (sum >= hardValues[clauseToCheck]); //return whether the accumulated sum is geq the related value
        }
        else
        {
            // Loop over each variable that could occur in clause (all vars)
            for (int i=softIndices[clauseToCheck]; i < softIndices[clauseToCheck+1]; i++)
            {
                // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
                if (softLiterals[i] > 0)
                {
                    sum = sum + ((vars.get(softLiterals[i])) ? 1 : 0);
                }
                // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
                else if (softLiterals[i] < 0)
                {
                    sum = sum + ((vars.get(softLiterals[i])) ? 0 : 1);
                }
                // If the literal is 0, don't consider it (no "else" needed)
            }
            return (sum >= softValues[clauseToCheck]); //return whether the accumulated sum is geq the related value
        }
    }    

    public static int calcCurrentClauseCost(int clause, boolean hard)
    {   
        // If clause is satisfied, cost is 0, otherwise return associated cost
        if (hard) {return (checkSAT(clause, true)) ? 0 : hardCost;}
        else {return (checkSAT(clause, false)) ? 0 : softCosts[clause];}
        
    }
}
