package org.elsys.ip.tester;

import org.elsys.ip.tester.assignments.GraderMaven;
import org.elsys.ip.tester.assignments.GraderSockets001;

import java.io.File;

public class Tester {
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

        float grade = 0f;

        if (homework.equals("maven")) {
            grade = new GraderMaven().grade(file.toPath());
        }
        else if (homework.equals("sockets001")) {
            grade = new GraderSockets001().grade(file.toPath());
        }
        else {
            System.out.println(homework + " doesn't exists.");
            System.exit(1);
        }
        System.out.println("-----------------------------------------");
        System.out.println("Your grade is " + grade);
        System.out.println("-----------------------------------------");
    }
}
