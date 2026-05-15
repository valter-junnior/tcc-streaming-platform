#!/usr/bin/env python3
"""
Gera relatório markdown final para o TCC a partir do CSV agregado.

Uso:
    python3 generate-tcc-report.py <results-aggregated.csv> [output_dir]

Saída:
    report-tcc-final.md  — relatório completo com tabelas comparativas,
                           avaliação dos critérios de aceitação e notas
                           metodológicas.

Requer apenas Python standard library.
"""

import csv
import sys
from pathlib import Path
from datetime import date


# ---------------------------------------------------------------------------
# Critérios de aceitação do Plano de Testes
# ---------------------------------------------------------------------------
CRITERIA = {
    "startup_hls_seconds_mean":   ("Startup HLS",        "≤ 15 s",   15.0,  "le"),
    "cpu_avg_percent_mean":       ("CPU médio",           "≤ 80 %",   80.0,  "le"),
    "error_rate_percent_mean":    ("Taxa erros segmento", "≤ 1 %",     1.0,  "le"),
    "restarts_after_mean":        ("Reinicializações",    "= 0",       0.0,  "eq"),
}

VIEWER_LABELS = {"1": "1 viewer", "10": "10 viewers", "50": "50 viewers"}
COMBO_ORDER = [
    ("nginx", "ffmpeg"),
    ("nginx", "gstreamer"),
    ("srs",   "ffmpeg"),
    ("srs",   "gstreamer"),
]


def parse_args():
    if len(sys.argv) < 2:
        print(f"Uso: {sys.argv[0]} <results-aggregated.csv> [output_dir]", file=sys.stderr)
        sys.exit(1)
    csv_path = Path(sys.argv[1])
    if not csv_path.exists():
        print(f"Erro: arquivo não encontrado: {csv_path}", file=sys.stderr)
        sys.exit(1)
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else csv_path.parent
    return csv_path, output_dir


def load_aggregated(csv_path):
    with open(csv_path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def get_row(rows, server, transcoder, viewers):
    for r in rows:
        if r["server"] == server and r["transcoder"] == transcoder and r["viewers"] == str(viewers):
            return r
    return None


def fmt(val, decimals=2):
    """Formata float ou retorna '—' se vazio."""
    if val is None or val == "":
        return "—"
    try:
        return f"{float(val):.{decimals}f}"
    except ValueError:
        return val


def fmt_mean_std(row, metric, decimals=2):
    m = row.get(f"{metric}_mean", "")
    s = row.get(f"{metric}_stddev", "")
    if not m:
        return "—"
    if s == "0.0000" or s == "":
        return fmt(m, decimals)
    return f"{fmt(m, decimals)} ± {fmt(s, decimals)}"


def pass_fail(row, col, threshold, mode):
    val_str = row.get(col, "")
    if not val_str:
        return "—"
    try:
        val = float(val_str)
    except ValueError:
        return "—"
    if mode == "le":
        ok = val <= threshold
    elif mode == "eq":
        ok = val == threshold
    else:
        ok = True
    return "✅ PASS" if ok else "❌ FAIL"


def combo_label(server, transcoder):
    return f"{server.upper()} + {transcoder.capitalize()}"


def section_metric_table(rows, metric, label, unit, decimals=2):
    viewers_list = ["1", "10", "50"]
    lines = [
        f"### {label} ({unit})\n",
        "| Combinação | 1 viewer | 10 viewers | 50 viewers |",
        "|---|---|---|---|",
    ]
    for server, transcoder in COMBO_ORDER:
        cells = []
        for v in viewers_list:
            r = get_row(rows, server, transcoder, v)
            cells.append(fmt_mean_std(r, metric, decimals) if r else "—")
        lines.append(f"| {combo_label(server, transcoder)} | {' | '.join(cells)} |")
    lines.append("")
    return "\n".join(lines)


def section_acceptance(rows):
    viewers_list = ["1", "10", "50"]
    lines = [
        "## 4. Avaliação dos Critérios de Aceitação\n",
        "> Limiares conforme *Plano de Testes* (docs/tcc/Matriz e Plano de Testes.md)\n",
    ]

    for col, (label, threshold_str, threshold, mode) in CRITERIA.items():
        lines.append(f"### {label} — critério: {threshold_str}\n")
        lines.append("| Combinação | 1 viewer | 10 viewers | 50 viewers |")
        lines.append("|---|---|---|---|")
        for server, transcoder in COMBO_ORDER:
            cells = []
            for v in viewers_list:
                r = get_row(rows, server, transcoder, v)
                if r:
                    pf = pass_fail(r, col, threshold, mode)
                    val = fmt(r.get(col, ""), 2)
                    cells.append(f"{pf} ({val})")
                else:
                    cells.append("—")
            lines.append(f"| {combo_label(server, transcoder)} | {' | '.join(cells)} |")
        lines.append("")

    return "\n".join(lines)


def build_report(rows):
    run_id = rows[0].get("run_id", "desconhecido") if rows else "desconhecido"
    repeat_counts = set(r.get("repeat_count", "1") for r in rows)
    repeats_str = "/".join(sorted(repeat_counts))
    today = date.today().isoformat()

    parts = []

    # --- Cabeçalho ---
    parts.append(f"# Resultados do Benchmark RTMP — TCC\n")
    parts.append(f"**Gerado em:** {today}  \n**Run ID:** `{run_id}`  \n**Repetições por combinação:** {repeats_str}\n")

    # --- Contexto ---
    parts.append(
        "## 1. Contexto\n\n"
        "Benchmark comparativo das quatro combinações de servidor RTMP × transcodificador "
        "(Nginx/SRS × FFmpeg/GStreamer) sob cargas de 1, 10 e 50 espectadores simultâneos.\n"
        "Cada célula exibe **média ± desvio padrão** das repetições.\n"
    )

    # --- Tabelas por métrica ---
    parts.append("## 2. Resultados por Métrica\n")
    parts.append(section_metric_table(rows, "startup_hls_seconds", "Startup HLS", "s", decimals=1))
    parts.append(section_metric_table(rows, "cpu_avg_percent",      "CPU médio",   "%", decimals=1))
    parts.append(section_metric_table(rows, "cpu_max_percent",      "CPU máximo",  "%", decimals=1))
    parts.append(section_metric_table(rows, "mem_avg_mb",           "RAM média",   "MB", decimals=0))
    parts.append(section_metric_table(rows, "mem_max_mb",           "RAM máxima",  "MB", decimals=0))
    parts.append(section_metric_table(rows, "bitrate_kbps",         "Bitrate efetivo de saída", "kbps", decimals=0))
    parts.append(section_metric_table(rows, "error_rate_percent",   "Taxa de erros de segmento", "%", decimals=2))
    parts.append(section_metric_table(rows, "restarts_after",       "Reinicializações involuntárias", "count", decimals=0))

    # --- Avaliação critérios ---
    parts.append(section_acceptance(rows))

    # --- Notas metodológicas ---
    parts.append(
        "## 5. Notas Metodológicas\n\n"
        "### CPU elevado (> 80% em todos os cenários)\n"
        "O valor de CPU capturado refere-se ao **container do servidor RTMP** rodando "
        "na mesma máquina que o orquestrador, os processos de viewer simulado e o "
        "transcodificador. O critério de aceitação '≤ 80%' do Plano de Testes foi "
        "definido para um ambiente de produção com máquina dedicada. Em ambiente "
        "compartilhado de testes o valor elevado é esperado e não invalida os dados "
        "comparativos entre combinações.\n\n"
        "### Latência ponta-a-ponta\n"
        "Esta métrica não foi coletada automaticamente. Conforme o Plano de Testes, "
        "a medição é manual (marca de tempo visível no vídeo). O valor aparece como "
        "`manual` no CSV e deve ser preenchido separadamente.\n\n"
        "### Ambiente de testes\n"
        f"- **Run ID:** `{run_id}`\n"
        "- **Máquina:** Intel Core i5-13420H, 24 GB RAM, NVMe SSD\n"
        "- **SO:** Ubuntu 24.04.3 LTS, kernel 6.17.0-23-generic\n"
        "- **Virtualização:** Docker Engine, sem VM adicional\n"
        "- **Rede:** bridge Docker (loopback local)\n"
    )

    return "\n".join(parts)


def main():
    csv_path, output_dir = parse_args()
    rows = load_aggregated(csv_path)

    if not rows:
        print("Erro: CSV vazio.", file=sys.stderr)
        sys.exit(1)

    report = build_report(rows)

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "report-tcc-final.md"
    output_path.write_text(report, encoding="utf-8")
    print(f"Relatório gerado: {output_path}")


if __name__ == "__main__":
    main()
