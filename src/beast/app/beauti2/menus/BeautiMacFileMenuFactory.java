package beast.app.beauti2.menus;

import jam.framework.*;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class BeautiMacFileMenuFactory implements MenuFactory {

    private final boolean isMultiDocument;

    public BeautiMacFileMenuFactory(boolean isMultiDocument) {
        this.isMultiDocument = isMultiDocument;
    }

    public String getMenuName() {
        return "File";
    }

    public void populateMenu(JMenu menu, AbstractFrame frame) {

        Application application = Application.getApplication();
        JMenuItem item;

        if (isMultiDocument) {
            item = new JMenuItem(application.getNewAction());
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MenuBarFactory.MENU_MASK));
            menu.add(item);
        }

        item = new JMenuItem(application.getOpenAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MenuBarFactory.MENU_MASK));
        menu.add(item);

        if (application.getRecentFileMenu() != null) {
            JMenu subMenu = application.getRecentFileMenu();
            menu.add(subMenu);
        }

        menu.addSeparator();

        item = new JMenuItem(frame.getCloseWindowAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, MenuBarFactory.MENU_MASK));
        menu.add(item);

        item = new JMenuItem(frame.getSaveAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuBarFactory.MENU_MASK));
        menu.add(item);

        item = new JMenuItem(frame.getSaveAsAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuBarFactory.MENU_MASK + ActionEvent.SHIFT_MASK));
        menu.add(item);

        item = new JMenuItem("Revert to Saved");
        item.setEnabled(false);
        menu.add(item);

        if (frame.getImportAction() != null || frame.getExportAction() != null) {
            menu.addSeparator();

            if (frame.getImportAction() != null) {
                item = new JMenuItem(frame.getImportAction());
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK));
                menu.add(item);
            }

            if (frame.getExportAction() != null) {
                item = new JMenuItem(frame.getExportAction());
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuBarFactory.MENU_MASK));
                menu.add(item);
            }
        }

        menu.addSeparator();

		// On Windows and Linux platforms, each window has its own menu so items which are not needed
		// are simply missing. In contrast, on Mac, the menu is for the application so items should
		// be enabled/disabled as frames come to the front.
        if (frame instanceof BeautiFileMenuHandler) {
		    item = new JMenuItem(((BeautiFileMenuHandler)frame).getAddonManagerAction());
//		    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, MenuBarFactory.MENU_MASK));
		    menu.add(item);
            menu.addSeparator();
		} else {
		    // If the frame is not a BeautiFileMenuHandler then add a disabled version.
            item = new JMenuItem(BeautiFileMenuHandler.ADD_ON_MANAGER);
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuBarFactory.MENU_MASK));
            item.setEnabled(false);
            menu.add(item);
            menu.addSeparator();
		}

        item = new JMenuItem(frame.getPrintAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MenuBarFactory.MENU_MASK));
        menu.add(item);

        item = new JMenuItem(application.getPageSetupAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MenuBarFactory.MENU_MASK + ActionEvent.SHIFT_MASK));
        menu.add(item);

    }

    public int getPreferredAlignment() {
        return LEFT;
    }
}
