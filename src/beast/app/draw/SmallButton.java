package beast.app.draw;

import javax.swing.*;
import java.awt.*;

/**
 * @author Alexei Drummond
 */
public class SmallButton extends JButton {

    public enum ButtonType {roundRect, square, toolbar}
    
    public SmallButton(String label, boolean isEnabled) {
        this(label, isEnabled, ButtonType.square);
    }

    public SmallButton(String label, boolean isEnabled, ButtonType buttonType) {
        super(label);
        setEnabled(isEnabled);
        setButtonType(buttonType);
    }

    public void setButtonType(ButtonType buttonType) {
        putClientProperty("JButton.buttonType", buttonType.toString());    
    }
    
    public void setImg(Image image) {
        setIcon(new ImageIcon(image));
    }

}
