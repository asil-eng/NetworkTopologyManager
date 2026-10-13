package ntm.ui;

import ntm.model.Device;
import ntm.model.DeviceType;
import ntm.topology.TopologyPlan;
import ntm.topology.TopologyTemplate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Lets the user pick a topology template and its size, with a live preview.
 */
class TemplateDialog extends JDialog {

    private final JComboBox<TopologyTemplate> templateBox =
            new JComboBox<>(TopologyTemplate.all().toArray(new TopologyTemplate[0]));
    private final JSpinner sizeSpinner = new JSpinner();
    private final JLabel sizeLabel = new JLabel();
    private final JTextArea descriptionArea = new JTextArea(3, 40);
    private final PreviewPanel preview = new PreviewPanel();
    private boolean confirmed;

    TemplateDialog(JFrame owner) {
        super(owner, "Create Topology from Template", true);
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Topology:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(templateBox, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0;
        form.add(sizeLabel, gc);
        gc.gridx = 1;
        form.add(sizeSpinner, gc);

        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setOpaque(false);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        form.add(descriptionArea, gc);
        add(form, BorderLayout.NORTH);

        preview.setPreferredSize(new Dimension(520, 340));
        preview.setBorder(BorderFactory.createTitledBorder("Preview"));
        add(preview, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createButton = new JButton("Create");
        JButton cancelButton = new JButton("Cancel");
        createButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
        buttons.add(createButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(createButton);

        templateBox.addActionListener(e -> onTemplateChanged());
        sizeSpinner.addChangeListener(e -> updatePreview());
        onTemplateChanged();

        pack();
        setLocationRelativeTo(owner);
    }

    boolean isConfirmed() {
        return confirmed;
    }

    TopologyTemplate getTemplate() {
        return (TopologyTemplate) templateBox.getSelectedItem();
    }

    int getTopologySize() {
        return (Integer) sizeSpinner.getValue();
    }

    private void onTemplateChanged() {
        TopologyTemplate template = getTemplate();
        sizeLabel.setText(template.getSizeLabel() + ":");
        sizeSpinner.setModel(new SpinnerNumberModel(template.getDefaultSize(),
                template.getMinSize(), template.getMaxSize(), 1));
        descriptionArea.setText(template.getDescription());
        updatePreview();
    }

    private void updatePreview() {
        preview.setPlan(getTemplate().createPlan(getTopologySize()));
    }

    /** Draws a small scaled-down picture of the plan: dots for devices, lines for cables. */
    private static class PreviewPanel extends JPanel {

        private TopologyPlan plan;

        PreviewPanel() {
            setBackground(Color.WHITE);
        }

        void setPlan(TopologyPlan plan) {
            this.plan = plan;
            repaint();
        }

        private static Color colorOf(DeviceType type) {
            return switch (type) {
                case ROUTER -> new Color(25, 118, 210);
                case SWITCH -> new Color(0, 150, 136);
                case PC -> new Color(96, 125, 139);
                case SERVER -> new Color(123, 31, 162);
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (plan == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 30;
            double scale = Math.min(1.0, Math.min(
                    (getWidth() - 2.0 * pad) / Math.max(1, plan.getWidth()),
                    (getHeight() - 2.0 * pad - 20) / Math.max(1, plan.getHeight())));
            int offsetX = (int) ((getWidth() - plan.getWidth() * scale) / 2);
            int offsetY = pad;
            int half = Device.SIZE / 2;

            g2.setColor(new Color(120, 144, 156));
            g2.setStroke(new BasicStroke(2f));
            for (TopologyPlan.Edge edge : plan.getEdges()) {
                TopologyPlan.Node a = plan.getNodes().get(edge.from());
                TopologyPlan.Node b = plan.getNodes().get(edge.to());
                g2.drawLine(offsetX + (int) ((a.x() + half) * scale), offsetY + (int) ((a.y() + half) * scale),
                        offsetX + (int) ((b.x() + half) * scale), offsetY + (int) ((b.y() + half) * scale));
            }

            for (TopologyPlan.Node node : plan.getNodes()) {
                int cx = offsetX + (int) ((node.x() + half) * scale);
                int cy = offsetY + (int) ((node.y() + half) * scale);
                g2.setColor(colorOf(node.type()));
                g2.fillOval(cx - 9, cy - 9, 18, 18);
            }

            g2.setColor(Color.DARK_GRAY);
            g2.drawString(plan.getNodes().size() + " devices, " + plan.getEdges().size() + " cables",
                    12, getHeight() - 12);
        }
    }
}
