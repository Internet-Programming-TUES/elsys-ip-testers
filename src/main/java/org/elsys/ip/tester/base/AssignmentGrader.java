package org.elsys.ip.tester.base;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;

public interface AssignmentGrader {
    float grade(Path path, PrintWriter reportWriter);
}
