package org.jtikz;

import java.awt.*;
import javax.swing.*;

public class GraphicsInterfaceRepaintManager extends RepaintManager {
    AbstractGraphicsInterface g;

    public GraphicsInterfaceRepaintManager(AbstractGraphicsInterface g) {
        this.g = g;
        setDoubleBufferingEnabled(false);
    }

    public Image getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return new GraphicsInterfaceImage(g.create());
    }

    public Image getVolatileOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return new GraphicsInterfaceImage(g.create());
    }
}