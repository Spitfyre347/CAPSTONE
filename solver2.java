import java.util.BitSet;
import java.util.Random;

public class solver2 {

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
        reader.InitializeClauses("test.txt", false);

        // Get data from reader
        numVars = reader.getNumVars();
        numClauses = reader.getNumClauses();
        clauseCosts = reader.getCosts();
        currentCosts = new int[numClauses]; // initialise current cost list
        literals = reader.getLiterals();
        values = reader.getValues();

        // Set up float for each clause
        int[] clauseFloats = new int[numClauses];

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

        // Set up make and break scores for each variable
        long[] makeScores = new long[numVars];
        long[] breakScores = new long[numVars];

        // Define flattened 2d array to link variables to clauses
        int[] clauses = new int[numVars*numClauses];

        // Link variables to clauses
        for (int i=0; i < numClauses; i++)
        {
            for (int j=0; j < numVars; j++)
            {
                clauses[j*numClauses + i] = (literals[numVars*i+j] == 0) ? 0 : 1;
            }   
        }

        // Calculate floats and total cost of initial state
        long curTotalCost = 0;
        for (int i=0; i < numClauses; i++)
        {
            clauseFloats[i] = checkFloat(i);
            curTotalCost += calcCurrentClauseCost(i);
        }

        System.out.println("Initial cost: "+String.valueOf(curTotalCost));

        // Calculate make and break scores for each variable:
        long bestBreakScore = Long.MAX_VALUE;
        long bestMakeScore = 0;
        int bestFlip = -1;
        int sign = 1;
        for (int v=0; v < numVars; v++) // iterate through each variable
        {
            // Initialise make and break scores
            breakScores[v] = 0;
            makeScores[v] = 0;

            for (int c=0; c < numClauses; c++) // check each clause per variable
            {
                if (clauses[v*numClauses+c] == 1) // only consider clauses that contain the current variable
                {
                    sign = (literals[c*numVars+v] < 0) ? -1 : 1; // check sign of literal

                    if (clauseFloats[c]==0) // check if break score will increase
                    {
                        if (vars.get(v) && sign==1) {breakScores[v] = breakScores[v] + clauseCosts[c];} // increase breakscore by cost of clause
                        else if (!vars.get(v) && sign==-1) {breakScores[v] = breakScores[v] + clauseCosts[c];}
                    }
                    else if (clauseFloats[c]==-1) // check if make score will increase
                    {
                        if (!vars.get(v) && sign==1) {makeScores[v] = makeScores[v] + clauseCosts[c];} // increase makescore by cost of clause
                        else if (vars.get(v) && sign==-1) {makeScores[v] = makeScores[v] + clauseCosts[c];}
                    }
                }
            }
            
            // Keep track of best break score
            if (breakScores[v] < bestBreakScore) // best is minimum
            {
                bestBreakScore = breakScores[v];
                bestFlip = v; // our heuristic prioritises break score
            }
            else if (breakScores[v] == bestBreakScore) // tiebreaker
            {
                if (makeScores[v] > bestMakeScore) // best is maximum
                {
                    bestMakeScore = makeScores[v];
                    bestFlip = v;
                }
            }
            // Keep track of best make score
            if (makeScores[v] > bestMakeScore) // best is maximum
            {
                bestMakeScore = makeScores[v];
            }

            System.out.println(String.valueOf(v)+" - break score: " + String.valueOf(breakScores[v]));
            System.out.println(String.valueOf(v)+" - make score: " + String.valueOf(makeScores[v]));
            System.out.println("Best flip: "+String.valueOf(bestFlip));
        }

        int t = 0;
        final double RANDOM_CHANCE = 0.01;
        while (true)
        {
            // Algorithm:
            // flip:
            // a) variable with highest breakscore
            // b) if tied, variable with highest makescore
            // c) with small chance, random flip
            // recalculate float for each clause
            // recalculate break and make scores for each variable

            t++;

            // Small chance for random flip:
            if (random.nextDouble() < RANDOM_CHANCE) {bestFlip = random.nextInt(numVars);}
            // Otherwise take greedy flip
            else {vars.flip(bestFlip);}

            // Recalculate floats
            for (int i=0; i < numClauses; i++)
            {
                clauseFloats[i] = checkFloat(i);
            }

            // Calculate make and break scores for each variable:
            bestBreakScore = Long.MAX_VALUE;
            bestMakeScore = 0;
            bestFlip = -1;
            for (int v=0; v < numVars; v++) // iterate through each variable
            {
                // Initialise make and break scores
                breakScores[v] = Long.MAX_VALUE;
                makeScores[v] = 0;

                for (int c=0; c < numClauses; c++) // check each clause per variable
                {
                    if (clauses[v*numClauses+c] == 1) // only consider clauses that contain the current variable
                    {
                        sign = (literals[c*numVars+v] < 0) ? -1 : 1; // check sign of literal

                        if (clauseFloats[c]==0) // check if break score will increase
                        {
                            if (vars.get(v) && sign==1) {breakScores[v] = breakScores[v] + clauseCosts[c];} // increase breakscore by cost of clause
                            else if (!vars.get(v) && sign==-1) {breakScores[v] = breakScores[v] + clauseCosts[c];}
                        }
                        else if (clauseFloats[c]==-1) // check if make score will increase
                        {
                            if (!vars.get(v) && sign==1) {makeScores[v] = makeScores[v] + clauseCosts[c];} // increase makescore by cost of clause
                            else if (vars.get(v) && sign==-1) {makeScores[v] = makeScores[v] + clauseCosts[c];}
                        }
                    }
                }
                
                // Keep track of best break score
                if (breakScores[v] < bestBreakScore) // best is minimum
                {
                    bestBreakScore = breakScores[v];
                    bestFlip = v; // our heuristic prioritises break score
                }
                else if (breakScores[v] == bestBreakScore) // tiebreaker
                {
                    if (makeScores[v] > bestMakeScore) // best is maximum
                    {
                        bestMakeScore = makeScores[v];
                        bestFlip = v;
                    }
                }
                // Keep track of best make score
                if (makeScores[v] > bestMakeScore) // best is maximum
                {
                    bestMakeScore = makeScores[v];
                }
            }

            // Calculate and display total cost periodically to check progress
            if (t % 100000 == 0)
            {
                // Calculate cost
                curTotalCost = 0;
                for (int c=0; c < numClauses; c++)
                {
                    curTotalCost += calcCurrentClauseCost(c);
                }
                System.out.println("Clause cost at time " + String.valueOf(t) + ": " + String.valueOf(curTotalCost));
            }
            
            if (t > T) {break;} // if time is up, end run
        }

        System.out.println("Final Best Cost: " + String.valueOf(curTotalCost));

        // Create string output
        //String output = "(";
        //for (int k=0; k < numVars-1; k++)
        //{
        //    output = output + ((bestAssignment.get(k)) ? "1" : "0") + ", ";
        //}
        //output = output + ((bestAssignment.get(numVars-1)) ? "1" : "0") + ")";

        //System.out.println("Corresponding Assignment: " + output);
    }

    public static int checkFloat(int clauseToCheck)
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
        return (sum - values[clauseToCheck]); //return the float, i.e. sum - cost of clause (as we require sum to be >= cost for clause to be SAT)
    }

    public static boolean checkSAT(int clauseToCheck)
    {
        return (checkFloat(clauseToCheck) >= 0); //return whether the float is >= 0
    }    

    public static int calcCurrentClauseCost(int clause)
    {   
        // If clause is satisfied, cost is 0, otherwise return associated cost
        return (checkSAT(clause)) ? 0 : clauseCosts[clause];
    }
}
