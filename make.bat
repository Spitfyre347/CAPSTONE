@echo off
REM ================================
REM Build and run solver4 Java program
REM ================================

REM --- Step 1: Clean old class files ---
if exist *.class (
    del /Q *.class
)

REM --- Step 2: Compile all Java files targeting Java 17 ---
javac --release 17 solver4.java CapstoneFileReader.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

REM --- Step 3: Set default arguments ---
set filename=test2.txt
set T=10000
set RESTART=50000
set RANDOM_CHANCE=0.01
set PROPORTION=0.1
set PROB_HEAVY=0.5

REM --- Step 4: Run the program ---
java solver4 %filename% %T% %RESTART% %RANDOM_CHANCE% %PROPORTION% %PROB_HEAVY%
if errorlevel 1 (
    echo Program failed to run!
    pause
    exit /b 1
)

echo.
echo Program finished successfully.
pause
