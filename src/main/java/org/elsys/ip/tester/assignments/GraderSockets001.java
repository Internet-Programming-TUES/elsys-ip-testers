package org.elsys.ip.tester.assignments;

import org.elsys.ip.tester.base.AbstractAssignmentGrader;
import org.elsys.ip.tester.base.AsyncResult;
import org.elsys.ip.tester.base.Result;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderSockets001 extends AbstractAssignmentGrader {

    @Override
    protected void gradeInternal(Path path) throws Exception {
        test("mvn clean package", 0.04f, () -> {
            mvn(path, "clean", "package");
        });

        File serverJar = requireNotNull(test("find server.jar", 0.03f, () -> findSingleFile(path, ".*server.*\\.jar").get()));
        File clientJar = requireNotNull(test("find client.jar", 0.03f, () -> findSingleFile(path, ".*client.*\\.jar").get()));

        // Test server input validation
        test("server -5", 0.05f, () -> {
            Result result = java(path, 1, "-jar", serverJar.getAbsolutePath(), "-5");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid arguments");
        });

        test("server 65536", 0.05f, () -> {
            Result result = java(path, 1, "-jar", serverJar.getAbsolutePath(), "65536");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid arguments");
        });

        test("server A B", 0.05f, () -> {
            Result result = java(path, 1, "-jar", serverJar.getAbsolutePath(), "A", "B");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid arguments");
        });

        test("server 5555 [TWICE]", 0.1f, () -> {
            AsyncResult asyncResult = javaAsync(path, "-jar", serverJar.getAbsolutePath(), "5555");
            delay(1000);

            Result result = java(path, 2, "-jar", serverJar.getAbsolutePath(), "5555");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("port is already in use");

            assertThat(asyncResult.getProcess().isAlive()).isTrue();
            asyncResult.kill();
        });

        // Test client input validation
        test("client badHost:5555", 0.05f, () -> {
            Result result = java(path, 3, "-jar", clientJar.getAbsolutePath(), "badHost:5555");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid host");
        });

        test("client localhost:-5", 0.05f, () -> {
            Result result = java(path, 1, "-jar", clientJar.getAbsolutePath(), "localhost:-5");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid arguments");
        });

        test("client localhost:70000", 0.05f, () -> {
            Result result = java(path, 1, "-jar", clientJar.getAbsolutePath(), "localhost:70000");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid arguments");
        });

        test("client localhost:5555 another", 0.05f, () -> {
            Result result = java(path, 1, "-jar", clientJar.getAbsolutePath(), "localhost:5555", "another");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("invalid arguments");
        });

        test("client localhost:5555 [No server]", 0.05f, () -> {
            Result result = java(path, 4, "-jar", clientJar.getAbsolutePath(), "localhost:5555");
            assertThat(result.getOutput()).isEmpty();
            assertThat(result.getError()).isEqualTo("connection not possible");
        });

        AsyncResult server = javaAsync(path, "-jar", serverJar.getAbsolutePath(), "5555");
        delay(1000);

        test("server / client localhost:5555 > quit", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("test");
            assertThat(client.canRead()).isFalse();
            client.println("quit");
            assertThat(client.readLine()).isEqualTo("server disconnect");
            delay(1000);
            assertThat(client.getProcess().isAlive()).isFalse();
            assertThat(client.getProcess().exitValue()).isEqualTo(0);
            assertThat(server.getProcess().isAlive()).isTrue();
        });

        test("server / client localhost:5555 > Exit", 0.1f, () -> {
            AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            client.println("test");
            assertThat(client.canRead()).isFalse();
            client.println("Exit");
            assertThat(client.readLine()).isEqualTo("server disconnect");
            delay(1000);
            assertThat(client.getProcess().isAlive()).isFalse();
            assertThat(client.getProcess().exitValue()).isEqualTo(0);
            assertThat(server.getProcess().isAlive()).isTrue();
        });

        test("server / client localhost:5555 [MULTIPLE CLIENTS]", 0.1f, () -> {
            AsyncResult client1 = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");
            AsyncResult client2 = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");

            delay(1000);

            assertThat(client1.getProcess().isAlive()).isTrue();
            assertThat(client2.getProcess().isAlive()).isTrue();

            client1.println("eXit");
            delay(1000);
            assertThat(client1.readLine()).isEqualTo("server disconnect");
            delay(2000);
            assertThat(client1.getProcess().isAlive()).isFalse();
            assertThat(client1.getProcess().exitValue()).isEqualTo(0);
            assertThat(server.getProcess().isAlive()).isTrue();

            assertThat(client2.getProcess().isAlive()).isTrue();
            client2.kill();
        });

        test("server / client localhost:5555 [10 clients]", 0.1f, () -> {
            List<AsyncResult> asyncResults = new ArrayList<>();
            for (int i = 0; i < 10; ++i) {
                AsyncResult client = javaAsync(path, "-jar", clientJar.getAbsolutePath(), "localhost:5555");
                asyncResults.add(client);
            }

            asyncResults.forEach(ar -> assertThat(ar.getProcess().isAlive()).isTrue());

            delay(1000);

            for (AsyncResult asyncResult : asyncResults) {
                asyncResult.println("quit");
            }

            delay(5000);

            asyncResults.parallelStream().forEach(ar -> assertThat(ar.readLine()).isEqualTo("server disconnect"));
            assertThat(asyncResults.stream().filter(ar -> ar.getProcess().isAlive()).count()).isEqualTo(0);
        });
    }
}
