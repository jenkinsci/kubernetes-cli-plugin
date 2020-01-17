package org.jenkinsci.plugins.kubernetes.cli.helpers;

import org.apache.commons.compress.utils.IOUtils;

public class TestResourceLoader {
    public static String load(String name) {
        try {
            return new String(IOUtils.toByteArray(TestResourceLoader.class.getResourceAsStream("../"+name))).replaceAll("\\r\\n", "\n");
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }
}
