package beast.app.beauti;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * JOptionPane but with text wrapping.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class WrappedOptionPane extends JOptionPane {

    @Override
    public int getMaxCharactersPerLineCount() {
        return 70;
    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param message message to display
     */
    static public void showWrappedMessageDialog(Object message) {
        showWrappedMessageDialog(null, message);
    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param parentComponent parent component
     * @param message      message to display
     */
    static public void showWrappedMessageDialog(Component parentComponent, Object message) {
        WrappedOptionPane pane = new WrappedOptionPane();
        pane.setMessage(message);
        pane.setMessageType(INFORMATION_MESSAGE);

        JDialog dialog = pane.createDialog(parentComponent, "Message");
        dialog.setModal(true);
        dialog.setVisible(true);
    }
}
