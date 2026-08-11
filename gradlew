#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar:$APP_HOME/gradle/wrapper/gradle-cli.jar:$APP_HOME/gradle/wrapper/gradle-files.jar"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD=java
fi

exec "$JAVA_CMD" ${DEFAULT_JVM_OPTS:-} ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"