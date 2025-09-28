# Compiler and runtime
JAVAC = javac
JAVA  = java

# Main class
MAIN = solver4

# Find all Java source files
SOURCES = $(wildcard *.java)
CLASSES = $(SOURCES:.java=.class)

# Default arguments
DEFAULT_ARGS = myfile.txt 10000 50000 0.01 0.1 0.5
NUM_RUNS = 5

# Default target: compile and run default number of runs in parallel
all: $(CLASSES)
	@echo "Launching $(NUM_RUNS) parallel runs..."
	@for i in $(shell seq 1 $(NUM_RUNS)); do \
		$(JAVA) $(MAIN) $(DEFAULT_ARGS) & \
	done
	@wait
	@echo "All runs completed."

# Compile .java -> .class
%.class: %.java
	$(JAVAC) $<

# Run with custom arguments
run: $(CLASSES)
	$(JAVA) $(MAIN) $(ARGS)

# Clean up compiled .class files
clean:
	rm -f *.class
