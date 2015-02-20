/*
 * Copyright (C) 2015 Tim Vaughan (tgvaughan@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package beast.app.beauti;

import beast.util.AddOnManager;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Tim Vaughan (tgvaughan@gmail.com)
 */
public class BeautiFileSelector extends JDialog {

    final JFileChooser fc;
    String response;

    public BeautiFileSelector() {

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);

        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));

        fc = new JFileChooser();
        fc.addActionListener((ActionEvent e) -> {
            response = e.getActionCommand();
            dispose();
        });

        cp.add(fc);

        JPanel rightPanel = new JPanel();
        rightPanel.setBorder(new TitledBorder("Example folders"));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        Map<String, File> exampleDirs = getExampleDirs();
        JComboBox comboBox = new JComboBox(exampleDirs.keySet().toArray());
        comboBox.setMaximumSize(new Dimension(
            comboBox.getMaximumSize().width,
            comboBox.getPreferredSize().height));
        rightPanel.add(comboBox);
        
        rightPanel.add(Box.createVerticalStrut(5));

        JButton jumpButton = new JButton("Jump to example dir...");
        jumpButton.addActionListener((ActionEvent e) -> {
            File exampleDir = exampleDirs.get((String)comboBox.getSelectedItem());
            fc.setCurrentDirectory(exampleDir);
        });
        rightPanel.add(jumpButton);

        rightPanel.add(Box.createVerticalGlue());

        cp.add(rightPanel);
        
        pack();
    }

    public void setSelectedFile(File file) {
        fc.setSelectedFile(file);
    }

    public void setFilter(FileFilter filter) {
        fc.setFileFilter(filter);
    }
    
    public void addChoosableFileFilter(FileFilter filter) {
        fc.addChoosableFileFilter(filter);
    }

    public void setType(int type) {
        fc.setDialogType(type);
    }

    public void setMultiSelectionEnabled(boolean b) {
        fc.setMultiSelectionEnabled(b);
    }

    public File[] getSelectedFiles() {
        if (fc.isMultiSelectionEnabled())
            return fc.getSelectedFiles();
        else
            return new File[] { fc.getSelectedFile() };
    }

    public File getSelectedFile() {
        return fc.getSelectedFile();
    }

    public int showFileSelector() {
        setVisible(true);
        switch(response) {
            case JFileChooser.APPROVE_SELECTION:
                return JFileChooser.APPROVE_OPTION;
            case JFileChooser.CANCEL_SELECTION:
                return JFileChooser.CANCEL_OPTION;
            default:
                return JFileChooser.ERROR_OPTION;
        }
    }

    /**
     * Retrieve package example directories.
     * 
     * @return map from package names to example directories.
     */
    public final Map<String, File> getExampleDirs() {

        File userDir = new File(AddOnManager.getPackageUserDir());
        File[] packageDirs = userDir.listFiles((File pathname) -> (pathname.isDirectory()));

        Map<String, File> exampleDirs = new HashMap<>();
        for (File packageDir : packageDirs) {
            File exampleDir = new File(packageDir.getAbsolutePath() + File.separator + "examples");
            if (exampleDir.isDirectory())
                exampleDirs.put(packageDir.getName(), exampleDir);
        }

        return exampleDirs;
    }

    
    /**
     * Main method for debugging.
     * 
     * @param args 
     */
    public static void main(String[] args) {
        BeautiFileSelector bfs = new BeautiFileSelector();

        SwingUtilities.invokeLater(() -> {
            bfs.showFileSelector();
        });
    }

}
