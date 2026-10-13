package ntm.ui;

import ntm.dao.ReportDAO;
import ntm.model.Network;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Statistics about one network, calculated by SQL queries in ReportDAO.
 */
public class ReportsDialog extends JDialog {

    public ReportsDialog(JFrame owner, Network network) {
        super(owner, "Reports - " + network.getName(), true);
        ReportDAO reportDAO = new ReportDAO();
        JTabbedPane tabs = new JTabbedPane();

        try {
            tabs.addTab("Devices by Type", new BarChartPanel(reportDAO.countDevicesByType(network.getId())));
            tabs.addTab("Most Connected Devices", buildTable(
                    new String[]{"Device", "Type", "IP Address", "Cables"},
                    reportDAO.mostConnectedDevices(network.getId(), 10)));
            tabs.addTab("Packet Statistics", buildStatsPanel(reportDAO.packetStatistics(network.getId())));
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(owner, ex);
        }

        add(tabs, BorderLayout.CENTER);
        setSize(640, 430);
        setLocationRelativeTo(owner);
    }

    private JScrollPane buildTable(String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Object[] row : rows) {
            model.addRow(row);
        }
        JTable table = new JTable(model);
        table.setRowHeight(22);
        return new JScrollPane(table);
    }

    private JPanel buildStatsPanel(Map<String, String> stats) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            JLabel label = new JLabel(entry.getKey() + ":");
            JLabel value = new JLabel(entry.getValue());
            value.setFont(value.getFont().deriveFont(Font.BOLD, 16f));
            panel.add(label);
            panel.add(value);
        }
        return panel;
    }

    /** A simple bar chart drawn with Java 2D - no external chart library needed. */
    private static class BarChartPanel extends JPanel {

        // Same colours as Router, Switch, PC, Server on the canvas.
        private static final Color[] COLORS = {
                new Color(25, 118, 210), new Color(0, 150, 136), new Color(96, 125, 139), new Color(123, 31, 162)
        };

        private final Map<String, Integer> data;

        BarChartPanel(Map<String, Integer> data) {
            this.data = data;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int max = Math.max(1, data.values().stream().mapToInt(Integer::intValue).max().orElse(1));
            int left = 50;
            int right = getWidth() - 30;
            int top = 50;
            int bottom = getHeight() - 50;
            int chartHeight = bottom - top;
            int slot = (right - left) / Math.max(1, data.size());
            int barWidth = slot / 2;

            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Number of devices of each type", left, 30);

            g2.setColor(Color.GRAY);
            g2.drawLine(left, top, left, bottom);
            g2.drawLine(left, bottom, right, bottom);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            FontMetrics fm = g2.getFontMetrics();
            int i = 0;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int barHeight = (int) ((double) entry.getValue() / max * chartHeight);
                int x = left + i * slot + (slot - barWidth) / 2;

                g2.setColor(COLORS[i % COLORS.length]);
                g2.fillRect(x, bottom - barHeight, barWidth, barHeight);

                g2.setColor(Color.BLACK);
                String value = String.valueOf(entry.getValue());
                g2.drawString(value, x + (barWidth - fm.stringWidth(value)) / 2, bottom - barHeight - 6);
                g2.drawString(entry.getKey(), x + (barWidth - fm.stringWidth(entry.getKey())) / 2, bottom + 18);
                i++;
            }
        }
    }
}
