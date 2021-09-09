package beast.app.inputeditor;

import java.awt.Image;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import beast.app.draw.ModelBuilder;
import beast.pkgmgmt.BEASTClassLoader;

/**
 * @author Alexei Drummond
 */
public class SmallButton extends JButton {

	private static final long serialVersionUID = 1L;

	public enum ButtonType {roundRect, square, toolbar}
    
    public SmallButton(String label, boolean isEnabled) {
        this(label, isEnabled, ButtonType.square);
        setIcon(label);
    }

	public SmallButton(String label, boolean isEnabled, ButtonType buttonType) {
        super(label);
        setEnabled(isEnabled);
        setButtonType(buttonType);
        setIcon(label);
    }

	private void setIcon(String label) {
        if (label.equals("e")) {
        	setText("");
            URL url = BEASTClassLoader.classLoader.getResource(ModelBuilder.ICONPATH + "edit.png");
            Icon icon = new ImageIcon(url);
        	setIcon(icon);
            setBorder(BorderFactory.createEmptyBorder());
        }
	}

    public void setButtonType(ButtonType buttonType) {
        putClientProperty("JButton.buttonType", buttonType.toString());    
    }
    
    public void setImg(Image image) {
        setIcon(new ImageIcon(image));
    }

}
