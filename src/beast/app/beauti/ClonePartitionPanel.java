package beast.app.beauti;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class ClonePartitionPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    final BeautiPanel beautiPanel;
    final JComboBox<Object> cloneFromComboBox;
    final JButton okButton = new JButton("OK");

	public ClonePartitionPanel(BeautiPanel beautiPanel) {
        this.beautiPanel = beautiPanel;

        DefaultListModel<String> listModel = beautiPanel.listModel;
        Object[] models = new Object[listModel.getSize()];
        for(int i=0; i < listModel.getSize(); i++){
            models[i] = listModel.getElementAt(i);
        }

        cloneFromComboBox = new JComboBox<>(models);
        // has to be editable
        cloneFromComboBox.setEditable(true);
        // change the editor's document
        new S11InitialSelection(cloneFromComboBox);

        init();
    }


    public void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel jPanel = new JPanel(new FlowLayout());

        JLabel label = new JLabel("Clone from");
        jPanel.add(label);

        cloneFromComboBox.setMaximumRowCount(10);
        jPanel.add(cloneFromComboBox);

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(jPanel);
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(5));

        okButton.setName("ok");
        okButton.setToolTipText("Click to clone configuration from the above selected partition " +
                "into all selected partitions on the left.");
        okButton.addActionListener(e -> {
                clonePartitions();
            });
        add(okButton);

    } // init

    protected void clonePartitions() {
        String sourceId = cloneFromComboBox.getSelectedItem().toString();

        for (Object targetId : beautiPanel.listOfPartitions.getSelectedValuesList()) {
             beautiPanel.cloneFrom(sourceId, targetId.toString());
        }
    }
}
