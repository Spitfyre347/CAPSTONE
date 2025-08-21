import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Arrays;

public class CapstoneFileReader{

    // Instance variables to hold the parsed data
    // These arrays will be populated with the data read from the file
    private int[] costs = null;
    private int[] literals = null;
    private String[] operators = null;
    private int[] values = null;

    int numVariables = 0;
    int numClauses = 0;

    // Getters for the instance variables
    public int getNumVars() { return numVariables; }
    public int getNumClauses() { return numClauses; }
    public int[] getCosts() { return costs; }


    /**
     * Reads a file and parses its contents into the instance variables.
     * The file is expected to be in a specific format as described in the comments.
     *
     * @param path The path to the file to be read.
     */
    public void readFile(String path)
    {

        try (BufferedReader bReader = new BufferedReader(new FileReader(path))) {

            boolean initialised = false;

            String line = "";
            int clauseCounter = 0;
            int hardCost = 0;

            String[] lineHolder = null;

            while ((line = bReader.readLine()) != null){

                //We make sure that it isn't a blank line
                line = line.trim();

                if(line.isEmpty()) continue;

                //If it's a comment line, we don't care
                if (line.charAt(0) == 'c'){
                    //do nothing
                } 
                
                //If it's p, we want to initialise all our values
                else if(line.charAt(0) == 'p'){

                    //If there has been more than one header line, we stop reading the file
                    if(initialised){
                        throw new IllegalStateException("You cannot have more than one header line!");
                    }
                    lineHolder = line.split("\\s+");

                    numClauses = Integer.parseInt(lineHolder[3]);
                    numVariables =Integer.parseInt(lineHolder[2]);
                    hardCost = Integer.parseInt(lineHolder[4]);

                    costs = new int[numClauses];
                    literals = new int[numClauses*numVariables];
                    operators = new String[numClauses];
                    values = new int[numClauses];

                    initialised = true;

                } else {

                    lineHolder = line.split("\\s+");
                    //At this point, the first character is either a cost for a clause, or random garbage
                    try{
                        int num = Integer.parseInt(lineHolder[0]);
                        //Costs may not be negative, so if one is we stop reading the line
                        if(num < 0){
                            System.out.println("Invalid line detected - A cost may not be negative");
                            System.out.println("Line: " + line);
                            break;
                        }

                        //Is the clause too short? Not sure how to check if it's too long yet. Hard cost?
                        if(lineHolder.length < 3){
                            System.out.println("Invalid line - Line too short, missing information");
                            System.out.println("Line: " + line);
                        }

                        costs[clauseCounter] = Integer.parseInt(lineHolder[0]);
                        
                        int literalCounter = 1;
                        int literalNum = Integer.parseInt(lineHolder[literalCounter]);
                        //Now, we finally have a valid clause. Now, does it have a string operator, or does it end in zero?
                        try{
                            int num2 = Integer.parseInt(lineHolder[lineHolder.length-2]);
                            //It ends in zero (checking this still needs to be added)

                            while(literalNum !=0){
                                literals[(Math.abs(literalNum)-1) + (clauseCounter) * numVariables] = literalNum;
                                literalCounter++;
                                literalNum = Integer.parseInt(lineHolder[literalCounter]);
                            }

                            values[clauseCounter] = Integer.parseInt(lineHolder[lineHolder.length-1]);
                            clauseCounter++;

                        } catch(Exception e){
                            //It has a string operator in the second last position

                            while (literalCounter < lineHolder.length -2){
                                literals[(Math.abs(literalNum)-1) + (clauseCounter) * numVariables] = literalNum;
                                literalCounter++;

                                //Temporary workaround (needs attention later)
                                if(literalCounter == lineHolder.length - 2){
                                    break;
                                }
                                literalNum = Integer.parseInt(lineHolder[literalCounter]);
                            }

                            operators[clauseCounter] = lineHolder[literalCounter];
                            values[clauseCounter] = Integer.parseInt(lineHolder[lineHolder.length-1]);
                            clauseCounter++;
                        }
                        
                        

                    //If the line we read starts with something other than p, c, or a cost then we stop reading the file.
                    } catch (Exception e){
                        System.out.println("Invalid line detected - Line does not fit format of clause, comment or header");
                        System.out.println("Line: " + line);
                        e.printStackTrace();
                        break;
                    }

                }
            }

            System.out.println("Costs:");
            System.out.println(Arrays.toString(costs));
            System.out.println("Literals:");
            System.out.println(Arrays.toString(literals));
            System.out.println(literals.length);
            System.out.println("Operators:");
            System.out.println(Arrays.toString(operators));
            System.out.println("Values:");
            System.out.println(Arrays.toString(values));

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CapstoneFileReader reader = new CapstoneFileReader();
        reader.readFile("test.txt");
    }

}
