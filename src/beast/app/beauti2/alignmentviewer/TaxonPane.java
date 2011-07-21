package beast.app.beauti2.alignmentviewer;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: TaxonPane.java,v 1.1 2005/11/01 23:52:04 rambaut Exp $
 */
public class TaxonPane extends JPanel {

	private JList taxonList;
	private DefaultListModel taxonListModel;

	public TaxonPane() {

		setLayout(new BorderLayout());

		taxonListModel = new DefaultListModel();
		taxonList = new JList(taxonListModel);
		taxonList.setFont(new Font("sansserif", Font.PLAIN, 10));

		add(taxonList, BorderLayout.CENTER);
	}

	public void setAlignmentBuffer(AlignmentBuffer alignment) {
		taxonListModel.removeAllElements();
		if (alignment != null) {
			for (int i = 0; i < alignment.getSequenceCount(); i++) {
				taxonListModel.addElement(alignment.getTaxonLabel(i));
			}
		}
	}

	public void setRowHeight(int rowHeight) {
		taxonList.setFixedCellHeight(rowHeight);
	}

}
