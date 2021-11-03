package org.elsys.ip.tester.assignments;

import okhttp3.Response;
import org.elsys.ip.tester.base.AbstractAssignmentGrader;
import org.elsys.ip.tester.base.mixins.HTTPMixin;
import org.elsys.ip.tester.base.mixins.TimeMixin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderServlet extends AbstractAssignmentGrader implements HTTPMixin, TimeMixin {
    @Override
    protected void gradeInternal(Path path) {
        Path target = requireNotNull(test("mvn clean package", 0.025f, () -> {
            mvn(path, "clean", "package");
            return getTarget(path).get();
        }));

        assertThat(isPortInUse(8080)).isFalse();

        File jarFile = requireNotNull(test("find jar file", 0.025f, () -> findSingleFile(target, ".*\\.jar").get()));
        javaAsync(target, "-jar", jarFile.getName());
        delay(5000);

        test("404 Not Found", 0.075f, () -> {
            Response response = makeHTTPRequest(createRequest("/missing-id", "GET"));
            assertThat(response.code()).isEqualTo(404);
            assertThat(getResponseText(response)).isEqualTo("");

            response = makeHTTPRequest(createRequest("/missing-id/lap", "PUT"));
            assertThat(response.code()).isEqualTo(404);
            assertThat(getResponseText(response)).isEqualTo("");

            response = makeHTTPRequest(createRequest("/missing-id", "DELETE"));
            assertThat(response.code()).isEqualTo(404);
            assertThat(getResponseText(response)).isEqualTo("");
        });

        test("400 Bad Request", 0.075f, () -> {
            Response response = makeHTTPRequest(createRequest("", "DELETE"));

            assertThat(response.code()).isEqualTo(400);
            assertThat(getResponseText(response)).isEqualTo("");

            response = makeHTTPRequest(createRequest("/id/lab/new", "PUT"));

            assertThat(response.code()).isEqualTo(400);
            assertThat(getResponseText(response)).isEqualTo("");
        });

        test("start/get", 0.15f, () -> {
            Response response = makeHTTPRequest(createRequest("/start", "POST"));
            assertThat(response.code()).isEqualTo(201);
            String id = getResponseText(response);
            assertThat(id).isNotEmpty();

            delay(12000);

            response = makeHTTPRequest(createRequest("/" + id, "GET"));
            assertThat(response.code()).isEqualTo(200);
            String result = getResponseText(response);
            Duration duration = createDuration(result);
            duration.assertIs(12);
        });

        test("start/stop", 0.15f, () -> {
            Response response = makeHTTPRequest(createRequest("/start", "POST"));
            assertThat(response.code()).isEqualTo(201);
            String id = getResponseText(response);
            assertThat(id).isNotEmpty();

            delay(12000);

            response = makeHTTPRequest(createRequest("/" + id, "DELETE"));
            assertThat(response.code()).isEqualTo(200);
            String result = getResponseText(response);
            List<Lap> laps = createLaps(result);
            assertThat(laps).hasSize(1);
            laps.get(0).assertIs(1, 12, 12);
        });

        test("start/lab/stop", 0.2f, () -> {
            Response response = makeHTTPRequest(createRequest("/start", "POST"));
            assertThat(response.code()).isEqualTo(201);
            String id = getResponseText(response);
            assertThat(id).isNotEmpty();

            delay(7000);

            response = makeHTTPRequest(createRequest("/" + id, "GET"));
            assertThat(response.code()).isEqualTo(200);
            String result = getResponseText(response);
            Duration duration = createDuration(result);
            duration.assertIs(7);

            delay(13000);

            response = makeHTTPRequest(createRequest("/" + id + "/lap", "PUT"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            duration = createDuration(result);
            duration.assertIs(20);

            delay(3000);

            response = makeHTTPRequest(createRequest("/" + id + "/lap", "PUT"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            duration = createDuration(result);
            duration.assertIs(3);

            response = makeHTTPRequest(createRequest("/" + id, "GET"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            duration = createDuration(result);
            duration.assertIs(23);

            delay(3000);

            response = makeHTTPRequest(createRequest("/" + id, "DELETE"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            List<Lap> laps = createLaps(result);
            assertThat(laps).hasSize(3);
            laps.get(0).assertIs(1, 20, 20);
            laps.get(1).assertIs(2, 3, 23);
            laps.get(2).assertIs(3, 3, 26);
        });

        test("multiple timers", 0.3f, () -> {
            Response response = makeHTTPRequest(createRequest("/start", "POST"));
            assertThat(response.code()).isEqualTo(201);
            String timer1ID = getResponseText(response);
            assertThat(timer1ID).isNotEmpty();

            delay(7000);

            response = makeHTTPRequest(createRequest("/start", "POST"));
            assertThat(response.code()).isEqualTo(201);
            String timer2ID = getResponseText(response);
            assertThat(timer2ID).isNotEmpty();

            response = makeHTTPRequest(createRequest("/" + timer1ID + "/lap", "PUT"));
            assertThat(response.code()).isEqualTo(200);
            String result = getResponseText(response);
            Duration duration = createDuration(result);
            duration.assertIs(7);

            delay(3000);

            response = makeHTTPRequest(createRequest("/" + timer1ID, "GET"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            duration = createDuration(result);
            duration.assertIs(10);

            response = makeHTTPRequest(createRequest("/" + timer2ID, "GET"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            duration = createDuration(result);
            duration.assertIs(3);

            response = makeHTTPRequest(createRequest("/" + timer1ID, "DELETE"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            List<Lap> laps = createLaps(result);
            assertThat(laps).hasSize(2);
            laps.get(0).assertIs(1, 7, 7);
            laps.get(1).assertIs(2, 3, 10);

            response = makeHTTPRequest(createRequest("/" + timer2ID, "DELETE"));
            assertThat(response.code()).isEqualTo(200);
            result = getResponseText(response);
            laps = createLaps(result);
            assertThat(laps).hasSize(1);
            laps.get(0).assertIs(1, 3, 3);
        });
    }
}
