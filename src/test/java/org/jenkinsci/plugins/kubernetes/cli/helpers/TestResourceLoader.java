package org.jenkinsci.plugins.kubernetes.cli.helpers;

import java.io.IOException;
import java.io.InputStream;

public class TestResourceLoader {
    public static String loadAsString(String name) {
        return new String(loadAsByteArray(name)).replaceAll("\\r\\n", "\n");
    }

    public static byte[] loadAsByteArray(String name) {
        try (InputStream inputStream = TestResourceLoader.class.getResourceAsStream("../" + name)) {
            if (inputStream == null) {
                throw new RuntimeException("Could not find resource:[" + name + "].");
            }
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource:[" + name + "].", e);
        }
    }
}
