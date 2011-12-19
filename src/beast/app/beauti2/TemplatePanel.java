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

    public TemplatePanel(JFrame frame, BeautiDoc doc) {
        this.frame = frame;
        this.doc = doc;

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);
        panel.add(new JLabel("Template:"));
        panel.add(templateCombo);

        add(panel);

        List<AbstractAction> templateActions = getTemplateActions();
        for (AbstractAction a: templateActions) {
            templateCombo.addItem(a);
        }

        templateCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                AbstractAction a = (AbstractAction)templateCombo.getSelectedItem();
                a.actionPerformed(null);
            }
        });
    }

    class TemplateAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        String m_sFileName;

        public TemplateAction(File file) {
            super("xx");
            m_sFileName = file.getAbsolutePath();
            String sName = m_sFileName.substring(m_sFileName.lastIndexOf("/") + 1, m_sFileName.length() - 4);
            putValue(Action.NAME, sName);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (JOptionPane.showConfirmDialog(frame, "Changing templates means the information input so far will be lost. " +
                        "Are you sure you want to change templates?", "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION) ==
                        JOptionPane.YES_OPTION) {
                    doc.loadNewTemplate(m_sFileName);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Something went wrong loading the template: " + ex.getMessage());
            }
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
