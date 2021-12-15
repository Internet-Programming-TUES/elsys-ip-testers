package org.elsys.ip.tester.assignments;

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

    }
}
