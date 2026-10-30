package com.hostel.management.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Robust .env loader that discovers and loads environment variables from a .env file
 * located in the project root, parent directories, or custom specified path.
 *
 * Variables loaded are registered into System properties (if not already set)
 * so they are immediately available to Spring Boot, JavaMailSender, and JDBC.
 *
 * Security: NEVER logs secrets, passwords, or credentials to terminal or logs.
 */
public final class DotenvLoader {

    private static final Logger log = Logger.getLogger(DotenvLoader.class.getName());
    private static volatile Map<String, String> cachedProperties = null;

    private DotenvLoader() {}

    /**
     * Loads .env properties into System properties and returns an unmodifiable Map.
     */
    public static synchronized Map<String, String> load() {
        if (cachedProperties != null) {
            return cachedProperties;
        }

        File envFile = findEnvFile();
        if (envFile == null || !envFile.exists() || !envFile.isFile()) {
            log.fine("[DOTENV] No .env file found in working directory or parent directories.");
            cachedProperties = Collections.emptyMap();
            return cachedProperties;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // Strip surrounding matching double or single quotes
                if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) ||
                    (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                    value = value.substring(1, value.length() - 1);
                }

                // If Google 16-character App Password was pasted with spaces (e.g. 'abcd efgh ijkl mnop'),
                // automatically sanitize spaces so SMTP authentication succeeds.
                if ((key.equals("MAIL_PASSWORD") || key.equals("SPRING_MAIL_PASSWORD")) && value.contains(" ")) {
                    String noSpaces = value.replace(" ", "");
                    if (noSpaces.length() == 16) {
                        value = noSpaces;
                    }
                }

                properties.put(key, value);

                // Set into System properties if not already provided via command line or OS env
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
            checkIdeaWorkspaceFallback(properties);
            log.info(String.format("[DOTENV] Loaded %d environment variable(s) from '%s'", properties.size(), envFile.getAbsolutePath()));
        } catch (Exception e) {
            log.warning("[DOTENV] Failed to read .env file: " + e.getMessage());
        }

        checkIdeaWorkspaceFallback(properties);
        cachedProperties = Collections.unmodifiableMap(properties);
        return cachedProperties;
    }

    private static void checkIdeaWorkspaceFallback(Map<String, String> properties) {
        String user = properties.get("MAIL_USERNAME");
        String pass = properties.get("MAIL_PASSWORD");
        if (user != null && !user.isBlank() && !user.contains("your.email") &&
            pass != null && !pass.isBlank() && !pass.contains("your16char")) {
            return;
        }

        File currentDir = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        File candidate = currentDir;
        for (int i = 0; i < 4 && candidate != null; i++) {
            File workspaceXml = new File(candidate, ".idea/workspace.xml");
            if (workspaceXml.exists() && workspaceXml.isFile()) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(workspaceXml), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        if (l.contains("<env name=\"") && l.contains("value=\"")) {
                            int nStart = l.indexOf("name=\"") + 6;
                            int nEnd = l.indexOf("\"", nStart);
                            int vStart = l.indexOf("value=\"") + 7;
                            int vEnd = l.indexOf("\"", vStart);
                            if (nStart > 5 && nEnd > nStart && vStart > 6 && vEnd > vStart) {
                                String name = l.substring(nStart, nEnd).trim();
                                String val = l.substring(vStart, vEnd).trim();
                                if (!properties.containsKey(name) || properties.get(name).contains("your.email") || properties.get(name).contains("your16char")) {
                                    properties.put(name, val);
                                    if (System.getProperty(name) == null && System.getenv(name) == null) {
                                        System.setProperty(name, val);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            }
            candidate = candidate.getParentFile();
        }
    }

    /**
     * Locates the .env file by checking custom system property or searching up directory hierarchy.
     */
    public static File findEnvFile() {
        // 1. Check system property DOTENV_PATH
        String customPath = System.getProperty("DOTENV_PATH");
        if (customPath != null && !customPath.isBlank()) {
            File f = new File(customPath);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }

        // 2. Search upwards from user.dir up to 4 directory levels
        File currentDir = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        File candidate = currentDir;
        for (int i = 0; i < 4 && candidate != null; i++) {
            File testFile = new File(candidate, ".env");
            if (testFile.exists() && testFile.isFile()) {
                return testFile;
            }
            candidate = candidate.getParentFile();
        }

        return null;
    }
}
