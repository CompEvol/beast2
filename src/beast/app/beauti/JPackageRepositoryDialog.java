/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package beast.app.beauti;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import beast.util.PackageManager;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class JPackageRepositoryDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public JPackageRepositoryDialog(final JFrame frame) {
        super(frame);

        setModal(true);
        setTitle("BEAST 2 Package Repository Manager");
        
        // Get current list of URLs:
        List<URL> urls;
        try {
            urls = PackageManager.getRepositoryURLs();
        } catch (MalformedURLException exception) {
            urls = new ArrayList<>();
            try {
                urls.add(new URL(PackageManager.PACKAGES_XML));
            } catch (MalformedURLException e) {
                // Hard-coded URL is broken. Should never happen!
                e.printStackTrace();
            }
        }

        // Assemble table
        final RepoTableModel repoTableModel = new RepoTableModel(urls);
        final JTable repoTable = new JTable(repoTableModel);
		int size = repoTable.getFont().getSize();
		repoTable.setRowHeight(20 * size/13);
        repoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(repoTable);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons
        Box box = Box.createHorizontalBox();
        
        // ADD URL
        JButton addURLButton = new JButton("Add URL");
        addURLButton.addActionListener(e -> {
            String newURLString = (String)JOptionPane.showInputDialog(frame,
                    "Enter package repository URL",
                    "Add repository URL",JOptionPane.PLAIN_MESSAGE, null, null, "http://");

            if (newURLString == null)
                return; // User canceled

            URL newURL = null;
            try {
                newURL = new URL(newURLString);
            } catch (MalformedURLException exception) {
                JOptionPane.showMessageDialog(frame, "Invalid URL.");
                return;
            }

            if (repoTableModel.urls.contains(newURL)) {
                JOptionPane.showMessageDialog(frame, "Repository already exists!");
                return;
            }

            try {
                if (newURL.getHost() == null)
                    return;

                InputStream is = newURL.openStream();
                is.close();

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Could not access URL.");
                return;
            }

            // Add to table:
            repoTableModel.urls.add(newURL);
            repoTableModel.fireTableDataChanged();
        });
        box.add(addURLButton);
        
        // DELETE URL
        JButton deleteURLButton = new JButton("Delete selected URL");
        deleteURLButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Really delete this repository?") ==JOptionPane.YES_OPTION) {
                repoTableModel.urls.remove(repoTable.getSelectedRow());
                repoTableModel.fireTableDataChanged();
            }
        });
        deleteURLButton.setEnabled(false);
        box.add(deleteURLButton);
        
        // DONE
        JButton OKButton = new JButton("Done");
        OKButton.addActionListener(e -> {
            PackageManager.saveRepositoryURLs(repoTableModel.urls);
            setVisible(false);
        });
        box.add(OKButton);
        getContentPane().add(box, BorderLayout.PAGE_END);

        // Action listeners to disable/enable delete button
        ListSelectionModel listSelectionModel = repoTable.getSelectionModel();
        listSelectionModel.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;

            if (listSelectionModel.isSelectedIndex(0))
                deleteURLButton.setEnabled(false);
            else
                deleteURLButton.setEnabled(true);
        });

        // Set size and location of dialog
        Dimension dim = scrollPane.getPreferredSize();
        Dimension dim2 = box.getPreferredSize();
        setSize(dim.width + 30, dim.height + dim2.height + 30);
        Point frameLocation = frame.getLocation();
        Dimension frameSize = frame.getSize();
        setLocation(frameLocation.x + frameSize.width / 2 - dim.width / 2,
                frameLocation.y + frameSize.height / 2 - dim.height / 2);
    }

    /**
     * Class of tables containing the current list of package repositories.
     */
    class RepoTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		
		public List<URL> urls;

        public RepoTableModel(List<URL> urls) {
            this.urls = urls;
        }

        @Override
        public int getRowCount() {
            return urls.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "Package repository URLs";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return urls.get(rowIndex);
        }
    }

}
