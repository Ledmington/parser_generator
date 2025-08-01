#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]] ; then
  echo "Expected the jar file to run tests against."
  exit 1
fi

if ! JAVA=$(which java) ; then
  echo "Could not find java command"
  if [[ -v JAVA_HOME ]] ; then
    JAVA="${JAVA_HOME}/bin/java"
  else
    echo "Could not find JAVA_HOME"
    exit 1
  fi
fi

echo "Using java at '${JAVA}'"
${JAVA} --version
echo ""

JAR_FILE=$1
INTEGRATION_TEST_DIR="$(dirname "${BASH_SOURCE[0]}")"
TEST_CASES=$(find "${INTEGRATION_TEST_DIR}" -mindepth 1 -maxdepth 1 -type d -printf "%f\n" | sort)
for test_dir in ${TEST_CASES} ; do
  DIR="$(mktemp -d)"
  GENERATED="${DIR}/Main.java"
  OUTPUT="$(mktemp)"
  INPUT="${INTEGRATION_TEST_DIR}/${test_dir}"

  echo " - ${test_dir}..."
  ${JAVA} -jar "${JAR_FILE}" -g "${INPUT}/grammar" -o "${GENERATED}" --main --overwrite
  ${JAVA} "${GENERATED}" "${INPUT}/input" > "${OUTPUT}"
  diff "${OUTPUT}" "${INPUT}/expected"
  echo -e " - ${test_dir} \e[32mOK\e[0m"
done

echo "All tests passed!"
