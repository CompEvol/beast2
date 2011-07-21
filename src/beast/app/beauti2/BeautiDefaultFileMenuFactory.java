package beast.app.beauti2;

import jam.framework.AbstractFrame;
import jam.framework.Application;
import jam.framework.MenuBarFactory;
import jam.framework.MenuFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

/**
 * @author rambaut
 *         Date: Dec 26, 2004
 *         Time: 11:01:06 AM
 */
public class BeautiDefaultFileMenuFactory implements MenuFactory {


    public BeautiDefaultFileMenuFactory() {
    }

    public String getMenuName() {
        return "File";
    }

    public void populateMenu(JMenu menu, AbstractFrame frame) {

        JMenuItem item;

        Application application = Application.getApplication();
        menu.setMnemonic('F');

        item = new JMenuItem(application.getNewAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MenuBarFactory.MENU_MASK));
        menu.add(item);

        item = new JMenuItem(frame.getImportAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK));
        menu.add(item);

//        item = new JMenuItem(((BeautiFrame)frame).getImportTraitsAction());
//        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK + ActionEvent.ALT_MASK));
//        menu.add(item);

//        menu.addSeparator();
//
//        item = new JMenuItem(frame.getOpenAction());
//        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MenuBarFactory.MENU_MASK));
//        menu.add(item);
//
//        item = new JMenuItem(frame.getSaveAsAction());
//        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuBarFactory.MENU_MASK));
//        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem(frame.getExportAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuBarFactory.MENU_MASK));
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem(frame.getPrintAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MenuBarFactory.MENU_MASK));
        menu.add(item);

        item = new JMenuItem(application.getPageSetupAction());
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem(application.getExitAction());
        menu.add(item);
    }

    public int getPreferredAlignment() {
        return LEFT;
    }
}
