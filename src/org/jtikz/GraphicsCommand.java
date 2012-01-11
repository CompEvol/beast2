package org.jtikz;

import java.awt.Shape;

public class GraphicsCommand {
    Object command;
    Shape clip;
    public GraphicsCommand(Object command, AbstractGraphicsInterface creator) {
        this.command = command;
        clip = creator.getClip();
        AbstractGraphicsInterface t = creator;
        while(clip == null && t.parent != null) {
            t = t.parent;
            clip = t.getClip();
        }
        System.err.println("Clip: " + clip);
    }
    public Object getCommand() {
        return command;
    }
    public Shape getClip() {
        return clip;
    }
}
