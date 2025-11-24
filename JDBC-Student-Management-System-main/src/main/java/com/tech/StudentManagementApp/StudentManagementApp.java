package com.tech.StudentManagementApp;

import java.sql.*;
import java.util.Scanner;


public class StudentManagementApp {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/student_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "shaurya2508";

    private static Connection conn;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
            loop();
        } catch (SQLException e) {
            System.err.println("DB connection error: " + e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
            sc.close();
        }
    }

    private static void loop() {
        while (true) {
            System.out.println("\n1:Add Student  2:View Students  3:Update Student  4:Delete Student");
            System.out.println("5:Add Result   6:View Results   7:Update Result   8:Delete Result   9:Exit");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": addStudent(); break;
                case "2": viewStudents(); break;
                case "3": updateStudent(); break;
                case "4": deleteStudent(); break;
                case "5": addResult(); break;
                case "6": viewResults(); break;
                case "7": updateResult(); break;
                case "8": deleteResult(); break;
                case "9": return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    // Helper that executes INSERT/UPDATE/DELETE using PreparedStatement#setObject
    // returns number of affected rows or -1 on error
    
    private static int execUpdate(String sql, Object... params) {
        try (PreparedStatement p = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) p.setObject(i + 1, params[i]);
            return p.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            return -1;
        }
    }

    // Helper: check existence
    private static boolean exists(String sql, Object param) {
        try (PreparedStatement p = conn.prepareStatement(sql)) {
            p.setObject(1, param);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            return false;
        }
    }

    private static void addStudent() {
        System.out.print("Student ID: ");
        String id = sc.nextLine().trim();
        if (id.isEmpty()) { System.out.println("ID required"); return; }
        if (exists("SELECT 1 FROM students WHERE student_id = ?", id)) { System.out.println("Student ID already exists"); return; }

        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Course: ");
        String course = sc.nextLine().trim();
        if (name.isEmpty() || course.isEmpty()) { System.out.println("Name and Course required"); return; }

        execUpdate("INSERT INTO students(student_id, name, course) VALUES (?, ?, ?)", id, name, course);
        // no success message per your request
    }

    private static void viewStudents() {
        String sql = "SELECT student_id, name, course FROM students";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            boolean any = false;
            System.out.printf("%-15s %-20s %-15s%n", "ID", "Name", "Course");
            while (rs.next()) {
                any = true;
                System.out.printf("%-15s %-20s %-15s%n", rs.getString(1), rs.getString(2), rs.getString(3));
            }
            if (!any) System.out.println("No students");
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
    }

    private static void updateStudent() {
        System.out.print("Student ID to update: ");
        String id = sc.nextLine().trim();
        if (!exists("SELECT 1 FROM students WHERE student_id = ?", id)) { System.out.println("Student not found"); return; }

        System.out.print("New name (leave blank to keep): ");
        String name = sc.nextLine().trim();
        System.out.print("New course (leave blank to keep): ");
        String course = sc.nextLine().trim();

        if (name.isEmpty() || course.isEmpty()) {
            // fetch current values if blank
            String q = "SELECT name, course FROM students WHERE student_id = ?";
            try (PreparedStatement p = conn.prepareStatement(q)) {
                p.setObject(1, id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        if (name.isEmpty()) name = rs.getString(1);
                        if (course.isEmpty()) course = rs.getString(2);
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL Error: " + e.getMessage());
                return;
            }
        }

        execUpdate("UPDATE students SET name = ?, course = ? WHERE student_id = ?", name, course, id);
    }

    private static void deleteStudent() {
        System.out.print("Student ID to delete: ");
        String id = sc.nextLine().trim();
        if (!exists("SELECT 1 FROM students WHERE student_id = ?", id)) { System.out.println("Student not found"); return; }
        System.out.print("Confirm delete (y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) { System.out.println("Cancelled"); return; }

        execUpdate("DELETE FROM students WHERE student_id = ?", id);
        // ON DELETE CASCADE in DB should remove results if schema set
    }

    private static void addResult() {
        System.out.print("Student ID: ");
        String sid = sc.nextLine().trim();
        if (!exists("SELECT 1 FROM students WHERE student_id = ?", sid)) { System.out.println("Student must exist"); return; }
        if (exists("SELECT 1 FROM results WHERE student_id = ?", sid)) { System.out.println("Result already exists"); return; }

        Integer m1 = readMark("Marks1 (0-100): "); if (m1 == null) return;
        Integer m2 = readMark("Marks2 (0-100): "); if (m2 == null) return;
        Integer m3 = readMark("Marks3 (0-100): "); if (m3 == null) return;

        int total = m1 + m2 + m3;
        double perc = total / 3.0;
        String grade = gradeFor(perc);

        execUpdate("INSERT INTO results(student_id, marks1, marks2, marks3, total, percentage, grade) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sid, m1, m2, m3, total, perc, grade);
    }

    private static void viewResults() {
        String sql = "SELECT student_id, marks1, marks2, marks3, total, percentage, grade FROM results";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            boolean any = false;
            System.out.printf("%-12s %-4s %-4s %-4s %-5s %-10s %-5s%n", "StudentID","M1","M2","M3","Total","Percentage","Grade");
            while (rs.next()) {
                any = true;
                System.out.printf("%-12s %-4d %-4d %-4d %-5d %-10.2f %-5s%n",
                        rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getDouble(6), rs.getString(7));
            }
            if (!any) System.out.println("No results");
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
    }

    private static void updateResult() {
        System.out.print("Student ID for result update: ");
        String sid = sc.nextLine().trim();
        if (!exists("SELECT 1 FROM results WHERE student_id = ?", sid)) { System.out.println("Result not found"); return; }

        // fetch current marks
        int cur1=0, cur2=0, cur3=0;
        String q = "SELECT marks1, marks2, marks3 FROM results WHERE student_id = ?";
        try (PreparedStatement p = conn.prepareStatement(q)) {
            p.setObject(1, sid);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) { cur1 = rs.getInt(1); cur2 = rs.getInt(2); cur3 = rs.getInt(3); }
            }
        } catch (SQLException e) { System.out.println("SQL Error: " + e.getMessage()); return; }

        System.out.print("Marks1 (" + cur1 + "): ");
        String t = sc.nextLine().trim();
        Integer m1 = t.isEmpty() ? cur1 : parseMark(t); if (m1 == null) { System.out.println("Invalid mark"); return; }

        System.out.print("Marks2 (" + cur2 + "): ");
        t = sc.nextLine().trim();
        Integer m2 = t.isEmpty() ? cur2 : parseMark(t); if (m2 == null) { System.out.println("Invalid mark"); return; }

        System.out.print("Marks3 (" + cur3 + "): ");
        t = sc.nextLine().trim();
        Integer m3 = t.isEmpty() ? cur3 : parseMark(t); if (m3 == null) { System.out.println("Invalid mark"); return; }

        int total = m1 + m2 + m3;
        double perc = total / 3.0;
        String grade = gradeFor(perc);

        execUpdate("UPDATE results SET marks1=?, marks2=?, marks3=?, total=?, percentage=?, grade=? WHERE student_id=?",
                m1, m2, m3, total, perc, grade, sid);
    }

    private static void deleteResult() {
        System.out.print("Student ID whose result to delete: ");
        String sid = sc.nextLine().trim();
        if (!exists("SELECT 1 FROM results WHERE student_id = ?", sid)) { System.out.println("Result not found"); return; }
        System.out.print("Confirm delete (y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) { System.out.println("Cancelled"); return; }

        execUpdate("DELETE FROM results WHERE student_id = ?", sid);
    }

    // helpers
    private static Integer readMark(String prompt) {
        System.out.print(prompt);
        String t = sc.nextLine().trim();
        return parseMark(t);
    }

    private static Integer parseMark(String s) {
        try {
            int m = Integer.parseInt(s);
            if (m < 0 || m > 100) return null;
            return m;
        } catch (NumberFormatException e) { return null; }
    }

    private static String gradeFor(double p) {
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        return "F";
    }
}
