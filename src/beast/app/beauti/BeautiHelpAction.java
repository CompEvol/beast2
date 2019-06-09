package beast.app.beauti;

import java.awt.event.ActionEvent;

import beast.app.draw.MyAction;

/** Sub-classes of this class in packages will get
 * a menu item in the Help menu in BEAUti
 * 
 * Sub-classes must implement a constructor with argument BeautiDoc, e.g.,
 *  
 * MyBeautiHelpAction(BeautiDoc doc) {
 *    super("My help", "Gives help the way I like it", "myhelp", -1);
 *    this.doc = doc;
 * }
 * 
 */
public class BeautiHelpAction extends MyAction {
	private static final long serialVersionUID = 1L;

	public BeautiHelpAction(String name, String toolTipText, String icon, int acceleratorKey) {
		super(name, toolTipText, icon, acceleratorKey);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
	}

}
