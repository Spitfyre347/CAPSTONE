import random

n_vars = 15
n_constraints = 10000
top = 100  # weight for hard constraints

with open("unsat_soft.wcard", "w") as f:
    f.write(f"p wcard {n_vars} {n_constraints} {top}\n\n")

    # Add one hard constraint just to make the formula feasible
    f.write(f"{top} 1 1 2 0\n")  # at least one of x1 or x2 must be true

    # Add a contradictory pair of soft constraints on x1
    f.write("2 1 1 0\n")   # requires x1 = True
    f.write("3 1 -1 0\n")  # requires x1 = False

    # Generate the remaining random soft constraints
    for _ in range(n_constraints - 3):
        weight = random.choice([1, 2, 5])
        k = random.randint(1, n_vars)
        lits = [random.choice([i+1, -(i+1)]) for i in range(n_vars)]
        f.write(f"{weight} {k} " + " ".join(map(str, lits)) + " 0\n")
