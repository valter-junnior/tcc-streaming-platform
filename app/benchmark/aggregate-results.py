#!/usr/bin/env python3
"""
Agrega repetições do benchmark RTMP por combinação (server, transcoder, viewers).

Uso:
    python3 aggregate-results.py <results.csv> [output_dir]

Saída:
    results-aggregated.csv  — uma linha por (server, transcoder, viewers) com
                              <metrica>_mean, <metrica>_stddev e avaliação
                              pass/fail dos critérios de aceitação do TCC.

Requer apenas Python standard library (csv, statistics, sys, os, pathlib).
"""

import csv
import sys
import os
from pathlib import Path
from statistics import mean, stdev


METRICS = [
    "startup_hls_seconds",
    "latencia_ponta_a_ponta_s",
    "tempo_primeiro_segmento_s",
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

KEY_COLS = ("mode", "server", "transcoder", "viewers")

# Critérios de aceitação recalibrados (por carga)
STARTUP_THRESHOLD_SECONDS = 24.0
CPU_THRESHOLD_BY_VIEWERS = {
    "1": 240.0,
    "10": 240.0,
    "50": 280.0,
}
ERROR_THRESHOLD_BY_VIEWERS = {
    "1": 1.0,
    "10": 1.0,
    "50": 3.0,
}
RESTARTS_THRESHOLD = 0.0

PASS_FIELDS = [
    "pass_startup_hls_seconds",
    "pass_cpu_avg_percent",
    "pass_error_rate_percent",
    "pass_restarts_after",
]

TEST_MATRIX = {
    ("nginx", "ffmpeg", "1"): "T01",
    ("nginx", "ffmpeg", "10"): "T02",
    ("nginx", "ffmpeg", "50"): "T03",
    ("nginx", "gstreamer", "1"): "T04",
    ("nginx", "gstreamer", "10"): "T05",
    ("nginx", "gstreamer", "50"): "T06",
    ("srs", "ffmpeg", "1"): "T07",
    ("srs", "ffmpeg", "10"): "T08",
    ("srs", "ffmpeg", "50"): "T09",
    ("srs", "gstreamer", "1"): "T10",
    ("srs", "gstreamer", "10"): "T11",
    ("srs", "gstreamer", "50"): "T12",
}


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


def evaluate_criteria(result):
    """Adiciona colunas pass_* e overall_pass_fail ao dicionário result."""
    viewers = result.get("viewers", "")
    cpu_threshold = CPU_THRESHOLD_BY_VIEWERS.get(viewers, 240.0)
    error_threshold = ERROR_THRESHOLD_BY_VIEWERS.get(viewers, 1.0)

    def le_ok(col_name, threshold):
        try:
            return float(result.get(col_name, "")) <= threshold
        except ValueError:
            return False

    startup_ok = le_ok("startup_hls_seconds_mean", STARTUP_THRESHOLD_SECONDS)
    cpu_ok = le_ok("cpu_avg_percent_mean", cpu_threshold)
    error_ok = le_ok("error_rate_percent_mean", error_threshold)
    restart_ok = le_ok("restarts_after_mean", RESTARTS_THRESHOLD)

    result["pass_startup_hls_seconds"] = "PASS" if startup_ok else "FAIL"
    result["pass_cpu_avg_percent"] = "PASS" if cpu_ok else "FAIL"
    result["pass_error_rate_percent"] = "PASS" if error_ok else "FAIL"
    result["pass_restarts_after"] = "PASS" if restart_ok else "FAIL"

    all_pass = startup_ok and cpu_ok and error_ok and restart_ok
    if result.get("status") == "FAIL":
        all_pass = False
    result["overall_pass_fail"] = "PASS" if all_pass else "FAIL"
    return result


def build_output_fieldnames():
    base = list(KEY_COLS) + ["repeat_count", "status", "run_id"]
    for metric in METRICS:
        base.append(f"{metric}_mean")
        base.append(f"{metric}_stddev")
    for field in PASS_FIELDS:
        base.append(field)
    base.append("overall_pass_fail")
    return base


def write_aggregated(output_path, aggregated_rows, fieldnames):
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(aggregated_rows)
    print(f"Escrito: {output_path}")


def float_or_none(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def format_metric(value):
    v = float_or_none(value)
    if v is None:
        return ""
    return f"{v:.4f}"


def format_metric_or_na(value):
    v = float_or_none(value)
    if v is None:
        return "N/A"
    return f"{v:.4f}"


def build_status_and_notes(row):
    notes = []
    viewers = row.get("viewers", "")
    cpu_threshold = CPU_THRESHOLD_BY_VIEWERS.get(viewers, 240.0)
    error_threshold = ERROR_THRESHOLD_BY_VIEWERS.get(viewers, 1.0)
    crit_startup = row.get("pass_startup_hls_seconds", "")
    crit_cpu = row.get("pass_cpu_avg_percent", "")
    crit_error = row.get("pass_error_rate_percent", "")
    crit_restart = row.get("pass_restarts_after", "")
    has_startup = float_or_none(row.get("startup_hls_seconds_mean", "")) is not None
    has_cpu = float_or_none(row.get("cpu_avg_percent_mean", "")) is not None
    has_error = float_or_none(row.get("error_rate_percent_mean", "")) is not None
    has_restart = float_or_none(row.get("restarts_after_mean", "")) is not None

    if row.get("status") == "FAIL":
        notes.append("falha_execucao")
    if has_startup and crit_startup == "FAIL":
        notes.append(f"startup_hls>{STARTUP_THRESHOLD_SECONDS:.0f}s")
    if has_error and crit_error == "FAIL":
        notes.append(f"taxa_erros>{error_threshold:.0f}%")
    if has_cpu and crit_cpu == "FAIL":
        notes.append(f"cpu_medio>{cpu_threshold:.0f}%")
    if has_restart and crit_restart == "FAIL":
        notes.append("reinicios>0")

    critical_fail = (
        row.get("status") == "FAIL"
        or (has_startup and crit_startup == "FAIL")
        or (has_restart and crit_restart == "FAIL")
    )
    warning_fail = (has_cpu and crit_cpu == "FAIL") or (has_error and crit_error == "FAIL")

    if critical_fail:
        status_symbol = "✗"
    elif warning_fail:
        status_symbol = "⚠"
    else:
        status_symbol = "✓"

    obs = "ok" if not notes else "; ".join(notes)
    return status_symbol, obs


def build_final_sheet_rows(aggregated_rows):
    final_rows = []
    for row in aggregated_rows:
        if row.get("mode") != "full":
            continue

        key = (row.get("server", ""), row.get("transcoder", ""), row.get("viewers", ""))
        test_id = TEST_MATRIX.get(key, "NA")
        status_symbol, notes = build_status_and_notes(row)

        final_rows.append(
            {
                "ID Teste": test_id,
                "Servidor RTMP": row.get("server", "").upper(),
                "Transcodificador": row.get("transcoder", "").upper(),
                "Viewers": row.get("viewers", ""),
                "Repeticoes": row.get("repeat_count", ""),
                "Duracao Janela (s)": format_metric(row.get("measurement_window_seconds_mean", "")),
                "Latência Ponta a Ponta (s)": format_metric_or_na(row.get("latencia_ponta_a_ponta_s_mean", "")),
                "Startup HLS Média (s)": format_metric(row.get("startup_hls_seconds_mean", "")),
                "Startup HLS DP (s)": format_metric(row.get("startup_hls_seconds_stddev", "")),
                "Tempo até Primeiro Segmento (s)": format_metric_or_na(row.get("tempo_primeiro_segmento_s_mean", "")),
                "CPU Média (%)": format_metric(row.get("cpu_avg_percent_mean", "")),
                "CPU Máxima (%)": format_metric(row.get("cpu_max_percent_mean", "")),
                "RAM Média (MB)": format_metric(row.get("mem_avg_mb_mean", "")),
                "RAM Máxima (MB)": format_metric(row.get("mem_max_mb_mean", "")),
                "Bitrate Efetivo (kbps)": format_metric(row.get("bitrate_kbps_mean", "")),
                "Taxa de Erros (%)": format_metric(row.get("error_rate_percent_mean", "")),
                "Reinícios Sessão": format_metric(row.get("restarts_after_mean", "")),
                "Status": status_symbol,
                "Observações": notes,
            }
        )

    def sort_key(r):
        tid = r.get("ID Teste", "NA")
        if tid.startswith("T") and tid[1:].isdigit():
            return int(tid[1:])
        return 999

    return sorted(final_rows, key=sort_key)


def write_final_sheet(output_path, rows):
    fieldnames = [
        "ID Teste",
        "Servidor RTMP",
        "Transcodificador",
        "Viewers",
        "Repeticoes",
        "Duracao Janela (s)",
        "Latência Ponta a Ponta (s)",
        "Startup HLS Média (s)",
        "Startup HLS DP (s)",
        "Tempo até Primeiro Segmento (s)",
        "CPU Média (%)",
        "CPU Máxima (%)",
        "RAM Média (MB)",
        "RAM Máxima (MB)",
        "Bitrate Efetivo (kbps)",
        "Taxa de Erros (%)",
        "Reinícios Sessão",
        "Status",
        "Observações",
    ]
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Legenda Status", "Descrição"])
        writer.writerow(["✓", "Passou em critérios críticos e de alerta"])
        writer.writerow(["⚠", "Passou em critérios críticos, com alerta (CPU ou taxa de erros)"])
        writer.writerow(["✗", "Falhou em critério crítico ou falha de execução"])
        writer.writerow([])
        writer.writerow(["Critérios de Aceitação", "Regra"])
        writer.writerow(["Startup HLS Média (s)", "<= 24"])
        writer.writerow(["CPU Média (%)", "<= 240 (1 e 10 viewers); <= 280 (50 viewers)"])
        writer.writerow(["Taxa de Erros (%)", "<= 1 (1 e 10 viewers); <= 3 (50 viewers)"])
        writer.writerow(["Reinícios Sessão", "<= 0"])
        writer.writerow([])

        dict_writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        dict_writer.writeheader()
        dict_writer.writerows(rows)
    print(f"Escrito: {output_path}")


def main():
    csv_path, output_dir = parse_args()
    rows, _ = load_rows(csv_path)

    if not rows:
        print("Erro: CSV vazio ou sem linhas de dados.", file=sys.stderr)
        sys.exit(1)

    groups = group_by_key(rows)
    aggregated = [evaluate_criteria(aggregate_group(key, grp)) for key, grp in sorted(groups.items())]

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "results-aggregated.csv"
    fieldnames = build_output_fieldnames()
    write_aggregated(output_path, aggregated, fieldnames)

    final_sheet_rows = build_final_sheet_rows(aggregated)
    final_sheet_path = output_dir / "resultado.csv"
    write_final_sheet(final_sheet_path, final_sheet_rows)

    print("\nResumo:")
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
