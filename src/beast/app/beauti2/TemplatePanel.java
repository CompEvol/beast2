package beast.app.beauti2;

import beast.app.beauti.BeautiDoc;
import beast.util.AddOnManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class TemplatePanel extends JPanel {
    private final JFrame frame;
    private BeautiDoc doc;

    private JComboBox templateCombo = new JComboBox();

    private TemplateAction currentTemplate = null;

    public TemplatePanel(JFrame frame, BeautiDoc doc) {
        this.frame = frame;
        this.doc = doc;

        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);
        panel.add(new JLabel("Template:"));
        panel.add(templateCombo);

        add(panel, BorderLayout.NORTH);

        JPanel panel2 = new JPanel(new BorderLayout());
        panel2.setBorder(BorderFactory.createTitledBorder((String)null));

        add(panel2, BorderLayout.CENTER);

        JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final JButton applyButton = new JButton("Apply");
        applyButton.setEnabled(false);
        final JButton revertButton = new JButton("Revert");
        revertButton.setEnabled(false);
        panel3.add(applyButton);
        panel3.add(new JSeparator());
        panel3.add(revertButton);

        add(panel3, BorderLayout.SOUTH);

        List<AbstractAction> templateActions = getTemplateActions();
        for (AbstractAction a: templateActions) {
            templateCombo.addItem(a);
        }

        currentTemplate = (TemplateAction)templateCombo.getSelectedItem();

        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                TemplateAction a = (TemplateAction)templateCombo.getSelectedItem();
                a.actionPerformed(null);
                if (currentTemplate == a) {
                    // the template was loaded...
                    applyButton.setEnabled(false);
                    revertButton.setEnabled(false);
                }
            }
        });

        revertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                templateCombo.setSelectedItem(currentTemplate);
            }
        });

        templateCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (templateCombo.getSelectedItem() != currentTemplate) {
                    applyButton.setEnabled(true);
                    revertButton.setEnabled(true);
                } else {
                    applyButton.setEnabled(false);
                    revertButton.setEnabled(false);
                }
            }
        });
    }

    class TemplateAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private final File file;
        private final String name;

        public TemplateAction(File file) {
            super(file.getName());

            this.file = file;
            // AR files don't necessarily have 3 character suffixes...
//            m_sFileName = file.getAbsolutePath();
//            String name = m_sFileName.substring(m_sFileName.lastIndexOf("/") + 1, m_sFileName.length() - 4);
            String fileName = file.getName();
            name = (fileName.toLowerCase().endsWith(".xml") ? fileName.substring(0, fileName.length() - 4) : fileName);
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (JOptionPane.showConfirmDialog(frame, "Changing templates means the information input so far will be lost. " +
                        "Are you sure you want to change templates?", "Are you sure?", JOptionPane.YES_NO_OPTION) ==
                        JOptionPane.YES_OPTION) {
                    doc.loadNewTemplate(file.getAbsolutePath());
                    currentTemplate = this;
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Something went wrong loading the template: " + ex.getMessage());
            }
            // revert the selection
            templateCombo.setSelectedItem(currentTemplate);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private List<AbstractAction> getTemplateActions() {
        List<AbstractAction> actions = new ArrayList<AbstractAction>();
        List<String> sBeastDirectories = AddOnManager.getBeastDirectories();
        for (String sDir : sBeastDirectories) {
            File dir = new File(sDir + "/templates");
            getTemplateActionForDir(dir, actions);
        }
        return actions;
    }

    private void getTemplateActionForDir(File dir, List<AbstractAction> actions) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File template : files) {
                    if (template.getName().toLowerCase().endsWith(".xml")) {
                        try {
                            String sXML2 = BeautiDoc.load(template.getAbsolutePath());
                            if (sXML2.contains("<mergepoint ")) {
                                actions.add(new TemplateAction(template));
                            }
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

}
