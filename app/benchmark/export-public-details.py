#!/usr/bin/env python3
"""Export sanitized request and resource samples from one audited local run.

Usage: export-public-details.py <local-run-dir> <public-run-dir>
Only structured values from viewer request logs and CPU/RAM samples are copied.
Service logs, HLS playlists, network counters, URLs, paths, and credentials stay local.
"""

import csv
import re
import sys
from pathlib import Path
from statistics import mean

KEY_FIELDS = ("run_id", "scenario", "server", "transcoder", "viewers", "repeat_index")
REQUEST_FIELDS = KEY_FIELDS + (
    "viewer_index", "cycle_start_ts", "resource", "http_code", "curl_exit_code", "time_total_seconds"
)
SAMPLE_FIELDS = KEY_FIELDS + ("timestamp", "cpu_percent", "mem_used_mb")
RESOURCES = {"master", "variant", "segment", "master_content", "variant_content"}


def rows(path):
    with path.open(newline="", encoding="utf-8") as stream:
        yield from csv.DictReader(stream)


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    source, destination = (Path(arg).resolve() for arg in sys.argv[1:])
    if not (source / "results.csv").is_file() or source == destination:
        raise SystemExit("Fonte inválida ou igual ao destino")
    if not destination.is_dir():
        raise SystemExit("Destino público deve existir")

    runs = list(rows(source / "results.csv"))
    if len(runs) != 36 or any(run["status"] != "PASS" for run in runs):
        raise SystemExit("A exportação detalhada requer 36 execuções PASS")
    request_path = destination / "requests-detail.csv"
    sample_path = destination / "resource-samples.csv"
    if request_path.exists() or sample_path.exists():
        raise SystemExit("Os arquivos de destino já existem")

    request_count = sample_count = 0
    with request_path.open("w", newline="", encoding="utf-8") as request_file, sample_path.open(
        "w", newline="", encoding="utf-8"
    ) as sample_file:
        request_writer = csv.DictWriter(request_file, REQUEST_FIELDS, lineterminator="\n")
        sample_writer = csv.DictWriter(sample_file, SAMPLE_FIELDS, lineterminator="\n")
        request_writer.writeheader()
        sample_writer.writeheader()

        for run in runs:
            key = {field: run[field] for field in KEY_FIELDS}
            directory = source / (
                f"{run['scenario'].replace('+', '_')}-v{run['viewers']}-r{run['repeat_index']}"
                f"-try{run['retry_index']}_"
            )
            if not directory.is_dir():
                raise SystemExit(f"Diretório ausente para {key}")
            first, last = int(run["viewers_start_ts"]), int(run["viewers_end_ts"])
            local_requests = 0
            for index in range(1, int(run["viewers"]) + 1):
                path = directory / "viewers" / f"viewer-{index}-requests.csv"
                if not path.is_file():
                    raise SystemExit(f"Log de viewer ausente para {key}, viewer {index}")
                for item in rows(path):
                    if item["resource"] not in RESOURCES:
                        raise SystemExit(f"Recurso inesperado para {key}: {item['resource']}")
                    if not re.fullmatch(r"\d{3}|invalid", item["http_code"]):
                        raise SystemExit(f"Código HTTP inesperado para {key}")
                    stamp = int(item["timestamp"])
                    if not first <= stamp < last:
                        raise SystemExit(f"Timestamp fora da janela para {key}")
                    if item["resource"] in {"master", "variant", "segment"}:
                        local_requests += 1
                    request_writer.writerow({
                        **key,
                        "viewer_index": index,
                        "cycle_start_ts": stamp,
                        "resource": item["resource"],
                        "http_code": item["http_code"],
                        "curl_exit_code": item["curl_exit_code"],
                        "time_total_seconds": item["time_total_seconds"],
                    })
                    request_count += 1
            if local_requests != int(run["request_total"]):
                raise SystemExit(f"Total de requisições diverge para {key}")

            cpu, mem = [], []
            for item in rows(directory / "metrics-samples.csv"):
                stamp = int(item["timestamp"])
                if first <= stamp <= last:
                    cpu.append(float(item["cpu_percent"]))
                    mem.append(float(item["mem_used_mb"]))
                    sample_writer.writerow({
                        **key,
                        "timestamp": stamp,
                        "cpu_percent": item["cpu_percent"],
                        "mem_used_mb": item["mem_used_mb"],
                    })
                    sample_count += 1
            if not cpu or abs(mean(cpu) - float(run["cpu_avg_percent"])) > 0.0002:
                raise SystemExit(f"CPU diverge para {key}")
            if abs(mean(mem) - float(run["mem_avg_mb"])) > 0.0002:
                raise SystemExit(f"Memória diverge para {key}")

    print(f"Exportados {request_count} eventos de requisição e {sample_count} amostras de CPU/RAM")


if __name__ == "__main__":
    main()
