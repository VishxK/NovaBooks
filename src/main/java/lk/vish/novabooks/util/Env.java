package lk.vish.novabooks.util;

import java.io.InputStream;
import java.util.Properties;

public class Env {
    private static final Properties APP_PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = Env.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (inputStream == null) {
                System.err.println("ERROR: 'app.properties' file is NOT found in classpath (resources folder)!");
            } else {
                APP_PROPERTIES.load(inputStream);
                System.out.println("'app.properties' loaded successfully!");
            }
        } catch (Exception e) {
            System.err.println("Error loading application properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return APP_PROPERTIES.getProperty(key);
    }

    public static Properties getAppProperties() {
        return APP_PROPERTIES;
    }
}
