package io.bsoft.echo.lwjgl3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * On macOS, LWJGL3 requires the JVM flag {@code -XstartOnFirstThread}. If the flag is
 * missing this helper relaunches the current JVM with it so the game can be started
 * from any IDE or a plain {@code java -jar}.
 */
public final class StartupHelper {

    private static final String JVM_RESTARTED_ARG = "echoJvmRestarted";

    private StartupHelper() {
    }

    /**
     * @return true if the process was relaunched and the caller should exit immediately.
     */
    public static boolean startNewJvmIfRequired() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            return false;
        }
        if ("true".equals(System.getProperty(JVM_RESTARTED_ARG))) {
            System.err.println("ECHO: JVM restart did not apply -XstartOnFirstThread; continuing anyway.");
            return false;
        }
        long pid = ProcessHandle.current().pid();
        // The JVM exposes the flag through this env var; it is not reliably listed in the input arguments.
        if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid))) {
            return false;
        }
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.contains("-XstartOnFirstThread")) {
                return false;
            }
        }
        // Not present: relaunch with the flag.
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String mainClass = System.getenv("JAVA_MAIN_CLASS_" + pid);
        if (mainClass == null) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            mainClass = trace[trace.length - 1].getClassName();
        }
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-XstartOnFirstThread");
        command.add("-D" + JVM_RESTARTED_ARG + "=true");
        command.add("-cp");
        command.add(classpath);
        command.add(mainClass);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("ECHO: failed to relaunch JVM with -XstartOnFirstThread: " + e.getMessage());
            return false;
        }
        return true;
    }
}
