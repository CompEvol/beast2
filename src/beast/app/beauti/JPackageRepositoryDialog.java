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

import beast.util.AddOnManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class JPackageRepositoryDialog extends JDialog {

    public JPackageRepositoryDialog(final JFrame frame) {
        super(frame);

        setModal(true);
        setTitle("BEAST 2 Package Repository Manager");
        
        // Get current list of URLs:
        List<String> URLs;
        try {
            URLs = AddOnManager.getPackagesURL();
        } catch (MalformedURLException exception) {
            URLs = new ArrayList<String>();
            URLs.add(AddOnManager.PACKAGES_XML);
        }

        // Assemble table
        final RepoTableModel repoTableModel = new RepoTableModel(URLs);
        final JTable repoTable = new JTable(repoTableModel);
        JScrollPane scrollPane = new JScrollPane(repoTable);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons
        Box box = Box.createHorizontalBox();
        
        // ADD URL
        JButton addURLButton = new JButton("Add URL");
        addURLButton.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String newURL = (String)JOptionPane.showInputDialog(frame,
                        "Enter package repository URL",
                        "Add repository URL",JOptionPane.PLAIN_MESSAGE, null, null, "http://");
                
                if (newURL == null)
                    return; // User canceled
                
                if (!repoTableModel.URLs.contains(newURL)) {
                    
                    // Check that URL is accessible:
                    try {
                        URL url = new URL(newURL);
                        if (url.getHost() == null)
                            return;
                        
                        InputStream is = url.openStream();
                        is.close();
                      
                    } catch (MalformedURLException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid URL.");
                        return;
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Could not access URL.");
                        return;
                    }

                    // Add to table:                        
                    repoTableModel.URLs.add(newURL);
                    repoTableModel.fireTableDataChanged();  
                } else {
                    JOptionPane.showMessageDialog(frame, "Repository already exists!");
                }
            }
        });
        box.add(addURLButton);
        
        // DELETE URL
        JButton deleteURLButton = new JButton("Delete selected URL");
        deleteURLButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (repoTable.getSelectedRow()>0) {
                    if (JOptionPane.showConfirmDialog(frame, "Really delete this repository?") ==JOptionPane.YES_OPTION) {
                        repoTableModel.URLs.remove(repoTable.getSelectedRow());
                        repoTableModel.fireTableDataChanged();
                    }
                }
            }
        });
        box.add(deleteURLButton);
        
        // DONE
        JButton OKButton = new JButton("Done");
        OKButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                AddOnManager.savePackageURLs(repoTableModel.URLs);
                setVisible(false);
            }
        });
        box.add(OKButton);
        getContentPane().add(box, BorderLayout.PAGE_END);

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

        public List<String> URLs;

        public RepoTableModel(List<String> URLs) {
            this.URLs = URLs;
        }

        @Override
        public int getRowCount() {
            return URLs.size();
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
            return URLs.get(rowIndex);
        }
    }

}
