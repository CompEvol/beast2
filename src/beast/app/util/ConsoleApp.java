package beast.app.util;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import beast.base.Log;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/** console application with coloured output **/
public class ConsoleApp extends JFrame {

	private static final long serialVersionUID = 1L;
	private JTextPane textPane;
	static String title = "BEAST " + new beast.base.BEASTVersion2().getVersionString();

	public ConsoleApp() {
		super();
	}

	public ConsoleApp(String title, String header, Object o) {
		ConsoleApp.title = ConsoleApp.title + " " + title;
		ConsoleApp.main(new String[] {});
	}

	public void initComponents() {
		textPane = new JTextPane();

		PrintStream p1 = new PrintStream(new BOAS(Color.blue));
		PrintStream p2 = new PrintStream(new BOAS(Color.red));
		PrintStream p3 = new PrintStream(new BOAS(Color.green));
		System.setOut(p1);
		System.setErr(p2);
		Log.err = p2;
		Log.warning = p2;
		Log.info = p1;
		Log.debug = p3;
		Log.trace = p3;

		JPanel panel = new JPanel();
		JScrollPane scroller = new JScrollPane(textPane);
		panel.setPreferredSize(new Dimension(1024, 600));
		panel.add(scroller, BorderLayout.CENTER);

		textPane.setBackground(Color.white);
		textPane.setAlignmentX(LEFT_ALIGNMENT);

		getContentPane().add(scroller);
		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setTitle(title);
	}

	class Message {
		public Message(String data, Color style) {
			this.data = data;
			this.style = style;
		}

		String data;
		Color style;
	};

	void logToView(String _data, Color _style) {
		Color c = _style;
		AttributeSet aset = null;
		try {
			StyleContext sc = StyleContext.getDefaultStyleContext();
			aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

			aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
		} catch (Exception e) {
			// ignore
		}

		int len = textPane.getDocument().getLength();
		textPane.setCaretPosition(len);
		if (aset != null) {
			textPane.setCharacterAttributes(aset, false);
		}
		textPane.replaceSelection(_data + "\n");
	}

	/** logging with colour **/
	class BOAS extends ByteArrayOutputStream {
		Color style;
		StringBuilder buf = new StringBuilder();

		BOAS(Color style) {
			this.style = style;
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			super.write(b, off, len);
			log(b, off, len);
		};

		@Override
		public synchronized void write(int b) {
			super.write(b);
			log(b);
		};

		@Override
		public void write(byte[] b) throws java.io.IOException {
			super.write(b);
			log(b);
		};

		private void log(byte[] b, int off, int len) {
			for (int i = off; i < len; i++) {
				log(b[i]);
			}
		}

		private void log(int b) {
			if (b == '\n') {
				logToView(buf.toString(), style);
				buf = new StringBuilder();
			} else {
				buf.append((char) b);
			}

		}

		private void log(byte[] b) {
			for (byte i : b) {
				if (i == 0) {
					return;
				}
				log(i);
			}
		}

		@Override
		public void flush() throws java.io.IOException {
			super.flush();
		};

		@Override
		public void close() throws IOException {
			super.close();
		}
	};

	public static void main(String[] args) {
		ConsoleApp browser = new ConsoleApp();
		browser.initComponents();
		browser.setTitle(title);
		browser.setVisible(true);
	}
}
