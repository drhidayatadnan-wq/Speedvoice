#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
npm install --prefix "$tmp" --ignore-scripts --no-audit --no-fund pdfjs-dist@5.4.624
src="$tmp/node_modules/pdfjs-dist"
dest=app/src/main/assets/pdfjs
mkdir -p "$dest/legacy/build"
cp "$src/legacy/build/pdf.mjs" "$src/legacy/build/pdf.worker.mjs" "$dest/legacy/build/"
for d in cmaps standard_fonts wasm; do cp -R "$src/$d" "$dest/"; done
cp "$src/LICENSE" "$dest/"