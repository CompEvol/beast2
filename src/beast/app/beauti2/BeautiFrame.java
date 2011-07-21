/*
 * BeautiFrame.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package beast.app.beauti2;

import beast.app.beauti.*;
import beast.app.beauti2.util.BEAUTiImporter;
import beast.app.beauti2.util.TextUtil;
import beast.app.util.OSType;
import beast.app.util.Utils;
import jam.framework.DocumentFrame;
import jam.framework.Exportable;
import jam.util.IconUtils;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeautiFrame.java,v 1.22 2006/09/09 16:07:06 rambaut Exp $
 */
public class BeautiFrame extends DocumentFrame {

    private static final long serialVersionUID = 2114148696789612509L;

    public final JTabbedPane tabbedPane = new JTabbedPane();
    public final JLabel statusLabel = new JLabel();

    private JFileChooser importChooser; // make JFileChooser chooser remember previous path
    private JFileChooser exportChooser; // make JFileChooser chooser remember previous path

    final Icon gearIcon = IconUtils.getIcon(this.getClass(), "images/gear.png");

    public BeautiFrame(String title) {
        super();

        setTitle(title);

        // Prevent the application to close in requestClose()
        // after a user cancel or a failure in beast file generation
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        getOpenAction().setEnabled(false);
        getSaveAction().setEnabled(false);

        getFindAction().setEnabled(false);

        getZoomWindowAction().setEnabled(false);

        this.getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener() {
            public void ancestorMoved(HierarchyEvent e) {
            }

            public void ancestorResized(HierarchyEvent e) {
                setStatusMessage();
            }
        });
    }

    public void initializeComponents() {

        JPanel basePanel = new JPanel(new BorderLayout(6, 6));
        basePanel.setBorder(new BorderUIResource.EmptyBorderUIResource(new java.awt.Insets(12, 12, 12, 12)));
//        basePanel.setPreferredSize(new java.awt.Dimension(800, 600));

        getExportAction().setEnabled(false);
        JButton generateButton = new JButton(getExportAction());
        generateButton.putClientProperty("JButton.buttonType", "roundRect");

        JPanel panel2 = new JPanel(new BorderLayout(6, 6));
        panel2.add(statusLabel, BorderLayout.WEST);
        panel2.add(generateButton, BorderLayout.EAST);
        panel2.setMinimumSize(new java.awt.Dimension(10, 10));

//        basePanel.add(tabbedPane, BorderLayout.CENTER);
        //basePanel.add(Beauti.createBeauti(new String[0]), BorderLayout.CENTER);

        basePanel.add(panel2, BorderLayout.SOUTH);

        add(basePanel, BorderLayout.CENTER);

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        System.out.println("Screen width = " + d.width);
        System.out.println("Screen height = " + d.height);

        if (d.width < 1000 || d.height < 700) {
            setSize(new java.awt.Dimension(700, 550));
        } else {
            setSize(new java.awt.Dimension(1024, 768));
        }

            setMinimumSize(new java.awt.Dimension(640, 480));

        // make JFileChooser chooser remember previous path
        exportChooser = new JFileChooser(Utils.getCWD());
        exportChooser.setFileFilter(new FileNameExtensionFilter("BEAST XML File", "xml", "beast"));
        exportChooser.setDialogTitle("Generate BEAST XML File...");


        importChooser = new JFileChooser(Utils.getCWD());

        importChooser.setMultiSelectionEnabled(true);
        importChooser.setFileFilter(new FileNameExtensionFilter(
                        "Microsatellite (tab-delimited *.txt) Files", "txt"));
        importChooser.setFileFilter(new FileNameExtensionFilter(
                "NEXUS (*.nex) & BEAST (*.xml) Files", "nex", "nexus", "nx", "xml", "beast", "fa", "fasta", "afa"));
        importChooser.setDialogTitle("Import Aligment...");

//        Color focusColor = UIManager.getColor("Focus.color");
//        Border focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, focusColor);
//        dataPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
//        new FileDrop(null, dataPanel, focusBorder, new FileDrop.Listener() {
//            public void filesDropped(java.io.File[] files) {
//                importFiles(files);
//            }   // end filesDropped
//        }); // end FileDrop.Listener


    }

    public void doSelectAll() {
    }

    public final void dataSelectionChanged(boolean isSelected) {
        getDeleteAction().setEnabled(isSelected);
    }

    public final void modelSelectionChanged(boolean isSelected) {
        getDeleteAction().setEnabled(isSelected);
    }

    public void doDelete() {
//        if (tabbedPane.getSelectedComponent() == dataPanel) {
//            dataPanel.removeSelection();
////        } else if (tabbedPane.getSelectedComponent() == modelsPanel) {
////            modelsPanel.delete();
////        } else if (tabbedPane.getSelectedComponent() == treesPanel) {
////        	treesPanel.delete();
//        } else {
//            throw new RuntimeException("Delete should only be accessable from the Data and Models panels");
//        }

        setStatusMessage();
    }

    public boolean requestClose() {
//        if (isDirty() && options.hasData()) {
//            int option = JOptionPane.showConfirmDialog(this,
//                    "You have made changes but have not generated\n" +
//                            "a BEAST XML file. Do you wish to generate\n" +
//                            "before closing this window?",
//                    "Unused changes",
//                    JOptionPane.YES_NO_CANCEL_OPTION,
//                    JOptionPane.WARNING_MESSAGE);
//
//            if (option == JOptionPane.YES_OPTION) {
//                return !doGenerate();
//            } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.DEFAULT_OPTION) {
//                return false;
//            }
//            return true;
//        }
        return true;
    }

    public void doApplyTemplate() {
//        FileDialog dialog = new FileDialog(this,
//                "Apply Template",
//                FileDialog.LOAD);
//        dialog.setVisible(true);
//        if (dialog.getFile() != null) {
//            File file = new File(dialog.getDirectory(), dialog.getFile());
//            try {
//                readFromFile(file);
//            } catch (FileNotFoundException fnfe) {
//                JOptionPane.showMessageDialog(this, "Unable to open template file: File not found",
//                        "Unable to open file",
//                        JOptionPane.ERROR_MESSAGE);
//            } catch (IOException ioe) {
//                JOptionPane.showMessageDialog(this, "Unable to read template file: " + ioe.getMessage(),
//                        "Unable to read file",
//                        JOptionPane.ERROR_MESSAGE);
//            }
//        }
    }

    protected boolean readFromFile(File file) throws IOException {
        return false;
    }

    public String getDefaultFileName() {
//        return options.fileNameStem + ".beauti";
        return "untitled.xml";
    }

    protected boolean writeToFile(File file) throws IOException {
        return false;
    }

    public final void doImport() {
        int returnVal = importChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = importChooser.getSelectedFiles();
            importFiles(files);
        }
    }

    private void importFiles(File[] files) {
        for (File file : files) {
            if (file == null || file.getName().equals("")) {
                JOptionPane.showMessageDialog(this, "Invalid file name",
                        "Invalid file name", JOptionPane.ERROR_MESSAGE);
            } else {
//                try {
//                    BEAUTiImporter beautiImporter = new BEAUTiImporter(this);
//                    beautiImporter.importFromFile(file);
//
//                    setDirty();
////                    } catch (FileNotFoundException fnfe) {
////                        JOptionPane.showMessageDialog(this, "Unable to open file: File not found",
////                                "Unable to open file", JOptionPane.ERROR_MESSAGE);
//                } catch (IOException ioe) {
//                    JOptionPane.showMessageDialog(this, "File I/O Error unable to read file: " + ioe.getMessage(),
//                            "Unable to read file", JOptionPane.ERROR_MESSAGE);
//                    ioe.printStackTrace();
//                    // there may be other files in the list so don't return
////                    return;
//
//                } catch (NexusImporter.MissingBlockException ex) {
//                    JOptionPane.showMessageDialog(this, "TAXON, DATA or CHARACTERS block is missing in Nexus file: " + ex,
//                            "Missing Block in Nexus File",
//                            JOptionPane.ERROR_MESSAGE);
//                    ex.printStackTrace();
//
//                } catch (ImportException ime) {
//                    JOptionPane.showMessageDialog(this, "Error parsing imported file: " + ime,
//                            "Error reading file",
//                            JOptionPane.ERROR_MESSAGE);
//                    ime.printStackTrace();
//                    // there may be other files in the list so don't return
////                    return;
//                } catch (IllegalArgumentException illegEx) {
//                    JOptionPane.showMessageDialog(this, illegEx.getMessage(),
//                            "Illegal Argument Exception", JOptionPane.ERROR_MESSAGE);
//
//                } catch (Exception ex) {
//                    JOptionPane.showMessageDialog(this, "Fatal exception: " + ex,
//                            "Error reading file",
//                            JOptionPane.ERROR_MESSAGE);
//                    ex.printStackTrace();
//                    return;
//                }
            }
        }

//        setAllOptions();

//          // @Todo templates are not implemented yet...
////        getOpenAction().setEnabled(true);
////        getSaveAction().setEnabled(true);
        getExportAction().setEnabled(true);
    }


    public void setStatusMessage() {
        int width = this.getWidth() - 260; // minus generate button size
        if (width < 100) width = 100; // prevent too narrow
        String tw = "Status";
//        String tw = TextUtil.wrapText(options.statusMessage(), statusLabel, width);
//        System.out.println(this.getWidth() + "   " + tw);
        statusLabel.setText(tw);
    }

    public final boolean doGenerate() {
        return false;
    }

    public JComponent getExportableComponent() {

        JComponent exportable = null;
        Component comp = tabbedPane.getSelectedComponent();

        if (comp instanceof Exportable) {
            exportable = ((Exportable) comp).getExportableComponent();
        } else if (comp instanceof JComponent) {
            exportable = (JComponent) comp;
        }

        return exportable;
    }

    public boolean doSave() {
        return doSaveAs();
    }

    public boolean doSaveAs() {
        FileDialog dialog = new FileDialog(this,
                "Save Template As...",
                FileDialog.SAVE);

        dialog.setVisible(true);
        if (dialog.getFile() == null) {
            // the dialog was cancelled...
            return false;
        }

        File file = new File(dialog.getDirectory(), dialog.getFile());

        try {
            if (writeToFile(file)) {

                clearDirty();
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Unable to save file: " + ioe,
                    "Unable to save file",
                    JOptionPane.ERROR_MESSAGE);
        }

        return true;
    }

    public Action getOpenAction() {
        return openTemplateAction;
    }

    private final AbstractAction openTemplateAction = new AbstractAction("Apply Template...") {
        private static final long serialVersionUID = 2450459627280385426L;

        public void actionPerformed(ActionEvent ae) {
            doApplyTemplate();
        }
    };

    public Action getSaveAction() {
        return saveAsAction;
    }

    public Action getSaveAsAction() {
        return saveAsAction;
    }

    private final AbstractAction saveAsAction = new AbstractAction("Save Template As...") {
        private static final long serialVersionUID = 2424923366448459342L;

        public void actionPerformed(ActionEvent ae) {
            doSaveAs();
        }
    };

    public Action getImportAction() {
        return importAlignmentAction;
    }

    protected AbstractAction importAlignmentAction = new AbstractAction("Import Data...") {
        private static final long serialVersionUID = 3217702096314745005L;

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            doImport();
        }
    };

    public Action getExportAction() {
        return generateAction;
    }

    protected AbstractAction generateAction = new AbstractAction("Generate BEAST File...", gearIcon) {
        private static final long serialVersionUID = -5329102618630268783L;

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            doGenerate();
        }
    };

}
