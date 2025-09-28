HOW TO RUN:

Windows:
Run the following command:
.\make.bat FILENAME T RESTART RANDOM_CHANCE PROPORTION PROB_HEAVY NUM_RUNS
With default parameters:
.\make.bat

Linux:
Run the following command: 
make FILENAME=<myfile.txt> T=<10000> RESTART=<50000> RANDOM_CHANCE=<0.01> PROPORTION=<0.1> PROB_HEAVY=<0.5> NUM_RUNS=<1>
With default parameters:
make

The parameters are as follows:
FILENAME: the path of the problem file to solve
T: the maximum number of iterations that should be run 
RESTART: the number of iterations that should pass with no improvement before a random restart occurs
RANDOM_CHANCE: the probability of a random flip occurring (instead of greedy)
PROPORTION: the proportion of variables to randomly flip on random restart
PROB_HEAVY: the probability that a clause is chosen only from the "heavy" clauses (clauses with weight greater than the average)
NUM_RUNS: the number of simultaneous parallel runs of the solver that should take place

Default parameters are included in the make and batch files.

Note that solver4_windows.java and solver4.java are identical except that solver4_windows.java includes a scanner at the end 
of the main method so the terminal remains open after the program finishes running. The batchfile will automatically run the Windows version,
and the makefile will automatically run the regular version suitable for Linux.
