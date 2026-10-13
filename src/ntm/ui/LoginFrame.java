package ntm.ui;

import ntm.dao.UserDAO;
import ntm.dao.UserDAOImpl;
import ntm.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

/**
 * The first window: checks the username and password against the users table.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final UserDAO userDAO = new UserDAOImpl();

    public LoginFrame() {
        super("Network Topology Manager - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Network Topology Manager", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        panel.add(title, gc);

        JLabel subtitle = new JLabel("Design, manage and simulate computer networks", SwingConstants.CENTER);
        subtitle.setForeground(Color.GRAY);
        gc.gridy = 1;
        panel.add(subtitle, gc);

        gc.gridwidth = 1;
        gc.gridy = 2;
        panel.add(new JLabel("Username:"), gc);
        gc.gridx = 1;
        panel.add(usernameField, gc);

        gc.gridx = 0;
        gc.gridy = 3;
        panel.add(new JLabel("Password:"), gc);
        gc.gridx = 1;
        panel.add(passwordField, gc);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        gc.gridx = 0;
        gc.gridy = 4;
        gc.gridwidth = 2;
        panel.add(loginButton, gc);

        JLabel hint = new JLabel("Demo accounts: admin / admin123   or   viewer / viewer123", SwingConstants.CENTER);
        hint.setForeground(Color.GRAY);
        hint.setFont(hint.getFont().deriveFont(11f));
        gc.gridy = 5;
        panel.add(hint, gc);

        getRootPane().setDefaultButton(loginButton); // Enter key presses Login
        setContentPane(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            UiUtil.showError(this, "Please enter both username and password.");
            return;
        }
        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                UiUtil.showError(this, "Wrong username or password.");
                passwordField.setText("");
                return;
            }
            new DashboardFrame(user).setVisible(true);
            dispose();
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }
}
