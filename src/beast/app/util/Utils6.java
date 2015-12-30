package beast.app.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/** Utils that work with Java6 **/
public class Utils6 {

    public static class Canvas extends JComponent {
		private static final long serialVersionUID = 1L;
		
		Image imageBuffer;
        public Canvas() { }

        public void paintComponent( Graphics g ) {
            // copy buffered image
            if ( imageBuffer != null )
                g.drawImage(imageBuffer, 0,0, this);
        }

        /**
         Get a buffered (persistent) image for drawing on this component
         */
        public Graphics getBufferedGraphics() {
            Dimension dim = getSize();
            imageBuffer = createImage( dim.width, dim.height );
            return imageBuffer.getGraphics();
        }

        public void setBounds( int x, int y, int width, int height ) {
            setPreferredSize( new Dimension(width, height) );
            setMinimumSize( new Dimension(width, height) );
            super.setBounds( x, y, width, height );
        }
    }

    //Splash
    static Window splashScreen;
    /*
        This could live in the desktop script.
        However we'd like to get it on the screen as quickly as possible.
    */
    public static void startSplashScreen()
    {
        Image img = getIcon("beast/app/draw/icons/beauti.png").getImage();
        int width=img.getWidth(null), height=img.getHeight(null);
        Window win=new Window( new Frame() );
        win.pack();
        Canvas can = new Canvas();
        can.setSize( width, height ); // why is this necessary?
        Toolkit tk=Toolkit.getDefaultToolkit();
        Dimension dim=tk.getScreenSize();
        win.setBounds(
                dim.width/2-width/2, dim.height/2-height/2, width, height );
        win.add("Center", can);
//        Image img=tk.getImage(
//                Utils.class.getResource("beast.png") ); //what
        MediaTracker mt=new MediaTracker(can);
        mt.addImage(img,0);
        try { mt.waitForAll(); } catch ( Exception e ) { }
        Graphics gr=can.getBufferedGraphics();
        gr.drawImage(img, 0, 0, can);
        win.setVisible(true);
        win.toFront();
        splashScreen = win;
    }

    public static void endSplashScreen() {
        if ( splashScreen != null )
            splashScreen.dispose();
    }


	public static ImageIcon getIcon(String sIconLocation) {
	    try {
	        URL url = (URL) ClassLoader.getSystemResource(sIconLocation);
	        if (url == null) {
	            System.err.println("Cannot find icon " + sIconLocation);
	            return null;
	        }
	        ImageIcon icon = new ImageIcon(url);
	        return icon;
	    } catch (Exception e) {
	        System.err.println("Cannot load icon " + sIconLocation + " " + e.getMessage());
	        return null;
	    }
	}
}
