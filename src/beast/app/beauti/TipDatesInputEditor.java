package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.app.draw.BEASTObjectInputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.util.Log;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.TipDatesRandomWalker;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;

public class TipDatesInputEditor extends BEASTObjectInputEditor {

    public TipDatesInputEditor(BeautiDoc doc) {
        super(doc);
    }
    private static final long serialVersionUID = 1L;

    DateFormat dateFormat = DateFormat.getDateInstance();

    @Override
    public Class<?> type() {
        return Tree.class;
    }
    Tree tree;
    TraitSet traitSet;
    JComboBox<TraitSet.Units> unitsComboBox;
    JComboBox<String> relativeToComboBox;
    List<String> taxa;
    Object[][] tableData;
    JTable table;
    String m_sPattern = ".*(\\d\\d\\d\\d).*";
    JScrollPane scrollPane;
    List<Taxon> taxonsets;

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        this.itemNr = itemNr;
        if (itemNr >= 0) {
            tree = (Tree) ((List<?>) input.get()).get(itemNr);
        } else {
            tree = (Tree) input.get();
        }
        if (tree != null) {
            try {
                m_input = ((BEASTInterface) tree).getInput("trait");
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            m_beastObject = tree;
            traitSet = tree.getDateTrait();

            Box box = Box.createVerticalBox();

            JCheckBox useTipDates = new JCheckBox("Use tip dates", traitSet != null);
            useTipDates.addActionListener(e -> {
                    JCheckBox checkBox = (JCheckBox) e.getSource();
                    try {
                        if (checkBox.isSelected()) {
                            if (traitSet == null) {
                                traitSet = new TraitSet();
                                traitSet.initByName("traitname", "date",
                                        "taxa", tree.getTaxonset(),
                                        "value", "");
                                traitSet.setID("dateTrait.t:" + BeautiDoc.parsePartition(tree.getID()));
                            }
                            tree.setDateTrait(traitSet);
                        } else {
                            tree.setDateTrait(null);
                        }

                        refreshPanel();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                });
            Box box2 = Box.createHorizontalBox();
            box2.add(useTipDates);
            box2.add(Box.createGlue());
            box.add(box2);

            if (traitSet != null) {
                box.add(createButtonBox());
                box.add(createListBox());
                box.add(createSamplingBox());
            }
            add(box);
        }
    } // init
    final static int NO_TIP_SAMPLING = 0;
    final static int SAMPLE_TIPS_SAME_PRIOR = 1;
    final static int SAMPLE_TIPS_MULTIPLE_PRIOR = 2;
    final static String ALL_TAXA = "all";
    int m_iMode = NO_TIP_SAMPLING;

    private Component createSamplingBox() {
        Box samplingBox = Box.createHorizontalBox();
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"no tips sampling", "sample tips from taxon set:"});// ,"sample tips with individual priors"});

        // determine mode
        m_iMode = NO_TIP_SAMPLING;
        // count nr of TipDateScalers with weight > 0
        String treeID = tree.getID();
        String operatorID = "allTipDatesRandomWalker.t:" + treeID.substring(treeID.lastIndexOf(":") + 1);
        TipDatesRandomWalker operator = (TipDatesRandomWalker) doc.pluginmap.get(operatorID);
        if (operator != null && operator.m_pWeight.get() > 0) {
            m_iMode = SAMPLE_TIPS_SAME_PRIOR;
        }

        m_iMode = Math.min(m_iMode, 2);
        comboBox.setSelectedIndex(m_iMode);
        comboBox.addActionListener(e -> {
                selectMode(e);
            });
        samplingBox.add(comboBox);

        taxonsets = new ArrayList<>();
        Taxon allTaxa = new Taxon();
        allTaxa.setID(ALL_TAXA);
        taxonsets.add(allTaxa);
        List<String> taxonSetIDs = new ArrayList<>();
        taxonSetIDs.add(ALL_TAXA);
        for (Taxon taxon : doc.taxaset.values()) {
            if (taxon instanceof TaxonSet) {
                taxonsets.add(taxon);
                taxonSetIDs.add(taxon.getID());
            }
        }
        JComboBox<String> comboBox2 = new JComboBox<String>(taxonSetIDs.toArray(new String[]{}));
        
        if (operator == null) {
        	comboBox.setEnabled(false);
        	comboBox2.setEnabled(false);
        } else {
	        // find TipDatesSampler and set TaxonSet input
	        Taxon set = operator.m_taxonsetInput.get();
	        if (set != null) {
	            int i = taxonSetIDs.indexOf(set.getID());
	            comboBox2.setSelectedIndex(i);
	        }
	
	        comboBox2.addActionListener(e -> {
	                selectTaxonSet(e);
	            });
        }
        samplingBox.add(comboBox2);

        return samplingBox;
    }

    private void selectTaxonSet(ActionEvent e) {
        @SuppressWarnings("unchecked")
		JComboBox<String> comboBox = (JComboBox<String>) e.getSource();
        String taxonSetID = (String) comboBox.getSelectedItem();
        Taxon taxonset = null;;
        for (Taxon taxon : taxonsets) {
            if (taxon.getID().equals(taxonSetID)) {
                taxonset = taxon;
                break;
            }
        }

        if (taxonset.getID().equals(ALL_TAXA)) {
            taxonset = null;
        }
        try {
            // find TipDatesSampler and set TaxonSet input

            String treeID = tree.getID();
            String operatorID = "allTipDatesRandomWalker.t:" + treeID.substring(treeID.lastIndexOf(":") + 1);
            TipDatesRandomWalker operator = (TipDatesRandomWalker) doc.pluginmap.get(operatorID);
            Log.warning.println("treeID = " + treeID);
            Log.warning.println("operatorID = " + operatorID);
            Log.warning.println("operator = " + operator);
            operator.m_taxonsetInput.setValue(taxonset, operator);

//            for (BEASTObject beastObject : traitSet.outputs) {
//                if (beastObject instanceof Tree) {
//                    for (BEASTObject beastObject2 : beastObject.outputs) {
//                        if (beastObject2 instanceof TipDatesScaler) {
//                            TipDatesScaler operator = (TipDatesScaler) beastObject2;
//                            operator.m_taxonsetInput.setValue(taxonset, operator);
//                        }
//                    }
//                }
//            }
//
//            // TODO: find MRACPriors and set TaxonSet inputs
//            for (BEASTObject beastObject : traitSet.outputs) {
//                if (beastObject instanceof Tree) {
//                    for (BEASTObject beastObject2 : beastObject.outputs) {
//                        if (beastObject2 instanceof MRCAPrior) {
//                            MRCAPrior prior = (MRCAPrior) beastObject2;
//                            if (prior.m_bOnlyUseTipsInput.get()) {
//                                prior.m_taxonset.setValue(taxonset, prior);
//                            }
//                        }
//                    }
//                }
//            }
        } catch (Exception ex) {
            // TODO: handle exception
            ex.printStackTrace();
        }
    }

    private void selectMode(ActionEvent e) {
        JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
        m_iMode = comboBox.getSelectedIndex();
        try {
            // clear
            for (Object beastObject : traitSet.getOutputs()) {
                if (beastObject instanceof Tree) {
                    for (Object beastObject2 : BEASTInterface.getOutputs(beastObject)) {
                        if (beastObject2 instanceof TipDatesRandomWalker) {
                            TipDatesRandomWalker operator = (TipDatesRandomWalker) beastObject2;
                            switch (m_iMode) {
                                case NO_TIP_SAMPLING:
                                    operator.m_pWeight.setValue(0.0, operator);
                                    break;
                                case SAMPLE_TIPS_SAME_PRIOR:
                                    if (operator.getID().contains("allTipDatesRandomWalker")) {
                                        operator.m_pWeight.setValue(1.0, operator);
                                    } else {
                                        operator.m_pWeight.setValue(0.0, operator);
                                    }
                                    break;
                                case SAMPLE_TIPS_MULTIPLE_PRIOR:
                                    if (operator.getID().contains("allTipDatesRandomWalker")) {
                                        operator.m_pWeight.setValue(0.0, operator);
                                    } else {
                                        operator.m_pWeight.setValue(0.1, operator);
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TODO: handle exception
        }
    }

    private Component createListBox() {
        taxa = traitSet.taxaInput.get().asStringList();
        String[] columnData = new String[]{"Name", "Date", "Height"};
        tableData = new Object[taxa.size()][3];
        convertTraitToTableData();
        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        table = new JTable(tableData, columnData) {
            private static final long serialVersionUID = 1L;

            // method that induces table row shading
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.lightGray);
                } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(new Color(237, 243, 255));
                } else {
                    comp.setBackground(Color.white);
                }
                return comp;
            }
        };

        // set up editor that makes sure only doubles are accepted as entry
        // and only the Date column is editable.
        table.setDefaultEditor(Object.class, new TableCellEditor() {
            JTextField m_textField = new JTextField();
            int m_iRow,
                    m_iCol;

            @Override
            public boolean stopCellEditing() {
                table.removeEditor();
                String text = m_textField.getText();
//                try {
//                    Double.parseDouble(text);
//                } catch (Exception e) {
//                	try {
//                		Date.parse(text);
//                	} catch (Exception e2) {
//                        return false;
//					}
//                }
                tableData[m_iRow][m_iCol] = text;
                convertTableDataToTrait();
                convertTraitToTableData();
                return true;
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return table.getSelectedColumn() == 1;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow, int iCol) {
                if (!isSelected) {
                    return null;
                }
                m_iRow = iRow;
                m_iCol = iCol;
                m_textField.setText((String) value);
                return m_textField;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return false;
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
            }

            @Override
            public Object getCellEditorValue() {
                return null;
            }

            @Override
            public void cancelCellEditing() {
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
            }
        });
        table.setRowHeight(24);
        scrollPane = new JScrollPane(table);

// AJD: This ComponentListener breaks the resizing of the tip dates table, so I have removed it.
//        scrollPane.addComponentListener(new ComponentListener() {
//            @Override
//            public void componentShown(ComponentEvent e) {}
//
//            @Override
//            public void componentResized(ComponentEvent e) {
//                Component c = (Component) e.getSource();
//                while (c.getParent() != null && !(c.getParent() instanceof JSplitPane)) {
//                    c = c.getParent();
//                }
//                if (c.getParent() != null) {
//                    Dimension preferredSize = c.getSize();
//                    preferredSize.height = Math.max(preferredSize.height - 170, 0);
//                    preferredSize.width = Math.max(preferredSize.width - 25, 0);
//                    scrollPane.setPreferredSize(preferredSize);
//                } else if (doc.getFrame() != null) {
//                    Dimension preferredSize = doc.getFrame().getSize();
//                    preferredSize.height = Math.max(preferredSize.height - 170, 0);
//                    preferredSize.width = Math.max(preferredSize.width - 25, 0);
//                    scrollPane.setPreferredSize(preferredSize);
//                }
//            }
//
//            @Override
//            public void componentMoved(ComponentEvent e) {}
//
//            @Override
//            public void componentHidden(ComponentEvent e) {}
//        });

        return scrollPane;
    } // createListBox

    /* synchronise table with data from traitSet BEASTObject */
    private void convertTraitToTableData() {
        for (int i = 0; i < tableData.length; i++) {
            tableData[i][0] = taxa.get(i);
            tableData[i][1] = "0";
            tableData[i][2] = "0";
        }
        String[] traits = traitSet.traitsInput.get().split(",");
        for (String trait : traits) {
            trait = trait.replaceAll("\\s+", " ");
            String[] strs = trait.split("=");
            if (strs.length != 2) {
                break;
                //throw new Exception("could not parse trait: " + trait);
            }
            String taxonID = normalize(strs[0]);
            int taxonIndex = taxa.indexOf(taxonID);
//            if (iTaxon < 0) {
//                throw new Exception("Trait (" + taxonID + ") is not a known taxon. Spelling error perhaps?");
//            }
            if (taxonIndex >= 0) {
                tableData[taxonIndex][1] = normalize(strs[1]);
                tableData[taxonIndex][0] = taxonID;
            } else {
            	Log.warning.println("WARNING: File contains taxon " + taxonID + " that cannot be found in alignment");
            }
        }
        if (traitSet.traitNameInput.get().equals(TraitSet.DATE_BACKWARD_TRAIT)) {
            Double minDate = Double.MAX_VALUE;
            for (int i = 0; i < tableData.length; i++) {
                minDate = Math.min(minDate, parseDate((String) tableData[i][1]));
            }
            for (int i = 0; i < tableData.length; i++) {
                tableData[i][2] = parseDate((String) tableData[i][1]) - minDate;
            }
        } else {
            Double maxDate = 0.0;
            for (int i = 0; i < tableData.length; i++) {
                maxDate = Math.max(maxDate, parseDate((String) tableData[i][1]));
            }
            for (int i = 0; i < tableData.length; i++) {
                tableData[i][2] = maxDate - parseDate((String) tableData[i][1]);
            }
        }

        if (table != null) {
            for (int i = 0; i < tableData.length; i++) {
                table.setValueAt(tableData[i][1], i, 1);
                table.setValueAt(tableData[i][2], i, 2);
            }
        }
    } // convertTraitToTableData

    private double parseDate(String str) {
        // default, try to interpret the string as a number
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            // does not look like a number, try parsing it as a date
                if (str.matches(".*[a-zA-Z].*")) {
                    str = str.replace('/', '-');
                }

            //try {

                // unfortunately this deprecated date parser is the most flexible around at the moment...
                long time = Date.parse(str);
                Date date = new Date(time);

                // AJD
                // Ideally we would use a non-deprecated method like this one instead but it seems to have
                // far less support for different date formats.
                // for example it fails on "12-Oct-2014"
                //dateFormat.setLenient(true);
                //Date date = dateFormat.parse(str);

                Calendar calendar = dateFormat.getCalendar();
                calendar.setTime(date);

                // full year (e.g 2015)
                int year = calendar.get(Calendar.YEAR);
                double days = calendar.get(Calendar.DAY_OF_YEAR);

                double daysInYear = 365.0;

                if (calendar instanceof GregorianCalendar &&(((GregorianCalendar) calendar).isLeapYear(year))) {
                    daysInYear = 366.0;
                }

                double dateAsDecimal = year + days/daysInYear;

                return dateAsDecimal;
            //}
            //catch (ParseException e1) {
            //    System.err.println("*** WARNING: Failed to parse '" + str + "' as date using dateFormat " + dateFormat);
            //}
        }
        //return 0;
    } // parseStrings

    private String normalize(String str) {
        if (str.charAt(0) == ' ') {
            str = str.substring(1);
        }
        if (str.endsWith(" ")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * synchronise traitSet BEAST object with table data
     */
    private void convertTableDataToTrait() {
        String trait = "";
        for (int i = 0; i < tableData.length; i++) {
            trait += taxa.get(i) + "=" + tableData[i][1];
            if (i < tableData.length - 1) {
                trait += ",\n";
            }
        }
        try {
            traitSet.traitsInput.setValue(trait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * create box with comboboxes for selectin units and trait name *
     */
    private Box createButtonBox() {
        Box buttonBox = Box.createHorizontalBox();

        JLabel label = new JLabel("Dates specified as: ");
        label.setMaximumSize(MAX_SIZE);//new Dimension(1024, 22));
        buttonBox.add(label);
        unitsComboBox = new JComboBox<>(TraitSet.Units.values());
        unitsComboBox.setSelectedItem(traitSet.unitsInput.get());
        unitsComboBox.addActionListener(e -> {
                String selected = unitsComboBox.getSelectedItem().toString();
                try {
                    traitSet.unitsInput.setValue(selected, traitSet);
                    //System.err.println("Traitset is now: " + m_traitSet.m_sUnits.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        unitsComboBox.setMaximumSize(MAX_SIZE);//new Dimension(1024, 22));
        buttonBox.add(unitsComboBox);

        relativeToComboBox = new JComboBox<>(new String[]{"Since some time in the past", "Before the present"});
        if (traitSet.traitNameInput.get().equals(TraitSet.DATE_BACKWARD_TRAIT)) {
            relativeToComboBox.setSelectedIndex(1);
        } else {
            relativeToComboBox.setSelectedIndex(0);
        }
        relativeToComboBox.addActionListener(e -> {
                String selected = TraitSet.DATE_BACKWARD_TRAIT;
                if (relativeToComboBox.getSelectedIndex() == 0) {
                    selected = TraitSet.DATE_FORWARD_TRAIT;
                }
                try {
                    traitSet.traitNameInput.setValue(selected, traitSet);
                    Log.warning.println("Relative position is now: " + traitSet.traitNameInput.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                convertTraitToTableData();
            });
        relativeToComboBox.setMaximumSize(MAX_SIZE);//new Dimension(1024, 20));
        buttonBox.add(relativeToComboBox);
        buttonBox.add(Box.createGlue());

        JButton guessButton = new JButton("Guess");
        guessButton.setName("Guess");
        guessButton.addActionListener(e -> {
                GuessPatternDialog dlg = new GuessPatternDialog(null, m_sPattern);
                dlg.allowAddingValues();
                String trait = "";
                switch (dlg.showDialog("Guess dates")) {
                    case canceled:
                        return;
                    case trait:
                        trait = dlg.getTrait();
                        break;
                    case pattern:
                        for (String taxon : taxa) {
                            String match = dlg.match(taxon);
                            if (match == null) {
                                return;
                            }
                            double date = parseDate(match);
                            if (trait.length() > 0) {
                                trait += ",";
                            }
                            trait += taxon + "=" + date;
                        }
                        break;
                }
                try {
                    traitSet.traitsInput.setValue(trait, traitSet);
                    convertTraitToTableData();
                    convertTableDataToTrait();
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            });
        buttonBox.add(guessButton);


        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
                try {
                    traitSet.traitsInput.setValue("", traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            });
        buttonBox.add(clearButton);

        return buttonBox;
    } // createButtonBox
}
