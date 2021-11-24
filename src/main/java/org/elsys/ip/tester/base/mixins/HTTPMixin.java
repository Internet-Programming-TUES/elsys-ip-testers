package org.elsys.ip.tester.base.mixins;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

public interface HTTPMixin {
    OkHttpClient client = new OkHttpClient();
    String baseURL = "http://localhost";
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    Gson gson = new Gson();

    String getBasePath();

    int getPort();

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
        return createRequest(path, method, null);
    }

    default Request createRequest(String path, String method, Object body) {
        return createRequest(path, method, body, null);
    }

    default Request createRequest(String path, String method, Object body, Map<String, String> headers) {
        RequestBody requestBody = body == null ? createEmptyRequestBody(method) : RequestBody.create(gson.toJson(body), JSON);
        try {
            Request.Builder builder = new Request.Builder().url(URI.create(baseURL + ":" + getPort() + "/" + getBasePath() + path).normalize().toURL().toString()).method(method, requestBody);
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    builder.addHeader(header.getKey(), header.getValue());
                }
            }

            return builder.build();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
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

    default <T> T getResponseObject(Response response, Class<T> clazz) {
        return gson.fromJson(getResponseText(response), clazz);
    }
}
