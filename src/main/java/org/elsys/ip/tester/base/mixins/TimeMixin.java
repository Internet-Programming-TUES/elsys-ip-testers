package org.elsys.ip.tester.base.mixins;

import org.assertj.core.data.Offset;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public interface TimeMixin {
    Offset<Integer> ALLOWED_TIME_DELTA = Offset.offset(2);

    class Duration {
        int duration;

        Duration(String duration) {
            Pattern pattern = Pattern.compile("(\\d\\d:\\d\\d:\\d\\d)");
            Matcher matcher = pattern.matcher(duration);
            assertThat(matcher.matches()).isTrue();

            this.duration = getTimeInSeconds(matcher.group(1));
        }

        public void assertIs(int expected) {
            assertThat(duration).isCloseTo(expected, ALLOWED_TIME_DELTA);
        }

        private int getTimeInSeconds(String time) {
            List<Integer> timeGroups = Arrays.stream(time.split(":")).map(Integer::parseInt).collect(Collectors.toList());
            return timeGroups.get(0) * 60 * 60 + timeGroups.get(1) * 60 + timeGroups.get(2);
        }
    }

    class Lap {
        int number;
        Duration start;
        Duration end;

        public Lap(String lab) {
            Pattern pattern = Pattern.compile("(\\d\\d) (\\d\\d:\\d\\d:\\d\\d) / (\\d\\d:\\d\\d:\\d\\d)");
            Matcher matcher = pattern.matcher(lab);
            assertThat(matcher.matches()).isTrue();
            number = Integer.parseInt(matcher.group(1));
            start = new Duration(matcher.group(2));
            end = new Duration(matcher.group(3));
        }

        public void assertIs(int expectedNumber, int expectedStart, int expectedEnd) {
            assertThat(number).isEqualTo(expectedNumber);
            start.assertIs(expectedStart);
            end.assertIs(expectedEnd);
        }
    }

    default Duration createDuration(String duration) {
        return new Duration(duration);
    }

    default List<Lap> createLaps(String laps) {
        return Arrays.stream(laps.split("\n")).map(Lap::new).collect(Collectors.toList());
    }
}
