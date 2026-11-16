#!/usr/bin/env python3
"""Export only reviewed benchmark measurements for the public data set.

The original per-run CSV and detailed logs stay under app/benchmark/results/.
Paths, internal URLs, stream keys, container names, and free-form errors are
excluded by an explicit allowlist. Use this exporter for every public run.
"""

import csv
import sys
from pathlib import Path

PUBLIC_FIELDS = (
    "run_id",
    "mode",
    "scenario",
    "server",
    "transcoder",
    "repeat_index",
    "viewers",
    "duration_seconds",
    "status",
    "measurement_start_ts",
    "measurement_end_ts",
    "measurement_window_seconds",
    "startup_hls_seconds",
    "latencia_ponta_a_ponta_s",
    "tempo_primeiro_segmento_s",
    "request_total",
    "request_errors",
    "error_rate_percent",
    "cpu_avg_percent",
    "cpu_max_percent",
    "mem_avg_mb",
    "mem_max_mb",
    "net_in_mb",
    "net_out_mb",
    "bitrate_kbps",
    "restarts_before",
    "restarts_after",
    "retry_index",
)


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Uso: export-public-results.py <results.csv> <public-results.csv>")

    source, destination = map(Path, sys.argv[1:])
    if source.resolve() == destination.resolve():
        raise SystemExit("Entrada e saída devem ser arquivos diferentes")

    with source.open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        missing = set(PUBLIC_FIELDS) - set(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"Colunas obrigatórias ausentes: {', '.join(sorted(missing))}")
        rows = [{field: row[field] for field in PUBLIC_FIELDS} for row in reader]

    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=PUBLIC_FIELDS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)

    print(f"Exportadas {len(rows)} repetições públicas em {destination}")


if __name__ == "__main__":
    main()
