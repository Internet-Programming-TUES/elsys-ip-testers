package org.elsys.ip.tester.assignments;

import org.elsys.ip.tester.base.AsyncResult;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderSockets002 extends GraderSockets001 {
    @Override
    protected void gradeInternal(Path path) throws Exception {
        super.gradeInternal(path);
        multiplyGrade(0.1f);

        test("server / client localhost:5555 > invalid input", 0.2f, () -> {
            AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("invalid");
            assertThat(client.readLine()).isEqualTo("invalid input");
            delay(1000);

            client.println("time +2:00");
            assertThat(client.readLine()).isEqualTo("invalid input");
            delay(1000);

            client.println("time -2:00");
            assertThat(client.readLine()).isEqualTo("invalid input");
            delay(1000);

            client.println("time 02:00");
            assertThat(client.readLine()).isEqualTo("invalid input");
            delay(1000);
        });

        test("server / client localhost:5555 > invalid time zone", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("time +02:30");
            assertThat(client.readLine()).isEqualTo("invalid time zone");
            delay(1000);

            client.println("Exit");
            assertThat(client.readLine()).isEqualTo("server disconnect");
            delay(1000);
            assertThat(client.getProcess().isAlive()).isFalse();
            assertThat(client.getProcess().exitValue()).isEqualTo(0);
            assertThat(server.getProcess().isAlive()).isTrue();
        });

        test("server / client localhost:5555 > time", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-Duser.timezone=Europe/Sofia", "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("time");
            assertThat(client.readLine()).isEqualTo(getTime(7200));
            delay(1000);
        });

        test("server / client localhost:5555 > time +03:00", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("time +03:00");
            assertThat(client.readLine()).isEqualTo(getTime(10800));
            delay(1000);
        });

        test("server / client localhost:5555 > time -03:00", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("time -03:00");
            assertThat(client.readLine()).isEqualTo(getTime(-10800));
            delay(1000);
        });

        test("server / client localhost:5555 > client different time zone", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-Duser.timezone=America/Havana", "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("time");
            assertThat(client.readLine()).isEqualTo(getTime(-18000));
            delay(1000);
        });

        test("server / client localhost:5555 > 24-hour format", 0.2f, () -> {
            AsyncResult client = javaAsync(path, "-Duser.timezone=Europe/Sofia", "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("time");
            assertThat(client.readLine()).isEqualTo(getTime(7200));
            delay(1000);
            client.println("time +14:00");
            assertThat(client.readLine()).isEqualTo(getTime(50400));
            delay(1000);
        });
    }

    private String getTime(int offset) {
        return LocalDateTime.now(ZoneOffset.UTC).plusSeconds(offset).format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
