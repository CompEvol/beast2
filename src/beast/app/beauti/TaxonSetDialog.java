package beast.app.beauti;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

public class TaxonSetDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	public boolean m_bOK = false;
	TaxonSet m_taxonSet;
	String m_sID;
	List<Taxon> m_candidates;
	
	JTextField m_IDEntry;

	JTextField m_filterEntry;

	JList m_listOfTaxonCandidates;
    DefaultListModel m_listModel1;
	JList m_listOfTaxonSet;
    DefaultListModel m_listModel2;

	
	public TaxonSetDialog(TaxonSet taxonSet, Set<Taxon> candidates) {
		// initialize state
		m_taxonSet = taxonSet;
		m_sID = taxonSet.getID();
		// create components
		Box box = Box.createVerticalBox();
		box.add(createIDBox());
		box.add(createFilterBox());
		box.add(createTaxonSelector());
		box.add(Box.createVerticalGlue());
		box.add(createCancelOKButtons());
		
		// initialise lists
		List<Taxon> taxonset = taxonSet.m_taxonset.get();
		Comparator<Taxon> comparator = new Comparator<Taxon>() {
			public int compare(Taxon o1, Taxon o2) {
				return o1.getID().compareTo(o2.getID());
			}
		};
		Collections.sort(taxonset, comparator);
		m_candidates = new ArrayList<Taxon>();
		m_candidates.addAll(candidates);
		Collections.sort(m_candidates, comparator);
		
    	for (Taxon taxon : taxonset) {
    		m_listModel2.addElement(taxon);
    	}
    	for (Taxon taxon : m_candidates) {
    		m_listModel1.addElement(taxon);
    	}
    	for (int i = 0 ; i < m_listModel2.size(); i++) {
    		m_listModel1.removeElement(m_listModel2.get(i));
    	}

		add(box);
		setSize(new Dimension(400,600));
		setModal(true);
	} // c'tor
	
	private Component createFilterBox() {
		Box box = Box.createHorizontalBox();
		JLabel label = new JLabel("Filter:");
		box.add(label);
		m_filterEntry = new JTextField();
		Dimension size = new Dimension(100,20);
		m_filterEntry.setMinimumSize(size);
		m_filterEntry.setPreferredSize(size);
		m_filterEntry.setSize(size);
		m_filterEntry.setToolTipText("Enter regular expression to match taxa");
		m_filterEntry.setMaximumSize(new Dimension(1024, 20));
		box.add(m_filterEntry);
		box.add(Box.createHorizontalGlue());
		
		m_filterEntry.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				processEntry();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				processEntry();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				processEntry();
			}
		});
		return box;
	}
	
	private void processEntry() {
		String sFilter = ".*" + m_filterEntry.getText() + ".*";
		
		m_listModel1.clear();
		for (Taxon taxon : m_candidates) {
			if (taxon.getID().matches(sFilter)) {
				m_listModel1.addElement(taxon);
			}
    	}
    	for (int i = 0 ; i < m_listModel2.size(); i++) {
    		m_listModel1.removeElement(m_listModel2.get(i));
    	}
	}

	Component createIDBox() {
		Box box = Box.createHorizontalBox();
		box.add(new JLabel("Taxon set label:"));
		m_IDEntry = new JTextField();
		m_IDEntry.setText(m_sID);
		box.add(m_IDEntry);
		m_IDEntry.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				m_sID = m_IDEntry.getText();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				m_sID = m_IDEntry.getText();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				m_sID = m_IDEntry.getText();
			}
		});
		
		box.setMaximumSize(new Dimension(400,100));
		return box;
	}
	
	Component createTaxonSelector() {
		Box box = Box.createHorizontalBox();
		
		// list of taxa to select from
		m_listModel1 = new DefaultListModel();
		m_listOfTaxonCandidates = new JList(m_listModel1);
		m_listOfTaxonCandidates.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane scroller = new JScrollPane(m_listOfTaxonCandidates);
		box.add(scroller);
		
		// add buttons to select/deselect taxa
		Box buttonBox = Box.createVerticalBox();
		buttonBox.add(Box.createGlue());
		JButton selectButton = new JButton(">>");
		selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	int [] nSelected = m_listOfTaxonCandidates.getSelectedIndices();
            	for (int i : nSelected) {
            		m_listModel2.addElement(m_listModel1.get(i));
            	}
            	for (int i = 0 ; i < m_listModel2.size(); i++) {
            		m_listModel1.removeElement(m_listModel2.get(i));
            	}
            }
        });
		buttonBox.add(selectButton);
		JButton deselectButton = new JButton("<<");
		deselectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	int [] nSelected = m_listOfTaxonSet.getSelectedIndices();
            	for (int i : nSelected) {
            		m_listModel1.addElement(m_listModel2.get(i));
            	}
            	for (int i = 0 ; i < m_listModel1.size(); i++) {
            		m_listModel2.removeElement(m_listModel1.get(i));
            	}
            }
        });
		buttonBox.add(deselectButton);
		buttonBox.add(Box.createGlue());
		box.add(buttonBox);
		
		// list of taxa in taxon set
		m_listModel2 = new DefaultListModel();
		m_listOfTaxonSet = new JList(m_listModel2);
		m_listOfTaxonSet.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane scroller2 = new JScrollPane(m_listOfTaxonSet);
		box.add(scroller2);
		return box;
	} // createTaxonSelector
	
	Component createCancelOKButtons() {
        Box cancelOkBox = Box.createHorizontalBox();
        cancelOkBox.setBorder(new EtchedBorder());
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	m_taxonSet.setID(m_sID);
            	List<Taxon> taxa = m_taxonSet.m_taxonset.get();
            	while (taxa.size() > 0) {
            		taxa.remove(0);
            	}
            	for (int i = 0 ; i < m_listModel2.size(); i++) {
            		taxa.add((Taxon) m_listModel2.get(i));
            	}
            	m_bOK = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        cancelOkBox.add(Box.createHorizontalGlue());
        cancelOkBox.add(okButton);
        cancelOkBox.add(Box.createHorizontalGlue());
        cancelOkBox.add(cancelButton);
        cancelOkBox.add(Box.createHorizontalGlue());
        return cancelOkBox;
	} // createCancelOKButtons
	
	
} // class TaxonSetDialog
