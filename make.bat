@echo off
REM ================================
REM Build and run solver4 Java program multiple times
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
set filename=myfile.txt
set T=10000
set RESTART=50000
set RANDOM_CHANCE=0.01
set PROPORTION=0.1
set PROB_HEAVY=0.5
set NUM_RUNS=5

REM --- Step 4: Run multiple instances in parallel ---
for /L %%i in (1,1,%NUM_RUNS%) do (
    start "" java solver4 %filename% %T% %RESTART% %RANDOM_CHANCE% %PROPORTION% %PROB_HEAVY%
)

echo.
echo Launched %NUM_RUNS% parallel runs.
pause
