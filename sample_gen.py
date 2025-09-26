import random

n_vars = 50
n_constraints = 2000
top = 100  # weight for hard constraints

operators = [">=", "<=", "="]

with open("sample.wcard", "w") as f:
    # Write header (optional, since you are using a custom format)
    f.write(f"p wcard {n_vars} {n_constraints} {top}\n\n")

    # Add one hard constraint (just to keep formula feasible)
    lits = [1, 2]  # x1, x2
    f.write(f"{top} " + " ".join(map(str, lits)) + " >= 1\n")

    # Add contradictory soft constraints on x1
    f.write(f"2 1 >= 1\n")   # requires x1 = True
    f.write(f"3 -1 >= 1\n")  # requires x1 = False

    # Generate remaining random constraints
    for _ in range(n_constraints - 3):
        cost = random.randint(1, 80)
        num_lits = random.randint(3, n_vars)  # random number of literals
        lits = [random.choice([i+1, -(i+1)]) for i in range(num_lits)]
        op = random.choice(operators)
        k = random.randint(1, num_lits)  # bound depends on #lits
        f.write(f"{cost} " + " ".join(map(str, lits)) + f" {op} {k}\n")
