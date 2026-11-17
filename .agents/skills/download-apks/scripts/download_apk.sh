#!/usr/bin/env bash
set -eu

curl --fail --location --silent --show-error --output "$3" \
  "https://play.chsbuffer.work/download/$1/$2/base.apk?old_store=1&device_name=px_9a"
