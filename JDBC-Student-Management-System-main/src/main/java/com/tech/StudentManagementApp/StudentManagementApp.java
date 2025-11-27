package com.tech.StudentManagementApp;

import java.sql.*;
import java.util.Scanner;

public class StudentManagementApp {
    static final String JDBC_URL = "jdbc:mysql://localhost:3306/student_db";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "root";

    static Connection conn;
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);

            while (true) {
                System.out.println("\n1:Add Student  2:View Students  3:Update Student  4:Delete Student");
                System.out.println("5:Add Result   6:View Results   7:Update Result   8:Delete Result   9:Exit");
                System.out.print("Choice: ");
                String c = sc.nextLine();

                try {
                    if (c.equals("1")) addStudent();
                    else if (c.equals("2")) viewStudents();
                    else if (c.equals("3")) updateStudent();
                    else if (c.equals("4")) deleteStudent();
                    else if (c.equals("5")) addResult();
                    else if (c.equals("6")) viewResults();
                    else if (c.equals("7")) updateResult();
                    else if (c.equals("8")) deleteResult();
                    else if (c.equals("9")) break;
                    else System.out.println("Invalid choice");
                }
                catch (SQLException e) {
                    System.out.println("SQL Error: " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.out.println("DB connection error: " + e.getMessage());
        }
        finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
            sc.close();
            System.out.println("Exit");
        }
    }


    static boolean existsStudent(String id) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE student_id = '" + id + "'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }


    static boolean existsResult(String id) throws SQLException {
        String sql = "SELECT 1 FROM results WHERE student_id = '" + id + "'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }


    static Integer parseMark(String s) {
        if (!s.matches("\\d+")) return null;
        int m = Integer.parseInt(s);
        if (m < 0 || m > 100) return null;
        return m;
    }

    static String gradeFor(double p) {
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        return "F";
    }


    static void addStudent() throws SQLException {
        System.out.print("Student ID: ");
        String id = sc.nextLine();
        if (id.length() == 0) { System.out.println("ID required"); return; }
        if (existsStudent(id)) { System.out.println("Student ID already exists"); return; }

        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Course: ");
        String course = sc.nextLine();

        if (name.length() == 0 || course.length() == 0) { System.out.println("Name and Course required"); return; }

        String sql = "INSERT INTO students(student_id, name, course) VALUES ('" 
                     + id + "', '" + name + "', '" + course + "')";

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }


    static void viewStudents() throws SQLException {
        String sql = "SELECT student_id, name, course FROM students";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            boolean any = false;
            System.out.printf("%-15s %-20s %-15s%n", "ID", "Name", "Course");
            while (rs.next()) {
                any = true;
                System.out.printf("%-15s %-20s %-15s%n",
                        rs.getString(1), rs.getString(2), rs.getString(3));
            }
            if (!any) System.out.println("No students");
        }
    }

    
    static void updateStudent() throws SQLException {
        System.out.print("Student ID to update: ");
        String id = sc.nextLine();
        if (!existsStudent(id)) { System.out.println("Student not found"); return; }

        System.out.print("New Name (leave blank to keep): ");
        String name = sc.nextLine();
        System.out.print("New Course (leave blank to keep): ");
        String course = sc.nextLine();

        if (name.length() == 0 || course.length() == 0) {
            String q = "SELECT name, course FROM students WHERE student_id = '" + id + "'";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
                if (rs.next()) {
                    if (name.length() == 0) name = rs.getString(1);
                    if (course.length() == 0) course = rs.getString(2);
                }
            }
        }

        String sql = "UPDATE students SET name = '" + name + "', course = '" + course +
                     "' WHERE student_id = '" + id + "'";

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }


    static void deleteStudent() throws SQLException {
        System.out.print("Student ID to delete: ");
        String id = sc.nextLine();
        if (!existsStudent(id)) { System.out.println("Student not found"); return; }
        System.out.print("Confirm delete (y/N): ");
        String a = sc.nextLine();
        if (!a.equalsIgnoreCase("y")) { System.out.println("Cancelled"); return; }

        String sql = "DELETE FROM students WHERE student_id = '" + id + "'";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }


    static void addResult() throws SQLException {
        System.out.print("Student ID: ");
        String sid = sc.nextLine();

        if (!existsStudent(sid)) { System.out.println("Student must exist"); return; }
        if (existsResult(sid)) { System.out.println("Result already exists"); return; }

        System.out.print("Marks1: ");
        Integer m1 = parseMark(sc.nextLine());
        if (m1 == null) { System.out.println("Invalid mark"); return; }

        System.out.print("Marks2: ");
        Integer m2 = parseMark(sc.nextLine());
        if (m2 == null) { System.out.println("Invalid mark"); return; }

        System.out.print("Marks3: ");
        Integer m3 = parseMark(sc.nextLine());
        if (m3 == null) { System.out.println("Invalid mark"); return; }

        int total = m1 + m2 + m3;
        double perc = total / 3.0;
        String grade = gradeFor(perc);

        String sql = "INSERT INTO results(student_id, marks1, marks2, marks3, total, percentage, grade) VALUES ('"
                + sid + "', " + m1 + ", " + m2 + ", " + m3 + ", " + total + ", " + perc + ", '" + grade + "')";

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }


    static void viewResults() throws SQLException {
        String sql = "SELECT student_id, marks1, marks2, marks3, total, percentage, grade FROM results";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            boolean any = false;
            System.out.printf("%-12s %-4s %-4s %-4s %-6s %-10s %-5s%n",
                    "StudentID","M1","M2","M3","Total","Percentage","Grade");

            while (rs.next()) {
                any = true;
                System.out.printf("%-12s %-4d %-4d %-4d %-6d %-10.2f %-5s%n",
                        rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getDouble(6), rs.getString(7));
            }

            if (!any) System.out.println("No results");
        }
    }


    static void updateResult() throws SQLException {
        System.out.print("Student ID for result update: ");
        String sid = sc.nextLine();
        if (!existsResult(sid)) { System.out.println("Result not found"); return; }

        int cur1 = 0, cur2 = 0, cur3 = 0;
        String q = "SELECT marks1, marks2, marks3 FROM results WHERE student_id = '" + sid + "'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            if (rs.next()) {
                cur1 = rs.getInt(1);
                cur2 = rs.getInt(2);
                cur3 = rs.getInt(3);
            }
        }

        System.out.print("Marks1 (" + cur1 + "): ");
        String t = sc.nextLine();
        Integer m1 = t.length() == 0 ? cur1 : parseMark(t);
        if (m1 == null) { System.out.println("Invalid"); return; }

        System.out.print("Marks2 (" + cur2 + "): ");
        t = sc.nextLine();
        Integer m2 = t.length() == 0 ? cur2 : parseMark(t);
        if (m2 == null) { System.out.println("Invalid"); return; }

        System.out.print("Marks3 (" + cur3 + "): ");
        t = sc.nextLine();
        Integer m3 = t.length() == 0 ? cur3 : parseMark(t);
        if (m3 == null) { System.out.println("Invalid"); return; }

        int total = m1 + m2 + m3;
        double perc = total / 3.0;
        String grade = gradeFor(perc);

        String sql = "UPDATE results SET marks1 = " + m1 + ", marks2 = " + m2 + ", marks3 = " + m3 +
                ", total = " + total + ", percentage = " + perc + ", grade = '" + grade +
                "' WHERE student_id = '" + sid + "'";

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }


    static void deleteResult() throws SQLException {
        System.out.print("Student ID whose result to delete: ");
        String sid = sc.nextLine();
        if (!existsResult(sid)) { System.out.println("Result not found"); return; }

        System.out.print("Confirm delete (y/N): ");
        String a = sc.nextLine();
        if (!a.equalsIgnoreCase("y")) { System.out.println("Cancelled"); return; }

        String sql = "DELETE FROM results WHERE student_id = '" + sid + "'";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }
}
