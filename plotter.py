from pathlib import Path
import csv
import math
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

HERE = Path(__file__).resolve().parent

if len(sys.argv) > 1:
    CSV_FILE = Path(sys.argv[1]).resolve()
else:
    candidates = [
        HERE / "results/results.csv",
        HERE.parent / "results/results.csv"
    ]
    CSV_FILE = next(
        (p for p in candidates if p.is_file()),
        candidates[0]
    )

if not CSV_FILE.is_file():
    raise SystemExit(
        f"CSV not found: {CSV_FILE}\nRun Benchmark first."
    )

ALGORITHMS = [
    "Kruskal",
    "Prim",
    "Kosaraju",
    "Kahn",
    "DagShortestPath"
]

COLORS = [
    "#2563eb",
    "#ea580c",
    "#15803d",
    "#9333ea",
    "#be185d"
]

SIZES = [1000, 3162, 10000, 31623, 100000]

# Чтение CSV.
with CSV_FILE.open(encoding="utf-8-sig", newline="") as file:
    reader = csv.DictReader(file)

    required = {
        "algorithm", "graph", "n", "m",
        "dsu_ops", "edge_scans", "comparisons"
    }

    if not required.issubset(reader.fieldnames or []):
        raise SystemExit("CSV has incorrect column names.")

    rows = list(reader)

# Преобразование строк в числа.
for row in rows:
    for key in [
        "n", "m", "dsu_ops", "edge_scans", "comparisons"
    ]:
        row[key] = int(row[key])

    row["operations"] = (
            row["dsu_ops"]
            + row["edge_scans"]
            + row["comparisons"]
    )

# Должно быть 5 алгоритмов × 2 плотности × 5 размеров.
if len(rows) != 50:
    raise SystemExit(
        f"Expected 50 rows, found {len(rows)}."
    )

groups = []

for algorithm, color in zip(ALGORITHMS, COLORS):
    for density, factor in [("sparse", 4), ("dense", 20)]:
        data = sorted(
            [
                r for r in rows
                if r["algorithm"] == algorithm
                   and r["graph"] == density
            ],
            key=lambda r: r["n"]
        )

        if (
                [r["n"] for r in data] != SIZES
                or any(r["m"] != factor * r["n"] for r in data)
        ):
            raise SystemExit(
                f"Incomplete or invalid data: "
                f"{algorithm}/{density}"
            )

        groups.append((algorithm, density, color, data))

OUT = CSV_FILE.parent / "plots"
OUT.mkdir(parents=True, exist_ok=True)

charts = [
    (
        1,
        "01_operations.png",
        "Operations vs n"
    ),
    (
        2,
        "02_scans_per_edge.png",
        "Edge scans per input edge"
    ),
    (
        3,
        "03_kruskal_share.png",
        "Fraction of edges examined by Kruskal"
    )
]

for kind, filename, title in charts:
    fig, ax = plt.subplots(figsize=(11, 6))

    for algorithm, density, color, data in groups:
        if kind == 3 and algorithm != "Kruskal":
            continue

        x = [r["n"] for r in data]

        if kind == 1:
            y = [r["operations"] for r in data]
        else:
            y = [r["edge_scans"] / r["m"] for r in data]

        ax.plot(
            x,
            y,
            marker="o",
            color=color,
            linestyle="-" if density == "sparse" else "--",
            label=f"{algorithm} / {density}"
        )

    ax.set_xscale("log")

    if kind == 1:
        ax.set_yscale("log")

    ax.set_xlabel("Number of vertices n")
    ax.set_ylabel(
        "Counted operations"
        if kind == 1
        else "edge_scans / m"
    )

    ax.set_title(title)
    ax.grid(True, which="both", alpha=0.25)

    ax.legend(
        loc="upper left",
        bbox_to_anchor=(1.02, 1),
        fontsize=9
    )

    fig.tight_layout()

    fig.savefig(
        OUT / filename,
        dpi=200,
        bbox_inches="tight"
    )

    plt.close(fig)
    print(f"Saved: {OUT / filename}")


# Наклон прямой на log-log графике методом наименьших квадратов.
def slope(data, counter):
    x = [math.log(r["n"]) for r in data]
    y = [math.log(r[counter]) for r in data]

    mean_x = sum(x) / len(x)
    mean_y = sum(y) / len(y)

    numerator = sum(
        (a - mean_x) * (b - mean_y)
        for a, b in zip(x, y)
    )

    denominator = sum(
        (a - mean_x) ** 2
        for a in x
    )

    return numerator / denominator


with (CSV_FILE.parent / "slopes.csv").open(
        "w",
        newline="",
        encoding="utf-8"
) as file:
    writer = csv.writer(file)

    writer.writerow([
        "algorithm",
        "density",
        "edge_scans_slope",
        "operations_slope"
    ])

    for algorithm, density, _, data in groups:
        writer.writerow([
            algorithm,
            density,
            slope(data, "edge_scans"),
            slope(data, "operations")
        ])

print("Done: 3 PNG charts and slopes.csv")