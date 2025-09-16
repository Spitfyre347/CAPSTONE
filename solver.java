import java.util.BitSet;
import java.util.Random;

public class solver {

    // Max runtime
    private final static int T = 10000000;

    static int numVars;
    static int numClauses;
    static int[] clauseCosts;
    static int[] currentCosts;
    static int[] literals;
    static int[] values;
    static BitSet vars;

    public static void main(String[] args) {
        Random random = new Random();
        
        // Read in wcard file 
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.InitializeClauses("test3.wcard", false);

        // Get data from reader
        numVars = reader.getNumVars();
        numClauses = reader.getNumClauses();
        clauseCosts = reader.getCosts();
        currentCosts = new int[numClauses]; // initialise current cost list
        literals = reader.getLiterals();
        values = reader.getValues();

        // Boolean variables
        vars = new BitSet(numVars);

        // Set up boolean assignment
        for (int i=0; i < numVars; i++)
        {
            // Initiate boolean assignment randomly
            if (random.nextInt(2) == 1)
            {
                vars.set(i);
            }
        }

        // Define 2d array to link variables to clauses
        int[][] clauses = new int[numVars][numClauses];

        // Link variables to clauses
        for (int i=0; i < numClauses; i++)
        {
            for (int j=0; j < numVars; j++)
            {
                clauses[j][i] = (literals[numVars*i+j] == 0) ? 0 : 1;
            }   
        }

        // Calculate cost of initial state
        int curCost = 0;
        for (int i=0; i < numClauses; i++)
        {
            currentCosts[i] = calcCurrentClauseCost(i);
            curCost = curCost + currentCosts[i];
        }

        System.out.println("Initial cost: " + (Integer.toString(curCost))); ///

        // Record overall best assignment and best score
        BitSet bestAssignment = new BitSet(numVars);
        int bestScore = Integer.MAX_VALUE;

        // Create new int array to store updated costs for each clause
        int[] updatedCosts = new int[numClauses];
        int t = 0;
        int minCost;
        int minCostFlip;
        int tempCost = 0;
        while (true)
        {
            // for each variable:
            //      flip, then update cost for affected clauses
            //      sum to get "total cost of flip"
            
            minCost = curCost; // for each pass, reset the min cost, and corresponding variable flip
            minCostFlip = -1;
            for (int i=0; i<numVars; i++)
            {
                // flip var
                vars.flip(i);
                tempCost = 0;

                // recalculate cost of hypothetical scenario
                for (int j=0; j < numClauses; j++)
                {
                    // If the current clause must be checked (i.e. has been affected by a flip), then update cost
                    updatedCosts[j] = (clauses[i][j] == 1) ? calcCurrentClauseCost(j) : currentCosts[j];
                    tempCost = tempCost + updatedCosts[j];
                }

                vars.flip(i); // flip var back

                // take min to get which flip results in lowest cost
                if (tempCost < minCost) 
                {
                    minCost = tempCost;
                    minCostFlip = i;
                }
                t++;
            }       
            
            // Check if no improvements made, then restart with random assignments
            if (minCostFlip == -1) 
            {
                if (minCost < bestScore)
                {
                    bestScore = minCost;
                    bestAssignment = (BitSet)vars.clone();
                    System.out.println("New Best Cost: " + Integer.toString(bestScore));
                }

                if (t > T) {break;} // if time is up, end run

                // Assuming time isn't up - restart algorithm with randomly assigned vars
                for (int i=0; i < numVars; i++)
                {
                    // Initiate boolean assignment randomly
                    if (random.nextInt(2) == 1)
                    {
                        vars.set(i);
                    }
                }

                minCostFlip = random.nextInt(numVars);
            }

            // flip variable and update clauses if it results in an improvement
            vars.flip(minCostFlip);

            // recalculate cost of current scenario
            curCost = 0;
            for (int j=0; j < numClauses; j++)
            {
                // If the current clause must be checked (i.e. has been affected by a flip), then update cost
                currentCosts[j] = (clauses[minCostFlip][j] == 1) ? calcCurrentClauseCost(j) : currentCosts[j];
                curCost = curCost + currentCosts[j];
            }

            //if (t > T) {break;} // if time is up, end run
        }

        System.out.println("Final Best Cost: " + Integer.toString(bestScore));

        // Create string output
        //String output = "(";
        //for (int k=0; k < numVars-1; k++)
        //{
        //    output = output + ((bestAssignment.get(k)) ? "1" : "0") + ", ";
        //}
        //output = output + ((bestAssignment.get(numVars-1)) ? "1" : "0") + ")";

        //System.out.println("Corresponding Assignment: " + output);
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
