package beast.app.draw;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import beast.app.util.Utils;
import beast.util.BEASTClassLoader;



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
        super(name);
        try {
        	init(name, toolTipText, icon, KeyStroke.getKeyStroke(acceleratorKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));        
        } catch (Throwable  e) {
        	e.printStackTrace();
        }
    } // c'tor

    public MyAction(String name, String toolTipText, String icon, String acceleratorKey) {
        super(name);
        try {
        	init(name, toolTipText, icon, KeyStroke.getKeyStroke(acceleratorKey));
        } catch (Throwable  e) {
        	e.printStackTrace();
        }
    } // c'tor

    public MyAction(String name, String toolTipText, String icon, KeyStroke acceleratorKeystroke) {
        super(name);
        init(name, toolTipText, icon, acceleratorKeystroke);
    }
    
    private void init(String name, String toolTipText, String icon, KeyStroke acceleratorKeystroke) {
        try {
        	// setToolTipText(toolTipText);
        	putValue(Action.SHORT_DESCRIPTION, toolTipText);
        	putValue(Action.LONG_DESCRIPTION, toolTipText);
        	if (acceleratorKeystroke != null && acceleratorKeystroke.getKeyCode() >= 0) {
        		putValue(Action.ACCELERATOR_KEY, acceleratorKeystroke);
        	}
        	putValue(Action.MNEMONIC_KEY, new Integer(name.charAt(0)));
        
	        if (!Utils.isMac()) {
		        java.net.URL tempURL = BEASTClassLoader.classLoader.getResource(ModelBuilder.ICONPATH + icon + ".png");
		        if (tempURL != null) {
		            putValue(Action.SMALL_ICON, new ImageIcon(tempURL));
		        } else {
		            putValue(Action.SMALL_ICON, new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_4BYTE_ABGR)));
		        }
	        }
        } catch (Throwable  e) {
        	e.printStackTrace();
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
