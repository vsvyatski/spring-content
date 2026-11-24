#!/usr/bin/env bash
set -euo pipefail

REVISION_ARG=""
if [ -n "${TRAVIS_TAG:-}" ]; then
  export BUILD_TYPE=release/"$TRAVIS_TAG"
  REVISION_ARG="-Drevision=$TRAVIS_TAG"
fi

echo "BUILD_TYPE: ${BUILD_TYPE:-}"

# Signing: maven-gpg-plugin (ci profile) expects a key in the local gpg agent — e.g. setup-java on GitHub Actions.
mvn -B "$REVISION_ARG" -U -P ci,docs deploy -DskipTests=true --settings .mvn/settings.xml
