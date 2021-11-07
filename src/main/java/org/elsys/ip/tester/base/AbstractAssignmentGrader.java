package org.elsys.ip.tester.base;

import org.apache.commons.lang3.StringUtils;
import org.elsys.ip.tester.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public abstract class AbstractAssignmentGrader implements AssignmentGrader {
    private static final long timeout = 5 * 60L; //5min
    private static final String processOutputFile = "output.txt";
    private static final String processErrorFile = "error.txt";
    private static final String mvn;
    private static final String java;

    static {
        String osName = System.getProperty("os.name");
        boolean isWindows = osName.toLowerCase().startsWith("windows");
        if (isWindows) {
            mvn = "mvn.cmd";
            java = "java.exe";
        } else {
            mvn = "mvn";
            java = "java";
        }
    }

    private float grade = 0f;
    private final List<String> failedTests = new ArrayList<>();

    @Override
    public float grade(Path path, PrintWriter reportWriter) {
        try {
            gradeInternal(path);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            t.printStackTrace();
        } finally {
            if (!failedTests.isEmpty()) {
                reportWriter.println("<<<<<<<<<<< Failed tests:");
                failedTests.forEach(ft -> reportWriter.println(ft));
            }

            // Clear all started processes
            AsyncResult.allProcesses.forEach(p -> {
                try {
                    p.destroyForcibly();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
        return ((int) (grade * 100)) / 100.0f;
    }

    protected abstract void gradeInternal(Path path) throws Exception;

    protected void multiplyGrade(float multiplier) {
        grade *= multiplier;
    }

    protected Optional<Path> getTarget(Path path) {
        try {
            return Optional.of(path.resolve("target"));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    protected Optional<File> findSingleFile(Path path, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            return Files.walk(path).map(p -> p.toFile()).filter(f -> pattern.matcher(f.getName()).matches()).collect(StreamUtils.toSingleton());
        } catch (IOException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private Result process(Path path, int expectedErrorCode, String command, String[] args) {
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
                process.destroyForcibly();
                throw new RuntimeException(command + " didn't finish in " + timeout + " seconds.");
            }

            System.out.println("Process finished with exit code: " + process.exitValue());

            if (process.exitValue() != expectedErrorCode) {
                throw new RuntimeException(command + " didn't finish with exit code " + expectedErrorCode + ". Exit code: " + process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                result = new String(Files.readAllBytes(output.toPath())).trim();
                if (!result.isEmpty()) {
                    System.out.println("-----Result: " + result);
                    System.out.println("-----END OF RESULT");
                }

                errorResult = new String(Files.readAllBytes(error.toPath())).trim();
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

    private AsyncResult processAsync(Path path, String command, String[] args) {
        try {
            System.out.println("Executing: " + command + " " + StringUtils.join(args, " "));
            List<String> commandAndArgs = new ArrayList<>();
            commandAndArgs.add(command);
            Arrays.stream(args).forEach(e -> commandAndArgs.add(e));
            return new AsyncResult(
                    new ProcessBuilder(commandAndArgs).directory(path.toFile()).start());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Result mvn(Path path, String... args) {
        return process(path, 0, mvn, args);
    }

    protected Result java(Path path, String... args) {
        return process(path, 0, java, args);
    }

    protected Result java(Path path, int errorCode, String... args) {
        return process(path, errorCode, java, args);
    }

    protected AsyncResult javaAsync(Path path, String... args) {
        return processAsync(path, java, args);
    }

    protected <T> Optional<T> test(String name, float points, Supplier<T> block) {
        System.out.println("==================== BEGIN " + name + " =======================");
        try {
            T result = block.get();
            grade += points;
            System.out.println("======= Grade += " + points);
            return Optional.ofNullable(result);
        } catch (Throwable t) {
            failedTests.add(name);
            t.printStackTrace();
            System.out.println(t.getMessage());
            return Optional.empty();
        } finally {
            System.out.println("===================== END " + name + " ========================");
            System.out.println();
        }
    }

    protected void test(String name, float points, Runnable block) {
        test(name, points, () -> {
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

    protected void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
