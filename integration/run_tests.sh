#!/bin/bash

set -euo pipefail

if [[ $# -ne 1 ]] ; then
  echo "Expected the jar file to run tests against."
  exit 1
fi

JAR_FILE=$1
INTEGRATION_TEST_DIR="$(dirname "${BASH_SOURCE[0]}")"
for test_dir in "${INTEGRATION_TEST_DIR}"/*/ ; do
  cd "${test_dir}"
  java -jar "${JAR_FILE}" -g grammar -o /tmp/Main.java --main --overwrite
  java /tmp/Main.java input > /tmp/actual
  diff /tmp/actual expected
  echo "${test_dir}... OK"
  cd ..
done
