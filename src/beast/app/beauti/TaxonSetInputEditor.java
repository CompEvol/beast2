package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.app.draw.InputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

public class TaxonSetInputEditor extends InputEditor.Base {
    private static final long serialVersionUID = 1L;
    List<Taxon> m_taxonset;
    List<Taxon> m_lineageset;
    Map<String, String> m_taxonMap;
    JTable m_table;
    DefaultTableModel m_model = new DefaultTableModel();

    JTextField filterEntry;
    String m_sFilter = ".*";
    int m_sortByColumn = 0;
    boolean m_bIsAscending = true;

	public TaxonSetInputEditor(BeautiDoc doc) {
		super(doc);
	}

    @Override
    public Class<?> type() {
        return TaxonSet.class;
    }

    @Override
    public void init(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_input = input;
        m_plugin = plugin;
		this.itemNr = itemNr;
        TaxonSet taxonset = (TaxonSet) m_input.get();
        if (taxonset == null) {
            return;
        }
        List<Taxon> taxonsets = new ArrayList<Taxon>();

        List<Taxon> taxa = taxonset.m_taxonset.get();
        for (Taxon taxon : taxa) {
            taxonsets.add((TaxonSet) taxon);
        }
        add(getContent(taxonsets));
        if (taxa.size() == 1 && taxa.get(0).getID().equals("Beauti2DummyTaxonSet")) {
            taxa.clear();
            try {
                // species is first character of taxon
                guessTaxonSets("(.).*", 0);
                for (Taxon taxonset2 : m_taxonset) {
                    for (Taxon taxon : ((TaxonSet) taxonset2).m_taxonset.get()) {
                        m_lineageset.add(taxon);
                        m_taxonMap.put(taxon.getID(), taxonset2.getID());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            taxonSetToModel();
            modelToTaxonset();
        }
    }

    private Component getContent(List<Taxon> taxonset) {
        m_taxonset = taxonset;
        m_taxonMap = new HashMap<String, String>();
        m_lineageset = new ArrayList<Taxon>();
        for (Taxon taxonset2 : m_taxonset) {
            for (Taxon taxon : ((TaxonSet) taxonset2).m_taxonset.get()) {
                m_lineageset.add(taxon);
                m_taxonMap.put(taxon.getID(), taxonset2.getID());
            }
        }

        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        m_model = new DefaultTableModel();
        m_model.addColumn("Taxon");
        m_model.addColumn("Species/Population");
        taxonSetToModel();

        m_table = new JTable(m_model) {
            private static final long serialVersionUID = 1L;

            // method that induces table row shading
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                // even index, selected or not selected
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.gray);
                } else if (Index_row % 2 == 0) {
                    comp.setBackground(new Color(237, 243, 255));
                } else {
                    comp.setBackground(Color.white);
                }
                return comp;
            }
        };

        // set up editor that makes sure only doubles are accepted as entry
        // and only the Date column is editable.
        m_table.setDefaultEditor(Object.class, new TableCellEditor() {
            JTextField m_textField = new JTextField();
            int m_iRow
                    ,
                    m_iCol;

            @Override
            public boolean stopCellEditing() {
                m_table.removeEditor();
                String sText = m_textField.getText();
                System.err.println(sText);
                m_model.setValueAt(sText, m_iRow, m_iCol);
                // try {
                // Double.parseDouble(sText);
                // } catch (Exception e) {
                // return false;
                // }
                modelToTaxonset();
                return true;
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return m_table.getSelectedColumn() == 1;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow,
                                                         int iCol) {
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
        m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        m_table.getColumnModel().getColumn(0).setPreferredWidth(250);
        m_table.getColumnModel().getColumn(1).setPreferredWidth(250);

        JTableHeader header = m_table.getTableHeader();
        header.addMouseListener(new ColumnHeaderListener());

        JScrollPane pane = new JScrollPane(m_table);
        Box tableBox = Box.createHorizontalBox();
        tableBox.add(Box.createHorizontalGlue());
        tableBox.add(pane);
        tableBox.add(Box.createHorizontalGlue());

        Box box = Box.createVerticalBox();
        box.add(createFilterBox());
        box.add(tableBox);
        box.add(createButtonBox());
        return box;
    }

    private Component createButtonBox() {
        Box buttonBox = Box.createHorizontalBox();

        JButton fillDownButton = new JButton("Fill down");
        fillDownButton.setName("Fill down");
        fillDownButton.setToolTipText("replaces all taxons in selection with the one that is selected at the top");
        fillDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = m_table.getSelectedRows();
                if (rows.length < 2) {
                    return;
                }
                String sTaxon = (String) ((Vector<?>) m_model.getDataVector().elementAt(rows[0])).elementAt(1);
                for (int i = 1; i < rows.length; i++) {
                    m_model.setValueAt(sTaxon, rows[i], 1);
                }
                modelToTaxonset();
            }
        });

        JButton guessButton = new JButton("Guess");
        guessButton.setName("Guess");
        guessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guess();
            }
        });

        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(fillDownButton);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(guessButton);
        buttonBox.add(Box.createHorizontalGlue());
        return buttonBox;
    }

    public class ColumnHeaderListener extends MouseAdapter {
        public void mouseClicked(MouseEvent evt) {
            // The index of the column whose header was clicked
            int vColIndex = m_table.getColumnModel().getColumnIndexAtX(evt.getX());
            if (vColIndex == -1) {
                return;
            }
            if (vColIndex != m_sortByColumn) {
                m_sortByColumn = vColIndex;
                m_bIsAscending = true;
            } else {
                m_bIsAscending = !m_bIsAscending;
            }
            taxonSetToModel();
        }
    }

    private void guess() {
        GuessPatternDialog dlg = new GuessPatternDialog(this, m_sPattern);
        String sPattern = dlg.showDialog("Guess taxon sets");
        if (sPattern != null) {
            try {
                guessTaxonSets(sPattern, 0);
                m_lineageset.clear();
                for (Taxon taxonset2 : m_taxonset) {
                    for (Taxon taxon : ((TaxonSet) taxonset2).m_taxonset.get()) {
                        m_lineageset.add(taxon);
                        m_taxonMap.put(taxon.getID(), taxonset2.getID());
                    }
                }
                taxonSetToModel();
                modelToTaxonset();
                m_sPattern = sPattern;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * guesses taxon sets based on pattern in sRegExp based on the taxa in
     * m_rawData
     */
    public int guessTaxonSets(String sRegexp, int nMinSize) throws Exception {
        m_taxonset.clear();
        HashMap<String, TaxonSet> map = new HashMap<String, TaxonSet>();
        Pattern m_pattern = Pattern.compile(sRegexp);
        Set<Taxon> taxa = new HashSet<Taxon>();
        Set<String> taxonIDs = new HashSet<String>();
        for (Alignment alignment : getDoc().alignments) {
            for (Sequence sequence : alignment.m_pSequences.get()) {
                String sID = sequence.m_sTaxon.get();
                if (!taxonIDs.contains(sID)) {
                    Taxon taxon = new Taxon();
                    // ensure sequence and taxon do not get same ID
                    if (sequence.getID().equals(sequence.m_sTaxon.get())) {
                        sequence.setID("_" + sequence.getID());
                    }
                    taxon.setID(sequence.m_sTaxon.get());
                    taxa.add(taxon);
                    taxonIDs.add(sID);
                }
            }
        }

        for (Taxon taxon : taxa) {
            if (!(taxon instanceof TaxonSet)) {
                Matcher matcher = m_pattern.matcher(taxon.getID());
                if (matcher.find()) {
                    String sMatch = matcher.group(1);
                    try {
                        if (map.containsKey(sMatch)) {
                            TaxonSet set = map.get(sMatch);
                            set.m_taxonset.setValue(taxon, set);
                        } else {
                            TaxonSet set = new TaxonSet();
                            set.setID(sMatch);
                            set.m_taxonset.setValue(taxon, set);
                            map.put(sMatch, set);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        // add taxon sets
        int nIgnored = 0;
        for (TaxonSet set : map.values()) {
            if (set.m_taxonset.get().size() > nMinSize) {
                m_taxonset.add(set);
            } else {
                nIgnored += set.m_taxonset.get().size();
            }
        }
        return nIgnored;
    }

    String m_sPattern = "^(.+)[-_\\. ](.*)$";


    private Component createFilterBox() {
        Box filterBox = Box.createHorizontalBox();
        filterBox.add(new JLabel("filter: "));
        // Dimension size = new Dimension(100,20);
        filterEntry = new JTextField();
        filterEntry.setColumns(20);
        // filterEntry.setMinimumSize(size);
        // filterEntry.setPreferredSize(size);
        // filterEntry.setSize(size);
        filterEntry.setToolTipText("Enter regular expression to match taxa");
        filterEntry.setMaximumSize(new Dimension(1024, 20));
        filterBox.add(filterEntry);
        filterBox.add(Box.createHorizontalGlue());
        filterEntry.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                processFilter();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                processFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                processFilter();
            }

            private void processFilter() {
                String sFilter = ".*" + filterEntry.getText() + ".*";
                try {
                    // sanity check: make sure the filter is legit
                    sFilter.matches(sFilter);
                    m_sFilter = sFilter;
                    taxonSetToModel();
                    m_table.repaint();
                } catch (PatternSyntaxException e) {
                    // ignore
                }
            }
        });
        return filterBox;
    }

    /**
     * for convert taxon sets to table model *
     */
    @SuppressWarnings("unchecked")
    private void taxonSetToModel() {
        // count number of lineages that match the filter
        int i = 0;
        for (String sLineageID : m_taxonMap.keySet()) {
            if (sLineageID.matches(m_sFilter)) {
                i++;
            }
        }

        // clear table model
        while (m_model.getRowCount() > 0) {
            m_model.removeRow(0);
        }

        // fill table model with lineages matching the filter
        for (String sLineageID : m_taxonMap.keySet()) {
            if (sLineageID.matches(m_sFilter)) {
                Object[] rowData = new Object[2];
                rowData[0] = sLineageID;
                rowData[1] = m_taxonMap.get(sLineageID);
                m_model.addRow(rowData);
            }
        }

        @SuppressWarnings("rawtypes")
        Vector data = m_model.getDataVector();
        Collections.sort(data, new Comparator<Vector<?>>() {
            @Override
            public int compare(Vector<?> v1, Vector<?> v2) {
                String o1 = (String) v1.get(m_sortByColumn);
                String o2 = (String) v2.get(m_sortByColumn);
                if (o1.equals(o2)) {
                    o1 = (String) v1.get(1 - m_sortByColumn);
                    o2 = (String) v2.get(1 - m_sortByColumn);
                }
                if (m_bIsAscending) {
                    return o1.compareTo(o2);
                } else {
                    return o2.compareTo(o1);
                }
            }

        });
        m_model.fireTableRowsInserted(0, m_model.getRowCount());
    }

    /**
     * for convert table model to taxon sets *
     */
    private void modelToTaxonset() {
        // update map
        for (int i = 0; i < m_model.getRowCount(); i++) {
            String sLineageID = (String) ((Vector<?>) m_model.getDataVector().elementAt(i)).elementAt(0);
            String sTaxonSetID = (String) ((Vector<?>) m_model.getDataVector().elementAt(i)).elementAt(1);

            // new taxon set?
            if (!m_taxonMap.containsValue(sTaxonSetID)) {
                // create new taxon set
                TaxonSet taxonset = new TaxonSet();
                taxonset.setID(sTaxonSetID);
                m_taxonset.add(taxonset);
            }
            m_taxonMap.put(sLineageID, sTaxonSetID);
        }

        // clear old taxon sets
        for (Taxon taxon : m_taxonset) {
            TaxonSet set = (TaxonSet) taxon;
            set.m_taxonset.get().clear();
        }

        // group lineages with their taxon sets
        for (String sLineageID : m_taxonMap.keySet()) {
            for (Taxon taxon : m_lineageset) {
                if (taxon.getID().equals(sLineageID)) {
                    String sTaxonSet = m_taxonMap.get(sLineageID);
                    for (Taxon taxon2 : m_taxonset) {
                        TaxonSet set = (TaxonSet) taxon2;
                        if (set.getID().equals(sTaxonSet)) {
                            try {
                                set.m_taxonset.setValue(taxon, set);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        // remove unused taxon sets
        for (int i = m_taxonset.size() - 1; i >= 0; i--) {
            if (((TaxonSet) m_taxonset.get(i)).m_taxonset.get().size() == 0) {
                m_taxonset.remove(i);
            }
        }

        TaxonSet taxonset = (TaxonSet) m_input.get();
        taxonset.m_taxonset.get().clear();
        for (Taxon taxon : m_taxonset) {
            try {
                taxonset.m_taxonset.setValue(taxon, taxonset);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
