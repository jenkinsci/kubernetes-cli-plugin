package org.jenkinsci.plugins.kubernetes.cli.helpers;

import org.checkerframework.checker.nullness.qual.NonNull;

/*
 From https://gist.github.com/brianguertin/ada4b65c6d1c4f6d3eee3c12b6ce021b
 */
public class Version implements Comparable<Version> {
    @NonNull
    public final int[] numbers;

    public Version(@NonNull String version) {
        final String split[] = version.split("\\-")[0].split("\\.");
        numbers = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            numbers[i] = Integer.valueOf(split[i]);
        }
    }

    @Override
    public int compareTo(@NonNull Version another) {
        final int maxLength = Math.max(numbers.length, another.numbers.length);
        for (int i = 0; i < maxLength; i++) {
            final int left = i < numbers.length ? numbers[i] : 0;
            final int right = i < another.numbers.length ? another.numbers[i] : 0;
            if (left != right) {
                return left < right ? -1 : 1;
            }
        }
        return 0;
    }
}
