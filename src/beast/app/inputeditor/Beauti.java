package beast.app.inputeditor;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

abstract public class Beauti extends JTabbedPane {

	public JFrame frame;
	
    abstract public void autoSetClockRate(boolean flag);
    abstract public void allowLinking(boolean flag);
    abstract public void autoUpdateFixMeanSubstRate(boolean flag);

}
