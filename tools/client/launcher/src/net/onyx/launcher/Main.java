package net.onyx.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Thin wrapper around RuneLite.
 *
 * <p>Its one essential job is isolation: {@code RuneLite.RUNELITE_DIR} is
 * {@code new File(System.getProperty("user.home"), ".runelite")} with no override property, so
 * without redirecting {@code user.home} first this client would share config, profiles, plugins
 * and the game cache with the player's real RuneLite install.
 *
 * <p>RuneLite is invoked reflectively so that the property is set before its class initialiser
 * ever runs.
 */
public final class Main {
    private static final String APP = "Onyx";
    private static final String JAV_CONFIG =
            System.getProperty("onyx.javConfig", "https://rsps.onyxleeds.co.uk/jav_config.ws");

    /** Resolved once, before {@code user.home} is mutated -- recomputing it after would
     * nest a second data dir inside the first. */
    private static final Path DATA_DIR = appDataDir();

    public static void main(String[] args) {
        Path home = DATA_DIR;
        try {
            Files.createDirectories(home);
            System.setProperty("user.home", home.toString());
        } catch (Exception e) {
            fail("Could not create the " + APP + " data folder at:\n" + home, e);
            return;
        }

        List<String> argv = new ArrayList<>();
        argv.add("--jav_config=" + JAV_CONFIG);
        argv.add("--disable-telemetry");
        argv.addAll(Arrays.asList(args));

        try {
            Class<?> runelite = Class.forName("net.runelite.client.RuneLite");
            Method main = runelite.getMethod("main", String[].class);
            main.invoke(null, (Object) argv.toArray(new String[0]));
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            fail(APP + " failed to start.", cause);
        }
    }

    /** Per-user data directory, kept away from ~/.runelite. */
    private static Path appDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home");
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null && !local.isEmpty()) {
                return Paths.get(local, APP);
            }
            return Paths.get(userHome, "AppData", "Local", APP);
        }
        if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", APP);
        }
        return Paths.get(userHome, ".local", "share", APP.toLowerCase());
    }

    /** Write a log next to the data dir and show a dialog, so a friend has something to send. */
    private static void fail(String message, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String body = LocalDateTime.now() + "\n" + message + "\n\n" + sw;

        Path log = DATA_DIR.resolve("launcher-error.log");
        try {
            Files.createDirectories(log.getParent());
            Files.writeString(log, body);
        } catch (Exception ignored) {
            // best effort
        }
        System.err.println(body);

        try {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    message + "\n\n" + t + "\n\nDetails were written to:\n" + log,
                    APP,
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Throwable headless) {
            // no display; the log and stderr will have to do
        }
        System.exit(1);
    }
}
