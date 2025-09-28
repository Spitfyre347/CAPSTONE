# Makefile for your Java program

# Compiler
JAVAC = javac
JAVA  = java

# Main class name (the one with public static void main)
MAIN = solver4

# Find all .java files automatically
SOURCES = $(wildcard *.java)
CLASSES = $(SOURCES:.java=.class)

# Default arguments (change these to what you want)
DEFAULT_ARGS = samples/small/test2.txt 10000 50000 0.01 0.1 0.5

# Default target: compile and run with default args
all: $(CLASSES)
	$(JAVA) $(MAIN) $(DEFAULT_ARGS)

# Compile .java -> .class
%.class: %.java
	$(JAVAC) $<

# Run with custom arguments: make run ARGS="..."
run: $(CLASSES)
	$(JAVA) $(MAIN) $(ARGS)

# Clean up compiled .class files
clean:
	rm -f *.class
