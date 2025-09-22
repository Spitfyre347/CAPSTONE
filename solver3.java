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
    static int[] softCosts; // soft clause costs
    static int[] unsat; // unSAT soft clauses
    static int[] dynamicCosts; // weighted soft clause costs
    static int[] softValues; // k-values for soft clauses
    static int[] hardValues; // k-values for hard clauses

    public static void setup()
    {

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
        Random random = new Random();
        
        // Read in wcard file 
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("test.txt", false);

        // Boolean variables
        vars = new BitSet(numVars);

        // Create an array for dynamic soft clause costs
        dynamicCosts = new int[numSoft];

        // Get initial boolean assignment from preprocessing
        // All hard clauses are SAT

        // Set up make and break scores for each variable
        long[] makeScores = new long[numVars];
        long[] breakScores = new long[numVars];

        // Link variables to clauses

        // Calculate total cost of initial state
        long curTotalCost = 0;
        for (int i=0; i < numSoft; i++)
        {
            curTotalCost += calcCurrentClauseCost(i);
        }

        System.out.println("Initial cost: "+String.valueOf(curTotalCost));

        long bestCost = Long.MAX_VALUE;
        BitSet bestAssignment = new BitSet(numVars);

        int t = 0;
        final double RANDOM_CHANCE = 0.01;
        int curClause = -1;
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

            // Calculate total cost of state
            curTotalCost = 0;
            for (int i=0; i < numSoft; i++) // only iterate over soft clauses, as we enforce hard cost = 0
            {
                curTotalCost += calcCurrentClauseCost(i);
            }

            if (curTotalCost < bestCost)
            {
                bestCost = curTotalCost;
                bestAssignment = (BitSet)vars.clone();
                System.out.println("New Best Cost: " + Long.toString(bestCost));
            }

            // Pick unsat soft clause with weighted probability:
            curClause = pickClause(unsat, random);

            // Iterate through variables in curClause
            for (int v=0; )


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

    public static boolean checkSAT(int clauseToCheck)
    {
        int sum = 0;
        // Loop over each variable that could occur in clause (all vars)
        for (int i=0; i < numVars; i++)
        {
            // If a positive literal is mentioned, check if it is set, and if so, add to total value on LHS of expression
            if (literals[numVars*clauseToCheck+i] > 0)
            {
                sum = sum + ((vars.get(i)) ? 1 : 0);
            }
            // If a negative literal is mentioned, check if it is NOT set, and if not, add to total value on LHS of expression
            else if (literals[numVars*clauseToCheck+i] < 0)
            {
                sum = sum + ((vars.get(i)) ? 0 : 1);
            }
            // If the literal is 0, don't consider it (no "else" needed)
        }
        return (sum >= values[clauseToCheck]); //return whether the accumulated sum is geq the related value
    }    

    public static int calcCurrentClauseCost(int clause)
    {   
        // If clause is satisfied, cost is 0, otherwise return associated cost
        return (checkSAT(clause)) ? 0 : clauseCosts[clause];
    }
}
