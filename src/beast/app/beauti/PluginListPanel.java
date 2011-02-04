package beast.app.beauti;

import beast.app.draw.PluginDialog;
import beast.core.Plugin;

import javax.swing.*;
import java.awt.*;

/**
 * @author Alexei Drummond
 */
public class PluginListPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	PluginDialog dialog;
    JList pluginList;

    public PluginListPanel(Plugin[] plugins, PluginDialog dialog) {

        this.dialog = dialog;
        pluginList = new JList(plugins);
        pluginList.setCellRenderer(new PluginCellRenderer());

        setLayout(new BorderLayout());
        add(BorderLayout.WEST, pluginList);
        add(BorderLayout.CENTER, dialog);
    }

    class PluginCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		public PluginCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            return super.getListCellRendererComponent(list, ((Plugin) value).getID(), index, isSelected, cellHasFocus);
        }
    }

}
