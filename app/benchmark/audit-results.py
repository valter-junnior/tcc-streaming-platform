#!/usr/bin/env python3
"""Audit one local RTMP benchmark run without publishing its raw artifacts."""

import argparse
import csv
import json
from collections import Counter
from pathlib import Path
from statistics import mean


def read_csv(path):
    with path.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def scenario_dir(root, row):
    name = f"{row['scenario'].replace('+', '_')}-v{row['viewers']}-r{row['repeat_index']}-try{row['retry_index']}_"
    return root / name


def audit_row(root, row):
    errors = []
    directory = scenario_dir(root, row)
    if row["status"] != "PASS":
        errors.append(f"status={row['status']} error={row.get('error_message', '')}")
    if float(row["startup_hls_seconds"]) < 0:
        errors.append("master playlist probe failed")
    if float(row["tempo_primeiro_segmento_s"]) < 0:
        errors.append("first segment probe failed")

    timing_path = directory / "timing.csv"
    if not timing_path.exists():
        return errors + ["missing timing.csv"]
    timing_rows = read_csv(timing_path)
    if len(timing_rows) != 1:
        return errors + ["timing.csv must contain exactly one row"]
    timing = timing_rows[0]
    if timing.get("variants_ok") != "1":
        errors.append("four-variant check failed")
    for key in ("publisher_alive_before_viewers", "publisher_alive_after_viewers"):
        if timing.get(key) != "1":
            errors.append(f"{key}={timing.get(key)}")
    first = int(timing["viewers_start_ts"])
    last = int(timing["viewers_end_ts"])
    if last - first != int(timing["viewers_window_seconds"]):
        errors.append("viewer timestamps do not match their duration")
    if last - first < int(row["duration_seconds"]):
        errors.append("viewer window shorter than nominal duration")

    variants_path = directory / "hls-variants.csv"
    if not variants_path.exists():
        errors.append("missing hls-variants.csv")
    else:
        variants = read_csv(variants_path)
        expected = {f"v{i}/playlist.m3u8" for i in range(4)}
        if {item["variant"] for item in variants} != expected:
            errors.append("four HLS variants not recorded")
        for item in variants:
            if item["playlist_http_code"] != "200" or item["segment_http_code"] != "200":
                errors.append(f"variant unavailable: {item['variant']}")

    viewers_dir = directory / "viewers"
    summaries = list(viewers_dir.glob("viewer-*.txt"))
    request_logs = list(viewers_dir.glob("viewer-*-requests.csv"))
    viewers = int(row["viewers"])
    if len(summaries) != viewers or len(request_logs) != viewers:
        errors.append(f"viewer files: {len(summaries)} summaries, {len(request_logs)} request logs, expected {viewers}")
    total = failed = 0
    for path in summaries:
        values = path.read_text(encoding="utf-8").strip().split(",")
        if len(values) != 2:
            errors.append(f"invalid viewer summary: {path.name}")
            continue
        total += int(values[0])
        failed += int(values[1])
    if total != int(row["request_total"]) or failed != int(row["request_errors"]):
        errors.append("viewer summaries disagree with results.csv")

    codes = Counter()
    for path in request_logs:
        for request in read_csv(path):
            codes[(request["resource"], request["http_code"])] += 1
    http_count = sum(count for (resource, _), count in codes.items() if resource in ("master", "variant", "segment"))
    bad_http = sum(count for (resource, code), count in codes.items() if resource in ("master", "variant", "segment") and code != "200")
    bad_content = sum(count for (resource, _), count in codes.items() if resource in ("master_content", "variant_content"))
    if http_count != total or bad_http + bad_content != failed:
        errors.append("request logs disagree with viewer summaries")

    samples_path = directory / "metrics-samples.csv"
    if not samples_path.exists():
        errors.append("missing metrics-samples.csv")
    else:
        samples = [sample for sample in read_csv(samples_path) if first <= int(sample["timestamp"]) <= last]
        if len(samples) < 3:
            errors.append(f"only {len(samples)} CPU/memory samples in viewer window")
        else:
            for source, result in (("cpu_percent", "cpu_avg_percent"), ("mem_used_mb", "mem_avg_mb")):
                average = mean(float(sample[source]) for sample in samples)
                if abs(average - float(row[result])) > 0.0002:
                    errors.append(f"{result} differs from metric samples")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("run_dir", type=Path)
    parser.add_argument("--require-full", action="store_true")
    args = parser.parse_args()
    root = args.run_dir.resolve()
    rows = read_csv(root / "results.csv")
    issues = {}
    keys = [(row["server"], row["transcoder"], row["viewers"], row["repeat_index"]) for row in rows]
    if len(keys) != len(set(keys)):
        issues["matrix"] = ["duplicate treatment/repetition rows"]
    if args.require_full:
        expected = {(server, transcoder, viewers, str(repeat))
                    for server in ("nginx", "srs")
                    for transcoder in ("ffmpeg", "gstreamer")
                    for viewers in ("1", "10", "50")
                    for repeat in range(1, 4)}
        if set(keys) != expected:
            issues.setdefault("matrix", []).append(f"expected 36 unique rows; got {len(set(keys))}")
    for row in rows:
        key = "/".join((row["scenario"], row["viewers"], row["repeat_index"]))
        row_issues = audit_row(root, row)
        if row_issues:
            issues[key] = row_issues
    report = {"run_id": root.name, "rows": len(rows), "issues": issues, "valid": not issues}
    (root / "audit.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    raise SystemExit(0 if report["valid"] else 1)


if __name__ == "__main__":
    main()
