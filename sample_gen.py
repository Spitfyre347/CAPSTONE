import random

n_vars = 4
n_constraints = 5
top = 100  # hard constraint weight

with open("sample.wcard", "w") as f:
    f.write(f"p wcard {n_vars} {n_constraints} {top}\n\n")
    for _ in range(n_constraints):
        weight = random.choice([1, 2, 5, top])
        k = random.randint(1, n_vars)  # bound
        lits = [random.choice([i+1, -(i+1)]) for i in range(n_vars)]
        f.write(f"{weight} {k} " + " ".join(map(str, lits)) + " 0\n")
