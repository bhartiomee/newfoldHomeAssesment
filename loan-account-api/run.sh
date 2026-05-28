#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$PROJECT_DIR"
mvn -q -DskipTests package
java -jar target/loan-account-api-1.0.0.jar
