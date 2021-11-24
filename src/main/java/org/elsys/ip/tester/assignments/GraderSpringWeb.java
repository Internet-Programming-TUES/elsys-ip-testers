package org.elsys.ip.tester.assignments;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import okhttp3.Response;
import org.elsys.ip.tester.base.AbstractAssignmentGrader;
import org.elsys.ip.tester.base.AsyncResult;
import org.elsys.ip.tester.base.mixins.HTTPMixin;
import org.elsys.ip.tester.base.mixins.TimeMixin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderSpringWeb extends AbstractAssignmentGrader implements HTTPMixin, TimeMixin {
    private static final int PORT = 8080;

    @Override
    protected void gradeInternal(Path path) {
        Path target = requireNotNull(test("mvn clean package", 0.025f, () -> {
            mvn(path, "clean", "package");
            return getTarget(path).get();
        }));

        assertThat(isPortInUse(PORT)).isFalse();

        File jarFile = requireNotNull(test("find jar file", 0.025f, () -> findSingleFile(target, ".*\\.jar").get()));
        AsyncResult serverProcess = javaAsync(target, "-jar", jarFile.getName());
        for (int i = 0; i < 10; ++i) {
            delay(2000);
            if (isPortInUse(PORT)) {
                break;
            }
        }
        assertThat(isPortInUse(PORT)).isTrue();

        test("404 Not Found", 0.075f, () -> {
            Response response = makeHTTPRequest(createRequest("/timer/missing-id", "GET"));
            assertThat(response.code()).isEqualTo(404);
        });

        test("400 Bad Request", 0.075f, () -> {
            Response response = makeHTTPRequest(createRequest("/timer", "POST", new BadRequestBody("name", "01:01:01", 5)));

            assertThat(response.code()).isEqualTo(400);
        });

        test("400 Bad Request", 0.075f, () -> {
            Response response = makeHTTPRequest(createRequest("/timer", "POST", new TimeRequestBody("name", "HH:01:01")));

            assertThat(response.code()).isEqualTo(400);
        });

        test("start", 0.15f, () -> {
            Response response = makeHTTPRequest(createRequest("/timer", "POST", new TimeRequestBody("name", "01:01:01")));
            assertThat(response.code()).isEqualTo(201);
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getName()).isEqualTo("name");
            assertThat(responseObject.getDone()).isNull();
            responseObject.toDuration().assertIs(60 * 60 + 60 + 1);
        });

        test("start hms format", 0.15f, () -> {
            Response response = makeHTTPRequest(createRequest("/timer", "POST", new HoursMinutesSecondsRequestBody("complex timer", 1, 5, 80)));
            assertThat(response.code()).isEqualTo(201);
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getName()).isEqualTo("complex timer");
            assertThat(responseObject.getDone()).isNull();
            responseObject.toDuration().assertIs(65 * 60 + 80);
        });

        test("start hms format request header", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new HoursMinutesSecondsRequestBody("request header", 1, 5, 80),
                            ImmutableMap.<String, String>builder().put("TIME-FORMAT", "seconds").build()
                    ));
            assertThat(response.code()).isEqualTo(201);
            TotalSecondsResponseBody responseObject = getResponseObject(response, TotalSecondsResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getName()).isEqualTo("request header");
            assertThat(responseObject.getDone()).isNull();
            responseObject.toDuration().assertIs(65 * 60 + 80);
        });

        test("start hms format request header 2", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new HoursMinutesSecondsRequestBody("request header 2", 1, 5, 70),
                            ImmutableMap.<String, String>builder().put("TIME-FORMAT", "hours-minutes-seconds").build()
                    ));
            assertThat(response.code()).isEqualTo(201);
            HoursMinutesSecondsResponseBody responseObject = getResponseObject(response, HoursMinutesSecondsResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getName()).isEqualTo("request header 2");
            assertThat(responseObject.getDone()).isNull();
            responseObject.toDuration().assertIs(65 * 60 + 70);
        });

        test("start/get", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("fast timer", "00:00:02")));
            assertThat(response.code()).isEqualTo(201);
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getName()).isEqualTo("fast timer");

            Duration duration = createDuration(responseObject.getTime());
            duration.assertIs(2);

            delay(3000);

            response = makeHTTPRequest(
                    createRequest(
                            "/timer/" + responseObject.getId(),
                            "GET"));
            assertThat(response.code()).isEqualTo(200);
            TotalSecondsResponseBody responseObjectGet = getResponseObject(response, TotalSecondsResponseBody.class);
            assertThat(responseObjectGet.getId()).isEqualTo(responseObject.getId());
            assertThat(responseObjectGet.getName()).isEqualTo("fast timer");
            assertThat(responseObjectGet.getDone()).isEqualTo("yes");
            responseObjectGet.toDuration().assertIs(0);
        });

        test("start/get headers", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("one minute timer", "00:01:00"),
                            ImmutableMap.<String, String>builder().put("TIME-FORMAT", "time").build()));
            assertThat(response.code()).isEqualTo(201);
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getName()).isEqualTo("one minute timer");
            responseObject.toDuration().assertIs(60);

            delay(5000);

            response = makeHTTPRequest(
                    createRequest(
                            "/timer/" + responseObject.getId(),
                            "GET",
                            null,
                            ImmutableMap.<String, String>builder().put("TIME-FORMAT", "seconds").build()));
            assertThat(response.code()).isEqualTo(200);
            TotalSecondsResponseBody responseObjectGet = getResponseObject(response, TotalSecondsResponseBody.class);
            assertThat(responseObjectGet.getId()).isEqualTo(responseObject.getId());
            assertThat(responseObjectGet.getName()).isEqualTo("one minute timer");
            responseObjectGet.toDuration().assertIs(55);
        });

        test("start/get headers 2", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("two minute timer", "00:02:00"),
                            ImmutableMap.<String, String>builder().put("TIME-FORMAT", "time").build()));
            assertThat(response.code()).isEqualTo(201);
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getId()).isNotEmpty();
            assertThat(responseObject.getDone()).isNull();
            assertThat(responseObject.getName()).isEqualTo("two minute timer");
            responseObject.toDuration().assertIs(120);

            response = makeHTTPRequest(
                    createRequest(
                            "/timer/" + responseObject.getId(),
                            "GET",
                            null,
                            ImmutableMap.<String, String>builder().put("TIME-FORMAT", "hours-minutes-seconds").build()));
            assertThat(response.code()).isEqualTo(200);
            HoursMinutesSecondsResponseBody responseObjectGet = getResponseObject(response, HoursMinutesSecondsResponseBody.class);
            assertThat(responseObjectGet.getId()).isEqualTo(responseObject.getId());
            assertThat(responseObjectGet.getName()).isEqualTo("two minute timer");
            assertThat(responseObjectGet.getDone()).isEqualTo("no");
            responseObjectGet.toDuration().assertIs(55);
        });

        test("start/get?long", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("long poll timer", "00:15:00")));
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);

            Stopwatch stopwatch = Stopwatch.createStarted();
            response = makeHTTPRequest(createRequest("/timer/" + responseObject.getId() + "?long=true", "GET"));
            stopwatch.stop();
            createDuration((int)stopwatch.elapsed(TimeUnit.SECONDS)).assertIs(10);
            assertThat(response.code()).isEqualTo(200);
            TimeResponseBody responseObjectGet = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObjectGet.getId()).isEqualTo(responseObject.getId());
            assertThat(responseObjectGet.getName()).isEqualTo("long poll timer");
            assertThat(responseObjectGet.getDone()).isEqualTo("no");
            responseObjectGet.toDuration().assertIs(5);

            stopwatch = Stopwatch.createStarted();
            response = makeHTTPRequest(createRequest("/timer/" + responseObject.getId() + "?long=true", "GET"));
            stopwatch.stop();
            createDuration((int)stopwatch.elapsed(TimeUnit.SECONDS)).assertIs(5);
            assertThat(response.code()).isEqualTo(200);
            responseObjectGet = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObjectGet.getId()).isEqualTo(responseObject.getId());
            assertThat(responseObjectGet.getName()).isEqualTo("long poll timer");
            assertThat(responseObjectGet.getDone()).isEqualTo("yes");
            responseObjectGet.toDuration().assertIs(0);
        });

        serverProcess.kill();

        serverProcess = javaAsync(target, "-jar", jarFile.getName());
        for (int i = 0; i < 10; ++i) {
            delay(2000);
            if (isPortInUse(PORT)) {
                break;
            }
        }

        test("start/get?long", 0.15f, () -> {
            Response response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("long poll timer", "00:05:00")));
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("1");
            TimeResponseBody responseObject = getResponseObject(response, TimeResponseBody.class);

            response = makeHTTPRequest(createRequest("/timer/" + responseObject.getId(), "GET"));
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("1");

            response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("long poll timer", "00:10:00")));
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("2");

            delay(6000);

            response = makeHTTPRequest(createRequest("/timer/" + responseObject.getId(), "GET"));
            responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getDone()).isEqualTo("yes");
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("1");

            response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("long poll timer", "00:55:00")));
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("2");
            response = makeHTTPRequest(
                    createRequest(
                            "/timer",
                            "POST",
                            new TimeRequestBody("long poll timer", "00:55:00")));
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("3");
            responseObject = getResponseObject(response, TimeResponseBody.class);
            delay(6000);

            response = makeHTTPRequest(createRequest("/timer/" + responseObject.getId(), "GET"));
            responseObject = getResponseObject(response, TimeResponseBody.class);
            assertThat(responseObject.getDone()).isEqualTo("no");
            assertThat(response.header("ACTIVE-TIMERS")).isEqualTo("3");
        });
    }

    class HoursMinutesSecondsRequestBody {
        private final String name;
        private final int hours;
        private final int minutes;
        private final int seconds;

        public HoursMinutesSecondsRequestBody(String name, int hours, int minutes, int seconds) {
            this.name = name;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }

        public String getName() {
            return name;
        }

        public int getHours() {
            return hours;
        }

        public int getMinutes() {
            return minutes;
        }

        public int getSeconds() {
            return seconds;
        }
    }

    class TimeRequestBody {
        private final String name;
        private final String time;

        public TimeRequestBody(String name, String time) {
            this.name = name;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }
    }

    class BadRequestBody extends TimeRequestBody {
        private final int seconds;

        public BadRequestBody(String name, String time, int seconds) {
            super(name, time);
            this.seconds = seconds;
        }

        public int getSeconds() {
            return seconds;
        }
    }

    class TimeResponseBody {
        private final String id;
        private final String name;
        private final String time;
        private final String done;

        public TimeResponseBody(String id, String name, String time, String done) {
            this.id = id;
            this.name = name;
            this.time = time;
            this.done = done;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }

        public String getDone() {
            return done;
        }

        public Duration toDuration() {
            return createDuration(getTime());
        }
    }

    class TotalSecondsResponseBody {
        private final String id;
        private final String name;
        private final int totalSeconds;
        private final String done;

        public TotalSecondsResponseBody(String id, String name, int totalSeconds, String done) {
            this.id = id;
            this.name = name;
            this.totalSeconds = totalSeconds;
            this.done = done;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getTotalSeconds() {
            return totalSeconds;
        }

        public String getDone() {
            return done;
        }

        public Duration toDuration() {
            return createDuration(getTotalSeconds());
        }
    }

    class HoursMinutesSecondsResponseBody {
        private final String id;
        private final String name;
        private final int seconds;
        private final int minutes;
        private final int hours;
        private final String done;

        public HoursMinutesSecondsResponseBody(String id, String name, int seconds, int minutes, int hours, String done) {
            this.id = id;
            this.name = name;
            this.seconds = seconds;
            this.minutes = minutes;
            this.hours = hours;
            this.done = done;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getSeconds() {
            return seconds;
        }

        public int getMinutes() {
            return minutes;
        }

        public int getHours() {
            return hours;
        }

        public String getDone() {
            return done;
        }

        public Duration toDuration() {
            return createDuration(getHours(), getMinutes(), getSeconds());
        }
    }
}
