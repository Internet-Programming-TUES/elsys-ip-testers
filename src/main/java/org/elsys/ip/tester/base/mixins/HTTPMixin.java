package org.elsys.ip.tester.base.mixins;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

public interface HTTPMixin {

    OkHttpClient client = new OkHttpClient();
    String baseURL = "http://localhost:8080/stopwatch";

    default boolean isPortInUse(int port) {
        try {
            new ServerSocket(port).close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    default RequestBody createEmptyRequestBody(String method) {
        switch (method) {
            case "POST":
            case "PUT":
                return RequestBody.create(null, new byte[]{});
            case "GET":
            case "DELETE":
            default:
                return null;
        }
    }

    default Request createRequest(String path, String method) {
        return new Request.Builder().url(baseURL + path).method(method, createEmptyRequestBody(method)).build();
    }

    default Response makeHTTPRequest(Request request) {
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException("Client error", e);
        }
    }

    default String getResponseText(Response response) {
        try {
            return Objects.requireNonNull(response.body()).string().trim();
        } catch (IOException e) {
            throw new RuntimeException("Cannot extract text from response", e);
        }
    }
}
