package ntm.ui;

import ntm.dao.UserDAO;
import ntm.dao.UserDAOImpl;
import ntm.model.User;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin-only window for adding and removing login accounts.
 */
public class UsersDialog extends JDialog {

    private final User currentUser;
    private final UserDAO userDAO = new UserDAOImpl();
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"ID", "Username", "Role"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<User> users = new ArrayList<>();

    public UsersDialog(JFrame owner, User currentUser) {
        super(owner, "Manage Users", true);
        this.currentUser = currentUser;
        setLayout(new BorderLayout(6, 6));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add User");
        JButton deleteButton = new JButton("Delete User");
        JButton closeButton = new JButton("Close");
        addButton.addActionListener(e -> addUser());
        deleteButton.addActionListener(e -> deleteUser());
        closeButton.addActionListener(e -> dispose());
        buttons.add(addButton);
        buttons.add(deleteButton);
        buttons.add(closeButton);
        add(buttons, BorderLayout.SOUTH);

        setSize(450, 320);
        setLocationRelativeTo(owner);
        loadUsers();
    }

    private void loadUsers() {
        try {
            users = userDAO.findAll();
            tableModel.setRowCount(0);
            for (User u : users) {
                tableModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getRole()});
            }
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }

    private void addUser() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"VIEWER", "ADMIN"});

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);
        form.add(new JLabel("Role:"));
        form.add(roleBox);

        while (JOptionPane.showConfirmDialog(this, form, "Add User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty()) {
                UiUtil.showError(this, "Username cannot be empty.");
                continue;
            }
            if (password.length() < 4) {
                UiUtil.showError(this, "Password must be at least 4 characters.");
                continue;
            }
            try {
                userDAO.create(username, password, (String) roleBox.getSelectedItem());
                loadUsers();
                return;
            } catch (SQLIntegrityConstraintViolationException ex) {
                UiUtil.showError(this, "The username \"" + username + "\" is already taken.");
            } catch (SQLException ex) {
                UiUtil.showDatabaseError(this, ex);
                return;
            }
        }
    }

    private void deleteUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtil.showInfo(this, "Select a user first.");
            return;
        }
        User selected = users.get(row);
        if (selected.getId() == currentUser.getId()) {
            UiUtil.showError(this, "You cannot delete the account you are logged in with.");
            return;
        }
        if (!UiUtil.confirm(this, "Delete user \"" + selected.getUsername() + "\"?")) {
            return;
        }
        try {
            userDAO.delete(selected.getId());
            loadUsers();
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }
}
