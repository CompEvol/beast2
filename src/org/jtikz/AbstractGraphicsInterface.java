package org.jtikz;

import java.text.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

public abstract class AbstractGraphicsInterface extends Graphics2D implements Closeable, Flushable {
    LinkedList<AbstractGraphicsInterface> children;
    AbstractGraphicsInterface parent;
    AffineTransform transform;
    boolean closed;
    String preamble;
    Shape currentClip;
    Color color;
    Font font;
    Thread shutdownHook;
    Color background;
    BasicStroke stroke;
    LinkedList<GraphicsCommand> commands;

    protected PrintStream out;

    protected static enum Action { DRAW, FILL, CLIP };

    protected void addCommand(Object command) {
        if(parent != null)
            parent.addCommand(command);
        else {
            System.err.println("Command: " + command);
            commands.addLast(new GraphicsCommand(command, this));
        }
    }

    protected LinkedList<GraphicsCommand> getCommands() {
        return commands;
    }

    protected abstract AbstractGraphicsInterface newInstance();

    public final AbstractGraphicsInterface create() {
        System.err.println("create()");
        AbstractGraphicsInterface g = newInstance();
        children.add(g);
        g.parent = this;
        g.setClip(getClip());
        g.transform = new AffineTransform(transform);
        System.err.println("Transform: " + g.transform);
        return g;
    }
    public final AbstractGraphicsInterface create(int x, int y, int width, int height) {
        System.err.println("create(" + x + ", " + y + ", " + width + ", " + height + ", " + currentClip + ")");
        AbstractGraphicsInterface g = create();
        g.setClip(x, y, width, height);
        g.translate(x, y);
        return g;
    }

    protected AbstractGraphicsInterface getParent() {
        return parent;
    }

    public AbstractGraphicsInterface() {
        this(System.out);
    }
    public AbstractGraphicsInterface(OutputStream os) {
        if(os == null)
            os = System.out;
        if(os instanceof PrintStream)
            out = (PrintStream)os;
        else
            out = new PrintStream(os);
        parent = null;
        transform = new AffineTransform();
        closed = false;
        preamble = "";
        shutdownHook = new Thread() {
                public void run() {
                    flush();
                }
            };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        flush();
    }

    public void setColor(Color c) {
        this.color = c;
    }
    public Color getColor() {
        return color;
    }

    public void draw(Shape shape) {
        handlePath(shape.getPathIterator(transform), Action.DRAW);
    }

    public void fill(Shape shape) {
        handlePath(shape.getPathIterator(transform), Action.FILL);
    }

    public void drawPolygon(Polygon p) {
        handlePath(p.getPathIterator(transform), Action.DRAW);
    }
    public void fillPolygon(Polygon p) {
        handlePath(p.getPathIterator(transform), Action.FILL);
    }
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        fillPolygon(new Polygon(xPoints, yPoints, nPoints));
    }
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        drawPolygon(new Polygon(xPoints, yPoints, nPoints));
    }
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if(nPoints < 2)
            return;
        else if(xPoints[0] == xPoints[nPoints - 1] && yPoints[0] == yPoints[nPoints - 1])
            drawPolygon(xPoints, yPoints, nPoints);
        else {
            double newx[] = new double[xPoints.length];
            double newy[] = new double[yPoints.length];
            Point2D.Double p1 = new Point2D.Double();
            Point2D.Double p2 = new Point2D.Double();
            for(int i=0; i<nPoints; i++) {
                p1.x = xPoints[i];
                p1.y = yPoints[i];
                transform.transform(p1, p2);
                newx[i] = p2.getX();
                newy[i] = p2.getY();
            }
            handlePolyline(newx, newy, nPoints);
        }
    }

    protected abstract void handlePath(PathIterator path, Action action);

    /**
     * By default this implementation calls handleLine() for each line
     * segment.  You can extend this function if you would like a more
     * intelligent implementation given your specific interface.
     */
    protected void handlePolyline(double[] xPoints, double[] yPoints, int nPoints) {
        for(int i=1; i<nPoints; i++)
            handleLine(xPoints[i-1], yPoints[i-1], xPoints[i], yPoints[i]);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        Point2D p1 = transform.transform(new Point2D.Double(x1, y1), null);
        Point2D p2 = transform.transform(new Point2D.Double(x2, y2), null);
        handleLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    protected abstract void handleLine(double x1, double y1, double x2, double y2);

    public void drawOval(int x, int y, int width, int height) {
        handleOval(x, y, width, height, false);
    }
    public void fillOval(int x, int y, int width, int height) {
        handleOval(x, y, width, height, true);
    }
    private void handleOval(int x, int y, int width, int height, boolean fill) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleOval(p1.getX(), p1.getY(), width, height, fill);
    }
    protected abstract void handleOval(double x, double y, double width, double height, boolean fill);

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        handleRoundRect(x, y, width, height, arcWidth, arcHeight, true);
    }
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        handleRoundRect(x, y, width, height, arcWidth, arcHeight, false);
    }
    public void fillRect(int x, int y, int width, int height) {
        handleRoundRect(x, y, width, height, 0, 0, true);
    }
    public void drawRect(int x, int y, int width, int height) {
        handleRoundRect(x, y, width, height, 0, 0, false);
    }
    private void handleRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean fill) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleRoundRect(p1.getX(), p1.getY(), width, height, arcWidth, arcHeight, fill);
    }
    protected abstract void handleRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight, boolean fill);

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        handleArc(x, y, width, height, startAngle, arcAngle, false);
    }
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        handleArc(x, y, width, height, startAngle, arcAngle, true);
    }
    private void handleArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleArc(p1.getX(), p1.getY(), width, height, startAngle, arcAngle, fill);
    }
    protected abstract void handleArc(double x, double y, double width, double height, int startAngle, int arcAngle, boolean fill);

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        /* TODO: implement this later! */
    }

    public void clearRect(int x, int y, int width, int height) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleClearRect(p1.getX(), p1.getY(), width, height);
    }
    protected abstract void handleClearRect(double x, double y, double width, double height);

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color color) {
        background = color;
    }

    public Composite getComposite() {
        return AlphaComposite.getInstance(AlphaComposite.SRC);
    }

    public void setComposite(Composite composite) {
        /* TODO: implement this later! */
    }

    public Paint getPaint() {
        return color;
    }

    public void setPaint(Paint paint) {
        if(paint instanceof Color)
            setColor((Color)paint);
        else
            return; /* TODO: implement this later! */
    }

    public void flush() {
        if(closed)
            throw new IllegalStateException("This AbstractGraphicsInterface has already been closed!");
        if(parent != null)
            return;
        flushInternal();
        children = new LinkedList<AbstractGraphicsInterface>();
        font = new Font("Arial", Font.PLAIN, 12);
        stroke = new BasicStroke();
        color = Color.BLACK;
        background = Color.WHITE;
        preamble = "";
        commands = new LinkedList<GraphicsCommand>();
    }

    protected abstract void flushInternal();

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public FontMetrics getFontMetrics() {
        //return new FontMetrics(font);
        return null; /* TODO: fix this later! */
    }

    public FontMetrics getFontMetrics(Font font) {
        //return new FontMetrics(font);
        return null; /* TODO: fix this later! */
    }

    public void setXORMode(Color c1) {
        /* TODO: implement this later! */
    }

    public void setPaintMode() {
        /* TODO: implement this later! */
    }

    public void dispose() {
        close();
    }

    public void close() {
        if(!closed) {
            for(AbstractGraphicsInterface child : children)
                child.close();
            flush();
            closed = true;
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

    public FontRenderContext getFontRenderContext() {
        return new FontRenderContext(transform, true, true);
    }

    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    public void transform(AffineTransform transform) {
        this.transform.concatenate(transform);
    }

    public void setTransform(AffineTransform transform) {
        this.transform.setTransform(transform);
    }

    public void setClip(Shape clip) {
        /* close off existing clips */
        this.currentClip = clip;
    }

    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle2D.Double(x, y, width, height));
    }

    public void clip(Shape clip) {
        /* TODO: fix this such that it actually intersects the clips! */
        setClip(clip);
    }

    public void clipRect(int x, int y, int width, int height) {
        clip(new Rectangle2D.Double(x, y, width, height));
    }

    public Shape getClip() {
        return currentClip;
    }

    public Rectangle getClipBounds() {
        return currentClip == null ? null : currentClip.getBounds();
    }

    public void drawString(String s, int x, int y) {
        drawString(s, (float)x, (float)y);
    }
    public void drawString(String s, float x, float y) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleDrawString(s, p1.getX(), p1.getY());
    }
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        StringBuffer s = new StringBuffer("");
        for(char c = iterator.first(); c != CharacterIterator.DONE; c = iterator.next())
            s.append(c);
        drawString(s.toString(), x, y);
    }
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float)x, (float)y);
    }
    protected abstract void handleDrawString(String s, double x, double y);

    private static class Repainter implements Runnable {
        Component component;
        Graphics g;
        public Repainter(Component component, Graphics g) {
            this.component = component;
            this.g = g;
        }
        public void run() {
            //component.repaint();
            component.paint(g);
        }
    }

    public void paintComponent(Component component) {
        javax.swing.RepaintManager old = javax.swing.RepaintManager.currentManager(component);
        javax.swing.RepaintManager.setCurrentManager(new GraphicsInterfaceRepaintManager(this));
        //component.paint(this);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Repainter(component, this));
        } catch(Exception e) { }
        javax.swing.RepaintManager.setCurrentManager(old);
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

    public void drawImage(BufferedImage image, BufferedImageOp op, int x, int y) { 
        BufferedImage img1 = op.filter(image, null);
        drawImage(img1, new AffineTransform(1f,0f,0f,1f,x,y), null);
    }
    public abstract boolean drawImage(Image img, AffineTransform xform, ImageObserver obs);

    public abstract void drawRenderableImage(RenderableImage img, AffineTransform xform);

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        if(stroke instanceof BasicStroke)
            this.stroke = (BasicStroke)stroke;
        else
            return; /* TODO: implement this later! */
    }

    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    public void translate(int x, int y) {
        translate((double)x, (double)y);
    }

    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    public void scale(double sx, double sy) {
        transform.shear(sx, sy);
    }

    public void rotate(double theta) {
        transform.rotate(theta);
    }

    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    public Object getRenderingHint(RenderingHints.Key key) {
        return null;
    }

    public RenderingHints getRenderingHints() {
        return new RenderingHints(null);
    }

    public void addRenderingHints(Map<?,?> hints) {
        /* TODO: implement this later ! */
    }

    public void addRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        /* TODO: implement this later ! */
    }

    public void setRenderingHints(Map<?,?> hints) {
        /* TODO: implement this later ! */
    }

    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        /* TODO: implement this later ! */
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return null; /* TODO: implement this later! */
    }

    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return true; /* TODO: implement this more intelligently later! */
    }

    public void drawGlyphVector(GlyphVector g, float x, float y) {
        /* TODO: implement this later! */
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) { return true; }
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) { return true; }
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) { return true; }
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) { return true; }
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) { return true; }
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) { return true; }
}