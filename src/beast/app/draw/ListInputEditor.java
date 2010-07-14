package beast.app.draw;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;

import beast.core.Input;
import beast.core.Plugin;

public class ListInputEditor extends InputEditor {
	private static final long serialVersionUID = 1L;
	/** for handling list of inputs **/
	JList m_list;
	DefaultListModel m_listModel;

	/** buttons for manipulating the list of inputs **/
	JButton m_addButton;
	JButton m_delButton;
	JButton m_editButton;

	
	public ListInputEditor() {
		super();
	}
	
	@Override
	public Class<?> type() {return ArrayList.class;}
	
	/** construct an editor consisting of 
	 * o a label
	 * o a button for selecting another plug-in
	 * o a set of buttons for adding, deleting, editing items in the list 
	 **/
	@Override
	public void init(Input<?> input, Plugin plugin) {
		m_input = input;
		m_plugin = plugin;
		JLabel label = new JLabel(input.getName());
		label.setToolTipText(input.getTipText());
		add(label);
		m_listModel = new DefaultListModel();
		for (Object o : (List<?>) input.get()) {
			if (o instanceof Plugin) {
				Plugin plugin2 = (Plugin) o;
				String sName = plugin2.getID();
				if (sName == null || sName.length() == 0) {
					sName = plugin2.getClass().getName();
					sName = sName.substring(sName.lastIndexOf('.') + 1);
				}
				m_listModel.addElement(sName);
			}
		}
		m_list = new JList(m_listModel);
		add(m_list);
		
		Box buttonBox = Box.createVerticalBox();
		m_addButton = new JButton("+");
		m_addButton.setToolTipText("Add item to the list");
		m_editButton = new JButton("E");
		m_editButton.setToolTipText("Edit item in the list");
		m_delButton = new JButton("-");
		m_addButton.setToolTipText("Delete item from the list");

		
		buttonBox.add(m_addButton);
		buttonBox.add(m_delButton);
		buttonBox.add(m_editButton);
		add(buttonBox);
		m_list.setSize(100,15);
		m_addButton.setSize(8,8);
		m_delButton.setSize(8,8);
		m_editButton.setSize(8,8);
		updateState();
	} // init
	
	
	void updateState() {
		m_editButton.setEnabled(m_list.isSelectionEmpty());
		m_delButton.setEnabled(m_list.isSelectionEmpty());
	}
	
} // class ListPluginInputEditor
