package ru.malik.savefrom.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class ProcessUtils {
    private static final long DESTROY_GRACE_SECONDS = 5L;

    private ProcessUtils() {
    }

    public static int waitFor(Process process, Duration timeout, String processName) throws InterruptedException {
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (finished) {
            return process.exitValue();
        }

        process.destroy();
        if (!process.waitFor(DESTROY_GRACE_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor();
        }

        throw new RuntimeException(processName + " timed out after " + timeout.toSeconds() + " seconds");
    }
}
