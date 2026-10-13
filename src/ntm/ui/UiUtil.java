package ntm.ui;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.sql.SQLException;

/**
 * Small helpers so every window shows messages the same way.
 */
public final class UiUtil {

    private UiUtil() {
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void showDatabaseError(Component parent, SQLException ex) {
        showError(parent, "Database error:\n" + ex.getMessage()
                + "\n\nCheck that MySQL is running and that db.properties has the right password.");
    }
}
