#!/bin/bash
# Source from AlmaLinux WSL before ./mvnw when Windows JDK is not 17.
# Usage: source scripts/wsl-java17-env.sh  (from repo root)
export JAVA_HOME=/usr/lib/jvm/java-17
export PATH="$JAVA_HOME/bin:$PATH"