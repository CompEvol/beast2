package beast.app.draw;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import beast.app.util.Utils;



/**
 * Base class used for definining actions with a name, tool tip text, possibly
 * an icon and accelerator key.
 */
public class MyAction extends AbstractAction {
    /**
     * for serialisation
     */
    private static final long serialVersionUID = -1L;

    /**
     * path for icons
     */

    public MyAction(String name, String toolTipText, String icon, int acceleratorKey) {
        this(name, toolTipText, icon, KeyStroke.getKeyStroke(acceleratorKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    } // c'tor

    public MyAction(String name, String toolTipText, String icon, String acceleratorKey) {
        this(name, toolTipText, icon, KeyStroke.getKeyStroke(acceleratorKey));
    } // c'tor

    public MyAction(String name, String toolTipText, String icon, KeyStroke acceleratorKeystroke) {
        super(name);
        // setToolTipText(toolTipText);
        putValue(Action.SHORT_DESCRIPTION, toolTipText);
        putValue(Action.LONG_DESCRIPTION, toolTipText);
        if (acceleratorKeystroke != null && acceleratorKeystroke.getKeyCode() >= 0) {
            putValue(Action.ACCELERATOR_KEY, acceleratorKeystroke);
        }
        putValue(Action.MNEMONIC_KEY, new Integer(name.charAt(0)));
        java.net.URL tempURL = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + icon + ".png");
        if (!Utils.isMac()) {
	        if (tempURL != null) {
	            putValue(Action.SMALL_ICON, new ImageIcon(tempURL));
	        } else {
	            putValue(Action.SMALL_ICON, new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
	        }
        }
    } // c'tor



    /*
      * Place holder. Should be implemented by derived classes. (non-Javadoc)
      * @see
      * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
      * )
      */
    @Override
	public void actionPerformed(ActionEvent ae) {}

} // class MyAction
