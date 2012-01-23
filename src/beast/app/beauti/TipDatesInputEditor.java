package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.app.draw.PluginInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.TipDatesScaler;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.MRCAPrior;

public class TipDatesInputEditor extends PluginInputEditor {
    private static final long serialVersionUID = 1L;


    @Override
    public Class<?> type() {
        return Tree.class;
    }

    Tree tree;
    TraitSet traitSet;
    JComboBox unitsComboBox;
    JComboBox relativeToComboBox;
    List<String> sTaxa;
    Object[][] tableData;
    JTable table;


    @Override
    public void init(Input<?> input, Plugin plugin, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        tree = (Tree) input.get();
        if (tree != null) {
            m_input = tree.m_trait;
            m_plugin = tree;
            traitSet = tree.m_trait.get();

            Box box = createVerticalBox();

            JCheckBox useTipDates = new JCheckBox("Use tip dates", traitSet != null);
            useTipDates.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JCheckBox checkBox = (JCheckBox) e.getSource();
                    try {
                        Container comp = checkBox.getParent();
                        comp.removeAll();
                        if (checkBox.isSelected()) {
                            if (traitSet == null) {
                                traitSet = new TraitSet();
                                traitSet.initByName("traitname", "date",
                                        "taxa", tree.m_taxonset.get(),
                                        "value", "");
                            }
                            m_input.setValue(traitSet, m_plugin);
                            comp.add(checkBox);
                            comp.add(createButtonBox());
                            comp.add(createListBox());
                            comp.add(createSamplingBox());
                        } else {
                            m_input.setValue(null, m_plugin);
                            comp.add(checkBox);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            });
            box.add(useTipDates);

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
        JComboBox comboBox = new JComboBox(new String[]{"no tips sampling", "sample tips with same prior"});// ,"sample tips with individual priors"});

        // determine mode
        m_iMode = NO_TIP_SAMPLING;
        // count nr of TipDateScalers with weight > 0
        for (Plugin plugin : traitSet.outputs) {
            if (plugin instanceof Tree) {
                for (Plugin plugin2 : plugin.outputs) {
                    if (plugin2 instanceof TipDatesScaler) {
                        TipDatesScaler operator = (TipDatesScaler) plugin2;
                        if (operator.m_pWeight.get() > 0) {
                            m_iMode = SAMPLE_TIPS_SAME_PRIOR;
                        }
                    }
                }
            }
        }
        m_iMode = Math.min(m_iMode, 2);
        comboBox.setSelectedIndex(m_iMode);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectMode(e);
            }
        });
        samplingBox.add(comboBox);


        List<Taxon> taxonsets = new ArrayList<Taxon>();
        Taxon allTaxa = new Taxon();
        allTaxa.setID(ALL_TAXA);
        taxonsets.add(allTaxa);
        for (Taxon taxon : doc.taxaset) {
            if (taxon instanceof TaxonSet) {
                taxonsets.add(taxon);
            }
        }
        JComboBox comboBox2 = new JComboBox(taxonsets.toArray());

        // find TipDatesSampler and set TaxonSet input
        for (Plugin plugin : traitSet.outputs) {
            if (plugin instanceof Tree) {
                for (Plugin plugin2 : plugin.outputs) {
                    if (plugin2 instanceof TipDatesScaler) {
                        TipDatesScaler operator = (TipDatesScaler) plugin2;
                        Taxon set = operator.m_taxonsetInput.get();
                        if (set != null) {
                            comboBox2.setSelectedItem(set);
                        }
                    }
                }
            }
        }


        comboBox2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectTaxonSet(e);
            }
        });
        samplingBox.add(comboBox2);

        return samplingBox;
    }

    private void selectTaxonSet(ActionEvent e) {
        JComboBox comboBox = (JComboBox) e.getSource();
        Taxon taxonset = (Taxon) comboBox.getSelectedItem();
        if (taxonset.getID().equals(ALL_TAXA)) {
            taxonset = null;
        }
        try {
            // find TipDatesSampler and set TaxonSet input
            for (Plugin plugin : traitSet.outputs) {
                if (plugin instanceof Tree) {
                    for (Plugin plugin2 : plugin.outputs) {
                        if (plugin2 instanceof TipDatesScaler) {
                            TipDatesScaler operator = (TipDatesScaler) plugin2;
                            operator.m_taxonsetInput.setValue(taxonset, operator);
                        }
                    }
                }
            }

            // TODO: find MRACPriors and set TaxonSet inputs
            for (Plugin plugin : traitSet.outputs) {
                if (plugin instanceof Tree) {
                    for (Plugin plugin2 : plugin.outputs) {
                        if (plugin2 instanceof MRCAPrior) {
                            MRCAPrior prior = (MRCAPrior) plugin2;
                            if (prior.m_bOnlyUseTipsInput.get()) {
                                prior.m_taxonset.setValue(taxonset, prior);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TODO: handle exception
        }
    }

    private void selectMode(ActionEvent e) {
        JComboBox comboBox = (JComboBox) e.getSource();
        m_iMode = comboBox.getSelectedIndex();
        try {
            // clear
            for (Plugin plugin : traitSet.outputs) {
                if (plugin instanceof Tree) {
                    for (Plugin plugin2 : plugin.outputs) {
                        if (plugin2 instanceof TipDatesScaler) {
                            TipDatesScaler operator = (TipDatesScaler) plugin2;
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
        sTaxa = traitSet.m_taxa.get().asStringList();
        String[] columnData = new String[]{"Name", "Date", "Height"};
        tableData = new Object[sTaxa.size()][3];
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
            int m_iRow
                    ,
                    m_iCol;

            @Override
            public boolean stopCellEditing() {
                table.removeEditor();
                String sText = m_textField.getText();
                try {
                    Double.parseDouble(sText);
                } catch (Exception e) {
                    return false;
                }
                tableData[m_iRow][m_iCol] = sText;
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
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    } // createListBox

    /* synchronise table with data from traitSet Plugin */
    private void convertTraitToTableData() {
        for (int i = 0; i < tableData.length; i++) {
            tableData[i][0] = sTaxa.get(i);
            tableData[i][1] = "0";
            tableData[i][2] = "0";
        }
        String[] sTraits = traitSet.m_traits.get().split(",");
        for (String sTrait : sTraits) {
            sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            if (sStrs.length != 2) {
                break;
                //throw new Exception("could not parse trait: " + sTrait);
            }
            String sTaxonID = normalize(sStrs[0]);
            int iTaxon = sTaxa.indexOf(sTaxonID);
//            if (iTaxon < 0) {
//                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
//            }
            tableData[iTaxon][0] = sTaxonID;
            tableData[iTaxon][1] = normalize(sStrs[1]);
        }
        if (traitSet.m_sTraitName.get().equals("date-forward")) {
            for (int i = 0; i < tableData.length; i++) {
                tableData[i][2] = tableData[i][1];
            }
        } else {
            Double fMaxDate = 0.0;
            for (int i = 0; i < tableData.length; i++) {
                fMaxDate = Math.max(fMaxDate, parseDouble((String) tableData[i][1]));
            }
            for (int i = 0; i < tableData.length; i++) {
                tableData[i][2] = fMaxDate - parseDouble((String) tableData[i][1]);
            }
        }

        if (table != null) {
            for (int i = 0; i < tableData.length; i++) {
                table.setValueAt(tableData[i][1], i, 1);
                table.setValueAt(tableData[i][2], i, 2);
            }
        }
    } // convertTraitToTableData

    private double parseDouble(String sStr) {
        // default, try to interpret the string as a number
        try {
            return Double.parseDouble(sStr);
        } catch (NumberFormatException e) {
            // does not look like a number
        }
        return 0;
    } // parseStrings

    private String normalize(String sStr) {
        if (sStr.charAt(0) == ' ') {
            sStr = sStr.substring(1);
        }
        if (sStr.endsWith(" ")) {
            sStr = sStr.substring(0, sStr.length() - 1);
        }
        return sStr;
    }

    /**
     * synchronise traitSet Plugin with table data
     */
    private void convertTableDataToTrait() {
        String sTrait = "";
        for (int i = 0; i < tableData.length; i++) {
            sTrait += sTaxa.get(i) + "=" + tableData[i][1];
            if (i < tableData.length - 1) {
                sTrait += ",\n";
            }
        }
        try {
            traitSet.m_traits.setValue(sTrait, traitSet);
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
        label.setMaximumSize(new Dimension(1024, 20));
        buttonBox.add(label);
        unitsComboBox = new JComboBox(TraitSet.Units.values());
        unitsComboBox.setSelectedItem(traitSet.m_sUnits.get());
        unitsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sSelected = (String) unitsComboBox.getSelectedItem().toString();
                try {
                    traitSet.m_sUnits.setValue(sSelected, traitSet);
                    //System.err.println("Traitset is now: " + m_traitSet.m_sUnits.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        unitsComboBox.setMaximumSize(new Dimension(1024, 20));
        buttonBox.add(unitsComboBox);

        relativeToComboBox = new JComboBox(new String[]{"Since some time in the past", "Before the present"});
        relativeToComboBox.setSelectedItem(traitSet.m_sTraitName.get());
        relativeToComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sSelected = "date-forward";
                if (relativeToComboBox.getSelectedIndex() == 0) {
                    sSelected = "date-backward";
                }
                try {
                    traitSet.m_sTraitName.setValue(sSelected, traitSet);
                    System.err.println("Relative position is now: " + traitSet.m_sTraitName.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                convertTraitToTableData();
            }
        });
        relativeToComboBox.setMaximumSize(new Dimension(1024, 20));
        buttonBox.add(relativeToComboBox);
        buttonBox.add(Box.createGlue());

        JButton guessButton = new JButton("Guess");
        guessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sPattern = ".*(\\d\\d\\d\\d).*";
                sPattern = JOptionPane.showInputDialog(null, "Pattern to match", sPattern);
                if (sPattern == null) {
                    return;
                }
                Pattern pattern = Pattern.compile(sPattern);
                String sTrait = "";
                for (String sTaxon : sTaxa) {
                    Matcher matcher = pattern.matcher(sTaxon);
                    if (matcher.find()) {
                        String sMatch = matcher.group(1);
                        double nDate = Double.parseDouble(sMatch);
                        if (sTrait.length() > 0) {
                            sTrait += ",";
                        }
                        sTrait += sTaxon + "=" + nDate;
                    }
                }
                try {
                    traitSet.m_traits.setValue(sTrait, traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            }
        });
        buttonBox.add(guessButton);


        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    traitSet.m_traits.setValue("", traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
            }
        });
        buttonBox.add(clearButton);

        return buttonBox;
    } // createButtonBox
}
