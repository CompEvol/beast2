package beast.app.beauti;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.PatternSyntaxException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JSeparator;
import javax.swing.JButton;

import beast.app.util.Utils;
import javax.swing.JLabel;

public class GuessPatternDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	public static final String EXAMPLE_FORMAT = "<html>A proper trait file is tab delimited. <br>"
			+ "The first row is always <font color=red>traits</font> followed by the keyword <br>"
			+ "(e.g. <font color=red>species</font> in *BEAST) in the second column and separated <br>"
			+ "by <font color=red>tab</font>. The rest rows are mapping taxa to species, which list <br>"
			+ "taxon name in the first column and species name in the second column separated by <br>"
			+ "<font color=red>tab</font>. For example: <br>" + "traits\tspecies<br>" + "taxon1\tspeciesA<br>"
			+ "taxon2\tspeciesA<br>" + "taxon3\tspeciesB<br>" + "... ...<br>"
			+ "Once mapping file is loaded, the trait named by keyword <font color=red>species</font> <br>"
			+ "is displayed in the main panel, and the message of using *BEAST is also displayed on <br>"
			+ "the bottom of main frame.<br>"
			+ "For multi-alignment, the default of *BEAST is unlinking all models: substitution model, <br>"
			+ "clock model, and tree models.</html>";

	public enum Status {
		canceled, pattern, trait
	};

	public String trait = null;

	public String getTrait() {
		return trait;
	}

	Component m_parent;
	JPanel guessPanel;
	ButtonGroup group;
	JRadioButton b1 = new JRadioButton("use everything");
	JRadioButton b2 = new JRadioButton("split on character");
	JRadioButton b3 = new JRadioButton("use regular expression");
	JRadioButton b4 = new JRadioButton("read from file");

	int m_location = 0;
	int m_splitlocation = 0;
	String m_sDelimiter = ".";
	JTextField regexpEntry;
	String pattern;

	public String getPattern() {
		return pattern;
	}

	private JTextField txtFile;

	public GuessPatternDialog(Component parent, String pattern) {
		m_parent = parent;
		this.pattern = pattern;
		guessPanel = new JPanel();
		GridBagLayout gbl_guessPanel = new GridBagLayout();
		gbl_guessPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 };
		guessPanel.setLayout(gbl_guessPanel);

		group = new ButtonGroup();
		group.add(b1);
		group.add(b2);
		group.add(b3);
		group.add(b4);
		group.setSelected(b1.getModel(), true);
		b1.setName(b1.getText());
		b2.setName(b2.getText());
		b3.setName(b3.getText());
		b4.setName(b4.getText());

		createDelimiterBox(b1);
		createSplitBox(b2);
		createRegExtpBox(b3);
		
				regexpEntry = new JTextField();
				regexpEntry.setText(pattern);
				regexpEntry.setColumns(10);
				regexpEntry.setToolTipText("Enter regular expression to match taxa");
				regexpEntry.setMaximumSize(new Dimension(1024, 20));
				GridBagConstraints gbc2 = new GridBagConstraints();
				gbc2.insets = new Insets(0, 0, 5, 5);
				gbc2.anchor = GridBagConstraints.WEST;
				gbc2.gridwidth = 4;
				gbc2.gridx = 1;
				gbc2.gridy = 5;
				guessPanel.add(regexpEntry, gbc2);
				regexpEntry.getDocument().addDocumentListener(new DocumentListener() {
					@Override
					public void removeUpdate(DocumentEvent e) {
						b3.setSelected(true);
					}

					@Override
					public void insertUpdate(DocumentEvent e) {
						b3.setSelected(true);
					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						b3.setSelected(true);
					}
				});

		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.gridwidth = 5;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 6;
		guessPanel.add(separator, gbc_separator);

		GridBagConstraints gbc_rdbtnReadFromFile = new GridBagConstraints();
		gbc_rdbtnReadFromFile.anchor = GridBagConstraints.WEST;
		gbc_rdbtnReadFromFile.insets = new Insets(0, 0, 0, 5);
		gbc_rdbtnReadFromFile.gridx = 0;
		gbc_rdbtnReadFromFile.gridy = 7;
		guessPanel.add(b4, gbc_rdbtnReadFromFile);

		JButton btnReadFromFile = new JButton("Browse");
		btnReadFromFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file = Utils.getLoadFile("Load trait from file", new File(Beauti.g_sDir), "Select trait file", "dat","txt");
				if (file != null) {
					txtFile.setText(file.getPath());
					b4.setSelected(true);
				}
			}
		});
		
				txtFile = new JTextField();
				txtFile.setText("File");
				GridBagConstraints gbc_txtFile = new GridBagConstraints();
				gbc_txtFile.gridwidth = 2;
				gbc_txtFile.insets = new Insets(0, 0, 0, 5);
				gbc_txtFile.fill = GridBagConstraints.HORIZONTAL;
				gbc_txtFile.gridx = 1;
				gbc_txtFile.gridy = 7;
				guessPanel.add(txtFile, gbc_txtFile);
				txtFile.setColumns(10);
		GridBagConstraints gbc_btnReadFromFile = new GridBagConstraints();
		gbc_btnReadFromFile.insets = new Insets(0, 0, 0, 5);
		gbc_btnReadFromFile.gridx = 3;
		gbc_btnReadFromFile.gridy = 7;
		guessPanel.add(btnReadFromFile, gbc_btnReadFromFile);

		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("Show format of trait file");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(m_parent, EXAMPLE_FORMAT);
			}
		});
		GridBagConstraints gbc_btnHelp = new GridBagConstraints();
		gbc_btnHelp.insets = new Insets(0, 0, 0, 5);
		gbc_btnHelp.gridx = 4;
		gbc_btnHelp.gridy = 7;
		guessPanel.add(btnHelp, gbc_btnHelp);
	}

	private void createDelimiterBox(JRadioButton b) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 1;
		guessPanel.add(b, gbc);

		JComboBox combo = new JComboBox(new String[] { "after first", "after last", "before first", "before last" });
		combo.setName("delimiterCombo");
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.anchor = GridBagConstraints.WEST;
		gbc2.gridwidth = 2;
		gbc2.insets = new Insets(0, 0, 5, 5);
		gbc2.gridx = 1;
		gbc2.gridy = 1;
		guessPanel.add(combo, gbc2);
		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox combo = (JComboBox) e.getSource();
				m_location = combo.getSelectedIndex();
				b1.setSelected(true);
			}
		});
	}

	private void createSplitBox(JRadioButton b) {
		
				JComboBox combo2 = new JComboBox(new String[] { ".", ",", "_", "-", " ", "/", ":", ";" });
				combo2.setName("delimiterCombo2");
				GridBagConstraints gbc3 = new GridBagConstraints();
				gbc3.insets = new Insets(0, 0, 5, 5);
				gbc3.gridx = 3;
				gbc3.gridy = 1;
				guessPanel.add(combo2, gbc3);
				combo2.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JComboBox combo = (JComboBox) e.getSource();
						m_sDelimiter = (String) combo.getSelectedItem();
						b1.setSelected(true);
					}
				});

		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.gridwidth = 6;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 2;
		guessPanel.add(separator, gbc_separator);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 3;
		guessPanel.add(b, gbc);
	}

	public void createRegExtpBox(JRadioButton b) {
				
						JComboBox combo2 = new JComboBox(new String[] { ".", ",", "_", "-", " ", "/", ":", ";" });
						combo2.setName("splitCombo2");
						GridBagConstraints gbc3 = new GridBagConstraints();
						gbc3.anchor = GridBagConstraints.WEST;
						gbc3.insets = new Insets(0, 0, 5, 5);
						gbc3.gridx = 1;
						gbc3.gridy = 3;
						guessPanel.add(combo2, gbc3);
						combo2.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								JComboBox combo = (JComboBox) e.getSource();
								m_sDelimiter = (String) combo.getSelectedItem();
								b2.setSelected(true);
							}
						});
				
				JLabel lblAndTakeGroups = new JLabel("and take group(s):");
				GridBagConstraints gbc_lblAndTakeGroups = new GridBagConstraints();
				gbc_lblAndTakeGroups.gridwidth = 2;
				gbc_lblAndTakeGroups.insets = new Insets(0, 0, 5, 5);
				gbc_lblAndTakeGroups.gridx = 2;
				gbc_lblAndTakeGroups.gridy = 3;
				guessPanel.add(lblAndTakeGroups, gbc_lblAndTakeGroups);
		
				JComboBox combo_1 = new JComboBox(new String[] { "1", "2", "3", "4", "1-2", "2-3", "3-4", "1-3", "2-4" });
				combo_1.setName("splitCombo");
				GridBagConstraints gbc_combo_1 = new GridBagConstraints();
				gbc_combo_1.anchor = GridBagConstraints.WEST;
				gbc_combo_1.insets = new Insets(0, 0, 5, 5);
				gbc_combo_1.gridx = 4;
				gbc_combo_1.gridy = 3;
				guessPanel.add(combo_1, gbc_combo_1);
				combo_1.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JComboBox combo = (JComboBox) e.getSource();
						m_splitlocation = combo.getSelectedIndex();
						b2.setSelected(true);
					}
				});

		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.gridwidth = 6;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 4;
		guessPanel.add(separator, gbc_separator);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 5;
		guessPanel.add(b, gbc);
	}

	public Status showDialog(String title) {

		JOptionPane optionPane = new JOptionPane(guessPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
				null, new String[] { "Cancel", "OK" }, "OK");
		optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

		final JDialog dialog = optionPane.createDialog(m_parent, title);
		dialog.setName("GuessTaxonSets");
		// dialog.setResizable(true);
		dialog.pack();

		dialog.setVisible(true);

		// if (optionPane.getValue() == null) {
		// System.exit(0);
		// }
		String sDelimiter = m_sDelimiter;
		if (sDelimiter.equals(".") || sDelimiter.equals("/")) {
			sDelimiter = "\\" + sDelimiter;
		}
		if (b1.getModel() == group.getSelection()) {
			switch (m_location) {
			case 0: // "after first",
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "(.*)$";
				break;
			case 1: // "after last",
				pattern = "^.*" + sDelimiter + "(.*)$";
				break;
			case 2: // "before first",
				pattern = "^([^" + sDelimiter + "]+)" + sDelimiter + ".*$";
				break;
			case 3: // "before last"
				pattern = "^(.*)" + sDelimiter + ".*$";
				break;
			}
		}
		if (b2.getModel() == group.getSelection()) {
			switch (m_splitlocation) {
			case 0: // "1"
				pattern = "^([^" + sDelimiter + "]+)" + ".*$";
				break;
			case 1: // "2"
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+)" + ".*$";
				break;
			case 2: // "3"
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "([^"
						+ sDelimiter + "]+)" + ".*$";
				break;
			case 3: // "4"
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "[^"
						+ sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+)" + ".*$";
				break;
			case 4: // "1-2"
				pattern = "^([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
				break;
			case 5: // "2-3"
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+" + sDelimiter + "[^"
						+ sDelimiter + "]+)" + ".*$";
				break;
			case 6: // "3-4"
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "([^"
						+ sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
				break;
			case 7: // "1-3"
				pattern = "^([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "[^"
						+ sDelimiter + "]+)" + ".*$";
				break;
			case 8: // "2-4"
				pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+" + sDelimiter + "[^"
						+ sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
			}
		}
		if (b3.getModel() == group.getSelection()) {
			pattern = regexpEntry.getText();
		}
		if (b4.getModel() == group.getSelection()) {
			try {
				BufferedReader fin = new BufferedReader(new FileReader(txtFile.getText()));
				StringBuffer buf = new StringBuffer();
				// eat up header
				fin.readLine();
				// process data
				while (fin.ready()) {
					String str = fin.readLine();
					str = str.replaceFirst("\t", "=") + ",";
					// only add entries that are non-empty
					if (!str.matches("^\\s+=.*$")) {
						buf.append(str);
					}
				}
				fin.close();
				trait = buf.toString().trim();
				while (trait.endsWith(",")) {
					trait = trait.substring(0, trait.length() - 1).trim();
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(m_parent, "Loading trait from file failed:" + e.getMessage());
				return Status.canceled;
			}
			return Status.trait;
		}

		// sanity check
		try {
			pattern.matches(pattern);
		} catch (PatternSyntaxException e) {
			JOptionPane.showMessageDialog(this, "This is not a valid regular expression");
			return Status.canceled;
		}

		if (optionPane.getValue().equals("OK")) {
			System.err.println("Pattern = " + pattern);
			return Status.pattern;
		} else {
			return Status.canceled;
		}
	}
	
}
