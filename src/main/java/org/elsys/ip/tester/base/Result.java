package org.elsys.ip.tester.base;

public class Result {
    private final String output;
    private final String error;

    public Result(String output, String error) {
        this.output = output;
        this.error = error;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
