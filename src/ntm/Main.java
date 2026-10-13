package ntm;

import ntm.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the Network Topology Manager application.
 */
public class Main {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Nimbus is not available, so the default look and feel is used.
        }

        // Swing components must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
