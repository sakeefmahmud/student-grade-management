import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

class StudentData {
    String id, name;
    int submission, exam;
    double attendance;

    StudentData(String id, String name, int sub, int exam, double att) {
        this.id = id;
        this.name = name;
        this.submission = sub;
        this.exam = exam;
        this.attendance = att;
    }

    int getAttendanceMark() {
        double perc = (attendance / 20) * 100;
        if (perc > 90) return 10;
        else if (perc > 80) return 9;
        else if (perc > 70) return 8;
        else if (perc > 60) return 7;
        else if (perc > 50) return 6;
        else return 0;
    }

    int getInternalMark() {
        return submission + getAttendanceMark();
    }

    int getTotalMark() {
        return getInternalMark() + exam;
    }
}

public class GradeAppSwing {
    static String DB_URL = "jdbc:sqlite:grades.db";

    public static void main(String[] args) {
        initDatabase();

        JFrame frame = new JFrame("Grade Sheet");
        frame.setSize(700, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField id = new JTextField(), name = new JTextField(),
                sub = new JTextField(), exam = new JTextField(), att = new JTextField(),
                searchId = new JTextField();
        JButton add = new JButton("Add Student");
        JButton saveTxt = new JButton("Save to TXT");
        JButton saveCsv = new JButton("Export to CSV");
        JButton searchBtn = new JButton("Search & Update");
        JTextArea output = new JTextArea();
        output.setEditable(false);

        panel.add(new JLabel("Student ID:")); panel.add(id);
        panel.add(new JLabel("Name:")); panel.add(name);
        panel.add(new JLabel("Tutorial Marks (0-20):")); panel.add(sub);
        panel.add(new JLabel("Exam Marks (0-70):")); panel.add(exam);
        panel.add(new JLabel("Attended Classes (0-20):")); panel.add(att);
        panel.add(add);
        panel.add(saveTxt);
        panel.add(saveCsv);
        panel.add(new JLabel("Search by Student ID:")); panel.add(searchId);
        panel.add(searchBtn);
        panel.add(new JScrollPane(output));

        add.addActionListener(_ -> {
            try {
                StudentData s = new StudentData(id.getText(), name.getText(),
                        Integer.parseInt(sub.getText()), Integer.parseInt(exam.getText()),
                        Double.parseDouble(att.getText()));
                addStudentToDB(s);
                output.append("Added: " + s.id + " - " + s.name + "\n");
                id.setText(""); name.setText(""); sub.setText(""); exam.setText(""); att.setText("");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Invalid input!");
            }
        });

        saveTxt.addActionListener(_ -> {
            ArrayList<StudentData> list = getAllStudents();
            try (FileWriter fw = new FileWriter("gradesheet.txt")) {
                fw.write(String.format("%-15s %-20s %-15s %-15s %-15s %-15s\n",
                        "Student ID", "Name", "Tutorial Marks", "Attendance Marks", "Internal Marks", "Total Marks"));
                for (StudentData s : list) {
                    fw.write(String.format("%-15s %-20s %-15d %-15d %-15d %-15d\n",
                            s.id, s.name, s.submission, s.getAttendanceMark(),
                            s.getInternalMark(), s.getTotalMark()));
                }
                JOptionPane.showMessageDialog(frame, "gradesheet.txt saved!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Failed to save TXT file.");
            }
        });

        saveCsv.addActionListener(_ -> {
            ArrayList<StudentData> list = getAllStudents();
            try (FileWriter fw = new FileWriter("gradesheet.csv")) {
                fw.write("Student ID,Name,Tutorial Marks,Attendance Marks,Internal Marks,Total Marks\n");
                for (StudentData s : list) {
                    fw.write(String.format("%s,%s,%d,%d,%d,%d\n",
                            s.id, s.name, s.submission, s.getAttendanceMark(),
                            s.getInternalMark(), s.getTotalMark()));
                }
                JOptionPane.showMessageDialog(frame, "gradesheet.csv saved!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Failed to save CSV file.");
            }
        });

        searchBtn.addActionListener(_ -> {
            String search = searchId.getText();
            ArrayList<StudentData> list = getAllStudents();
            for (StudentData s : list) {
                if (s.id.equals(search)) {
                    String newName = JOptionPane.showInputDialog("Name:", s.name);
                    int newSub = Integer.parseInt(JOptionPane.showInputDialog("Tutorial Marks:", s.submission));
                    int newExam = Integer.parseInt(JOptionPane.showInputDialog("Exam Marks:", s.exam));
                    double newAtt = Double.parseDouble(JOptionPane.showInputDialog("Attendance:", s.attendance));
                    s.name = newName; s.submission = newSub; s.exam = newExam; s.attendance = newAtt;
                    updateStudentInDB(s);
                    JOptionPane.showMessageDialog(frame, "Student updated!");
                    break;
                }
            }
        });

        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    static void initDatabase() {
        try (Connection con = DriverManager.getConnection(DB_URL);
             Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "submission INTEGER," +
                    "exam INTEGER," +
                    "attendance REAL)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void addStudentToDB(StudentData s) {
        String sql = "INSERT INTO students(id,name,submission,exam,attendance) VALUES(?,?,?,?,?)";
        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, s.id);
            pst.setString(2, s.name);
            pst.setInt(3, s.submission);
            pst.setInt(4, s.exam);
            pst.setDouble(5, s.attendance);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static ArrayList<StudentData> getAllStudents() {
        ArrayList<StudentData> list = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try (Connection con = DriverManager.getConnection(DB_URL);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new StudentData(rs.getString("id"), rs.getString("name"),
                        rs.getInt("submission"), rs.getInt("exam"), rs.getDouble("attendance")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    static void updateStudentInDB(StudentData s) {
        String sql = "UPDATE students SET name=?, submission=?, exam=?, attendance=? WHERE id=?";
        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, s.name);
            pst.setInt(2, s.submission);
            pst.setInt(3, s.exam);
            pst.setDouble(4, s.attendance);
            pst.setString(5, s.id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
