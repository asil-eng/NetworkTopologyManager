package ntm.ui;

import ntm.dao.PacketLogDAO;
import ntm.dao.PacketLogDAOImpl;
import ntm.model.Network;
import ntm.model.PacketLog;
import ntm.model.User;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;

/**
 * Shows the history of every packet sent in this network, read from the packet_logs table.
 */
public class PacketLogDialog extends JDialog {

    private final Network network;
    private final PacketLogDAO packetLogDAO = new PacketLogDAOImpl();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Time", "From", "To", "Algorithm", "Route", "Hops", "Latency (ms)", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public PacketLogDialog(JFrame owner, Network network, User user) {
        super(owner, "Packet Log - " + network.getName(), true);
        this.network = network;
        setLayout(new BorderLayout());

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Log");
        clearButton.setEnabled(user.canEditNetworks());
        clearButton.addActionListener(e -> clearLog());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttons.add(clearButton);
        buttons.add(closeButton);
        add(buttons, BorderLayout.SOUTH);

        setSize(950, 420);
        setLocationRelativeTo(owner);
        loadLogs();
    }

    private void loadLogs() {
        try {
            tableModel.setRowCount(0);
            for (PacketLog log : packetLogDAO.findByNetwork(network.getId())) {
                tableModel.addRow(new Object[]{
                        log.getSentAt(),
                        log.getSourceName() == null ? "(deleted)" : log.getSourceName(),
                        log.getDestinationName() == null ? "(deleted)" : log.getDestinationName(),
                        log.getAlgorithm(), log.getPath(), log.getHops(), log.getTotalLatency(), log.getStatus()
                });
            }
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }

    private void clearLog() {
        if (!UiUtil.confirm(this, "Delete the whole packet history for this network?")) {
            return;
        }
        try {
            packetLogDAO.deleteByNetwork(network.getId());
            loadLogs();
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
    }
}
