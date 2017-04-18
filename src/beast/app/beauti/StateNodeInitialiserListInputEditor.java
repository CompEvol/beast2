package beast.app.beauti;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import beast.app.draw.InputEditor;
import beast.app.draw.ListInputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.StateNodeInitialiser;

public class StateNodeInitialiserListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	public StateNodeInitialiserListInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return List.class;
	}

	@Override
	public Class<?> baseType() {
		return StateNodeInitialiser.class;
	}

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
	}

	@Override
	protected InputEditor addPluginItem(Box itemBox, BEASTInterface beastObject) {
		final StateNodeInitialiser currentInitialiser = (StateNodeInitialiser) beastObject;
		Input initialInput = beastObject.getInput("initial");
		List<BeautiSubTemplate> sAvailablePlugins = doc.getInputEditorFactory().getAvailableTemplates(initialInput,
				(BEASTInterface) beastObject, null, doc);

		JComboBox<?> comboBox = null;

		if (sAvailablePlugins.size() > 0) {
			sAvailablePlugins.remove(sAvailablePlugins.size() - 1);

			comboBox = new JComboBox<>(sAvailablePlugins.toArray());
			String sID = beastObject.getID();
			try {
				sID = sID.substring(0, sID.indexOf('.'));
			} catch (Exception e) {
				throw new RuntimeException("Improperly formatted ID: " + sID);
			}
			for (BeautiSubTemplate template : sAvailablePlugins) {
				if (template.matchesName(sID)) {
					comboBox.setSelectedItem(template);
				}
			}
			comboBox.setName("Initialiser");

			comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JComboBox<?> currentComboBox = (JComboBox<?>) e.getSource();
							BeautiSubTemplate template = (BeautiSubTemplate) currentComboBox.getSelectedItem();
							PartitionContext partitionContext;
							partitionContext = doc.getContextFor(beastObject);

							try {
								Object o = template.createSubNet(partitionContext, true);
								StateNodeInitialiser newInitialiser = (StateNodeInitialiser) o;
								List<StateNodeInitialiser> inits = (List<StateNodeInitialiser>) m_input.get();
								int i = inits.indexOf(currentInitialiser);
								inits.set(i, newInitialiser);
								System.out.println(inits.size());
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							sync();
							refreshPanel();
						}
					});
				}
			});

		}

		String name = beastObject.getID();
		Object o = beastObject.getInput("initial").get();
		if (o instanceof BEASTInterface) {
			name = ((BEASTInterface) o).getID();
		}

		if (name == null || name.length() == 0) {
			name = beastObject.getClass().getName();
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		JLabel label = new JLabel("Initial " + name + ":");

		itemBox.add(Box.createRigidArea(new Dimension(5, 1)));
		itemBox.add(label);
		if (comboBox != null) {
			itemBox.add(comboBox);
		}
		itemBox.add(Box.createHorizontalGlue());
		return this;
	}
}
