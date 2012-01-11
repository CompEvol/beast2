package org.jtikz;

import java.text.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

/**
 * A Graphics2D replacement for outputting in TikZ/PGF.
 *
 * Here is an example:
<pre>
TikzGraphics2D t = new TikzGraphics2D();
panel.paint(t);
</pre>
or, alternatively,
<pre>
t.paintComponent(frame);
</pre>
 *
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public class TikzGraphics2D extends AbstractGraphicsInterface {
    /**
     * The version of JTikZ.
     */
    public static final String VERSION  = "0.1";
    /**
     * The revision date for this version of JTikZ.
     */
    public static final String REV_DATE = "2009-10-18";

    Hashtable<Color,String> colors;
    String preamble;
    int colorId;

    /**
     * Creates a new TikzGraphics2D object that will output the code to <code>system.out</code>.
     */
    public TikzGraphics2D() {
        this(null);
    }

    /**
     * Creates a new TikzGraphics2D object that will output the code
     * to the given output stream.  If <code>os</code> is
     * <code>null</code> then it will default to
     * <code>system.out</code>.
     */
    public TikzGraphics2D(OutputStream os) {
        super(os);
        preamble = "";
        colorId = 0;
        colors = new Hashtable<Color,String>();
    }

    protected TikzGraphics2D newInstance() {
        return new TikzGraphics2D();
    }

    protected String colorToTikz(Color color) {
        if(getParent() != null)
            return ((TikzGraphics2D)getParent()).colorToTikz(color);
        String s = colors.get(color);
        if(s == null) {
            preamble += "\\definecolor{color" + colorId + "}{rgb}{" + (color.getRed() / 255.0) + "," + (color.getGreen() / 255.0) + "," + (color.getBlue() / 255.0) + "}\n";
            s = "color" + (colorId++) + (color.getAlpha() < 255 ? ",opacity=" + (color.getAlpha() / 255.0) : "");
            colors.put(color, s);
        }
        return s;
    }
    String handleOptions() {
        return handleOptions("");
    }
    String handleOptions(String options) {
        return handleOptions(options, false);
    }
    void addOption(StringBuffer oldOptions, String newOption) {
        if(!oldOptions.toString().equals("") && !newOption.equals(""))
            oldOptions.append(", ");
        oldOptions.append(newOption);
    }
    String handleOptions(String options, boolean isText) {
        StringBuffer o = new StringBuffer(options);
        if(!color.equals(Color.BLACK))
            addOption(o, (isText ? "text=" : "") + colorToTikz(color));
        if(color.getAlpha() != 255)
            addOption(o, (isText ? "text " : "") + "opacity=" + ((double)color.getAlpha() / 255.0));
        if(stroke.getDashArray() != null && stroke.getDashArray().length > 1) {
            StringBuilder builder = new StringBuilder();
            builder.append("dash pattern=");
            
            for (int i = 0; i < stroke.getDashArray().length; i++) {
                if (i % 2 == 0) {
                    builder.append("on ");
                } else {
                    builder.append("off ");
                }
                builder.append(stroke.getDashArray()[i] + " ");
            }
            addOption(o, builder.toString());
        }
        if(stroke.getLineWidth() != 1.0)
            addOption(o, "line width=" + stroke.getLineWidth() + "pt");
        if(o.toString().equals(""))
            return "";
        else
            return "[" + o + "]";
    }

    /**
     * Converts an arbitrary string to TeX.  This will
     * strip/replace/escape all necessary TeX commands.  For example,
     * "\n" will be replaced by "\\".
     */
    public String toTeX(String s) {
        return s.replaceAll("\n","\\\\")
                .replaceAll("&","\\&")
                .replaceAll("#","\\#");
    }

    public void drawRenderedImage(RenderedImage image, AffineTransform xform) {
        ColorModel c = image.getColorModel();
        Raster r = image.getData();
        for(int x=0; x<image.getWidth(); x++) {
            for(int y=0; y<image.getHeight(); y++) {
                /* TODO: implement this later! */
            }
        }
    }
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        /* TODO: implement the affine transform! */
        img.getSource().startProduction(new PixelConsumer(img, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, true));
        return true;
    }
    private class PixelConsumer extends PixelGrabber {
        public PixelConsumer(Image img, int x, int y, int w, int h, boolean forceRGB) {
            super(img, x, y, w, h, forceRGB);
        }
        public void handlesinglepixel(int x, int y, int pixel) {
            int alpha = (pixel >> 24) & 0xff;
            int red   = (pixel >> 16) & 0xff;
            int green = (pixel >>  8) & 0xff;
            int blue  = (pixel      ) & 0xff;
            addCommand("{\\pgfsys@color@rgb{" + (red / 255.0) + "}{" + (green / 255.0) + "}{" + (blue / 255.0) + "}\\fill (" + (x - 0.5) + "pt, " + (y - 0.5) + "pt) rectangle (" + (x + 0.5) + "pt, " + (y + 0.5) + "pt);}");
        }
    }
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        /* TODO: implement this later! */
    }

    protected void handleDrawString(String s, double x, double y) {
        addCommand("\\node" + handleOptions("",true) + " at (" + x + "pt, " + y + "pt) {" + toTeX(s) + "};");
    }

    protected void handlePath(PathIterator i, Action action) {
        handlePathInternal(i, action);
    }

    String handlePathInternal(PathIterator i, Action action) {
        double s[] = new double[6];
        String tikz = "";
        if(action.equals(Action.CLIP))
            tikz += "\\path[clip]";
        else
            tikz += "\\" + (action.equals(Action.FILL) ? "fill" : "draw") + handleOptions();
        while(!i.isDone()) {
            int type = i.currentSegment(s);
            i.next();
            switch(type) {
            case PathIterator.SEG_LINETO:
                tikz += " --";
            case PathIterator.SEG_MOVETO:
                tikz += " (" + s[0] + "pt, " + s[1] + "pt)";
                break;
            case PathIterator.SEG_QUADTO:
                // TODO: Implement this later!
                break;
            case PathIterator.SEG_CUBICTO:
                tikz += " .. (" + s[0] + "pt, " + s[1] + "pt) and (" + s[2] + "pt, " + s[3] + "pt) .. (" + s[4] + "pt, " + s[5] + "pt)";
                break;
            case PathIterator.SEG_CLOSE:
                tikz += " -- cycle";
                break;
            }
        }
        tikz += ";";
        if(!action.equals(Action.CLIP))
            addCommand(tikz);
        return tikz;
    }
    protected void handleLine(double x1, double y1, double x2, double y2) {
        addCommand("\\draw" + handleOptions() + " (" + x1 + "pt, " + y1 + "pt) -- (" + x2 + "pt, " + y2 + "pt);");
    }
    protected void handleOval(double x, double y, double width, double height, boolean fill) {
        double rw = width / 2.0;
        double rh = height / 2.0;
        double cx = x + rw;
        double cy = y + rh;
        addCommand("\\" + (fill ? "fill" : "draw") + handleOptions() + " (" + cx + "pt, " + cy + "pt) ellipse (" + rw + "pt and " + rh + "pt);");
    }
    protected void handleArc(double x, double y, double width, double height, int startAngle, int arcAngle, boolean fill) {
        double radiusx = width / 2.0;
        double radiusy = height / 2.0;
        double centerx = x + radiusx;
        double centery = y + radiusy;

        double startx = centerx + radiusx * Math.cos(Math.toRadians(-startAngle));
        double starty = centery + radiusy * Math.sin(Math.toRadians(-startAngle));
        
        addCommand("\\" + (fill ? "fill" : "draw") + handleOptions() + " (" + centerx + "pt, " + centery + "pt) -- (" + startx + "pt, " + starty + "pt) arc (" + (-startAngle) + ":" + ((-startAngle) - arcAngle) + ":" + radiusx + "pt and " + radiusy + "pt) -- cycle;");
    }
    
    protected void flushInternal() {
        if((preamble != null && !preamble.equals("")) || (getCommands() != null && !getCommands().isEmpty())) {
            out.print(preamble);
            out.println("\\begin{tikzpicture}[yscale=-1]");
            /* close out any existing clipping scopes */
            setClip(null);
            //out.print(tikz);
            int indent = 1;
            Shape lastClip = null;
            String tikz = "";
            for(GraphicsCommand c : commands) {
                if(!(c.getClip() == lastClip || c.getClip().equals(lastClip))) {
                    while(indent > 1) {
                        indent--;
                        for(int i=0; i<indent; i++)
                            out.print("  ");
                        out.println("\\end{scope}");
                    }
                    for(int i=0; i<indent; i++)
                        out.print("  ");
                    out.println("\\begin{scope}");
                    indent++;
                    for(int i=0; i<indent; i++)
                        out.print("  ");
                    out.print(handlePathInternal(c.getClip().getPathIterator(transform), Action.CLIP));
                    lastClip = c.clip;
                }
                for(int i=0; i<indent; i++)
                    out.print("  ");
                out.println(c.command);
            }
            while(indent > 1) {
                indent--;
                for(int i=0; i<indent; i++)
                    out.print("  ");
                out.println("\\end{scope}");
            }
            out.println("\\end{tikzpicture}");
        }

        preamble = "";
        colorId = 0;
        colors = new Hashtable<Color,String>();
    }

    protected void handlePolyline(double[] xPoints, double[] yPoints, int nPoints) {
        String tikz = "";
        tikz += "\\draw (" + xPoints[0] + "pt, " + yPoints[0] + "pt)";
        for(int i=1; i<nPoints; i++)
            tikz += " -- (" + xPoints[i] + "pt, " + yPoints[i] + "pt)";
        tikz += ";";
        addCommand(tikz);
    }

    protected void handleRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight, boolean fill) {
        String tikz = "";
        double angle = (double)(arcWidth + arcHeight) / 2.0;
        String options = "";
        if(angle != 0.0)
            options = "rounded corners =" + angle + "pt";
        tikz += "\\" + (fill ? "fill" : "draw") + handleOptions(options) + " (" + x + "pt, " + y + "pt) -- (" + (x + width - 1) + "pt, " + y + "pt) -- (" + (x + width - 1) + "pt, " + (y + height - 1) + "pt) -- (" + x + "pt, " + (y + height - 1) + "pt) -- cycle;";
        addCommand(tikz);
    }
    protected void handleClearRect(double x, double y, double width, double height) {
        addCommand("\\fill[" + colorToTikz(background) + "] (" + x + "pt, " + y + "pt) -- (" + (x + width - 1) + "pt, " + y + "pt) -- (" + (x + width - 1) + "pt, " + (y + height - 1) + "pt) -- (" + x + "pt, " + (y + height - 1) + "pt) -- cycle;");
    }
}
