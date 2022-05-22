package org.jenkinsci.plugins.kubernetes.cli.helpers;

import org.apache.commons.compress.utils.IOUtils;

public class TestResourceLoader {
    public static String loadAsString(String name) {
        return new String(loadAsByteArray(name)).replaceAll("\\r\\n", "\n");
    }

    public static byte[] loadAsByteArray(String name) {
        try {
            return IOUtils.toByteArray(TestResourceLoader.class.getResourceAsStream("../" + name));
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }
}
