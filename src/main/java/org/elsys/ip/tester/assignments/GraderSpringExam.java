package org.elsys.ip.tester.assignments;

import com.google.common.collect.ImmutableMap;
import okhttp3.Response;
import org.elsys.ip.tester.base.AbstractAssignmentGrader;
import org.elsys.ip.tester.base.AsyncResult;
import org.elsys.ip.tester.base.mixins.HTTPMixin;
import org.elsys.ip.tester.base.mixins.TimeMixin;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderSpringExam extends AbstractAssignmentGrader implements HTTPMixin, TimeMixin {
    private static final int PORT = 8080;

    @Override
    public String getBasePath() {
        return "";
    }

    @Override
    public int getPort() {
        return PORT;
    }

    @Override
    protected void gradeInternal(Path path) {
        Path target = requireNotNull(test("mvn clean package", 0.05f, () -> {
            mvn(path, "clean", "package");
            return getTarget(path).get();
        }));

        assertThat(isPortInUse(PORT)).isFalse();

        File jarFile = requireNotNull(test("find jar file", 0.05f, () -> findSingleFile(target, ".*\\.jar").get()));
        AsyncResult serverProcess = javaAsync(target, "-jar", jarFile.getName());
        for (int i = 0; i < 30; ++i) {
            delay(2000);
            if (isPortInUse(PORT)) {
                break;
            }
        }
        assertThat(isPortInUse(PORT)).isTrue();

        test("404 Not Found", 0.1f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes/missing-id", "GET"));
            assertThat(response.code()).isEqualTo(404);
        });

        test("create note", 0.1f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("Hello World")));
            assertThat(response.code()).isEqualTo(201);
            String responseObject = getResponseObject(response, String.class);
            assertThat(responseObject).isNotEmpty();
        });

        test("create and read note", 0.2f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("Gospodine?")));
            String id = getResponseObject(response, String.class);

            Response getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET"));
            NoteWithId responseObject = getResponseObject(getResponse, NoteWithId.class);
            assertThat(getResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("Gospodine?");
        });

        test("delete 404", 0.05f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes/missing-id", "DELETE"));
            assertThat(response.code()).isEqualTo(404);
        });

        test("delete", 0.05f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("Nobody cares.")));
            String id = getResponseObject(response, String.class);

            Response getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET"));
            assertThat(getResponse.code()).isEqualTo(200);

            Response deleteResponse = makeHTTPRequest(createRequest("/notes/" + id, "DELETE"));
            assertThat(deleteResponse.code()).isEqualTo(204);

            Response getResponse2 = makeHTTPRequest(createRequest("/notes/" + id, "GET"));
            assertThat(getResponse2.code()).isEqualTo(404);
        });

        test("put 404", 0.05f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes/missing-id", "PUT", new Note("Expect failure")));
            assertThat(response.code()).isEqualTo(404);
        });

        test("put", 0.05f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("I got this far, am I?")));
            String id = getResponseObject(response, String.class);

            Response getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET"));
            assertThat(getResponse.code()).isEqualTo(200);

            Response putResponse = makeHTTPRequest(createRequest("/notes/" + id, "PUT", new Note("Please, leave me alone!!!")));
            NoteWithId responseObject = getResponseObject(putResponse, NoteWithId.class);
            assertThat(putResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("Please, leave me alone!!!");

            Response getResponse2 = makeHTTPRequest(createRequest("/notes/" + id, "GET"));
            NoteWithId responseObject2 = getResponseObject(getResponse2, NoteWithId.class);
            assertThat(getResponse2.code()).isEqualTo(200);
            assertThat(responseObject2.getId()).isEqualTo(id);
            assertThat(responseObject2.getText()).isEqualTo("Please, leave me alone!!!");
        });

        test("create and read note upper lower", 0.1f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("Don'T Yield @ me")));
            String id = getResponseObject(response, String.class);

            Response getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET", null,
                    ImmutableMap.<String, String>builder().put("char-case", "uppercase").build()));
            NoteWithId responseObject = getResponseObject(getResponse, NoteWithId.class);
            assertThat(getResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("DON'T YIELD @ ME");

            getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET", null,
                    ImmutableMap.<String, String>builder().put("char-case", "lowercase").build()));
            responseObject = getResponseObject(getResponse, NoteWithId.class);
            assertThat(getResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("don't yield @ me");
        });

        test("put previous", 0.1f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("Just one more time")));
            String id = getResponseObject(response, String.class);

            Response putResponse = makeHTTPRequest(createRequest("/notes/" + id, "PUT", new Note("And we can celebrate")));
            NoteWithId responseObject = getResponseObject(putResponse, NoteWithId.class);
            assertThat(putResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("And we can celebrate");
            assertThat(putResponse.header("previous")).isEqualTo("Just one more time");
        });

        test("all", 0.1f, () -> {
            Response response = makeHTTPRequest(createRequest("/notes", "POST", new Note("It's been a long day")));
            String id = getResponseObject(response, String.class);

            Response getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET", null,
                    ImmutableMap.<String, String>builder().put("char-case", "uppercase").build()));
            NoteWithId responseObject = getResponseObject(getResponse, NoteWithId.class);
            assertThat(getResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("IT'S BEEN A LONG DAY");

            Response putResponse = makeHTTPRequest(createRequest("/notes/" + id, "PUT", new Note("without you, my Friend.")));
            NoteWithId putResponseObject = getResponseObject(putResponse, NoteWithId.class);
            assertThat(putResponse.code()).isEqualTo(200);
            assertThat(putResponseObject.getId()).isEqualTo(id);
            assertThat(putResponseObject.getText()).isEqualTo("without you, my Friend.");
            assertThat(putResponse.header("previous")).isEqualTo("It's been a long day");

            getResponse = makeHTTPRequest(createRequest("/notes/" + id, "GET", null,
                    ImmutableMap.<String, String>builder().put("char-case", "lowercase").build()));
            responseObject = getResponseObject(getResponse, NoteWithId.class);
            assertThat(getResponse.code()).isEqualTo(200);
            assertThat(responseObject.getId()).isEqualTo(id);
            assertThat(responseObject.getText()).isEqualTo("without you, my friend.");

            Response deleteResponse = makeHTTPRequest(createRequest("/notes/" + id, "DELETE"));
            assertThat(deleteResponse.code()).isEqualTo(204);

            Response getResponse2 = makeHTTPRequest(createRequest("/notes/" + id, "GET"));
            assertThat(getResponse2.code()).isEqualTo(404);

            deleteResponse = makeHTTPRequest(createRequest("/notes/" + id, "DELETE"));
            assertThat(deleteResponse.code()).isEqualTo(404);

            putResponse = makeHTTPRequest(createRequest("/notes/" + id, "PUT", new Note("")));
            assertThat(putResponse.code()).isEqualTo(404);

            Response lastResponse = makeHTTPRequest(createRequest("/notes", "POST", new Note("And I'll tell you all about it when I see you again")));
            String lastId = getResponseObject(lastResponse, String.class);
            assertThat(lastId).isNotEqualTo(id);
        });
    }

    class Note {
        final String text;

        public Note(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    class NoteWithId {
        final String text;
        final String id;

        public NoteWithId(String text, String id) {
            this.text = text;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }
    }
}
