package beast.app.beauti;

import javax.swing.*;
import java.awt.*;

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
        showWrappedMessageDialog(null, message, null);
    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param parentComponent parent component
     * @param message message to display
     */
    static public void showWrappedMessageDialog(Component parentComponent, Object message) {
        showWrappedMessageDialog(parentComponent, message, null);
    }

    /**
     * Display a message dialog with long lines wrapped at word breaks so
     * that the text width is limited to 70 chars.
     *
     * @param parentComponent parent component
     * @param message      message to display
     * @param fontName     name of font used to display message
     */
    static public void showWrappedMessageDialog(Component parentComponent, Object message, String fontName) {
        Object oldFont = null;
        if (fontName != null) {
            oldFont = UIManager.get("OptionPane.messageFont");

            int oldFontSize = 12;
            if (oldFont instanceof Font)
                oldFontSize = ((Font) oldFont).getSize();
            UIManager.put("OptionPane.messageFont", new Font(fontName, Font.PLAIN, oldFontSize));
        }

        WrappedOptionPane pane = new WrappedOptionPane();
        pane.setMessage(message);
        pane.setMessageType(INFORMATION_MESSAGE);

        JDialog dialog = pane.createDialog(parentComponent, "Message");
        dialog.setModal(true);
        dialog.setVisible(true);

        if (oldFont != null)
            UIManager.put("OptionPane.messageFont", oldFont);
    }
}
