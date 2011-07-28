/*
 * BeautiMacFileMenuFactory.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.app.beauti2;

import jam.framework.MenuFactory;
import jam.framework.AbstractFrame;
import jam.framework.Application;
import jam.framework.MenuBarFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

/**
 * @author rambaut
 *         Date: Dec 26, 2004
 *         Time: 11:02:45 AM
 */
public class BeautiMacFileMenuFactory implements MenuFactory {

    public BeautiMacFileMenuFactory() {
    }

    public String getMenuName() {
        return "File";
    }

    public void populateMenu(JMenu menu, AbstractFrame frame) {

        Application application = Application.getApplication();
        JMenuItem item;

        item = new JMenuItem(application.getNewAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MenuBarFactory.MENU_MASK));
        menu.add(item);

        if (frame instanceof BeautiFrame) {
            item = new JMenuItem(frame.getImportAction());
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK));
            menu.add(item);

//            item = new JMenuItem(((BeautiFrame)frame).getImportTraitsAction());
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK + ActionEvent.ALT_MASK));
//            menu.add(item);

//            menu.addSeparator();
//
//            item = new JMenuItem(((BeautiFrame)frame).getOpenAction());
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MenuBarFactory.MENU_MASK));
//            menu.add(item);
//
//            item = new JMenuItem(frame.getSaveAsAction());
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuBarFactory.MENU_MASK));
//            menu.add(item);

            menu.addSeparator();

            item = new JMenuItem(frame.getExportAction());
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuBarFactory.MENU_MASK));
            menu.add(item);
        } else {
            // If the frame is not a BeautiFrame then create a dummy set of disabled menu options.
            // At present the only situation where this may happen is in Mac OS X when no windows
            // are open and the menubar is created by the hidden frame.

            item = new JMenuItem("Import Data...");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK));
            item.setEnabled(false);
            menu.add(item);

//            item = new JMenuItem("Import Traits...");
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, MenuBarFactory.MENU_MASK + ActionEvent.ALT_MASK));
//            item.setEnabled(false);
//            menu.add(item);

//            menu.addSeparator();
//
//            item = new JMenuItem("Apply Template...");
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MenuBarFactory.MENU_MASK));
//            item.setEnabled(false);
//            menu.add(item);
//
//            item = new JMenuItem("Save Template As...");
//            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuBarFactory.MENU_MASK));
//            item.setEnabled(false);
//            menu.add(item);

            menu.addSeparator();

            item = new JMenuItem("Generate BEAST File...");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuBarFactory.MENU_MASK));
            item.setEnabled(false);
            menu.add(item);
        }

        menu.addSeparator();

        item = new JMenuItem(frame.getCloseWindowAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, MenuBarFactory.MENU_MASK));
        menu.add(item);

        menu.addSeparator();

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
