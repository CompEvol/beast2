/*
 * File ModelBuilder.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
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
package beast.app.draw;



//import com.itextpdf.awt.PdfGraphics2D;
//import com.itextpdf.text.pdf.PdfContentByte;
//import com.itextpdf.text.pdf.PdfWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import beast.app.util.Utils;
import beast.core.BEASTInterface;
import beast.evolution.alignment.Sequence;
import beast.util.PackageManager;
import beast.util.Randomizer;
import beast.util.XMLProducer;

/**
 * program for drawing BEAST 2.0 models *
 */

public class ModelBuilder extends JPanel implements ComponentListener {
    /**
     * for serialisation
     */
    static final long serialVersionUID = 1L;

    /**
     * File extension for XML format containing all graphical details (colour,
     * position, etc) NB This is not the extension for GenerationD model files.
     */
    static public final String FILE_EXT = ".xml";
    final public static String ICONPATH = "beast/app/draw/icons/";

    /**
     * contains the Panel
     */
    JScrollPane m_jScrollPane;
    public DrawPanel g_panel;

    final static int MODE_SELECT = 0;
    final static int MODE_MOVE = 1;
    final static int MODE_ARROW = 7;

    final static int MODE_FUNCTION = 9;

    Shape m_drawShape = null;
    Rectangle m_selectRect = null;
    int m_nMode = MODE_SELECT;

    boolean m_bViewOperators = true;
    boolean m_bViewLoggers = true;
    boolean m_bViewSequences = true;
    boolean m_bViewState = true;

    //	boolean m_bViewOperators = false;
//	boolean m_bViewLoggers = false;
//	boolean m_bViewSequences = false;
//	boolean m_bViewState = false;
    boolean m_bRelax = false;

    // flag to indicate the model can be edited.
    // set to false for read only view of the model, e.g. from beauti
    boolean m_bIsEditable = true;
    

    public void setEditable(boolean isEditable) {
        m_bIsEditable = isEditable;
    }

    /**
     * number of seconds to 'relax' after loading a file *
     */
    int m_nRelaxSeconds = 10;
    /**
     * menu item indicating whether to relax or not *
     */
    JCheckBoxMenuItem m_viewRelax;

    Action a_new = new ActionNew();
    Action a_load = new ActionLoad();
    Action a_save = new ActionSave();
    Action a_saveas = new ActionSaveAs();
    ActionExport a_export = new ActionExport();
    ActionPrint a_print = new ActionPrint();
    Action a_quit = new ActionQuit();

    Action a_undo = new ActionUndo();
    Action a_redo = new ActionRedo();
    Action a_selectall = new ActionSelectAll();
    Action a_delnode = new ActionDeleteNode();
    Action a_cutnode = new ActionCutNode();
    Action a_copynode = new ActionCopyNode();
    Action a_pastenode = new ActionPasteNode();
    Action a_group = new ActionCollapse();
    // Action a_ungroup = new ActionUngroup();

    Action a_select = new ActionSelect();
    Action a_arrow = new ActionArrow();
    Action a_function = new ActionFunction();

    Action a_fillcolor = new ActionFillColor();
    Action a_pencolor = new ActionPenColor();
    Action a_tofront = new ActionToFront();
    Action a_forward = new ActionForward();
    Action a_toback = new ActionToBack();
    Action a_backward = new ActionBackward();

    Action a_alignleft = new ActionAlignLeft();
    Action a_alignright = new ActionAlignRight();
    Action a_aligntop = new ActionAlignTop();
    Action a_alignbottom = new ActionAlignBottom();
    Action a_centerhorizontal = new ActionCenterHorizontal();
    Action a_centervertical = new ActionCenterVertical();
    Action a_spacehorizontal = new ActionSpaceHorizontal();
    Action a_spacevertical = new ActionSpaceVertical();

    Action a_about = new ActionAbout();

    Action a_relax = new ActionRelax();
    Action a_viewOperators = new ActionViewOperators();
    Action a_viewLoggers = new ActionViewLoggers();
    Action a_viewSequences = new ActionViewSequences();
    Action a_layout = new ActionLayout();


    ClipBoard m_clipboard = new ClipBoard();
    /**
     * name of current file, used for saving (as opposed to saveAs) *
     */
    String m_sFileName = "";
    /**
     * the document in the model/view pattern *
     */
    public Document m_doc = new Document();
    /**
     * current directory for opening files *
     */
    String m_sDir = System.getProperty("user.dir");

    public ModelBuilder() {

    } // GBDraw c'tor

    class ClipBoard {
        String m_sText = null;

        public ClipBoard() {
            if (a_pastenode != null) {
                a_pastenode.setEnabled(false);
            }
        }

        public boolean hasText() {
            return m_sText != null;
        }

        public String getText() {
            return m_sText;
        }

        public void setText(String text) {
            m_sText = text;
            a_pastenode.setEnabled(true);
        }
    } // class ClipBoard

    Selection m_Selection = new Selection();


    ExtensionFileFilter ef1 = new ExtensionFileFilter(".xml", "BEAST files");
    ExtensionFileFilter ef2 = new ExtensionFileFilter(".gif", "GIF images");
    ExtensionFileFilter ef3 = new ExtensionFileFilter(".jpg", "JPG images");
    ExtensionFileFilter ef4 = new ExtensionFileFilter(".bmp", "BMP images");
    ExtensionFileFilter ef5 = new ExtensionFileFilter(".png", "PNG images");

    boolean validateModel() {
        int status = m_doc.isValidModel();
        if (status == Document.STATUS_OK) {
            return true;
        }
        String msg = "<html>Document is not valid: ";
        switch (status) {
            case Document.STATUS_CYCLE:
                msg += "there is a cycle in the model.";
                break;
            case Document.STATUS_EMPTY_MODEL:
                msg += "The model is empty, there is nothing to save.";
                break;
            case Document.STATUS_NOT_RUNNABLE:
                msg += "there is no top level runnable item in the model (e.g. an MCMC node).";
                break;
            case Document.STATUS_ORPHANS_IN_MODEL:
                msg += "there are orphaned items in the model (i.e. beastObjects that have no parents).";
                break;
        }
        msg += "<br>Do you still want to try to save the model?</html>";
        if (JOptionPane.showConfirmDialog(this, msg, "Model not valid",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }

    class ActionSave extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389110859355156L;

        public ActionSave() {
            super("Save", "Save Graph", "save", KeyEvent.VK_S);
        } // c'tor

        public ActionSave(String name, String toolTipText, String icon,
                          int acceleratorKey) {
            super(name, toolTipText, icon, acceleratorKey);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            if (!m_sFileName.equals("")) {
                if (!validateModel()) {
                    return;
                }
                saveFile(m_sFileName);
                m_doc.isSaved();
            } else {
                if (saveAs()) {
                    m_doc.isSaved();
                }
            }
        } // actionPerformed

        boolean saveAs() {
            if (!validateModel()) {
                return false;
            }
            JFileChooser fc = new JFileChooser(m_sDir);
            fc.addChoosableFileFilter(ef1);
            fc.setDialogTitle("Save Model As");
            if (!m_sFileName.equals("")) {
                // can happen on actionQuit
                fc.setSelectedFile(new File(m_sFileName));
            }
            int rval = fc.showSaveDialog(g_panel);

            if (rval == JFileChooser.APPROVE_OPTION) {
                // System.out.println("Saving to file \""+
                // f.getAbsoluteFile().toString()+"\"");
                String fileName = fc.getSelectedFile().toString();
                if (fileName.lastIndexOf('/') > 0) {
                    m_sDir = fileName.substring(0, fileName.lastIndexOf('/'));
                }
                if (!fileName.endsWith(FILE_EXT))
                    fileName = fileName.concat(FILE_EXT);
                saveFile(fileName);
                return true;
            }
            return false;
        } // saveAs

        protected void saveFile(String fileName) {
            try {
                FileWriter outfile = new FileWriter(fileName);
                outfile.write(m_doc.toXML());
                outfile.close();
                m_sFileName = fileName;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } // saveFile
    } // class ActionSave

    class ActionPrint extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389001859354L;
        boolean m_bIsPrinting = false;

        public ActionPrint() {
            super("Print", "Print Graph", "print", KeyEvent.VK_P);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintable(g_panel);
            if (printJob.printDialog())
                try {
                    m_bIsPrinting = true;
                    printJob.print();
                    m_bIsPrinting = false;
                } catch (PrinterException pe) {
                    m_bIsPrinting = false;
                }
        } // actionPerformed

        public boolean isPrinting() {
            return m_bIsPrinting;
        }

    } // class ActionPrint

    class ActionSaveAs extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389110859354L;

        public ActionSaveAs() {
            super("Save As", "Save Graph As", "saveas", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            saveAs();
        } // actionPerformed
    } // class ActionSaveAs

    abstract class MyFileFilter extends FileFilter {
        @Override
		public boolean accept(File f) {
            return f.isDirectory()
                    || f.getName().toLowerCase().endsWith(getExtention());
        }

        abstract public String getExtention();
    }

    class ActionExport extends MyAction {
        boolean m_bIsExporting = false;
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -3027642085935519L;

        public ActionExport() {
            super("Export", "Export to graphics file", "export", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_bIsExporting = true;

//            JFileChooser fc = new JFileChooser(m_sDir);
//            fc.addChoosableFileFilter(new MyFileFilter() {
//                public String getExtention() {
//                    return ".bmp";
//                }
//
//                public String getDescription() {
//                    return "Bitmap files (*.bmp)";
//                }
//            });
//            fc.addChoosableFileFilter(new MyFileFilter() {
//                public String getExtention() {
//                    return ".jpg";
//                }
//
//                public String getDescription() {
//                    return "JPEG bitmap files (*.jpg)";
//                }
//            });
//            fc.addChoosableFileFilter(new MyFileFilter() {
//                public String getExtention() {
//                    return ".png";
//                }
//
//                public String getDescription() {
//                    return "PNG bitmap files (*.png)";
//                }
//            });
//            fc.setDialogTitle("Export Image As");
//            int rval = fc.showSaveDialog(g_panel);
//            if (rval == JFileChooser.APPROVE_OPTION) {
//	            String fileName = fc.getSelectedFile().toString();
            try {

                File file = Utils.getSaveFile("Export image (type determined by extention)", new File(m_sDir), "Image files", "png", "bmp", "jpg", "svg", "pdf");

                if (file != null) {
                    String fileName = file.getAbsolutePath();
                    if (fileName.lastIndexOf('/') > 0) {
                        m_sDir = fileName.substring(0, fileName.lastIndexOf('/'));
                    }
                    if (fileName != null && !fileName.equals("")) {
//                    if (!fileName.toLowerCase().endsWith(".png")
//                            && fileName.toLowerCase().endsWith(".jpg")
//                            && fileName.toLowerCase().endsWith(".bmp")
//                            && fileName.toLowerCase().endsWith(".svg")) {
//                        fileName += ((MyFileFilter) fc.getFileFilter())
//                                .getExtention();
//                    }

                        if (fileName.toLowerCase().endsWith(".pdf")) {
                        	JOptionPane.showMessageDialog(null, "Not implemented yet");
//                        	com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
//                        	PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fileName));
//                        	doc.setPageSize(new com.itextpdf.text.Rectangle(g_panel.getWidth(), g_panel.getHeight()));
//                        	doc.open();
//                        	PdfContentByte cb = writer.getDirectContent();
//                        	Graphics2D g2d = new PdfGraphics2D(cb, g_panel.getWidth(), g_panel.getHeight());
//                        	 
//                        	g_panel.paint(g2d);
//
//                        	g2d.dispose();
//                        	doc.close();
                            m_bIsExporting = false;
                            repaint();
                        	return;
                        } else if (fileName.toLowerCase().endsWith(".png")
                                || fileName.toLowerCase().endsWith(".jpg")
                                || fileName.toLowerCase().endsWith(".bmp")) {
                            BufferedImage bi;
                            Graphics g;
                            bi = new BufferedImage(g_panel.getWidth(), g_panel
                                    .getHeight(), BufferedImage.TYPE_INT_RGB);
                            g = bi.getGraphics();
                            g.setPaintMode();
                            g.setColor(getBackground());
                            g.fillRect(0, 0, g_panel.getWidth(), g_panel
                                    .getHeight());
                            g_panel.printAll(g);
                            try {
                                if (fileName.toLowerCase().endsWith(".png")) {
                                    ImageIO.write(bi, "png", new File(fileName));
                                } else if (fileName.toLowerCase().endsWith(".jpg")) {
                                    ImageIO.write(bi, "jpg", new File(fileName));
                                } else if (fileName.toLowerCase().endsWith(".bmp")) {
                                    ImageIO.write(bi, "bmp", new File(fileName));
                                }
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, fileName
                                        + " was not written properly: "
                                        + e.getMessage());
                                e.printStackTrace();
                            }
                            m_bIsExporting = false;
                            repaint();
                            return;
                        } else if (fileName.toLowerCase().endsWith(".svg")) {
                            writeSVG(fileName);
                            m_bIsExporting = false;
                            repaint();
                            return;
                        }
                        JOptionPane.showMessageDialog(null, "Extention of file "
                                + fileName
                                + " not recognized as png,bmp,jpg or svg file");
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Something went wrong while exporting image: " + e.getMessage());
            }

            m_bIsExporting = false;
            repaint();
        }

        private void writeSVG(String fileName) throws FileNotFoundException {
            PrintStream out = new PrintStream(fileName);
            out.println("<?xml version='1.0'?>\n" + "<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN'\n"
                    + "  'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n"
                    + "<svg xmlns='http://www.w3.org/2000/svg' version='1.1'\n" + "      width='" + getWidth()
                    + "' height='" + getHeight() + "' viewBox='0 0 " + getWidth() + " " + getHeight() + "'>\n"
                    + "<rect fill='#fff' width='" + getWidth() + "' height='" + getHeight() + "'/>");
            for (Shape shape : m_doc.m_objects) {
                shape.toSVG(out);
            }
            out.println("</svg>");
        }

        public boolean isExporting() {
            return m_bIsExporting;
        }
    } // class ActionExport

    class ActionQuit extends ActionSave {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038911085935515L;

        public ActionQuit() {
            super("Exit", "Exit Program", "exit", KeyEvent.VK_F4);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            if (!m_doc.m_bIsSaved) {
                int result = JOptionPane.showConfirmDialog(null,
                        "Drawing changed. Do you want to save it?",
                        "Save before closing?",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (result == JOptionPane.YES_OPTION) {
                    if (!saveAs()) {
                        return;
                    }
                }
            }
            System.exit(0);
        }
    } // class ActionQuit

    class ActionNew extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038911085935515L;

        public ActionNew() {
            super("New", "New Network", "new", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_sFileName = "";
            m_doc = new Document();
            m_Selection.setDocument(m_doc);
            updateStatus();
            m_doc.clearUndoStack();
            m_Selection.clear();
            m_drawShape = null;
        }
    } // class ActionNew

    class ActionLoad extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038911085935515L;

        public ActionLoad() {
            super("Load", "Load Graph", "open", KeyEvent.VK_O);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            JFileChooser fc = new JFileChooser(m_sDir);
            fc.addChoosableFileFilter(ef1);
            fc.setDialogTitle("Load Graph");
            int rval = fc.showOpenDialog(g_panel);

            if (rval == JFileChooser.APPROVE_OPTION) {
                String fileName = fc.getSelectedFile().toString();
                if (fileName.lastIndexOf('/') > 0) {
                    m_sDir = fileName.substring(0, fileName.lastIndexOf('/'));
                }
                g_panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                m_doc.loadFile(fileName);
                m_sFileName = fileName;
                g_panel.repaint();
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                setDrawingFlag();
                g_panel.repaint();
                g_panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    } // class ActionLoad

    class ActionUndo extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -3038910085935519L;

        public ActionUndo() {
            super("Undo", "Undo", "undo", KeyEvent.VK_Z);
            setEnabled(false);
        } // c'tor

        @Override
		public boolean isEnabled() {
            return m_doc.canUndo();
        }

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.undo();
            m_Selection.clear();
            setDrawingFlag();
            updateStatus();
        }
    } // ActionUndo

    class ActionRedo extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -4038910085935519L;

        public ActionRedo() {
            super("Redo", "Redo", "redo", KeyEvent.VK_Y);
            setEnabled(false);
        } // c'tor

        @Override
		public boolean isEnabled() {
            return m_doc.canRedo();
        }

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.redo();
            m_Selection.clear();
            setDrawingFlag();
            updateStatus();
        }
    } // ActionRedo

    class ActionSelectAll extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038912085935519L;

        public ActionSelectAll() {
            super("Select All", "Select All", "selectall", KeyEvent.VK_A);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_Selection.clear();
            for (int i = 0; i < m_doc.m_objects.size(); i++) {
                if (m_doc.m_objects.get(i).m_bNeedsDrawing) {
                    m_Selection.m_Selection.add(new Integer(i));
                }
            }
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionSelectAll

    class ActionDeleteNode extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038912085935519L;

        public ActionDeleteNode() {
            super("Delete Node", "Delete Node", "delnode", KeyEvent.VK_DELETE);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.deleteShapes(m_Selection.m_Selection);
            m_Selection.clear();
            updateStatus();
        }
    } // class ActionDeleteNode

    class ActionCopyNode extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038732085935519L;

        public ActionCopyNode() {
            super("Copy", "Copy Nodes", "copy", KeyEvent.VK_C);
            setEnabled(false);
        } // c'tor

        public ActionCopyNode(String name, String toolTipText, String icon,
                              int acceleratorKey) {
            super(name, toolTipText, icon, acceleratorKey);
        } // c'rot

        @Override
		public void actionPerformed(ActionEvent ae) {
            copy();
        }

        public void copy() {
            if (m_Selection.hasSelection()) {
                String xml = "<doc>\n";
                for (int i : m_Selection.m_Selection) {
                    xml += m_doc.m_objects.get(i).getXML() + "\n";
                }
                xml += "</doc>\n";
                m_clipboard.setText(xml);
            }
        } // copy
    } // class ActionCopyNode

    class ActionCutNode extends ActionCopyNode {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038822085935519L;

        public ActionCutNode() {
            super("Cut", "Cut Nodes", "cut", KeyEvent.VK_X);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            copy();
            m_doc.deleteShapes(m_Selection.m_Selection);
            m_Selection.clear();
            updateStatus();
        }
    } // class ActionCutNode

    class ActionPasteNode extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -2038732085935519L;

        public ActionPasteNode() {
            super("Paste", "Paste Nodes", "paste", KeyEvent.VK_V);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            try {
                m_doc.pasteShape(m_clipboard.getText());
                updateStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
		public boolean isEnabled() {
            return m_clipboard.hasText();
        }
    } // class ActionPasteNode

    class ActionCollapse extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionCollapse() {
            super("Collapse shapes", "Collapse", "collapse", -1);
            KeyStroke keyStroke = KeyStroke.getKeyStroke("G");
            putValue(Action.ACCELERATOR_KEY, keyStroke);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.collapse(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionGroup
    // class ActionUngroup extends MyAction {
    // private static final long serialVersionUID = -1;
    // public ActionUngroup() {
    // super("Ungroup shapes", "Ungroup", "ungroup", "Ctrl G");
    // setEnabled(false);
    // } // c'tor
    // public void actionPerformed(ActionEvent ae) {
    // m_doc.ungroup(m_Selection);
    // m_Selection.refreshTracker();
    // updateStatus();
    // }
    // } // class ActionUngroup

    class ActionSelect extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionSelect() {
            super("Select", "Select", "select", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_nMode = MODE_SELECT;
        }
    } // class ActionSelect

    class ActionArrow extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionArrow() {
            super("Arrow", "Arrow", "arrow", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_nMode = MODE_ARROW;
        }
    } // class ActionArrow

    class ActionFunction extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionFunction() {
            super("Function", "Function", "function", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_nMode = MODE_FUNCTION;
        }
    } // class ActionFunction

    class ActionFillColor extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionFillColor() {
            super("Fill color", "Fill color", "fillcolor", -1);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            Shape shape = m_doc.m_objects.get(m_Selection
                    .getSingleSelection());
            Color color = JColorChooser.showDialog(g_panel,
                    "Select Fill color", shape.getFillColor());
            if (color != null) {
                m_doc.setFillColor(color, m_Selection);
                g_panel.repaint();
            }
        }
    } // class ActionFillColor

    class ActionPenColor extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionPenColor() {
            super("Pen color", "Pen color", "pencolor", -1);
            setEnabled(false);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            Shape shape = m_doc.m_objects.get(m_Selection
                    .getSingleSelection());
            Color color = JColorChooser.showDialog(g_panel,
                    "Select Fill color", shape.getFillColor());
            if (color != null) {
                m_doc.setPenColor(color, m_Selection);
                g_panel.repaint();
            }

        }
    } // class ActionPenColor

    class ActionToFront extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -1;

        public ActionToFront() {
            super("Bring to front", "To front", "tofront", KeyEvent.VK_PLUS);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.toFront(m_Selection);
            g_panel.repaint();
        }
    } // class ActionToFront

    class ActionForward extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -1;

        public ActionForward() {
            super("Bring forward", "Forward", "forward", -1);
            KeyStroke keyStroke = KeyStroke.getKeyStroke("plus");
            putValue(Action.ACCELERATOR_KEY, keyStroke);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.forward(m_Selection);
            g_panel.repaint();
        }
    } // class ActionForward

    class ActionToBack extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -1;

        public ActionToBack() {
            super("Bring to back", "To back", "toback", KeyEvent.VK_MINUS);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.toBack(m_Selection);
            g_panel.repaint();
        }
    } // class ActionToBack

    class ActionBackward extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -1;

        public ActionBackward() {
            super("Bring backward", "Backward", "backward", -1);
            KeyStroke keyStroke = KeyStroke.getKeyStroke("minus");
            putValue(Action.ACCELERATOR_KEY, keyStroke);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.backward(m_Selection);
            g_panel.repaint();
        }
    } // class ActionBackward

    class ActionAlignLeft extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -3138642085935519L;

        public ActionAlignLeft() {
            super("Align Left", "Align Left", "alignleft", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.alignLeft(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionAlignLeft

    class ActionAlignRight extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -4238642085935519L;

        public ActionAlignRight() {
            super("Align Right", "Align Right", "alignright", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.alignRight(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionAlignRight

    class ActionAlignTop extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -5338642085935519L;

        public ActionAlignTop() {
            super("Align Top", "Align Top", "aligntop", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.alignTop(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionAlignTop

    class ActionAlignBottom extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -6438642085935519L;

        public ActionAlignBottom() {
            super("Align Bottom", "Align Bottom", "alignbottom", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.alignBottom(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionAlignBottom

    class ActionCenterHorizontal extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -7538642085935519L;

        public ActionCenterHorizontal() {
            super("Center Horizontal", "Center Horizontal", "centerhorizontal",
                    -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.centreHorizontal(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionCenterHorizontal

    class ActionCenterVertical extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -8638642085935519L;

        public ActionCenterVertical() {
            super("Center Vertical", "Center Vertical", "centervertical", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.centreVertical(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionCenterVertical

    class ActionSpaceHorizontal extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -9738642085935519L;

        public ActionSpaceHorizontal() {
            super("Space Horizontal", "Space Horizontal", "spacehorizontal", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.spaceHorizontal(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionSpaceHorizontal

    class ActionSpaceVertical extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -838642085935519L;

        public ActionSpaceVertical() {
            super("Space Vertical", "Space Vertical", "spacevertical", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_doc.spaceVertical(m_Selection);
            m_Selection.refreshTracker();
            updateStatus();
        }
    } // class ActionSpaceVertical

    class ActionAbout extends MyAction {
        /**
         * for serialisation
         */
        private static final long serialVersionUID = -20389110859353L;

        public ActionAbout() {
            super("About", "Help about", "about", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            "GenerationD Draw Tool\nRemco Bouckaert\nrrb@xm.co.nz\n2010",
                            "About Message", JOptionPane.PLAIN_MESSAGE);
        }
    } // class ActionAbout


    class ActionRelax extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionRelax() {
            super("Relax", "Relax positions", "about", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_bRelax = !m_bRelax;
            m_viewRelax.setState(m_bRelax);
            g_panel.repaint();
        }
    } // class ActionRelax

    class ActionViewLoggers extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewLoggers() {
            super("View loggers", "View loggers", "about", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_bViewLoggers = !m_bViewLoggers;
            setDrawingFlag();
            g_panel.repaint();
        }
    } // class ActionViewLoggers

    class ActionViewOperators extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewOperators() {
            super("View Operators", "View Operators", "viewoperators", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_bViewOperators = !m_bViewOperators;
            setDrawingFlag();
            g_panel.repaint();
        }
    } // class ActionViewOperators

    class ActionViewSequences extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionViewSequences() {
            super("View Sequences", "View Sequences", "viewsequences", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            m_bViewSequences = !m_bViewSequences;
            setDrawingFlag();
            g_panel.repaint();
        }
    } // class ActionViewSequences

    class ActionLayout extends MyAction {
        private static final long serialVersionUID = -1;

        public ActionLayout() {
            super("Layout", "Layout graph", "layout", -1);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent e) {
            g_panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            m_doc.layout();
            m_doc.adjustArrows();
            g_panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            repaint();
            g_panel.repaint();
        }
    } // class ActionViewSequences


    void updateStatus() {
        a_undo.setEnabled(m_doc.canUndo());
        a_redo.setEnabled(m_doc.canRedo());
        int selectionSize = m_Selection.m_Selection.size();
        boolean hasSelection = (selectionSize > 0);
        boolean hasGroupSelection = (selectionSize > 1);

        a_delnode.setEnabled(hasSelection);
        a_copynode.setEnabled(hasSelection);
        a_cutnode.setEnabled(hasSelection);
        a_pastenode.setEnabled(m_clipboard.hasText());

        a_fillcolor.setEnabled(hasSelection);
        a_pencolor.setEnabled(hasSelection);

        a_forward.setEnabled(hasSelection);
        a_backward.setEnabled(hasSelection);
        a_tofront.setEnabled(hasSelection);
        a_toback.setEnabled(hasSelection);

        a_group.setEnabled(hasSelection);

        a_alignbottom.setEnabled(hasGroupSelection);
        a_aligntop.setEnabled(hasGroupSelection);
        a_alignleft.setEnabled(hasGroupSelection);
        a_alignright.setEnabled(hasGroupSelection);
        a_spacehorizontal.setEnabled(hasGroupSelection);
        a_spacevertical.setEnabled(hasGroupSelection);
        a_centerhorizontal.setEnabled(hasGroupSelection);
        a_centervertical.setEnabled(hasGroupSelection);

        g_panel.repaint();
    }

    public JToolBar m_jTbTools;
    public JToolBar m_jTbTools2;

    public void init() {
        m_Selection.setDocument(m_doc);
        int size = UIManager.getFont("Label.font").getSize();
        setSize(2048 * size / 13, 2048 * size / 13);
        g_panel = new DrawPanel();
        m_jScrollPane = new JScrollPane(g_panel);
        makeToolbar();
        makeMenuBar();
        addComponentListener(this);
        this.setLayout(new BorderLayout());
        this.add(m_jScrollPane, BorderLayout.CENTER);
        g_panel.setPreferredSize(getSize());
    }

    void makeToolbar() {
        m_jTbTools = new JToolBar();
        m_jTbTools.setFloatable(false);
        // m_jTbTools.setLayout(new GridBagLayout());
        m_jTbTools.add(a_new);
        m_jTbTools.add(a_save);
        m_jTbTools.add(a_load);
        m_jTbTools.addSeparator(new Dimension(2, 2));
        m_jTbTools.add(a_cutnode);
        m_jTbTools.add(a_copynode);
        m_jTbTools.add(a_pastenode);
        m_jTbTools.addSeparator(new Dimension(2, 2));
        m_jTbTools.add(a_undo);
        m_jTbTools.add(a_redo);
        m_jTbTools.addSeparator(new Dimension(2, 2));

        m_jTbTools.add(a_select);
        m_jTbTools.add(a_function);
        // m_jTbTools.add(a_rect);
        // m_jTbTools.add(a_rrect);
        // m_jTbTools.add(a_ellipse);
        // m_jTbTools.add(a_line);
        // m_jTbTools.add(a_poly);
        m_jTbTools.add(a_arrow);
        m_jTbTools.addSeparator(new Dimension(2, 2));
        m_jTbTools.add(a_alignleft);
        m_jTbTools.add(a_alignright);
        m_jTbTools.add(a_aligntop);
        m_jTbTools.add(a_alignbottom);
        m_jTbTools.add(a_centerhorizontal);
        m_jTbTools.add(a_centervertical);
        m_jTbTools.add(a_spacehorizontal);
        m_jTbTools.add(a_spacevertical);

        setLayout(new BorderLayout());
        // add("North", m_jTbTools);
        // add("Center", g_panel);

        m_jTbTools2 = new JToolBar();
        m_jTbTools2.setFloatable(false);
        m_jTbTools2.add(a_relax);
        m_jTbTools2.add(a_viewLoggers);
        m_jTbTools2.add(a_viewOperators);
        m_jTbTools2.add(a_viewSequences);
        m_jTbTools2.add(a_layout);

    } // init

    boolean needsDrawing(BEASTInterface beastObject) {
        if (beastObject == null) {
            return true;
        }
        if (!m_bViewOperators && beastObject instanceof beast.core.Operator) {
            return false;
        }
        if (!m_bViewLoggers && beastObject instanceof beast.core.Logger) {
            return false;
        }
        if (!m_bViewSequences && beastObject instanceof Sequence) {
            return false;
        }
        if (!m_bViewState && beastObject instanceof beast.core.State) {
            return false;
        }
        return true;
    }

    public void setDrawingFlag() {
        for (int i = 0; i < m_doc.m_objects.size(); i++) {
            Shape shape = m_doc.m_objects.get(i);
            shape.m_bNeedsDrawing = false;
            if (shape.m_bNeedsDrawing) {
                shape.m_bNeedsDrawing = true;
            }
            if (shape instanceof BEASTObjectShape) {
                BEASTInterface beastObject = ((BEASTObjectShape) shape).m_beastObject;
                if (needsDrawing(beastObject)) {
                    shape.m_bNeedsDrawing = true;
                }
            } else if (shape instanceof InputShape) {
                BEASTObjectShape beastObjectShape = ((InputShape) shape).m_beastObjectShape;
                if (beastObjectShape != null) {
                    if (needsDrawing(beastObjectShape.m_beastObject)) {
                        shape.m_bNeedsDrawing = true;
                    }
                } else {
                    shape.m_bNeedsDrawing = true;
                }
            } else if (shape instanceof Arrow) {
                Shape tail = ((Arrow) shape).m_tailShape;
                boolean needsDrawing = true;
                if (tail instanceof BEASTObjectShape) {
                    needsDrawing = needsDrawing(((BEASTObjectShape) tail).m_beastObject);
                }
                if (needsDrawing) {
                    Shape head = ((Arrow) shape).m_headShape;
                    if (head instanceof InputShape) {
                        BEASTObjectShape beastObjectShape = ((InputShape) head).m_beastObjectShape;
                        if (beastObjectShape != null) {
                            needsDrawing = needsDrawing(beastObjectShape.m_beastObject);
                        }
                    }
                    if (needsDrawing) {
                        shape.m_bNeedsDrawing = true;
                    }
                }
            } else {
                shape.m_bNeedsDrawing = true;
            }
        }
    }

    class DrawPanel extends JPanel implements Printable {
        /**
         * for serialisation
         */
        static final long serialVersionUID = 1L;

        public DrawPanel() {
            setBackground(Color.white);
            addMouseMotionListener(new GBDrawMouseMotionListener());
            addMouseListener(new GBDrawMouseEventListener());
        }

        @Override
		public void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr;
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            rh.put(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHints(rh);
            g.setBackground(Color.WHITE);
            Rectangle r = g.getClipBounds();
            g.clearRect(r.x, r.y, r.width, r.height);

            m_doc.adjustInputs();
            for (int i = 0; i < m_doc.m_objects.size(); i++) {
                Shape shape = m_doc.m_objects.get(i);
                if (shape.m_bNeedsDrawing) {
                    shape.draw(g, this);
                }
            }
            if (!a_export.isExporting() && !a_print.isPrinting()) {
                if (m_drawShape != null) {
                    m_drawShape.draw(g, this);
                }
                if (m_Selection.m_tracker != null) {
                    g.setColor(Color.BLACK);
                    for (int i = 0; i < m_Selection.m_tracker.size(); i++) {
                        TrackPoint p = m_Selection.m_tracker
                                .get(i);
                        g.fillRect(p.m_nX - 4, p.m_nY - 4, 8, 8);
                    }
                }
                if (m_selectRect != null) {
                    g.setXORMode(Color.green);
                    g.draw(m_selectRect);
                }
            }

            if (m_bRelax) {
                m_doc.relax(true);
                repaint();
            }
            m_jScrollPane.revalidate();
        } // paintComponent

        /**
         * implementation of Printable, used for printing
         *
         * @see Printable
         */
        @Override
		public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
            if (pageIndex > 0) {
                return (NO_SUCH_PAGE);
            } else {
                Graphics2D g2d = (Graphics2D) g;
                g2d.translate(pageFormat.getImageableX(), pageFormat
                        .getImageableY());
                // Turn off double buffering
                paint(g2d);
                // Turn double buffering back on
                return (PAGE_EXISTS);
            }
        } // print

        /**
         * position clicked on
         */
        int m_nPosX = 0, m_nPosY = 0, m_nPoint;
        boolean m_bIsMoving = false;

        class GBDrawMouseMotionListener extends MouseMotionAdapter {

            @Override
			public void mouseDragged(MouseEvent me) {
                switch (m_nMode) {
                    case MODE_SELECT:
                        if (m_bIsMoving || m_Selection.isSingleSelection()
                                && m_Selection.intersects(me.getX(), me.getY())) {
                            m_bIsMoving = true;
                            Shape shape = m_doc.m_objects.get(m_Selection
                                    .getSingleSelection());
                            if (getCursor().getType() == Cursor.DEFAULT_CURSOR) {
                                // simple move operation
                                m_doc.moveShape(m_nPosX, m_nPosY, me.getX(), me
                                        .getY(), m_Selection.getSingleSelection());
                                m_Selection.m_tracker = shape.getTracker();
                            } else {
                                // resize/move point operation
                                m_doc.movePoint(m_nPoint, m_nPosX, m_nPosY, me
                                        .getX(), me.getY(), m_Selection
                                        .getSingleSelection());
                                m_Selection.m_tracker = shape.getTracker();
                            }
                            updateStatus();
                            break;
                        } else {
                            if (m_selectRect == null) {
                                if (m_Selection.intersects(me.getX(), me.getY())) {
                                    // move selection
                                    m_nMode = MODE_MOVE;
                                    // m_doc.addUndoGroupAction(m_Selection);
                                    m_nPosX = me.getX();
                                    m_nPosY = me.getY();
                                } else {
                                    // start new Rectangle
                                    m_selectRect = new Rectangle(me.getX(), me
                                            .getY(), 1, 1);
                                }
                            } else {
                                m_selectRect.width = me.getX() - m_selectRect.x;
                                m_selectRect.height = me.getY() - m_selectRect.y;
                            }
                            g_panel.repaint();
                        }
                        break;
                    case MODE_MOVE:
                        int dX = me.getX() - m_nPosX;
                        int dY = me.getY() - m_nPosY;
                        m_doc.moveShapes(dX, dY, m_Selection.m_Selection);
                        m_Selection.offset(dX, dY);
                        m_doc.adjustArrows();
                        m_nPosX = me.getX();
                        m_nPosY = me.getY();
                        repaint();
                        break;
                    case MODE_FUNCTION:
                        beast.app.draw.BEASTObjectShape function = (beast.app.draw.BEASTObjectShape) m_drawShape;

                        if (m_drawShape == null) {
                            function = new beast.app.draw.BEASTObjectShape();
                            function.m_x = me.getX();
                            function.m_y = me.getY();
                            function.m_w = 1;
                            function.m_h = 1;
                            m_drawShape = function;
                        } else {
                            function.m_w = me.getX() - function.m_x;
                            function.m_h = me.getY() - function.m_y;
                        }
                        m_Selection.m_tracker = function.getTracker();
                        g_panel.repaint();
                        break;
                    case MODE_ARROW:
                        Arrow arrow = (Arrow) m_drawShape;
                        if (m_drawShape == null) {
                            int selectionIndex = -1;
                            for (int i = 0; selectionIndex < 0
                                    && i < m_doc.m_objects.size(); i++) {
                                Shape shape = m_doc.m_objects.get(i);
                                if (shape.m_bNeedsDrawing
                                        && shape.intersects(me.getX(), me.getY())) {
                                    m_nPosX = shape.offsetX(me.getX());
                                    m_nPosY = shape.offsetY(me.getY());
                                    selectionIndex = i;
                                }
                            }
                            if (selectionIndex < 0) {
                                return;
                            }
                            Shape shape = m_doc.m_objects.get(selectionIndex);
                            if (shape instanceof Arrow) {
                                return;
                            }
                            if (shape instanceof InputShape) {
                                shape = ((InputShape) shape).m_beastObjectShape;
                            }
                            arrow = new Arrow((BEASTObjectShape) shape, me.getX(), me
                                    .getY());
                            arrow.m_w = 1;
                            arrow.m_h = 1;
                            m_drawShape = arrow;
                        } else {
                            arrow.setHead(me.getX() - arrow.m_x, me.getY()
                                    - arrow.m_y);
                        }
                        // m_Selection.m_tracker = arrow.getTracker();
                        g_panel.repaint();
                        break;
                }
            } // mouseDragged

            @Override
			public void mouseMoved(MouseEvent me) {
                if (m_Selection.m_tracker != null
                        && m_Selection.isSingleSelection()) {
                    if (getCursor().getType() != Cursor.DEFAULT_CURSOR) {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                    for (int i = 0; i < m_Selection.m_tracker.size(); i++) {
                        TrackPoint p = m_Selection.m_tracker
                                .get(i);
                        if (me.getX() > p.m_nX - 4 && me.getX() < p.m_nX + 4
                                && me.getY() > p.m_nY - 4
                                && me.getY() < p.m_nY + 4) {
                            m_nPoint = i;
                            m_nPosX = me.getX() - p.m_nX;
                            m_nPosY = me.getY() - p.m_nY;
                            setCursor(new Cursor(p.m_nCursor));
                            return;
                        }
                    }
                    Shape shape = m_doc.m_objects.get(m_Selection
                            .getSingleSelection());
                    m_nPosX = me.getX() - shape.m_x;
                    m_nPosY = me.getY() - shape.m_y;
                } else {
                    if (getCursor().getType() != Cursor.DEFAULT_CURSOR) {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                }

                // set up tool tip text
                for (int i = m_doc.m_objects.size() - 1; i >= 0; i--) {
                    Shape shape = m_doc.m_objects.get(i);
                    if (shape.m_bNeedsDrawing
                            && shape.intersects(me.getX(), me.getY())) {
                        if (shape instanceof BEASTObjectShape) {
                            BEASTObjectShape beastObject = (BEASTObjectShape) shape;
                            try {
                                String toolTip = "<html>";
                                for (InputShape input : beastObject.m_inputs) {
                                    toolTip += input.getLongLabel() + "<br>";
                                }
                                toolTip += "</html>";
                                setToolTipText(toolTip);
                            } catch (IllegalArgumentException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        return;
                    }
                }

            } // mouseMoved

        } // class GBDrawMouseMotionListener

        class GBDrawMouseEventListener extends MouseAdapter {
            @Override
			public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    handleDoubleClick(me);
                    return;
                }

                if (me.getButton() == MouseEvent.BUTTON3) {
                    handleRightClick(me);
                    return;
                }
                // otherwise, assume left click
                switch (m_nMode) {
                    case MODE_SELECT:
                        int selectionIndex = -1;
                        for (int i = m_doc.m_objects.size() - 1; selectionIndex < 0 && i >= 0; i--) {
                            Shape shape = m_doc.m_objects.get(i);
                            if (shape.m_bNeedsDrawing && !(shape instanceof Arrow) && shape.intersects(me.getX(), me.getY())) {
                                m_nPosX = shape.offsetX(me.getX());
                                m_nPosY = shape.offsetY(me.getY());
                                selectionIndex = i;
                            }
                        }
                        if (selectionIndex < 0) {
                            for (int i = m_doc.m_objects.size() - 1; selectionIndex < 0 && i >= 0; i--) {
                                Shape shape = m_doc.m_objects.get(i);
                                if (shape.m_bNeedsDrawing && shape.intersects(me.getX(), me.getY())) {
                                    m_nPosX = shape.offsetX(me.getX());
                                    m_nPosY = shape.offsetY(me.getY());
                                    selectionIndex = i;
                                }
                            }
                            if (selectionIndex < 0) {
                                return;
                            }
                        }
                        if ((me.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                            m_Selection.toggleSelection(selectionIndex);
                        } else if ((me.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                            m_Selection.addToSelection(selectionIndex);
                        } else {
                            m_Selection.clear();
                            m_Selection.setSingleSelection(selectionIndex);
                        }
                        updateStatus();
                        break;
                }
            } // mouseClicked

            void handleDoubleClick(MouseEvent me) {
                // find the shape intersecting the mouse pointer
                try {
                    for (int i = m_doc.m_objects.size() - 1; i >= 0; i--) {
                        Shape shape = m_doc.m_objects.get(i);
                        if (shape.m_bNeedsDrawing
                                && shape.intersects(me.getX(), me.getY())) {
                            if (shape instanceof InputShape) {
                                // resolve the associated input
                                InputShape ellipse = (InputShape) shape;
                                String input = ellipse.getInputName();
                                BEASTInterface beastObject = ellipse.getBEASTObject();
                                if (beastObject.isPrimitive(input)) {
                                    String valueString = "";
                                    if (beastObject.getInputValue(input) != null) {
                                        valueString = beastObject.getInputValue(input)
                                                .toString();
                                    }
                                    valueString = JOptionPane.showInputDialog(input
                                            + ":", valueString);
                                    if (valueString != null) {
                                        m_doc.setInputValue(ellipse.getPluginShape(), input, valueString);
                                        // ellipse.setLabel(input + "=" +
                                        // beastObject.getInputValue(input).toString());
                                        g_panel.repaint();
                                    }
                                }
                            }
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // handleDoubleClick

            void handleRightClick(MouseEvent me) {
                if (!m_bIsEditable) {
                    return;
                }
                JPopupMenu popupMenu = new JPopupMenu("Choose a value");

                if (!m_Selection.hasSelection()) {
                    int selectionIndex = -1;
                    for (int i = 0; selectionIndex < 0
                            && i < m_doc.m_objects.size(); i++) {
                        Shape shape = m_doc.m_objects.get(i);
                        if (shape.m_bNeedsDrawing
                                && shape.intersects(me.getX(), me.getY())) {
                            m_nPosX = shape.offsetX(me.getX());
                            m_nPosY = shape.offsetY(me.getY());
                            m_Selection.addToSelection(i);
                            break;
                        }
                    }
                }

                JMenuItem addNodeItem = new JMenuItem("Change label");
                ActionListener label = new ActionListener() {
                    @Override
					public void actionPerformed(ActionEvent ae) {
                        Shape shape = m_Selection.getSingleSelectionShape();
                        String name = (String) JOptionPane.showInputDialog(
                                null, shape.getID(), "New label",
                                JOptionPane.OK_CANCEL_OPTION, null, null, shape
                                .getID());
                        if (name == null || name.equals("")) {
                            return;
                        }
                        while (name.contains("\\n")) {
                            int i = name.indexOf("\\n");
                            name = name.substring(0, i - 1) + '\n'
                                    + name.substring(i + 2);
                        }
                        m_doc.setID(name, m_Selection.getSingleSelection());
                        repaint();
                    }
                };
                addNodeItem.addActionListener(label);
                addNodeItem.setEnabled(m_Selection.isSingleSelection());
                popupMenu.add(addNodeItem);

                JMenuItem isFilledMenu = new JMenuItem("Fill object");
                if (m_Selection.isSingleSelection()) {
                    Shape shape = m_Selection.getSingleSelectionShape();
                    if (shape.isFilled()) {
                        isFilledMenu = new JMenuItem("Don't fill object");
                    }
                }
                isFilledMenu.addActionListener(ae -> {
                        m_doc.toggleFilled(m_Selection.getSingleSelection());
                        g_panel.repaint();
                    });
                isFilledMenu.setEnabled(m_Selection.isSingleSelection());
                popupMenu.add(isFilledMenu);

                JMenuItem propertiesItem = new JMenuItem("Properties");
                propertiesItem.addActionListener(ae -> {
                        Shape shape = m_Selection.getSingleSelectionShape();
                        if (shape instanceof BEASTObjectShape) {
                            BEASTInterface beastObject = ((BEASTObjectShape) shape).m_beastObject;

                            List<BEASTInterface> beastObjects = new ArrayList<>();
                            for (Shape shape2 : m_doc.m_objects) {
                                if (shape2 instanceof BEASTObjectShape) {
                                    beastObjects.add(((BEASTObjectShape) shape2).m_beastObject);
                                }
                            }
                            BEASTObjectDialog dlg = new BEASTObjectDialog(beastObject, beastObject.getClass(), beastObjects, null);
                            if (dlg.showDialog()) {
                                // add newly created Plug-ins
                                int newShapes = 0;
                                if (beastObjects.size() < BEASTObjectPanel.g_plugins.size()) {
                                    for (BEASTInterface beastObject2 : BEASTObjectPanel.g_plugins.values()) {
                                        if (!beastObjects.contains(beastObject2)) {
                                            try {
                                                newShapes++;
                                                Shape shape2 = new BEASTObjectShape(beastObject2, m_doc);
                                                shape2.m_x = 10;
                                                shape2.m_y = newShapes * 50;
                                                shape2.m_w = 80;
                                                m_doc.addNewShape(shape2);
                                                updateStatus();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                                // re-attach all arrows
                                m_Selection.clear();
                                m_doc.recalcArrows();
                                m_doc.adjustArrows();
                            }
                            repaint();
                        }
                    });
                propertiesItem.setEnabled(m_Selection.isSingleSelection());
                popupMenu.add(propertiesItem);

                JMenuItem saveAsItem = new JMenuItem("Save as");
                saveAsItem.addActionListener(ae -> {
                        Shape shape = m_Selection.getSingleSelectionShape();
                        BEASTInterface beastObject = ((BEASTObjectShape) shape).m_beastObject;
                        JFileChooser fc = new JFileChooser(m_sDir);
                        fc.addChoosableFileFilter(ef1);
                        fc.setDialogTitle("Save BEASTObject As");
                        if (!m_sFileName.equals("")) {
                            // can happen on actionQuit
                            fc.setSelectedFile(new File(m_sFileName));
                        }
                        int rval = fc.showSaveDialog(g_panel);

                        if (rval == JFileChooser.APPROVE_OPTION) {
                            // System.out.println("Saving to file \""+
                            // f.getAbsoluteFile().toString()+"\"");
                            String fileName = fc.getSelectedFile().toString();
                            if (fileName.lastIndexOf('/') > 0) {
                                m_sDir = fileName.substring(0, fileName
                                        .lastIndexOf('/'));
                            }
                            if (!fileName.endsWith(FILE_EXT))
                                fileName = fileName.concat(FILE_EXT);
                            try {
                                FileWriter outfile = new FileWriter(fileName);
                                outfile.write(new XMLProducer().modelToXML(beastObject));
                                outfile.close();
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null,
                                        "Something went wrong when writing the file: "
                                                + e.getClass().getName() + " "
                                                + e.getMessage());
                            }
                            m_sFileName = fileName;
                        }
                        repaint();
                    });
                saveAsItem.setEnabled(m_Selection.isSingleSelection());
                popupMenu.add(saveAsItem);

                popupMenu.setLocation(me.getX(), me.getY());
                popupMenu.show(g_panel, me.getX(), me.getY());
            } // handleRightClick

            @Override
			public void mouseReleased(MouseEvent me) {
                if (m_drawShape != null) {
                    m_drawShape.normalize();
                }
                m_bIsMoving = false;
                boolean added = true;
                switch (m_nMode) {
                    case MODE_SELECT:
                        if (m_selectRect != null) {

                            if ((me.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0
                                    && (me.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
                                // fresh selection
                                m_Selection.clear();
                            }

                            for (int i = 0; i < m_doc.m_objects.size(); i++) {
                                if (m_doc.m_objects.get(i)
                                        .intersects(m_selectRect)
                                        && m_doc.m_objects.get(i).m_bNeedsDrawing) {
                                    if ((me.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                                        m_Selection.toggleSelection(i);
                                    } else {
                                        m_Selection.addToSelection(i);
                                    }
                                }
                            }
                            m_selectRect = null;
                            updateStatus();
                        }
                        return;
                    case MODE_MOVE:
                        m_nMode = MODE_SELECT;
                        updateStatus();
                        return;
                    case MODE_FUNCTION:
                        BEASTObjectShape function = (BEASTObjectShape) m_drawShape;
                        if (function == null) {
                            return;
                        }
                        if (function.m_w > 0 && function.m_h > 0) {
                            String functionClassName = (String) JOptionPane
                                    .showInputDialog(g_panel, "Select a constant",
                                            "select", JOptionPane.PLAIN_MESSAGE,
                                            null, m_doc.m_sPlugInNames, null);
                            if (functionClassName != null) {
                                try {
                                    function.init(functionClassName, m_doc);
                                    m_doc.addNewShape(function);
                                    updateStatus();
                                } catch (Exception e) {
                                    // TODO: handle exception
                                    e.printStackTrace();
                                }
                            } else {
                                added = false;
                            }
                        } else {
                            added = false;
                        }
                        break;
                    case MODE_ARROW:
                        Arrow arrow = (Arrow) m_drawShape;
                        if (m_drawShape != null) {
                            int selectionIndex = -1;
                            for (int i = 0; selectionIndex < 0
                                    && i < m_doc.m_objects.size(); i++) {
                                Shape shape = m_doc.m_objects.get(i);
                                if (shape.m_bNeedsDrawing
                                        && shape.intersects(me.getX(), me.getY())) {
                                    m_nPosX = shape.offsetX(me.getX());
                                    m_nPosY = shape.offsetY(me.getY());
                                    selectionIndex = i;
                                }
                            }
                            if (selectionIndex < 0) {
                                m_drawShape = null;
                                repaint();
                                return;
                            }
                            Shape target = m_doc.m_objects.get(selectionIndex);
                            if (!(target instanceof InputShape)) {
                                // only connect to inputs of functions
                                m_drawShape = null;
                                repaint();
                                return;
                            }

                            // check no cycle is introduced
                            InputShape target2 = (InputShape) target;
                            if (m_doc.isAscendant(arrow.m_tailShape.m_beastObject,
                                    target2.getBEASTObject())) {
                                JOptionPane
                                        .showMessageDialog(null,
                                                "Cannot make this connection since this creates a cycle in the model");
                                return;
                            }

                            try {
                                // try to add connection
                                // this links the input of the target beastObject
                                // to the source beastObject. If types mismatch,
                                // an exception is thrown and no arrow added.
                                arrow.setHead(target2, m_doc.m_objects, m_doc);
                                m_doc.addNewShape(arrow);
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, e.getMessage());
                                repaint();
                            }
                        }
                        break;
                }
                m_drawShape = null;
                m_nMode = MODE_SELECT;
                if (added) {
                    m_Selection.setSingleSelection(m_doc.m_objects.size() - 1);
                    g_panel.repaint();
                } else {
                    m_Selection.clear();
                    g_panel.repaint();
                }
            } // mouseReleased
        } // GBDrawMouseEventListener
    } // class DrawPanel

    public JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        fileMenu.add(a_new);
        fileMenu.add(a_load);
        fileMenu.add(a_save);
        fileMenu.add(a_saveas);
        fileMenu.addSeparator();
        fileMenu.add(a_export);
        fileMenu.add(a_print);
        fileMenu.addSeparator();
        fileMenu.add(a_quit);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        menuBar.add(editMenu);
        editMenu.add(a_undo);
        editMenu.add(a_redo);
        editMenu.addSeparator();
        editMenu.add(a_selectall);
        editMenu.add(a_delnode);
        editMenu.add(a_cutnode);
        editMenu.add(a_copynode);
        editMenu.add(a_pastenode);
        editMenu.addSeparator();
        editMenu.add(a_alignleft);
        editMenu.add(a_alignright);
        editMenu.add(a_aligntop);
        editMenu.add(a_alignbottom);
        editMenu.add(a_centerhorizontal);
        editMenu.add(a_centervertical);
        editMenu.add(a_spacehorizontal);
        editMenu.add(a_spacevertical);
        editMenu.addSeparator();
        editMenu.add(a_group);
        // editMenu.add(a_ungroup);
        editMenu.addSeparator();

        JMenu drawMenu = new JMenu("Draw");
        drawMenu.setMnemonic('D');
        menuBar.add(drawMenu);
        drawMenu.add(a_select);
        drawMenu.add(a_function);
        drawMenu.add(a_arrow);

        JMenu objectMenu = new JMenu("Object");
        objectMenu.setMnemonic('O');
        menuBar.add(objectMenu);
        objectMenu.add(a_fillcolor);
        objectMenu.add(a_pencolor);
        objectMenu.addSeparator();
        objectMenu.add(a_tofront);
        objectMenu.add(a_forward);
        objectMenu.add(a_toback);
        objectMenu.add(a_backward);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
        menuBar.add(viewMenu);
        final JCheckBoxMenuItem viewOperators = new JCheckBoxMenuItem(
                "Show Operators", m_bViewOperators);
        viewOperators.addActionListener(ae -> {
                m_bViewOperators = viewOperators.getState();
                setDrawingFlag();
                g_panel.repaint();
            });
        viewMenu.add(viewOperators);
        final JCheckBoxMenuItem viewLoggers = new JCheckBoxMenuItem(
                "Show Loggers", m_bViewLoggers);
        viewLoggers.addActionListener(ae -> {
                m_bViewLoggers = viewLoggers.getState();
                setDrawingFlag();
                g_panel.repaint();
            });
        viewMenu.add(viewLoggers);
        final JCheckBoxMenuItem viewSequences = new JCheckBoxMenuItem(
                "Show Sequences", m_bViewSequences);
        viewSequences.addActionListener(ae -> {
                m_bViewSequences = viewSequences.getState();
                setDrawingFlag();
                g_panel.repaint();
            });
        viewMenu.add(viewSequences);
        final JCheckBoxMenuItem viewState = new JCheckBoxMenuItem("Show State",
                m_bViewState);
        viewState.addActionListener(ae -> {
                m_bViewState = viewState.getState();
                setDrawingFlag();
                g_panel.repaint();
            });
        viewMenu.add(viewState);
        m_viewRelax = new JCheckBoxMenuItem("Relax", m_bRelax);
        m_viewRelax.addActionListener(ae -> {
                m_bRelax = m_viewRelax.getState();
                g_panel.repaint();
            });
        viewMenu.add(m_viewRelax);

        JMenuItem layoutMenu = new JMenuItem("Layout Visible Items");
        layoutMenu.setMnemonic('L');
        layoutMenu.addActionListener(e -> {
                g_panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                m_doc.layout();
                m_doc.adjustArrows();
                repaint();
                g_panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            });
        viewMenu.add(layoutMenu);
        viewMenu.addSeparator();

        final JCheckBoxMenuItem viewAllInputs = new JCheckBoxMenuItem("Show All Inputs",
                m_doc.m_bShowALlInputs);
        viewAllInputs.addActionListener(ae -> {
            	m_doc.m_bShowALlInputs = viewAllInputs.getState();
            	m_doc.reinit();
                setDrawingFlag();
                g_panel.repaint();
            });
        viewMenu.add(viewAllInputs);

        final JCheckBoxMenuItem sanitseIDs = new JCheckBoxMenuItem("Sanitise IDs",
                m_doc.m_bSanitiseIDs);
        sanitseIDs.addActionListener(ae -> {
            	m_doc.m_bSanitiseIDs = sanitseIDs.getState();
                setDrawingFlag();
                g_panel.repaint();
            });
        viewMenu.add(sanitseIDs);
        
        
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        menuBar.add(helpMenu);
        helpMenu.add(a_about);

        return menuBar;
    } // makeMenuBar

    public static void main(String args[]) {
        Randomizer.setSeed(127);
        try {
            PackageManager.loadExternalJars();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFrame f = new JFrame("Model Builder");
        ModelBuilder drawTest = new ModelBuilder();
        drawTest.init();
        JMenuBar menuBar = drawTest.makeMenuBar();
        f.setJMenuBar(menuBar);

        f.add(drawTest.m_jTbTools, BorderLayout.NORTH);
        f.add(drawTest, BorderLayout.CENTER);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.net.URL tempURL = ClassLoader
                .getSystemResource(ModelBuilder.ICONPATH + "/GenerationD.png");
        try {
            f.setIconImage(ImageIO.read(tempURL));
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }
        if (args.length > 0) {
            drawTest.m_doc.loadFile(args[0]);
            drawTest.setDrawingFlag();
        }
        int size = UIManager.getFont("Label.font").getSize();
        f.setSize(800 * size / 13, 600 * size / 13);
        f.setVisible(true);
    } // main

    // implements ComponentListener

    @Override
	public void componentHidden(ComponentEvent e) {
        m_jScrollPane.revalidate();
    }

    // implements ComponentListener

    @Override
	public void componentMoved(ComponentEvent e) {
        m_jScrollPane.revalidate();
    }

    // implements ComponentListener

    @Override
	public void componentResized(ComponentEvent e) {
        m_jScrollPane.revalidate();
    }

    // implements ComponentListener

    @Override
	public void componentShown(ComponentEvent e) {
        m_jScrollPane.revalidate();
    }

} // class ModelBuilder
