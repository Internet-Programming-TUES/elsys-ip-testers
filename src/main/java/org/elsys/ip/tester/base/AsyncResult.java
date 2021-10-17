package org.elsys.ip.tester.base;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AsyncResult {
    private final Process process;
    private final PrintWriter inputStream;
    private final BufferedReader outputStream;

    public static List<Process> allProcesses = new ArrayList<>();

    public AsyncResult(Process process) {
        this.process = process;
        this.inputStream = new PrintWriter(process.getOutputStream(), true);
        this.outputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));

        allProcesses.add(process);
    }

    public Process getProcess() {
        return process;
    }

    public void kill() {
        if (process.isAlive()) process.destroyForcibly();
        if (inputStream != null) inputStream.close();
    }

    public void println(String line) {
        System.out.println("> " + line);
        inputStream.println(line);
    }

    public boolean canRead() {
        try {
            return outputStream.ready();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return true;
        }
    }

    public String readLine() {
        try {
            int counter = 0;
            while (!outputStream.ready()) {
                if (counter > 50) {
                    return null;
                }
                Thread.sleep(100);
                counter += 1;
            }

            String result = outputStream.readLine();
            System.out.println("< " + result);
            return result;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
