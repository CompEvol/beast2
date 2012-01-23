/*
 * LogCombinerDialog.java
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

package beast.app.tools;


//import dr.app.gui.FileDrop;

import beast.app.beastapp.WholeNumberField;
import beast.app.beauti.Beauti;
import beast.app.util.FileDrop;
import beast.app.util.Utils;
//import dr.app.gui.table.TableEditorStopper;
//import dr.app.gui.components.WholeNumberField;
import jam.panels.ActionPanel;
import jam.panels.OptionsPanel;
import jam.table.TableRenderer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogCombinerDialog {
    private final JFrame frame;

    private final OptionsPanel optionPanel;

    private JTable filesTable = null;
    private FilesTableModel filesTableModel = null;

    private final JComboBox fileTypeCombo = new JComboBox(new String[]{"Log Files", "Tree Files"});
    private final JCheckBox decimalCheck = new JCheckBox("Convert numbers from scientific to decimal notation");
    private final JCheckBox renumberOutput = new JCheckBox("Renumber ouput states");
    private final JCheckBox resampleCheck = new JCheckBox("Resample states at lower frequency: ");
    private final WholeNumberField resampleText = new WholeNumberField(0, Integer.MAX_VALUE);

    private final List<FileInfo> files = new ArrayList<FileInfo>();

    private final JTextField fileNameText = new JTextField("not selected", 16);
    private File outputFile = null;

    public LogCombinerDialog(final JFrame frame, String titleString, Icon icon) {
        this.frame = frame;

        optionPanel = new OptionsPanel(12, 12);

        final JLabel titleText = new JLabel(titleString);
        titleText.setIcon(icon);
        optionPanel.addSpanningComponent(titleText);
        titleText.setFont(new Font("sans-serif", 0, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Taxon Sets
        filesTableModel = new FilesTableModel();
        filesTable = new JTable(filesTableModel);

        filesTable.getColumnModel().getColumn(0).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        filesTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        filesTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        TableEditorStopper.ensureEditingStopWhenTableLosesFocus(filesTable);

        filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                filesTableSelectionChanged();
            }
        });

        JScrollPane scrollPane1 = new JScrollPane(filesTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane1.setMaximumSize(new Dimension(10000, 120));

        ActionPanel actionPanel1 = new ActionPanel(false);
        actionPanel1.setAddAction(addFileAction);
        actionPanel1.setRemoveAction(removeFileAction);
        removeFileAction.setEnabled(false);

        JPanel controlPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel1.add(actionPanel1);

        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(new JLabel("Select input files:"), BorderLayout.NORTH);
        panel.add(scrollPane1, BorderLayout.CENTER);
        panel.add(actionPanel1, BorderLayout.SOUTH);

        Color focusColor = UIManager.getColor("Focus.color");
        Border focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, focusColor);
        new FileDrop(null, scrollPane1, focusBorder, new FileDrop.Listener() {
            public void filesDropped(java.io.File[] files) {
                addFiles(files);
            }   // end filesDropped
        }); // end FileDrop.Listener

        resampleText.setEnabled(false);
        resampleText.setColumns(12);
        resampleCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resampleText.setEnabled(resampleCheck.isSelected());
            }
        });

        ActionListener buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                File file = Utils.getSaveFile("Select output file...", new File(Beauti.g_sDir), "Beast log and tree files", "log", "trees");
                if (file == null) {
                    // the dialog was cancelled...
                    return;
                }
                outputFile = file;
                String sFileName = file.getAbsolutePath();
                if (sFileName.lastIndexOf('/') > 0) {
                    Beauti.g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
                }
                fileNameText.setText(outputFile.getName());

            }
        };

        JButton button = new JButton("Choose File...");
        button.addActionListener(buttonListener);

        JPanel panel2 = new JPanel(new BorderLayout(0, 0));
        panel2.add(resampleCheck, BorderLayout.CENTER);
        panel2.add(resampleText, BorderLayout.EAST);
        optionPanel.addComponentWithLabel("File type: ", fileTypeCombo);
        optionPanel.addComponent(decimalCheck);
        optionPanel.addComponent(renumberOutput);
        optionPanel.addComponent(panel2);

        optionPanel.addSpanningComponent(panel);

        fileNameText.setEditable(false);

        JPanel panel3 = new JPanel(new BorderLayout(0, 0));
        panel3.add(fileNameText, BorderLayout.CENTER);
        panel3.add(button, BorderLayout.EAST);
        optionPanel.addComponentWithLabel("Output File: ", panel3);
    }

    public boolean showDialog(String title) {

        addFileAction.setEnabled(true);
        removeFileAction.setEnabled(false);

        filesTableModel.fireTableDataChanged();

        JOptionPane optionPane = new JOptionPane(optionPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                new String[]{"Run", "Quit"},
                null);
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        final JDialog dialog = optionPane.createDialog(frame, title);
        //dialog.setResizable(true);
        dialog.pack();

        dialog.setVisible(true);

        return optionPane.getValue().equals("Run");
    }

    public String[] getFileNames() {
        String[] fileArray = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            FileInfo fileInfo = files.get(i);
            fileArray[i] = fileInfo.file.getPath();
        }
        return fileArray;
    }

    public int[] getBurnins() {
        int[] burnins = new int[files.size()];
        for (int i = 0; i < files.size(); i++) {
            FileInfo fileInfo = files.get(i);
            burnins[i] = fileInfo.burnin;
        }
        return burnins;
    }

    public boolean isTreeFiles() {
        return fileTypeCombo.getSelectedIndex() == 1;
    }

    public boolean convertToDecimal() {
        return decimalCheck.isSelected();
    }

    public boolean renumberOutputStates() {
        return renumberOutput.isSelected();
    }

    public boolean isResampling() {
        return resampleCheck.isSelected();
    }

    public int getResampleFrequency() {
        return resampleText.getValue();
    }

    public String getOutputFileName() {
        if (outputFile == null) return null;
        return outputFile.getPath();
    }

    private void filesTableSelectionChanged() {
        if (filesTable.getSelectedRowCount() == 0) {
            removeFileAction.setEnabled(false);
        } else {
            removeFileAction.setEnabled(true);
        }
    }

    private void addFiles(File[] fileArray) {
        int sel1 = files.size();
        for (File file : fileArray) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.file = file;
            fileInfo.burnin = 0;

            files.add(fileInfo);

            String sFileName = file.getAbsolutePath();
            if (sFileName.lastIndexOf('/') > 0) {
                Beauti.g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
            }
        }

        filesTableModel.fireTableDataChanged();

        int sel2 = files.size() - 1;
        filesTable.setRowSelectionInterval(sel1, sel2);

    }

    Action addFileAction = new AbstractAction("+") {

        /**
         *
         */
        private static final long serialVersionUID = 7602227478402204088L;

        public void actionPerformed(ActionEvent ae) {
            File[] files = Utils.getLoadFiles("Select log file", new File(Beauti.g_sDir), "Trace or tree log files", "log", "trees");
            if (files != null) {
                addFiles(files);
            }
        }
    };

    Action removeFileAction = new AbstractAction("-") {

        /**
         *
         */
        private static final long serialVersionUID = 5934278375005327047L;

        public void actionPerformed(ActionEvent ae) {
            int row = filesTable.getSelectedRow();
            if (row != -1) {
                files.remove(row);
            }

            filesTableModel.fireTableDataChanged();

            if (row >= files.size()) row = files.size() - 1;
            if (row >= 0) {
                filesTable.setRowSelectionInterval(row, row);
            }
        }
    };


    class FilesTableModel extends AbstractTableModel {
        /**
         *
         */
        private static final long serialVersionUID = 4153326364833213013L;
        private final String[] columns = {"File", "Burnin (percentage)"};

        public FilesTableModel() {
        }

        public int getColumnCount() {
            return columns.length;
        }

        public int getRowCount() {
            return files.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            FileInfo fileInfo = files.get(rowIndex);
            if (columnIndex == 0) {
                return fileInfo.file.getName();
            } else {
                return fileInfo.burnin;
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (columnIndex == 1);
        }

        /**
         * This empty implementation is provided so users don't have to implement
         * this method if their data model is not editable.
         *
         * @param aValue      value to assign to cell
         * @param rowIndex    row of cell
         * @param columnIndex column of cell
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            FileInfo fileInfo = files.get(rowIndex);
            if (columnIndex == 1) {
                fileInfo.burnin = (Integer) aValue;
            }
        }

        public String getColumnName(int columnIndex) {
            return columns[columnIndex];
        }

        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }
    }

    class FileInfo {
        File file;
        Integer burnin;
    }
}
