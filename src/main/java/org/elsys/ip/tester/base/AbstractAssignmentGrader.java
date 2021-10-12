package org.elsys.ip.tester.base;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractAssignmentGrader implements AssignmentGrader {
    private static final long timeout = 1 * 60L; //1min
    private static final String processOutputFile = "output.txt";
    private static final String processErrorFile = "error.txt";

    private float grade = 0f;

    @Override
    public float grade(Path path) {
        try {
            gradeInternal(path);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            t.printStackTrace();
        }
        return grade;
    }

    protected abstract void gradeInternal(Path path) throws Exception;

    protected Optional<Path> getTarget(Path path) {
        try {
            return Optional.of(path.resolve("target"));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    protected Optional<File> findFile(Path path, String extension) {
        return Arrays.stream(path.toFile().listFiles()).filter(p -> p.getName().endsWith(extension)).findFirst();
    }

    private Result process(Path path, String command, String[] args) {
        File output = new File(processOutputFile);
        File error = new File(processErrorFile);
        if (output.exists()) output.delete();
        if (error.exists()) error.delete();
        String result = null;
        String errorResult = null;
        try {
            output.createNewFile();
            error.createNewFile();
            System.out.println("Executing: " + command + " " + StringUtils.join(args, " "));
            List<String> commandAndArgs = new ArrayList<>();
            commandAndArgs.add(command);
            Arrays.stream(args).forEach(e -> commandAndArgs.add(e));
            Process process = new ProcessBuilder(commandAndArgs).directory(path.toFile()).redirectOutput(output).redirectError(error).start();

            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                throw new RuntimeException(command + " didn't finish in " + timeout + " seconds.");
            }

            System.out.println("Process finished with exit code: " + process.exitValue());

            if (process.exitValue() != 0) {
                throw new RuntimeException("$command didn't finish with exit code 0. Exit code: " + process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                result = new String(Files.readAllBytes(output.toPath()));
                System.out.println("-----Result: " + result);
                System.out.println("-----END OF RESULT");

                errorResult = new String(Files.readAllBytes(error.toPath()));
                if (!errorResult.isEmpty()) {
                    System.out.println("-----Error: " + errorResult);
                    System.out.println("-----END OF ERROR");
                }

                output.delete();
                error.delete();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        return new Result(result, errorResult);
    }

    protected Result mvn(Path path, String... args) {
        return process(path, "mvn", args);
    }

    protected Result java(Path path, String... args) {
        return process(path, "java", args);
    }

    protected <T> Optional<T> test(float points, Supplier<T> block) {
        System.out.println("==================== BEGIN TEST =======================");
        try {
            T result = block.get();
            grade += points;
            return Optional.ofNullable(result);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            return Optional.empty();
        } finally {
            System.out.println("===================== END TEST ========================");
        }
    }

    protected void test(float points, Runnable block) {
        test(points, () -> {
            block.run();
            return null;
        });
    }

    protected <T> T requireNotNull(Optional<T> optional) {
        if (!optional.isPresent()) {
            throw new RuntimeException("Value required.");
        }

        return optional.get();
    }
}
