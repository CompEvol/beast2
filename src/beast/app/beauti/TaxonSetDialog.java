package beast.app.beauti;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;



public class TaxonSetDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    public boolean isOK = false;
    TaxonSet taxonSet;
    String id;
    List<Taxon> _candidates;

    JTextField idEntry;

    JTextField filterEntry;

    JList<Taxon> listOfTaxonCandidates;
    DefaultListModel<Taxon> listModel1;
    JList<Taxon> listOfTaxonSet;
    DefaultListModel<Taxon> listModel2;


    Box box;
    BeautiDoc doc;
    
    public TaxonSetDialog(TaxonSet taxonSet, Set<Taxon> candidates, BeautiDoc doc) {
        // initialize state
        this.taxonSet = taxonSet;
        this.doc = doc;
        id = taxonSet.getID();
        // create components
        box = Box.createVerticalBox();
        box.add(createIDBox());
        box.add(createFilterBox());
        box.add(createTaxonSelector());
        box.add(Box.createVerticalGlue());
        //box.add(createCancelOKButtons());

        // initialise lists
        List<Taxon> taxonset = taxonSet.taxonsetInput.get();
        Comparator<Taxon> comparator = (o1, o2) -> o1.getID().compareTo(o2.getID());
        Collections.sort(taxonset, comparator);
        _candidates = new ArrayList<>();
        _candidates.addAll(candidates);
        Collections.sort(_candidates, comparator);

        for (Taxon taxon : taxonset) {
            listModel2.addElement(taxon);
        }
        for (Taxon taxon : _candidates) {
            listModel1.addElement(taxon);
        }
        for (int i = 0; i < listModel2.size(); i++) {
            listModel1.removeElement(listModel2.get(i));
        }

        add(box);
        int size = UIManager.getFont("Label.font").getSize();
        setSize(400 * size / 13, 600 * size / 13);
        setModal(true);
    } // c'tor
    
    public boolean showDialog() {
        JOptionPane optionPane = new JOptionPane(box,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                null,
                null);
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        Frame frame = (doc != null ? doc.getFrame(): Frame.getFrames()[0]);
        final JDialog dialog = optionPane.createDialog(frame, "Taxon set editor");
        dialog.pack();

        dialog.setVisible(true);

        int result = JOptionPane.CANCEL_OPTION;
        Integer value = (Integer) optionPane.getValue();
        if (value != null && value != -1) {
            result = value;
        }
        isOK =  (result != JOptionPane.CANCEL_OPTION);
        if (isOK) {
            taxonSet.setID(id);
            List<Taxon> taxa = taxonSet.taxonsetInput.get();
            while (taxa.size() > 0) {
                taxa.remove(0);
            }
            for (int i = 0; i < listModel2.size(); i++) {
                taxa.add(listModel2.get(i));
            }
            isOK = true;
            dispose();
        }
        return isOK;
    }

    

    private Component createFilterBox() {
        Box box = Box.createHorizontalBox();
        JLabel label = new JLabel("Filter:");
        box.add(label);
        filterEntry = new JTextField();
        filterEntry.setColumns(17);
        //Dimension size = new Dimension(100, 20);
        //filterEntry.setMinimumSize(size);
        //filterEntry.setPreferredSize(size);
        //filterEntry.setSize(size);
        filterEntry.setToolTipText("Enter regular expression to match taxa");
        filterEntry.setMaximumSize(new Dimension(1024, 50));
        box.add(filterEntry);
        box.add(Box.createHorizontalGlue());

        filterEntry.getDocument().addDocumentListener(new DocumentListener() {
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
        String filter = ".*" + filterEntry.getText() + ".*";

        listModel1.clear();
        for (Taxon taxon : _candidates) {
            if (taxon.getID().matches(filter)) {
                listModel1.addElement(taxon);
            }
        }
        for (int i = 0; i < listModel2.size(); i++) {
            listModel1.removeElement(listModel2.get(i));
        }
    }

    Component createIDBox() {
        Box box = Box.createHorizontalBox();
        box.add(new JLabel("Taxon set label:"));
        idEntry = new JTextField();
        idEntry.setName("idEntry");
        idEntry.setText(id);
        box.add(idEntry);
        idEntry.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                id = idEntry.getText();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                id = idEntry.getText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                id = idEntry.getText();
            }
        });

        box.setMaximumSize(new Dimension(400, 100));
        return box;
    }
    
    class TaxonCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setText(((Taxon)value).getID());
			return label;
		}
	}
    
    Component createTaxonSelector() {
        Box box = Box.createHorizontalBox();

        // list of taxa to select from
        listModel1 = new DefaultListModel<>();
        listOfTaxonCandidates = new JList<>(listModel1);
        listOfTaxonCandidates.setName("listOfTaxonCandidates");
        listOfTaxonCandidates.setBorder(BorderFactory.createEtchedBorder());
        listOfTaxonCandidates.setCellRenderer(new TaxonCellRenderer());
        
        JScrollPane scroller = new JScrollPane(listOfTaxonCandidates);
        box.add(scroller);

        // add buttons to select/deselect taxa
        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(Box.createGlue());
        JButton selectButton = new JButton(">>");
        selectButton.setName(">>");
        selectButton.addActionListener(e -> {
                int[] selected = listOfTaxonCandidates.getSelectedIndices();
                for (int i : selected) {
                    listModel2.addElement(listModel1.get(i));
                }
                for (int i = 0; i < listModel2.size(); i++) {
                    listModel1.removeElement(listModel2.get(i));
                }
            });
        buttonBox.add(selectButton);
        JButton deselectButton = new JButton("<<");
        deselectButton.setName("<<");
        deselectButton.addActionListener(e -> {
                int[] selected = listOfTaxonSet.getSelectedIndices();
                for (int i : selected) {
                    listModel1.addElement(listModel2.get(i));
                }
                for (int i = 0; i < listModel1.size(); i++) {
                    listModel2.removeElement(listModel1.get(i));
                }
            });
        buttonBox.add(deselectButton);
        buttonBox.add(Box.createGlue());
        box.add(buttonBox);

        // list of taxa in taxon set
        listModel2 = new DefaultListModel<>();
        listOfTaxonSet = new JList<>(listModel2);
        listOfTaxonSet.setBorder(BorderFactory.createEtchedBorder());
        listOfTaxonSet.setCellRenderer(new TaxonCellRenderer());

        JScrollPane scroller2 = new JScrollPane(listOfTaxonSet);
        box.add(scroller2);
        return box;
    } // createTaxonSelector

    Component createCancelOKButtons() {
        Box cancelOkBox = Box.createHorizontalBox();
        cancelOkBox.setBorder(new EtchedBorder());
        JButton okButton = new JButton("Ok");
        okButton.setName("OK");
        okButton.addActionListener(e -> {
                taxonSet.setID(id);
                List<Taxon> taxa = taxonSet.taxonsetInput.get();
                while (taxa.size() > 0) {
                    taxa.remove(0);
                }
                for (int i = 0; i < listModel2.size(); i++) {
                    taxa.add(listModel2.get(i));
                }
                isOK = true;
                dispose();
            });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setName("Cancel");
        cancelButton.addActionListener(e -> {
                dispose();
            });
        cancelOkBox.add(Box.createHorizontalGlue());
        cancelOkBox.add(okButton);
        cancelOkBox.add(Box.createHorizontalGlue());
        cancelOkBox.add(cancelButton);
        cancelOkBox.add(Box.createHorizontalGlue());
        return cancelOkBox;
    } // createCancelOKButtons


} // class TaxonSetDialog
