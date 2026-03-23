#!/bin/bash

set -euo pipefail

die() {
    echo "$*" >&2
    exit 255
}

if [[ $# -ne 1 ]] ; then
  die "Expected the jar file to run tests against."
fi

if [[ -n "${JAVA_HOME}" ]] ; then
  JAVA=$(realpath "${JAVA_HOME}/bin/java")
  if [[ ! -e ${JAVA} ]] ; then
    die "JAVA_HOME was set but could not find 'java' executable."
  fi
else
  echo "JAVA_HOME not set."
  JAVA=$(realpath "$(which java)")
  if [[ ! -e ${JAVA} ]] ; then
    die "Could not find 'java' executable."
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
  ${JAVA} -jar "${JAR_FILE}" -g "${INPUT}/grammar" -o "${GENERATED}" --package "" --main --overwrite
  ${JAVA} "${GENERATED}" "${INPUT}/input" > "${OUTPUT}"
  diff "${OUTPUT}" "${INPUT}/expected"
  echo -e " - ${test_dir} \e[32mOK\e[0m"
done

echo "All tests passed!"
