package org.elsys.ip.tester.base;

import java.io.File;
import java.nio.file.Path;

public interface AssignmentGrader {
    float grade(Path path);
}
