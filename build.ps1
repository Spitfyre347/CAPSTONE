# Compile all Java files
del *.class
javac --release 17 *.java

# Default arguments
$filename = "samples/small/test2.txt"
$T = 10000
$RESTART = 50000
$RANDOM_CHANCE = 0.01
$PROPORTION = 0.1
$PROB_HEAVY = 0.5

# Run program with defaults
java solver4 $filename $T $RESTART $RANDOM_CHANCE $PROPORTION $PROB_HEAVY
