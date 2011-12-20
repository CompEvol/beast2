package beast.app.beauti2.menus;

import jam.framework.*;
import jam.mac.Utils;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author rambaut
 *         Date: Feb 24, 2005
 *         Time: 5:12:11 PM
 */
public class ViewsMenuFactory implements MenuFactory {

    public String getMenuName() {
        return "Views";
    }

    public void populateMenu(JMenu menu, AbstractFrame frame) {
        JMenuItem item;

        if (frame instanceof ViewsMenuHandler) {
	        item = new JMenuItem(((ViewsMenuHandler)frame).getExampleAction());
	        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuBarFactory.MENU_MASK));
	        menu.add(item);
        }

    }

    public int getPreferredAlignment() {
        return LEFT;
    }
}
