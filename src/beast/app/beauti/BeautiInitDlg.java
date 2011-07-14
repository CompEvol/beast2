package beast.app.beauti;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.InputEditor;
import beast.app.draw.PluginPanel;
import beast.app.draw.ValidateListener;
import beast.app.draw.InputEditor.EXPAND;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.util.NexusParser;
import beast.util.XMLParser;
import beast.util.XMLProducer;

public class BeautiInitDlg extends JDialog implements ValidateListener {
	private static final long serialVersionUID = 1L;

	public enum ActionOnExit {
		UNKNOWN, SHOW_DETAILS_USE_TEMPLATE, SHOW_DETAILS_USE_XML_SPEC, WRITE_XML
	}

	BeautiInitDlg m_dlg;
	// Plugin m_plugin;
	JButton m_templateButton;
	JButton m_beastButton;
	JButton m_generateXMLButton;

	JButton m_startBeastButton;
	JButton m_startTemplateButton;

	BeautiDoc m_doc;
	ActionOnExit m_endState = ActionOnExit.UNKNOWN;
	String m_sOutputFileName = "beast.xml";
	String m_sXML;
	String m_sXMLName;
	String m_sTemplateXML;
	String m_sTemplateName;
	String m_sTemplateFileName;

	public BeautiInitDlg(String[] args, BeautiDoc doc) {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		m_doc = doc;
		m_dlg = this;
		parseArgs(args);
		InputEditor.g_bExpertMode = true;
		init();
		InputEditor.g_bExpertMode = false;
		if (m_endState != ActionOnExit.UNKNOWN) {
			dispose();
		} else {
			setModalityType(DEFAULT_MODALITY_TYPE);
			setVisible(true);
		}
	}

	void parseArgs(String[] args) {
		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (args[i].equals("")) {
					i += 1;
				} else if (args[i].equals("-h") || args[i].equals("-help")) {
					showUsageAndExit();
				} else if (args[i].equals("-xml")) {
					String sFileName = args[i + 1];
					m_sXML = load(sFileName);
					// XMLParser parser = new XMLParser();
					// m_doc.m_mcmc.setValue(parser.parseFile(sFileName),
					// m_doc);
					m_sXMLName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-template")) {
					String sFileName = args[i + 1];
					processTemplate(sFileName);
					m_sTemplateFileName = sFileName;
					m_sTemplateName = nameFromFile(sFileName);
					i += 2;
				} else if (args[i].equals("-nex")) {
					// NB: multiple -nex/-xmldata commands can be processed!
					String sFileName = args[i + 1];
					NexusParser parser = new NexusParser();
					parser.parseFile(sFileName);
					if (parser.m_filteredAlignments.size() > 0) {
						for (Alignment data : parser.m_filteredAlignments) {
							m_doc.m_alignments.setValue(data, m_doc);
						}
					} else {
						m_doc.m_alignments.setValue(parser.m_alignment, m_doc);
					}
					i += 2;
				} else if (args[i].equals("-xmldata")) {
					// NB: multiple -xmldata/-nex commands can be processed!
					String sFileName = args[i + 1];
					Plugin alignment = AlignmentListInputEditor.getXMLData(sFileName);
					m_doc.m_alignments.setValue(alignment, m_doc);
					i += 2;
				} else if (args[i].equals("-exitaction")) {
					if (args[i + 1].equals("writexml")) {
						m_endState = ActionOnExit.WRITE_XML;
					} else if (args[i + 1].equals("usetemplate")) {
						m_endState = ActionOnExit.SHOW_DETAILS_USE_TEMPLATE;
					} else if (args[i + 1].equals("usexml")) {
						m_endState = ActionOnExit.SHOW_DETAILS_USE_XML_SPEC;
					} else {
						throw new Exception("Expected one of 'writexml','usetemplate' or 'usexml', not " + args[i + 1]);
					}
					i += 2;
				} else if (args[i].equals("-out")) {
					m_sOutputFileName = args[i + 1];
					i += 2;
				}
				if (i == iOld) {
					throw new Exception("Wrong argument: " + args[i]);
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	} // parseArgs

	void showUsageAndExit() {
		System.out.println(usage());
		System.exit(0);
	}

	String usage() {
		return "java Beauti [options]\n" + "where options can be one of the following:\n"
				+ "-template [template file]\n" + "-nex [nexus data file]\n" + "-xmldat [beast xml file]\n"
				+ "-xml [beast file]\n" + "-out [output file name]\n" + "-exitaction [writexml|usetemplate|usexml]\n";
	}

	String nameFromFile(String sFileName) {
		if (sFileName.contains("/")) {
			return sFileName.substring(sFileName.lastIndexOf("/") + 1, sFileName.length() - 4);
		} else if (sFileName.contains("\\")) {
			return sFileName.substring(sFileName.lastIndexOf("\\") + 1, sFileName.length() - 4);
		}
		return sFileName.substring(0, sFileName.length() - 4);
	}

	void init() {
		try {
			setTitle("Beauti Start Dialog");

			setLayout(new BorderLayout());
			JLabel label = new JLabel(getIcon("0"));
			label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			add(label, BorderLayout.WEST);

			add(createCentralPanel(), BorderLayout.CENTER);
			// add(createOkCancelBox(), BorderLayout.SOUTH);
			setSize(new Dimension(600, 500));
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Could not create dialog: " + e.getMessage());
		}

	} // init

	Box m_createNewBox;
	Box m_loadExistingBox;

	Box createCentralPanel() throws Exception {
		Box centralBox = Box.createVerticalBox();
		m_createNewBox = Box.createVerticalBox();
		m_loadExistingBox = Box.createVerticalBox();

		Box radioBox = Box.createVerticalBox();
		// Create the radio buttons.
		JRadioButton firstButton = new JRadioButton(BeautiConfig.getButtonLabel(this, "Create new specification"));

		JRadioButton secondButton = new JRadioButton(BeautiConfig.getButtonLabel(this, "Load existing file"));

		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(firstButton);
		group.add(secondButton);

		// Register a listener for the radio buttons.
		firstButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_createNewBox.setVisible(true);
				m_loadExistingBox.setVisible(false);
			}
		});
		secondButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_createNewBox.setVisible(false);
				m_loadExistingBox.setVisible(true);
			}
		});
		radioBox.add(firstButton);
		radioBox.add(secondButton);
		radioBox.add(Box.createHorizontalGlue());
		radioBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				BeautiConfig.getButtonLabel(this, "How do you want to start:"), TitledBorder.LEFT,
				TitledBorder.DEFAULT_JUSTIFICATION));
		centralBox.add(Box.createVerticalStrut(10));
		centralBox.add(radioBox);

		// GUI stuff for starting a new analysis
		// JLabel label = new JLabel("<html>Select one of more sequences and " +
		// "a standard analysis.</html>");
		// m_createNewBox.add(Box.createGlue());
		// m_createNewBox.add(label);
		// m_createNewBox.add(Box.createGlue());
		// m_createNewBox.add(new JSeparator(JSeparator.HORIZONTAL));

		InputEditor sequenceInputEditor = PluginPanel.createInputEditor(m_doc.m_alignments, m_doc);
		sequenceInputEditor.addValidationListener(this);
		m_createNewBox.add(sequenceInputEditor);

		String sStartText = (m_doc.m_mcmc.get() != null ? m_doc.m_mcmc.get().getID() : "<html>Select template</html>");
		ButtonInputEditor inputEditor = new ButtonInputEditor("Analysis template:",
				"Select template containing an analysis, where alignments are replaced by those selected above",
				sStartText, false);
		String sFullInputName = m_doc.getClass().getName() + "." + m_doc.m_mcmc.getName();
		inputEditor.init(m_doc.m_mcmc, m_doc, BeautiConfig.g_inlinePlugins.contains(sFullInputName) ? EXPAND.TRUE
				: EXPAND.FALSE);
		inputEditor.setBorder(new EtchedBorder());
		inputEditor.setVisible(!BeautiConfig.g_sDisabledButtons.contains(this.getClass().getName()
				+ ".Analysis template:"));
		m_createNewBox.add(inputEditor);
		m_createNewBox.add(Box.createGlue());
		m_templateButton = inputEditor.m_button;
		if (m_sTemplateName != null) {
			m_templateButton.setText(m_sTemplateName);
		}

		m_createNewBox.add(Box.createGlue());
		// m_createNewBox.add(new JSeparator(JSeparator.HORIZONTAL));
		m_createNewBox.add(createStartWithTemplateButton());

		// GUI stuff for loading a new analysis
		// m_loadExistingBox.add(new JSeparator(JSeparator.HORIZONTAL));

		sStartText = (m_doc.m_mcmc.get() != null ? m_doc.m_mcmc.get().getID() : "<html>Select file</html>");
		ButtonInputEditor inputEditor2 = new ButtonInputEditor(BeautiConfig.getButtonLabel(this, "Beast file:"),
				BeautiConfig.getButtonLabel(this, "Select existing Beast II specification"), sStartText, true);
		sFullInputName = m_doc.getClass().getName() + "." + m_doc.m_mcmc.getName();
		inputEditor2.init(m_doc.m_mcmc, m_doc, (BeautiConfig.g_inlinePlugins.contains(sFullInputName) ? EXPAND.TRUE
				: EXPAND.FALSE));
		inputEditor2.setBorder(new EtchedBorder());
		inputEditor2.setVisible(true);
		m_loadExistingBox.add(inputEditor2);
		m_beastButton = inputEditor2.m_button;
		if (m_sXMLName != null) {
			m_beastButton.setText(m_sXMLName);
		}

		m_loadExistingBox.add(Box.createGlue());
		// m_loadExistingBox.add(new JSeparator(JSeparator.HORIZONTAL));
		m_loadExistingBox.add(createStartWithBeastButton());

		m_createNewBox.setVisible(false);
		m_loadExistingBox.setVisible(false);

		centralBox.add(m_createNewBox);
		centralBox.add(m_loadExistingBox);

		validate(ValidationStatus.IS_VALID);

		firstButton.setSelected(true);
		m_createNewBox.setVisible(true);
		m_loadExistingBox.setVisible(false);

		return centralBox;
	}

	Box createStartWithTemplateButton() {
		Box box = Box.createHorizontalBox();
		box.setAlignmentX(LEFT_ALIGNMENT);

		box.add(Box.createGlue());
		m_generateXMLButton = new JButton(BeautiConfig.getButtonLabel(this, "Generate XML"));
		m_generateXMLButton.setEnabled(false);
		box.add(m_generateXMLButton);
		m_generateXMLButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (fileSaveAs()) {
					m_endState = ActionOnExit.WRITE_XML;
					m_dlg.dispose();
				}
			}
		});

		box.add(Box.createGlue());
		m_startTemplateButton = new JButton(BeautiConfig.getButtonLabel(this, ">> details"));
		m_startTemplateButton.setEnabled(false);
		box.add(m_startTemplateButton);
		m_startTemplateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_endState = ActionOnExit.SHOW_DETAILS_USE_TEMPLATE;
				m_dlg.dispose();
			}
		});
		return box;
	}

	boolean fileSaveAs() {
		JFileChooser fileChooser = new JFileChooser(m_sOutputFileName);
		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
		fileChooser.setDialogTitle("Save Beast XML File");
		if (!m_sOutputFileName.equals("")) {
			// can happen on actionQuit
			fileChooser.setSelectedFile(new File(m_sOutputFileName));
		}
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			m_sOutputFileName = fileChooser.getSelectedFile().toString();
			return true;
		}
		return false;
	}

	Box createStartWithBeastButton() {
		Box box = Box.createHorizontalBox();
		box.setAlignmentX(LEFT_ALIGNMENT);
		box.add(Box.createGlue());
		m_startBeastButton = new JButton(BeautiConfig.getButtonLabel(this, ">> details"));
		m_startBeastButton.setEnabled(false);
		box.add(m_startBeastButton);
		m_startBeastButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_endState = ActionOnExit.SHOW_DETAILS_USE_XML_SPEC;
				m_dlg.dispose();
			}
		});

		return box;
	}

	public class ButtonInputEditor extends InputEditor {
		private static final long serialVersionUID = 1L;

		String m_sLabel;
		String m_sTipText;
		String m_sButtonText;
		JButton m_button;

		JComboBox m_templates;
		Map<String, String> m_sTemplates;

		boolean m_bBeastFile;

		public ButtonInputEditor() {
		}

		ButtonInputEditor(String sLabel, String sTipText, String sButtonText, boolean bBeastFile) {
			m_sLabel = sLabel;
			m_sTipText = sTipText;
			m_sButtonText = sButtonText;
			m_bBeastFile = bBeastFile;
		}

		@Override
		public Class<?> type() {
			return null;
		}

		final static String OTHER = "<other>";

		/** construct an editor consisting of a label and input entry **/
		public void init(Input<?> input, Plugin plugin, EXPAND bExpand) {
			m_input = input;
			m_plugin = plugin;

			addInputLabel(m_sLabel, m_sTipText);

			m_sTemplates = new HashMap<String, String>();
			File templates = new File("templates");
			File[] files = templates.listFiles();
			if (files != null) {
				for (File template : files) {
					String sFileName = template.getAbsolutePath();
					if (sFileName.toLowerCase().endsWith(".xml") && containsBeautiSpec(sFileName)) {
						m_sTemplates.put(nameFromFile(sFileName), sFileName);
					}
				}
			}
			// deal with non-standard template passed as command line argument
			if (m_sTemplateName != null && !m_sTemplates.containsKey(m_sTemplateName)) {
				m_sTemplates.put(m_sTemplateName, m_sTemplateFileName);
			}
			int nTemplates = m_sTemplates.size();
			String[] sTemplates = new String[nTemplates + 1];
			sTemplates[nTemplates] = OTHER;
			int k = 0;
			for (String sTemplate : m_sTemplates.keySet()) {
				sTemplates[k++] = sTemplate;
			}

			m_templates = new JComboBox(sTemplates);
			Dimension size = new Dimension(200, 20);
			m_templates.setPreferredSize(size);
			m_templates.setMaximumSize(size);
			if (m_sTemplateName != null) {
				m_templates.setSelectedItem(m_sTemplateName);
			} else {
				// initialise with default template
				m_templates.setSelectedItem(0);
				m_sTemplateName = (String) m_templates.getSelectedItem();
				if (m_sTemplateName.equals(OTHER)) {
					m_sTemplateName = null;
				} else {
					m_sTemplateFileName = m_sTemplates.get(m_sTemplateName);
					try {
						processTemplate(m_sTemplateFileName);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			m_templates.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox comboBox = (JComboBox) e.getSource();
					String sSelected = (String) comboBox.getSelectedItem();
					if (sSelected.equals(OTHER)) {
						loadTemplateFile();
						m_sTemplateName = m_templateButton.getText();
						m_sTemplates.put(m_sTemplateName, m_sTemplateFileName);
						comboBox.addItem(m_sTemplateName);
						m_templates.setSelectedItem(m_sTemplateName);
					} else {
						m_sTemplateName = m_sTemplates.get(sSelected);
						m_sTemplateFileName = m_sTemplates.get(m_sTemplateName);
						try {
							processTemplate(m_sTemplateFileName);
							m_templateButton.setText(nameFromFile(m_sTemplateFileName));
							if (m_doc.m_alignments.get().size() > 0) {
								m_startTemplateButton.setEnabled(true);
								m_generateXMLButton.setEnabled(true);
							}
						} catch (Exception e2) {
							System.err.println(e2.getMessage());
						}
					}
				}
			});
			add(m_templates);

			m_button = new JButton(m_sButtonText);
			// m_button.setMinimumSize(new Dimension(100,16));
			// m_button.setPreferredSize(new Dimension(200,24));
			if (input.get() != null) {
				m_button.setText(input.get().toString());
			}
			m_button.setToolTipText(input.getTipText());
			m_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						if (m_bBeastFile) {
							loadBeastFile();
						} else {
							loadTemplateFile();
						}
						// m_input.setValue(m_button.getText(), m_plugin);
					} catch (Exception ex) {
						// JOptionPane.showMessageDialog(null,
						// "Error while setting " + m_input.getName() + ": " +
						// ex.getMessage() +
						// " Leaving value at " + m_input.get());
						// m_button.setText(m_input.get() + "");
					}
					checkValidation();
				}
			});
			add(m_button);

			if (m_bBeastFile) {
				m_templates.setVisible(false);
			} else {
				m_button.setVisible(false);
			}

			addValidationLabel();
			m_validateLabel.setVisible(false);
			add(Box.createGlue());
		} // init

		@Override
		protected void checkValidation() {
			// do nothing
		}
	}

	public final static String ICONPATH = "beast/app/beauti/";

	public static ImageIcon getIcon(String sIcon) {
		String sIconLocation = ICONPATH + sIcon + ".png";
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

	void loadTemplateFile() {
		if (loadXmlFile(false)) {

		}
	}

	void loadBeastFile() {
		if (loadXmlFile(true)) {

		}
	}

	/** check that sFileName is an XML file containing a beauticonfig element **/
	boolean containsBeautiSpec(String sFileName) {
		try {
			File template = new File(sFileName);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// factory.setValidating(true);
			Document doc = factory.newDocumentBuilder().parse(template);
			doc.normalize();
			NodeList list = doc.getElementsByTagName("beauticonfig");
			return list.getLength() > 0;
		} catch (Exception e) {
			return false;
		}
	}

	boolean loadXmlFile(boolean updateBeastLabel) {
		JFileChooser fileChooser = new JFileChooser(Beauti.m_sDir);
		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
		fileChooser.setDialogTitle("Load Beast XML File");
		int rval = fileChooser.showOpenDialog(null);
		if (rval == JFileChooser.APPROVE_OPTION) {
			String sFileName = fileChooser.getSelectedFile().toString();
			if (sFileName.lastIndexOf('/') > 0) {
				Beauti.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
			}
			// XMLParser parser = new XMLParser();
			try {
				// sanity check: is it parseable??
				// parser.parseFile(sFileName);
				// m_doc.m_mcmc.setValue(parser.parseFile(sFileName), m_doc);
				if (updateBeastLabel) {
					m_sXML = load(sFileName);
					m_beastButton.setText(nameFromFile(sFileName));
					m_startBeastButton.setEnabled(true);
				} else {
					m_sTemplateFileName = sFileName;
					processTemplate(sFileName);
					m_templateButton.setText(nameFromFile(sFileName));
					if (m_doc.m_alignments.get().size() > 0) {
						m_startTemplateButton.setEnabled(true);
						m_generateXMLButton.setEnabled(true);
					}
				}
			} catch (Exception e) {
				if (updateBeastLabel) {
					m_startBeastButton.setEnabled(false);
				} else {
					m_startTemplateButton.setEnabled(false);
				}
				e.printStackTrace();
				JOptionPane.showMessageDialog(m_dlg, "Could not load file " + sFileName + ": " + e.getMessage());
				return false;
			}
			return true;
		}
		return false;
	}

	void processTemplate(String sFileName) throws Exception {
		final String MERGE_ELEMENT = "mergepoint";
		m_sTemplateXML = load(sFileName);
		// find merge points
		int i = 0;
		HashMap<String, String> sMergePoints = new HashMap<String, String>();
		while (i >= 0) {
			i = m_sTemplateXML.indexOf("<" + MERGE_ELEMENT, i + 1);
			if (i > 0) {
				int j = m_sTemplateXML.indexOf('>', i);
				String sStr = m_sTemplateXML.substring(i, j);
				sStr = sStr.replaceAll(".*id=", "");
				char c = sStr.charAt(0);
				sStr = sStr.replaceAll(c + "[^" + c + "]*$", "");
				sStr = sStr.substring(1);
				sMergePoints.put(sStr, "");
			}
		}

		// find XML to merge

		File templates = new File("templates");
		File[] files = templates.listFiles();
		if (files != null) {
			for (File template : files) {
				if (template.getName().toLowerCase().endsWith(".xml")) {
					try {

						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						// factory.setValidating(true);
						Document doc = factory.newDocumentBuilder().parse(template);
						doc.normalize();

						processBeautiConfig(doc);

						// find mergewith elements
						NodeList nodes = doc.getElementsByTagName("mergewith");
						for (int iMergeElement = 0; iMergeElement < nodes.getLength(); iMergeElement++) {
							Node mergeElement = nodes.item(iMergeElement);
							String sMergePoint = mergeElement.getAttributes().getNamedItem("point").getNodeValue();
							if (!sMergePoints.containsKey(sMergePoint)) {
								System.err.println("Cannot find merge point named " + sMergePoint + "from "
										+ template.getName() + " in template. MergeWith ignored.");
							} else {
								String sXML = "";
								NodeList children = mergeElement.getChildNodes();
								for (int iChild = 0; iChild < children.getLength(); iChild++) {
									sXML += nodeToString(children.item(iChild));
								}
								String sStr = sMergePoints.get(sMergePoint);
								sStr += sXML;
								sMergePoints.put(sMergePoint, sStr);
							}

						}
					} catch (Exception e) {
						if (!e.getMessage().contains("beast.app.beauti.InputConstraint")) {
							System.err.println(e.getMessage());
						}
					}
				}
			}

			// merge XML
			i = 0;
			while (i >= 0) {
				i = m_sTemplateXML.indexOf("<" + MERGE_ELEMENT, i + 1);
				if (i > 0) {
					int j = m_sTemplateXML.indexOf('>', i);
					String sStr = m_sTemplateXML.substring(i, j);
					sStr = sStr.replaceAll(".*id=", "");
					char c = sStr.charAt(0);
					sStr = sStr.replaceAll(c + "[^" + c + "]*$", "");
					sStr = sStr.substring(1);
					String sXML = sMergePoints.get(sStr);
					m_sTemplateXML = m_sTemplateXML.substring(0, i) + sXML + m_sTemplateXML.substring(j + 1);
				}
			}
		}

	}

	void processBeautiConfig(Document doc) throws Exception {
		// find configuration elements, process and remove
		NodeList nodes = doc.getElementsByTagName("beauticonfig");
		for (int iConfigElement = 0; iConfigElement < nodes.getLength(); iConfigElement++) {
			Node configElement = nodes.item(iConfigElement);
			String sXML = nodeToString(configElement);
			XMLParser parser = new XMLParser();
			parser.parseBareFragment(sXML, true);
			configElement.getParentNode().removeChild(configElement);
		}
	}

	String nodeToString(Node node) throws TransformerException {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		StringWriter buffer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(buffer));
		return buffer.toString();
	}

	static public String load(String sFileName) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
		StringBuffer buf = new StringBuffer();
		String sStr = null;
		while (fin.ready()) {
			sStr = fin.readLine();
			buf.append(sStr);
			buf.append('\n');
		}
		fin.close();

		String sXML = buf.toString();
		if (sXML.indexOf(XMLProducer.DO_NOT_EDIT_WARNING) > 0) {
			sXML = sXML.replaceAll("<!--\\s*" + XMLProducer.DO_NOT_EDIT_WARNING, "");
			int i = sXML.lastIndexOf("-->");
			sXML = sXML.substring(0, i) + sXML.substring(i + 3);
		}
		return sXML;
	}

	@Override
	public void validate(ValidationStatus state) {
		if (m_sXML != null) {
			m_startBeastButton.setEnabled(true);
		}
		try {
			m_doc.m_alignments.validate();
			// m_doc.m_mcmc.validate();
			if (m_sTemplateXML != null) {
				m_startTemplateButton.setEnabled(true);
				m_generateXMLButton.setEnabled(true);
			}
		} catch (Exception e) {
			m_startTemplateButton.setEnabled(false);
			m_generateXMLButton.setEnabled(false);
		}
	}

} // class BeautiInitDlg
