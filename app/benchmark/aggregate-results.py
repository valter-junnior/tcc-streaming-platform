#!/usr/bin/env python3
"""
Agrega repetições do benchmark RTMP por combinação (server, transcoder, viewers).

Uso:
    python3 aggregate-results.py <results.csv> [output_dir]

Saída:
    results-aggregated.csv  — uma linha por (server, transcoder, viewers) com
                              <metrica>_mean e <metrica>_stddev para cada
                              métrica numérica.

Requer apenas Python standard library (csv, statistics, sys, os, pathlib).
"""

import csv
import sys
import os
from pathlib import Path
from statistics import mean, stdev


METRICS = [
    "startup_hls_seconds",
    "cpu_avg_percent",
    "cpu_max_percent",
    "mem_avg_mb",
    "mem_max_mb",
    "bitrate_kbps",
    "error_rate_percent",
    "restarts_after",
    "request_errors",
    "measurement_window_seconds",
]

KEY_COLS = ("server", "transcoder", "viewers")


def parse_args():
    if len(sys.argv) < 2:
        print(f"Uso: {sys.argv[0]} <results.csv> [output_dir]", file=sys.stderr)
        sys.exit(1)
    csv_path = Path(sys.argv[1])
    if not csv_path.exists():
        print(f"Erro: arquivo não encontrado: {csv_path}", file=sys.stderr)
        sys.exit(1)
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else csv_path.parent
    return csv_path, output_dir


def load_rows(csv_path):
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        return list(reader), reader.fieldnames


def group_by_key(rows):
    groups = {}
    for row in rows:
        key = tuple(row[c] for c in KEY_COLS)
        groups.setdefault(key, []).append(row)
    return groups


def aggregate_group(key, group_rows):
    result = dict(zip(KEY_COLS, key))
    result["repeat_count"] = len(group_rows)

    # Preserva status: PASS apenas se todos passaram
    statuses = [r.get("status", "") for r in group_rows]
    result["status"] = "PASS" if all(s == "PASS" for s in statuses) else "FAIL"
    result["run_id"] = group_rows[0].get("run_id", "")

    for metric in METRICS:
        values = []
        for row in group_rows:
            raw = row.get(metric, "").strip()
            try:
                values.append(float(raw))
            except ValueError:
                pass

        if not values:
            result[f"{metric}_mean"] = ""
            result[f"{metric}_stddev"] = ""
        elif len(values) == 1:
            result[f"{metric}_mean"] = f"{values[0]:.4f}"
            result[f"{metric}_stddev"] = "0.0000"
        else:
            result[f"{metric}_mean"] = f"{mean(values):.4f}"
            result[f"{metric}_stddev"] = f"{stdev(values):.4f}"

    return result


def build_output_fieldnames():
    base = list(KEY_COLS) + ["repeat_count", "status", "run_id"]
    for metric in METRICS:
        base.append(f"{metric}_mean")
        base.append(f"{metric}_stddev")
    return base


def write_aggregated(output_path, aggregated_rows, fieldnames):
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(aggregated_rows)
    print(f"Escrito: {output_path}")


def main():
    csv_path, output_dir = parse_args()
    rows, _ = load_rows(csv_path)

    if not rows:
        print("Erro: CSV vazio ou sem linhas de dados.", file=sys.stderr)
        sys.exit(1)

    groups = group_by_key(rows)
    aggregated = [aggregate_group(key, grp) for key, grp in sorted(groups.items())]

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "results-aggregated.csv"
    fieldnames = build_output_fieldnames()
    write_aggregated(output_path, aggregated, fieldnames)

    print(f"\nResumo:")
    print(f"  Linhas de entrada : {len(rows)}")
    print(f"  Combinações únicas: {len(aggregated)}")
    for row in aggregated:
        print(
            f"  {row['server']:5s} + {row['transcoder']:10s} viewers={row['viewers']:>2s}"
            f"  repeticoes={row['repeat_count']}"
            f"  status={row['status']}"
        )


if __name__ == "__main__":
    main()
