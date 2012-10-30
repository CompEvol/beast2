package beast.app.draw;

import javax.swing.*;

import java.awt.*;
import java.net.URL;

/**
 * @author Alexei Drummond
 */
public class SmallButton extends JButton {

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
        	setLabel("");
            URL url = ClassLoader.getSystemResource(ModelBuilder.ICONPATH + "edit.png");
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
