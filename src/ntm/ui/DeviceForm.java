package ntm.ui;

import ntm.model.DeviceType;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;

/**
 * The small form used to add or edit a device. The fields keep their values
 * between showDialog() calls, so the user can fix a mistake without retyping.
 */
class DeviceForm {

    private final JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
    private final JTextField nameField;
    private final JTextField ipField;
    private final JCheckBox onlineBox;

    DeviceForm(DeviceType type, String name, String ip, boolean online) {
        nameField = new JTextField(name, 15);
        ipField = new JTextField(ip, 15);
        onlineBox = new JCheckBox("Online", online);

        panel.add(new JLabel("Type:"));
        panel.add(new JLabel(type.getLabel()));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("IP address:"));
        panel.add(ipField);
        panel.add(new JLabel("Status:"));
        panel.add(onlineBox);
    }

    /** Returns true if the user pressed OK. */
    boolean showDialog(Component parent, String title) {
        return JOptionPane.showConfirmDialog(parent, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    String getName() {
        return nameField.getText().trim();
    }

    String getIp() {
        return ipField.getText().trim();
    }

    boolean isOnline() {
        return onlineBox.isSelected();
    }
}
