package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.app.draw.PluginInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.tree.TraitSet;

public class TipDatesInputEditor extends PluginInputEditor {
	private static final long serialVersionUID = 1L;

	
	@Override
    public Class<?> type() {
        return TraitSet.class;
    }

	TraitSet m_traitSet;
	JComboBox m_unitsComboBox;
	JComboBox m_relativeToComboBox;
	List<String> m_sTaxa;
	Object[][] m_tableData;
	JTable m_table;
	
	
    @Override
    public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
		m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
        m_traitSet = (TraitSet) m_input.get();
        
        
        Box box = createVerticalBox();
        
        Box buttonBox = createButtonBox();
        box.add(buttonBox);
        Component listBox = createListBox();
        box.add(listBox);
        add(box);
    } // init

	private Component createListBox() {
		m_sTaxa = m_traitSet.m_taxa.get().getTaxaNames();
		String [] columnData = new String[] {"Name", "Date","Height"};
		m_tableData = new Object[m_sTaxa.size()][3];
		convertTraitToTableData();
		// set up table.
		// special features: background shading of rows
		// custom editor allowing only Date column to be edited.
		m_table = new JTable(m_tableData, columnData) {
			private static final long serialVersionUID = 1L;

			// method that induces table row shading 
			@Override
			public Component prepareRenderer (TableCellRenderer renderer,int Index_row, int Index_col) {
				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
				//even index, selected or not selected
				if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
					comp.setBackground(new Color(237,243,255));
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
			int m_iRow, m_iCol;
			@Override
			public boolean stopCellEditing() {
				m_table.removeEditor();
				String sText = m_textField.getText();
				try {
					Double.parseDouble(sText);
				} catch (Exception e) {
					return false;
				}
				m_tableData[m_iRow][m_iCol] = sText;
				convertTableDataToTrait();
				convertTraitToTableData();
				return true;
			}
		
			@Override
			public boolean isCellEditable(EventObject anEvent) {
				return m_table.getSelectedColumn() == 1;
			}
			
			
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow, int iCol) {
				if (!isSelected) {
					return null;
				}
				m_iRow = iRow;
				m_iCol = iCol;
				m_textField.setText((String)value);
				return m_textField; 			
			}

			@Override
			public boolean shouldSelectCell(EventObject anEvent) {return false;}
			@Override
			public void removeCellEditorListener(CellEditorListener l) {}
			@Override
			public Object getCellEditorValue() {return null;}
			@Override
			public void cancelCellEditing() {}
			@Override
			public void addCellEditorListener(CellEditorListener l) {}
		
		});				
		JScrollPane scrollPane = new JScrollPane(m_table);
		return scrollPane;
	} // createListBox

	/* synchronise table with data from traitSet Plugin */
	private void convertTraitToTableData() {
		for (int i = 0; i < m_tableData.length; i++) {
			m_tableData[i][0] = m_sTaxa.get(i);
			m_tableData[i][1] = "0";
			m_tableData[i][2] = "0";
		}
        String[] sTraits = m_traitSet.m_traits.get().split(",");
        for (String sTrait : sTraits) {
            sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            if (sStrs.length != 2) {
            	break;
                //throw new Exception("could not parse trait: " + sTrait);
            }
            String sTaxonID = normalize(sStrs[0]);
            int iTaxon = m_sTaxa.indexOf(sTaxonID);
//            if (iTaxon < 0) {
//                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
//            }
            m_tableData[iTaxon][0] = sTaxonID;
            m_tableData[iTaxon][1] = normalize(sStrs[1]);
        }
        if (m_traitSet.m_sTraitName.get().equals("date-forward")) {
        	for (int i = 0; i < m_tableData.length; i++) {
        		m_tableData[i][2] =  m_tableData[i][1];
        	}
        } else {
            Double fMaxDate = 0.0;
        	for (int i = 0; i < m_tableData.length; i++) {
        		fMaxDate = Math.max(fMaxDate, parseDouble((String)m_tableData[i][1]));
        	}
        	for (int i = 0; i < m_tableData.length; i++) {
        		m_tableData[i][2] =  fMaxDate - parseDouble((String)m_tableData[i][1]);
        	}
        }
        
        if (m_table != null) {
		    for (int i = 0; i < m_tableData.length; i++) {
				m_table.setValueAt(m_tableData[i][1], i, 1);
				m_table.setValueAt(m_tableData[i][2], i, 2);
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

	/** synchronise traitSet Plugin with table data*/
	private void convertTableDataToTrait() {
		String sTrait = "";
		for (int i = 0; i < m_tableData.length; i++) {
			sTrait += m_sTaxa.get(i) + "=" + m_tableData[i][1];
			if (i < m_tableData.length - 1) {
				sTrait += ",\n";
			}
		}
		try {
			m_traitSet.m_traits.setValue(sTrait, m_traitSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** create box with comboboxes for selectin units and trait name **/
	private Box createButtonBox() {
		Box buttonBox = Box.createHorizontalBox();
		
		JLabel label = new JLabel("Dates specified as: ");
		label.setMaximumSize(new Dimension(1024, 20));
		buttonBox.add(label);
		m_unitsComboBox = new JComboBox(TraitSet.Units.values());
		m_unitsComboBox.setSelectedItem(m_traitSet.m_sUnits.get());
		m_unitsComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                String sSelected = (String) m_unitsComboBox.getSelectedItem().toString();
                try {
                	m_traitSet.m_sUnits.setValue(sSelected, m_traitSet);
                	System.err.println("Traitset is now: " + m_traitSet.m_sUnits.get());
                } catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		m_unitsComboBox.setMaximumSize(new Dimension(1024, 20));
		buttonBox.add(m_unitsComboBox);
		
		m_relativeToComboBox = new JComboBox(new String[]{"Since some time in the past",  "Before the present"});
		m_relativeToComboBox.setSelectedItem(m_traitSet.m_sTraitName.get());
		m_relativeToComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String sSelected = "date-forward";
                if (m_relativeToComboBox.getSelectedIndex() == 0) {
                	sSelected = "date-backward";
                }
                try {
                	m_traitSet.m_sTraitName.setValue(sSelected, m_traitSet);
                	System.err.println("Relative position is now: " + m_traitSet.m_sTraitName.get());
                } catch (Exception ex) {
					ex.printStackTrace();
				}
                convertTraitToTableData();
			}
		});
		m_relativeToComboBox.setMaximumSize(new Dimension(1024, 20));
		buttonBox.add(m_relativeToComboBox);
		buttonBox.add(Box.createGlue());
		
		return buttonBox;
	} // createButtonBox
}
