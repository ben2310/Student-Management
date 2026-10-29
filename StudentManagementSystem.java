import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.Comparator;

public class StudentManagementSystem extends JFrame {
    private final ArrayList<Student> students = new ArrayList<>();
    private final Stack<Student> undoStack = new Stack<>();
    private final Queue<Student> registrationQueue = new LinkedList<>();

    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(12);
    private final JTextField courseField = new JTextField(10);
    private final JTextField semesterField = new JTextField(4);
    private final JTextField marksField = new JTextField(5);
    private final JTextField searchField = new JTextField(10);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Course", "Semester", "Marks"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel("Total students: 0");
    private final JLabel queueLabel = new JLabel("Queue: 0");

    public StudentManagementSystem() {
        setTitle("Student Management System");
        setSize(900, 600);
        setMinimumSize(new Dimension(750, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(245, 247, 251));
        setContentPane(root);

        JLabel title = new JLabel("STUDENT MANAGEMENT SYSTEM");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(30, 60, 120));
        root.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        root.add(center, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Student Details"));
        form.setBackground(Color.WHITE);
        addFormRow(form, "Student ID", idField);
        addFormRow(form, "Full Name", nameField);
        addFormRow(form, "Course", courseField);
        addFormRow(form, "Semester", semesterField);
        addFormRow(form, "Marks (0-100)", marksField);

        JPanel actions = new JPanel(new GridLayout(0, 2, 7, 7));
        actions.setBackground(Color.WHITE);
        actions.setBorder(BorderFactory.createTitledBorder("Actions"));
        addButton(actions, "Add Student", this::addStudent);
        addButton(actions, "Update Selected", this::updateStudent);
        addButton(actions, "Delete Selected", this::deleteStudent);
        addButton(actions, "Clear Fields", this::clearFields);
        addButton(actions, "Undo Last Add (Stack)", this::undoLastAdd);
        addButton(actions, "Join Queue", this::joinQueue);
        addButton(actions, "Process Queue (FIFO)", this::processQueue);
        addButton(actions, "Sort by Name", this::sortByName);
        addButton(actions, "Sort by Marks", this::sortByMarks);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setOpaque(false);
        left.add(form, BorderLayout.NORTH);
        left.add(actions, BorderLayout.CENTER);
        center.add(left, BorderLayout.WEST);

        JPanel records = new JPanel(new BorderLayout(8, 8));
        records.setBackground(Color.WHITE);
        records.setBorder(BorderFactory.createTitledBorder("Student Records"));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel("Search ID/name:"));
        searchPanel.add(searchField);
        addButton(searchPanel, "Search", this::searchStudent);
        addButton(searchPanel, "Show All", this::refreshTable);
        records.add(searchPanel, BorderLayout.NORTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedStudent();
        });
        records.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(records, BorderLayout.CENTER);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        status.setOpaque(false);
        status.add(countLabel);
        status.add(queueLabel);
        root.add(status, BorderLayout.SOUTH);
    }

    private void addFormRow(JPanel panel, String label, JTextField field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void addButton(JPanel panel, String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    private Student readForm() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String course = courseField.getText().trim();
        if (id.isEmpty() || name.isEmpty() || course.isEmpty()
                || semesterField.getText().trim().isEmpty()
                || marksField.getText().trim().isEmpty()) {
            message("Please fill in all fields.");
            return null;
        }
        try {
            int semester = Integer.parseInt(semesterField.getText().trim());
            double marks = Double.parseDouble(marksField.getText().trim());
            if (semester < 1 || semester > 12 || marks < 0 || marks > 100) {
                message("Semester must be 1-12 and marks must be 0-100.");
                return null;
            }
            return new Student(id, name, course, semester, marks);
        } catch (NumberFormatException ex) {
            message("Semester must be a whole number and marks must be numeric.");
            return null;
        }
    }

    private void addStudent() {
        Student s = readForm();
        if (s == null) return;
        if (findById(s.id) != null) {
            message("Student ID already exists.");
            return;
        }
        students.add(s);
        undoStack.push(s);
        refreshTable();
        clearFields();
        message("Student added successfully.");
    }

    private void updateStudent() {
        int row = table.getSelectedRow();
        if (row < 0) { message("Select a student first."); return; }
        Student updated = readForm();
        if (updated == null) return;
        int modelRow = table.convertRowIndexToModel(row);
        String oldId = String.valueOf(model.getValueAt(modelRow, 0));
        Student duplicate = findById(updated.id);
        if (duplicate != null && !oldId.equalsIgnoreCase(updated.id)) {
            message("That student ID is already in use.");
            return;
        }
        Student old = findById(oldId);
        int index = students.indexOf(old);
        if (index >= 0) students.set(index, updated);
        refreshTable();
        message("Student updated.");
    }

    private void deleteStudent() {
        int row = table.getSelectedRow();
        if (row < 0) { message("Select a student first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        String id = String.valueOf(model.getValueAt(modelRow, 0));
        students.removeIf(s -> s.id.equalsIgnoreCase(id));
        refreshTable();
        clearFields();
        message("Student deleted.");
    }

    private void undoLastAdd() {
        if (undoStack.empty()) { message("Undo stack is empty."); return; }
        Student last = undoStack.pop();
        if (students.remove(last)) message("Undo: removed " + last.name);
        else message("That student was already removed; stack updated.");
        refreshTable();
    }

    private void joinQueue() {
        Student s = readForm();
        if (s == null) return;
        registrationQueue.offer(s);
        updateCounts();
        message(s.name + " joined the registration queue.");
    }

    private void processQueue() {
        Student next = registrationQueue.poll();
        if (next == null) { message("Registration queue is empty."); return; }
        updateCounts();
        message("Processed next student: " + next.name + " (FIFO queue).");
    }

    private void searchStudent() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) { message("Enter an ID or name to search."); return; }
        model.setRowCount(0);
        for (Student s : students) {
            if (s.id.toLowerCase().contains(query) || s.name.toLowerCase().contains(query))
                addRow(s);
        }
    }

    private void sortByName() {
        students.sort(Comparator.comparing(s -> s.name.toLowerCase()));
        refreshTable();
    }

    private void sortByMarks() {
        students.sort(Comparator.comparingDouble(s -> s.marks));
        refreshTable();
    }

    private void loadSelectedStudent() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int r = table.convertRowIndexToModel(row);
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        nameField.setText(String.valueOf(model.getValueAt(r, 1)));
        courseField.setText(String.valueOf(model.getValueAt(r, 2)));
        semesterField.setText(String.valueOf(model.getValueAt(r, 3)));
        marksField.setText(String.valueOf(model.getValueAt(r, 4)));
    }

    private Student findById(String id) {
        for (Student s : students)
            if (s.id.equalsIgnoreCase(id)) return s;
        return null;
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (Student s : students) addRow(s);
        updateCounts();
    }

    private void addRow(Student s) {
        model.addRow(new Object[]{s.id, s.name, s.course, s.semester, s.marks});
    }

    private void updateCounts() {
        countLabel.setText("Total students: " + students.size());
        queueLabel.setText("Queue: " + registrationQueue.size());
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        courseField.setText("");
        semesterField.setText("");
        marksField.setText("");
        table.clearSelection();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text, "Student Management", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class Student {
        String id, name, course;
        int semester;
        double marks;
        Student(String id, String name, String course, int semester, double marks) {
            this.id = id; this.name = name; this.course = course;
            this.semester = semester; this.marks = marks;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new StudentManagementSystem().setVisible(true);
        });
    }
}
