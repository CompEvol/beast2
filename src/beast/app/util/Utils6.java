package beast.app.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import beast.core.util.Log;
import beast.util.AddOnManager;

/** Utils that work with Java6 **/
public class Utils6 {

    public static class Canvas extends JComponent {
		private static final long serialVersionUID = 1L;
		
		Image imageBuffer;
        public Canvas() { }

        @Override
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

        @Override
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


	public static ImageIcon getIcon(String iconLocation) {
	    try {
	        URL url = ClassLoader.getSystemResource(iconLocation);
	        if (url == null) {
	        	Log.warning.println("Cannot find icon " + iconLocation);
	            return null;
	        }
	        ImageIcon icon = new ImageIcon(url);
	        return icon;
	    } catch (Exception e) {
	    	Log.warning.println("Cannot load icon " + iconLocation + " " + e.getMessage());
	        return null;
	    }
	}
	
	public static boolean testCudaStatusOnMac() {
		String cudaStatusOnMac = "<html>It appears you have CUDA installed, but your computer hardware does not support it.<br>"
				+ "You need to remove CUDA before BEAST/BEAUti can start.<br>"
				+ "To remove CUDA, delete the following folders by typing in a terminal:<br>"
				+ "rm -r /Library/Frameworks/CUDA.framework<br>"
				+ "rm -r /Developer/NVIDIA<br>"
				+ "rm -r /usr/local/cuda<br>"
				+ "You may need 'sudo rm' instead of 'rm'</html>";
				
		if (isMac()) {
			// check any of these directories exist
			// /Library/Frameworks/CUDA.framework
			// /Developer/NVIDIA
			// /usr/local/cuda
			if (new File("/Library/Frameworks/CUDA.framework").exists() ||
					new File("/Developer/NVIDIA").exists() ||
					new File("/usr/local/cuda").exists()) {
				// there is evidence of CUDA being installed on this computer
				// try to create a BeagleTreeLikelihood using a separate process
				try {
				      String java = System.getenv("java.home");
				      if (java == null) {
				    	  java ="/usr/bin/java";
				      } else {
				    	  java += "/bin/java";
				      }
				      String beastJar = AddOnManager.getPackageUserDir();
				      beastJar += "/" + "BEAST" + "/" + "lib" + "/" + "beast.jar";
				      if (!new File(beastJar).exists()) {
				    	  Log.debug.println("Could not find beast.jar, giving up testCudaStatusOnMac");
				    	  return true;
				      }
				      //beastJar = "\"" + beastJar + "\"";
				      //beastJar = "/Users/remco/workspace/beast2/build/dist/beast.jar";
				      Process p = Runtime.getRuntime().exec(new String[]{java , "-cp" , beastJar , "beast.app.util.Utils"});
				      BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				      String line;
				      while ((line = input.readLine()) != null) {
				        Log.debug.println(line);
				      }
				      input.close();
				      if (p.exitValue() != 0) {
				    	  if (GraphicsEnvironment.isHeadless()) {
				    		  cudaStatusOnMac = cudaStatusOnMac.replaceAll("<br>", "\n");
				    		  cudaStatusOnMac = cudaStatusOnMac.replaceAll("<.?html>","\n");
				    		  Log.warning.println("WARNING: " + cudaStatusOnMac);
				    	  } else {
				    		  JOptionPane.showMessageDialog(null, cudaStatusOnMac);
				    	  }
				    	  return false;
				      }
				    }
				    catch (Exception err) {
				      err.printStackTrace();
				    }
			}
			
		}
		return true;
	}


    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().startsWith("linux");
    }
}
