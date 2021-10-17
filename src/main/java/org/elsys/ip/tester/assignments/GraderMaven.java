package org.elsys.ip.tester.assignments;

import org.elsys.ip.tester.base.AbstractAssignmentGrader;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class GraderMaven extends AbstractAssignmentGrader {

    @Override
    protected void gradeInternal(Path path) {
        Path target = requireNotNull(test("mvn clean package", 0.1f, () -> {
            mvn(path, "clean", "package");
            return getTarget(path).get();
        }));

        File jarFile = requireNotNull(test("find jar file", 0.1f, () -> findSingleFile(target, ".*\\.jar").get()));

        test("test 1", 0.2f, () -> {
            String result = java(target, "-jar", jarFile.getName(), "2", "88", "4", "5", "six", "7").getOutput();
            assertThat(result.trim().split("\\r?\\n")).containsExactly(
                    "2 is a prime",
                    "88 is not a prime",
                    "4 is not a prime",
                    "5 is a prime",
                    "six is not a number",
                    "7 is a prime");
        });

        test("test 2", 0.2f, () -> {
            String result = java(target, "-jar", jarFile.getName(), "2.0", "111111111111111111111111111").getOutput();
            assertThat(result.trim().split("\\r?\\n")).containsExactly(
                    "2.0 is not an integer",
                    "111111111111111111111111111 is out of bound");
        });

        test("test 3", 0.2f, () -> {
            String result = java(target, "-jar", jarFile.getName(), "-1", "-2", "-3", "0").getOutput();
            assertThat(result.trim().split("\\r?\\n")).containsExactly(
                    "-1 is not a prime",
                    "-2 is not a prime",
                    "-3 is not a prime",
                    "0 is not a prime");
        });

        test("test 4", 0.2f, () -> {
            String result = java(target, "-jar", jarFile.getName()).getOutput();
            assertThat(result.trim()).isEmpty();
        });
    }
}
