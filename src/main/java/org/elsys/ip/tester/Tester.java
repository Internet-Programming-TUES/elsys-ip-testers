package org.elsys.ip.tester;

import com.google.common.base.Supplier;
import org.elsys.ip.tester.assignments.*;
import org.elsys.ip.tester.base.AssignmentGrader;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Tester {
    private static final Map<String, Supplier<AssignmentGrader>> GRADERS = new HashMap<String, Supplier<AssignmentGrader>>() {{
        put("maven", GraderMaven::new);
        put("sockets001", GraderSockets001::new);
        put("sockets002", GraderSockets002::new);
        put("servlet", GraderServlet::new);
        put("springWeb", GraderSpringWeb::new);
        put("springExam", GraderSpringExam::new);
    }};

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Provide two arguments.");
            System.exit(1);
        }

        String homework = args[0];
        File file = new File(args[1]);
        if (!file.exists()) {
            System.out.println("Provided path is not valid.");
            System.exit(1);
        }

        AssignmentGrader grader = GRADERS.get(homework).get();
        if (grader == null) {
            System.out.println(homework + " doesn't exists.");
            System.exit(1);
        }

        float grade = grader.grade(file.toPath(), new PrintWriter(System.out, true));

        System.out.println("-----------------------------------------");
        System.out.println("Your grade is " + grade);
        System.out.println("-----------------------------------------");
    }
}
