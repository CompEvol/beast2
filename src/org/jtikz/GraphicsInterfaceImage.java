package org.jtikz;

import java.awt.*;
import java.awt.image.*;

public class GraphicsInterfaceImage extends Image {
    AbstractGraphicsInterface g;

    public GraphicsInterfaceImage(AbstractGraphicsInterface g) {
        this.g = g;
    }

    public void flush() {
        g.flush();
    }

    public Graphics getGraphics() {
        return g.create();
    }

    public int getWidth(ImageObserver io) {
        return Integer.MAX_VALUE;
    }

    public int getHeight(ImageObserver io) {
        return Integer.MAX_VALUE;
    }

    public ImageProducer getSource() {
        return null;
    }

    public Object getProperty(String name, ImageObserver observer) {
        return null;
    }
}