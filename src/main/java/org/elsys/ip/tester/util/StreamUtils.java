package org.elsys.ip.tester.util;

import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StreamUtils {
    public static <T> Collector<T, ?, Optional<T>> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() > 1) {
                        throw new IllegalStateException();
                    }
                    if (list.size() == 0) {
                        return Optional.empty();
                    }
                    return Optional.of(list.get(0));
                }
        );
    }
}
