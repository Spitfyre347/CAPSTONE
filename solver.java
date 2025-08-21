import java.util.BitSet;
import java.util.Random;

public class solver {

    // Max runtime
    private final static int T = 1000;

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
        reader.readFile("test.txt");

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
            System.out.println(vars.get(i)); ///
        }

        // Define 2d array to link variables to clauses
        int[][] clauses = new int[numVars][numClauses];

        // Link variables to clauses
        for (int i=0; i < numClauses; i++)
        {
            for (int j=0; j < numVars; j++)
            {
                clauses[j][i] = (literals[5*i+j] == 0) ? 0 : 1;
            }   
        }

        // Calculate cost of initial state
        int curCost = 0;
        for (int i=0; i < numClauses; i++)
        {
            currentCosts[i] = calcCurrentClauseCost(i);
            System.out.println(Integer.toString(currentCosts[i])); /// 
            curCost = curCost + currentCosts[i];
        }

        System.out.println("Initial cost: " + (Integer.toString(curCost))); ///

        // Create new int array to store updated costs for each clause
        int[] updatedCosts = new int[numClauses];
        int t = 0;
        int minCost;
        int curFlippedVar = 0;
        int minCostFlip;
        int tempCost = 0;
        while (t < T)
        {
            // for each variable:
            //      flip, then update cost for affected clauses
            //      sum to get "total cost of flip"
            
            minCost = curCost; // for each pass, reset the min cost, and corresponding variable flip
            minCostFlip = curFlippedVar;
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
                minCost = (tempCost <= minCost) ? tempCost : minCost;
                minCostFlip = (tempCost <= minCost) ? i : minCostFlip;
            }          
            // Check if no improvements can be made. Note that this must change later to avoid getting stuck in local maxima/minima  
            if (minCost >= curCost)
            {
                break;
            }

            // flip variable and update clauses if it results in an improvement
            vars.flip(minCostFlip);
            curFlippedVar = minCostFlip;

            // recalculate cost of hypothetical scenario
            curCost = 0;
            for (int j=0; j < numClauses; j++)
            {
                // If the current clause must be checked (i.e. has been affected by a flip), then update cost
                updatedCosts[j] = (clauses[minCostFlip][j] == 1) ? calcCurrentClauseCost(j) : currentCosts[j];
                curCost = curCost + updatedCosts[j];
            }
            t++;
            System.out.println("Current Cost: " + Integer.toString(curCost));
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
        return (sum <= values[clauseToCheck]); //return whether the accumulated sum is leq the related value
    }    

    public static int calcCurrentClauseCost(int clause)
    {   
        // If clause is satisfied, cost is 0, otherwise return associated cost
        return (checkSAT(clause)) ? 0 : clauseCosts[clause];
    }
}
