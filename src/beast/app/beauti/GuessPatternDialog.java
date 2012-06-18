package beast.app.beauti;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.PatternSyntaxException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class GuessPatternDialog extends JDialog {
        private static final long serialVersionUID = 1L;

        Component m_parent;
        Box guessPanel;
        ButtonGroup group;
        JRadioButton b1 = new JRadioButton("use everything");
        JRadioButton b2 = new JRadioButton("split on character, and take group(s):");
        JRadioButton b3 = new JRadioButton("use regular expression");

        int m_location = 0;
        int m_splitlocation = 0;
        String m_sDelimiter = ".";
        JTextField regexpEntry;
        String pattern;
        
        public GuessPatternDialog(Component parent, String pattern) {
            m_parent = parent;
            this.pattern = pattern;
            guessPanel = Box.createVerticalBox();

            group = new ButtonGroup();
            group.add(b1);
            group.add(b2);
            group.add(b3);
            group.setSelected(b1.getModel(), true);
            b1.setName(b1.getText());
            b2.setName(b2.getText());
            b3.setName(b3.getText());

            guessPanel.add(createDelimiterBox(b1));
            guessPanel.add(Box.createVerticalStrut(20));
            guessPanel.add(createSplitBox(b2));
            guessPanel.add(Box.createVerticalStrut(20));            
            guessPanel.add(createRegExtpBox(b3));
            guessPanel.add(Box.createVerticalStrut(20));
        }

        private Component createDelimiterBox(JRadioButton b) {
            Box box = Box.createHorizontalBox();
            box.add(b);

            JComboBox combo = new JComboBox(new String[]{"after first", "after last", "before first", "before last"});
            combo.setName("delimiterCombo");
            box.add(Box.createHorizontalGlue());
            box.add(combo);
            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JComboBox combo = (JComboBox) e.getSource();
                    m_location = combo.getSelectedIndex();
                    b1.setSelected(true);
                }
            });

            JComboBox combo2 = new JComboBox(new String[]{".", ",", "_", "-", " ", "/", ":", ";"});
            combo2.setName("delimiterCombo2");
            box.add(Box.createHorizontalGlue());
            box.add(combo2);
            combo2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JComboBox combo = (JComboBox) e.getSource();
                    m_sDelimiter = (String) combo.getSelectedItem();
                    b1.setSelected(true);
                }
            });
            box.add(Box.createHorizontalGlue());
            return box;
        }

        private Component createSplitBox(JRadioButton b) {
            Box box = Box.createHorizontalBox();
            box.add(b);

            JComboBox combo = new JComboBox(new String[]{"1","2","3","4","1-2","2-3","3-4","1-3","2-4"});
            combo.setName("splitCombo");
            box.add(Box.createHorizontalGlue());
            box.add(combo);
            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JComboBox combo = (JComboBox) e.getSource();
                    m_splitlocation = combo.getSelectedIndex();
                    b2.setSelected(true);
                }
            });

            JComboBox combo2 = new JComboBox(new String[]{".", ",", "_", "-", " ", "/", ":", ";"});
            combo2.setName("splitCombo2");
            box.add(Box.createHorizontalGlue());
            box.add(combo2);
            combo2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JComboBox combo = (JComboBox) e.getSource();
                    m_sDelimiter = (String) combo.getSelectedItem();
                    b2.setSelected(true);
                }
            });
            box.add(Box.createHorizontalGlue());
            return box;
        }

        public Component createRegExtpBox(JRadioButton b) {
            Box box = Box.createHorizontalBox();
            box.add(b);
            regexpEntry = new JTextField();
            regexpEntry.setText(pattern);
            regexpEntry.setColumns(30);
            regexpEntry.setToolTipText("Enter regular expression to match taxa");
            regexpEntry.setMaximumSize(new Dimension(1024, 20));
            box.add(Box.createHorizontalGlue());
            box.add(regexpEntry);
            box.add(Box.createHorizontalGlue());
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
            return box;
        }

        public String showDialog(String title) {

            JOptionPane optionPane = new JOptionPane(guessPanel, JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION, null, new String[]{"Cancel", "OK"}, "OK");
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
                    pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+)" + ".*$";
                    break;
            	case 3: // "4"
                    pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+)" + ".*$";
                    break;
            	case 4: // "1-2"
                    pattern = "^([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
                    break;
            	case 5: // "2-3"
                    pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
                    break;
            	case 6: // "3-4"
                    pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
                    break;
            	case 7: // "1-3"
                    pattern = "^([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
                    break;
            	case 8: // "2-4"
                    pattern = "^[^" + sDelimiter + "]+" + sDelimiter + "([^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+" + sDelimiter + "[^" + sDelimiter + "]+)" + ".*$";
            	}
            }
            if (b3.getModel() == group.getSelection()) {
                pattern = regexpEntry.getText();
            }

            // sanity check
            try {
                pattern.matches(pattern);
            } catch (PatternSyntaxException e) {
                JOptionPane.showMessageDialog(this, "This is not a valid regular expression");
                return null;
            }

            if (optionPane.getValue().equals("OK")) {
                System.err.println("Pattern = " + pattern);
                return pattern;
            } else {
                return null;
            }
        }
    }
