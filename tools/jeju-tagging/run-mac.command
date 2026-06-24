#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
INPUT="${1:-$HOME/Downloads/SSAFY_HOME_TRIP_202604/SSAFY_TRIP_Dump.sql}"
python3 jeju_tagging.py run --input "$INPUT"

