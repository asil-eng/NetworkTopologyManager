package ntm.ui;

import ntm.dao.NetworkDAO;
import ntm.dao.NetworkDAOImpl;
import ntm.model.Network;
import ntm.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window after login: lists all saved networks and lets the user
 * create, edit, delete, search and open them.
 */
public class DashboardFrame extends JFrame {

    private final User user;
    private final NetworkDAO networkDAO = new NetworkDAOImpl();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Description", "Devices", "Created By", "Created At"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private List<Network> networks = new ArrayList<>();

    public DashboardFrame(User user) {
        super("Network Topology Manager - Dashboard");
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildHeader(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(70);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openNetwork();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Saved Networks (double-click to open)"));
        add(scroll, BorderLayout.CENTER);

        add(buildButtons(), BorderLayout.SOUTH);

        setSize(950, 560);
        setLocationRelativeTo(null);
        loadNetworks();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        JLabel welcome = new JLabel("Welcome, " + user.getUsername() + "  [" + user.getRole() + "]");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 16f));
        header.add(welcome, BorderLayout.WEST);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        search.add(new JLabel("Search:"));
        search.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear");
        JButton logoutButton = new JButton("Logout");
        searchButton.addActionListener(e -> loadNetworks());
        searchField.addActionListener(e -> loadNetworks());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadNetworks();
        });
        logoutButton.addActionListener(e -> logout());
        search.add(searchButton);
        search.add(clearButton);
        search.add(logoutButton);
        header.add(search, BorderLayout.EAST);
        return header;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 6));

        JButton openButton = new JButton("Open Network");
        JButton newButton = new JButton("New Network");
        JButton editButton = new JButton("Edit Details");
        JButton deleteButton = new JButton("Delete Network");
        JButton refreshButton = new JButton("Refresh");
        JButton usersButton = new JButton("Manage Users");

        openButton.addActionListener(e -> openNetwork());
        newButton.addActionListener(e -> createNetwork());
        editButton.addActionListener(e -> editNetwork());
        deleteButton.addActionListener(e -> deleteNetwork());
        refreshButton.addActionListener(e -> loadNetworks());
        usersButton.addActionListener(e -> new UsersDialog(this, user).setVisible(true));

        // Viewers can only look - editing buttons are disabled for them.
        newButton.setEnabled(user.canEditNetworks());
        editButton.setEnabled(user.canEditNetworks());
        deleteButton.setEnabled(user.canEditNetworks());

        buttons.add(openButton);
        buttons.add(newButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);
        if (user.canManageUsers()) {
            buttons.add(usersButton);
        }
        return buttons;
    }

    private void loadNetworks() {
        try {
            networks = networkDAO.search(searchField.getText());
            tableModel.setRowCount(0);
            for (Network n : networks) {
                tableModel.addRow(new Object[]{
                        n.getId(), n.getName(), n.getDescription() == null ? "" : n.getDescription(),
                        n.getDeviceCount(), n.getCreatedByName() == null ? "-" : n.getCreatedByName(),
                        n.getCreatedAt()
                });
            }
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }

    private Network getSelectedNetwork() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtil.showInfo(this, "Please select a network in the table first.");
            return null;
        }
        return networks.get(row);
    }

    private void openNetwork() {
        Network network = getSelectedNetwork();
        if (network == null) {
            return;
        }
        NetworkEditorFrame editor = new NetworkEditorFrame(user, network);
        editor.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                loadNetworks(); // device counts may have changed
            }
        });
        editor.setVisible(true);
    }

    /** Shows a small form with name + description. Returns false if cancelled. */
    private boolean showNetworkForm(String title, JTextField nameField, JTextField descriptionField) {
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Network name:"));
        form.add(nameField);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return false;
            }
            if (!nameField.getText().isBlank()) {
                return true;
            }
            UiUtil.showError(this, "Network name cannot be empty.");
        }
    }

    private void createNetwork() {
        JTextField nameField = new JTextField(25);
        JTextField descriptionField = new JTextField(25);
        if (!showNetworkForm("New Network", nameField, descriptionField)) {
            return;
        }
        try {
            Network network = new Network(0, nameField.getText(), descriptionField.getText().trim(), user.getId());
            networkDAO.save(network);
            loadNetworks();
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }

    private void editNetwork() {
        Network network = getSelectedNetwork();
        if (network == null) {
            return;
        }
        JTextField nameField = new JTextField(network.getName(), 25);
        JTextField descriptionField = new JTextField(network.getDescription(), 25);
        if (!showNetworkForm("Edit Network", nameField, descriptionField)) {
            return;
        }
        try {
            network.setName(nameField.getText());
            network.setDescription(descriptionField.getText().trim());
            networkDAO.update(network);
            loadNetworks();
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }

    private void deleteNetwork() {
        Network network = getSelectedNetwork();
        if (network == null) {
            return;
        }
        if (!UiUtil.confirm(this, "Delete \"" + network.getName() + "\"?\n"
                + "All of its devices, cables and packet logs will be deleted too.")) {
            return;
        }
        try {
            networkDAO.delete(network.getId());
            loadNetworks();
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }

    private void logout() {
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
        new LoginFrame().setVisible(true);
    }
}
