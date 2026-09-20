#!/bin/bash
cd "$(dirname "$0")"
if [ -n "${1:-}" ]; then export TRIP_DUMP="$1"; fi
python3 place_tagging.py run A
echo; read -r -p "창을 닫아도 됩니다. 중간에 끊겼으면 이 파일을 다시 실행하세요. (Enter)" _
