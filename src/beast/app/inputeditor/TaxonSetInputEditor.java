package beast.app.inputeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.base.BEASTInterface;
import beast.base.Input;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
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
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
        TaxonSet taxonset = (TaxonSet) m_input.get();
        if (taxonset == null) {
            return;
        }
        List<Taxon> taxonsets = new ArrayList<>();

        List<Taxon> taxa = taxonset.taxonsetInput.get();
        for (Taxon taxon : taxa) {
            taxonsets.add(taxon);
        }
        add(getContent(taxonsets));
        if (taxa.size() == 1 && taxa.get(0).getID().equals("Beauti2DummyTaxonSet") || taxa.size() == 0) {
            taxa.clear();
            try {
                // species is first character of taxon
                guessTaxonSets("(.).*", 0);
                for (Taxon taxonset2 : m_taxonset) {
                    for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
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
        m_taxonMap = new HashMap<>();
        m_lineageset = new ArrayList<>();
        for (Taxon taxonset2 : m_taxonset) {
        	if (taxonset2 instanceof TaxonSet) {
		        for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
		            m_lineageset.add(taxon);
		            m_taxonMap.put(taxon.getID(), taxonset2.getID());
		        }
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
                String text = m_textField.getText();
                //Log.warning.println(text);
                m_model.setValueAt(text, m_iRow, m_iCol);

                // try {
                // Double.parseDouble(text);
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
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowNr,
                                                         int colNr) {
                if (!isSelected) {
                    return null;
                }
                m_iRow = rowNr;
                m_iCol = colNr;
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
        m_table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int size = m_table.getFont().getSize();
		m_table.setRowHeight(20 * size/13);
        m_table.getColumnModel().getColumn(0).setPreferredWidth(250 * size/13);
        m_table.getColumnModel().getColumn(1).setPreferredWidth(250 * size/13);

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
        fillDownButton.addActionListener(e -> {
                int[] rows = m_table.getSelectedRows();
                if (rows.length < 2) {
                    return;
                }
                String taxon = (String) ((Vector<?>) m_model.getDataVector().elementAt(rows[0])).elementAt(1);
                for (int i = 1; i < rows.length; i++) {
                    m_model.setValueAt(taxon, rows[i], 1);
                }
                modelToTaxonset();
            });

        JButton guessButton = new JButton("Guess");
        guessButton.setName("Guess");
        guessButton.addActionListener(e -> {
                guess();
            });

        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(fillDownButton);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(guessButton);
        buttonBox.add(Box.createHorizontalGlue());
        return buttonBox;
    }

    public class ColumnHeaderListener extends MouseAdapter {
        @Override
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
        switch(dlg.showDialog("Guess taxon sets")) {
        case canceled: return;
        case pattern: 
        String pattern = dlg.getPattern();
            try {
                guessTaxonSets(pattern, 0);
                m_sPattern = pattern;
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        case trait:
        	parseTrait(dlg.getTraitMap());
            break;
        }
        m_lineageset.clear();
        for (Taxon taxonset2 : m_taxonset) {
            for (Taxon taxon : ((TaxonSet) taxonset2).taxonsetInput.get()) {
                m_lineageset.add(taxon);
                m_taxonMap.put(taxon.getID(), taxonset2.getID());
            }
        }
        taxonSetToModel();
        modelToTaxonset();
    }

    /**
     * guesses taxon sets based on pattern in regExp based on the taxa in
     * m_rawData
     */
    public int guessTaxonSets(String regexp, int minSize) {
        m_taxonset.clear();
        HashMap<String, TaxonSet> map = new HashMap<>();
        Pattern m_pattern = Pattern.compile(regexp);
        Set<Taxon> taxa = new HashSet<>();
        Set<String> taxonIDs = new HashSet<>();
        for (Alignment alignment : getDoc().alignments) {
        	for (String id : alignment.getTaxaNames()) {
                if (!taxonIDs.contains(id)) {
                	Taxon taxon = getDoc().getTaxon(id);
	                taxa.add(taxon);
	                taxonIDs.add(id);
        		}
        	}
            for (Sequence sequence : alignment.sequenceInput.get()) {
                String id = sequence.taxonInput.get();
                if (!taxonIDs.contains(id)) {
                    Taxon taxon = getDoc().getTaxon(sequence.taxonInput.get());
                    // ensure sequence and taxon do not get same ID
                    if (sequence.getID().equals(sequence.taxonInput.get())) {
                        sequence.setID("_" + sequence.getID());
                    }
                    taxa.add(taxon);
                    taxonIDs.add(id);
                }
            }
        }

        List<String> unknowns = new ArrayList<>();
        for (Taxon taxon : taxa) {
            if (!(taxon instanceof TaxonSet)) {
                Matcher matcher = m_pattern.matcher(taxon.getID());
                String match;
                if (matcher.find()) {
                    match = matcher.group(1);
                } else {
                   	match = "UNKNOWN";
                   	unknowns.add(taxon.getID());
                }
                try {
                    if (map.containsKey(match)) {
                        TaxonSet set = map.get(match);
                        set.taxonsetInput.setValue(taxon, set);
                    } else {
                    	TaxonSet set = newTaxonSet(match);
                        set.taxonsetInput.setValue(taxon, set);
                        map.put(match, set);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (unknowns.size() > 0) {
        	showMisMatchMessage(unknowns);
        }
        // add taxon sets
        int ignored = 0;
        for (TaxonSet set : map.values()) {
            if (set.taxonsetInput.get().size() > minSize) {
                m_taxonset.add(set);
            } else {
                ignored += set.taxonsetInput.get().size();
            }
        }
        return ignored;
    }

    private TaxonSet newTaxonSet(String match) {
    	if (getDoc().taxaset.containsKey(match)) {
    		Taxon t = doc.taxaset.get(match);
    		if (t instanceof TaxonSet) {
    			TaxonSet set = (TaxonSet) t;
    			set.taxonsetInput.get().clear();
    			return set;
    		} else {
    			// TODO handle situation where taxon and set have same name (issue #135)
    		}
    	}
        TaxonSet set = new TaxonSet();
        set.setID(match);
		return set;
	}

	void parseTrait(Map<String,String> traitMap) {
        m_taxonset.clear();

        Set<Taxon> taxa = new HashSet<>();
        Set<String> taxonIDs = new HashSet<>();
        for (Alignment alignment : getDoc().alignments) {
        	if (alignment instanceof FilteredAlignment) {
        		alignment = ((FilteredAlignment)alignment).alignmentInput.get();
        	}
            for (String id : alignment.getTaxaNames()) {
                if (!taxonIDs.contains(id)) {
                    Taxon taxon = getDoc().getTaxon(id);
                    taxa.add(taxon);
                    taxonIDs.add(id);
                }
            }
        }

        HashMap<String, TaxonSet> map = new HashMap<>();
        List<String> unknowns = new ArrayList<>();
        for (Taxon taxon : taxa) {
            if (!(taxon instanceof TaxonSet)) {
                String match = traitMap.get(taxon.getID());
                if (match == null) {
                	match = "UNKNOWN";
                	unknowns.add(taxon.getID());
                }
                try {
                    if (map.containsKey(match)) {
                        TaxonSet set = map.get(match);
                        set.taxonsetInput.setValue(taxon, set);
                    } else {
                        TaxonSet set = newTaxonSet(match);
                        set.taxonsetInput.setValue(taxon, set);
                        map.put(match, set);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        // add taxon sets
        for (TaxonSet set : map.values()) {
             m_taxonset.add(set);
        }
        if (unknowns.size() > 0) {
        	showMisMatchMessage(unknowns);
        }
    }
    
    private void showMisMatchMessage(List<String> unknowns) {
    	JOptionPane.showMessageDialog(
    	    this, 
    	    "Some taxa did not have a match and are set to UNKNOWN:\n" + unknowns.toString().replaceAll(",", "\n"), 
    	    "Warning", 
    	    JOptionPane.INFORMATION_MESSAGE);
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
		int size = filterEntry.getFont().getSize();
        filterEntry.setMaximumSize(new Dimension(1024, 20 * size/13));
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
                String filter = ".*" + filterEntry.getText() + ".*";
                try {
                    // sanity check: make sure the filter is legit
                    filter.matches(filter);
                    m_sFilter = filter;
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
        // clear table model
        while (m_model.getRowCount() > 0) {
            m_model.removeRow(0);
        }

        // fill table model with lineages matching the filter
        for (String lineageID : m_taxonMap.keySet()) {
            if (lineageID.matches(m_sFilter)) {
                Object[] rowData = new Object[2];
                rowData[0] = lineageID;
                rowData[1] = m_taxonMap.get(lineageID);
                m_model.addRow(rowData);
            }
        }

        @SuppressWarnings("rawtypes")
        Vector data = m_model.getDataVector();
        Collections.sort(data, (Vector<?> v1, Vector<?> v2) -> {
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

        );
        m_model.fireTableRowsInserted(0, m_model.getRowCount());
    }

    /**
     * for convert table model to taxon sets *
     */
    private void modelToTaxonset() {

        // update map
        for (int i = 0; i < m_model.getRowCount(); i++) {
            String lineageID = (String) ((Vector<?>) m_model.getDataVector().elementAt(i)).elementAt(0);
            String taxonSetID = (String) ((Vector<?>) m_model.getDataVector().elementAt(i)).elementAt(1);

            // new taxon set?
            if (!m_taxonMap.containsValue(taxonSetID)) {
                // create new taxon set
                TaxonSet taxonset = newTaxonSet(taxonSetID);
                m_taxonset.add(taxonset);
            }
            m_taxonMap.put(lineageID, taxonSetID);
        }

        // clear old taxon sets
        for (Taxon taxon : m_taxonset) {
            TaxonSet set = (TaxonSet) taxon;
            set.taxonsetInput.get().clear();
            doc.registerPlugin(set);
        }

        // group lineages with their taxon sets
        for (String lineageID : m_taxonMap.keySet()) {
            for (Taxon taxon : m_lineageset) {
                if (taxon.getID().equals(lineageID)) {
                    String taxonSet = m_taxonMap.get(lineageID);
                    for (Taxon taxon2 : m_taxonset) {
                        TaxonSet set = (TaxonSet) taxon2;
                        if (set.getID().equals(taxonSet)) {
                            try {
                                set.taxonsetInput.setValue(taxon, set);
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
            if (((TaxonSet) m_taxonset.get(i)).taxonsetInput.get().size() == 0) {
                doc.unregisterPlugin(m_taxonset.get(i));
                m_taxonset.remove(i);
            }
        }

        TaxonSet taxonset = (TaxonSet) m_input.get();
        taxonset.taxonsetInput.get().clear();
        for (Taxon taxon : m_taxonset) {
            try {
                taxonset.taxonsetInput.setValue(taxon, taxonset);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
