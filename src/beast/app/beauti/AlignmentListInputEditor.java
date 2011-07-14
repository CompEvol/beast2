package beast.app.beauti;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.ListInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.util.NexusParser;
import beast.util.XMLParser;

public class AlignmentListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return List.class;
	}
	@Override
	public Class<?> baseType() {
		return Alignment.class;
	}
	
	@Override
    public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
		super.init(input, plugin, bExpand, bAddButtons);
		//m_editButton.setVisible(false);
	}
	
	@Override
	public List<Plugin> pluginSelector(Input<?> input, Plugin plugin, List<String> sTabuList) {
    	List<Plugin> selectedPlugins = new ArrayList<Plugin>();
		JFileChooser fileChooser = new JFileChooser(Beauti.m_sDir);
		
        fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
        String [] exts = {".nex",".nxs"};
        fileChooser.addChoosableFileFilter(new ExtensionFileFilter(exts, "Nexus file (*.nex)"));

//		fileChooser.addChoosableFileFilter(new MyFileFilter() {
//			public String getExtention(){return ".xml";}
//			public String getDescription() {return "Beast xml file (*.xml)";}
//		});
//		fileChooser.addChoosableFileFilter(new MyFileFilter() {
//			public String getExtention(){return ".nex";}
//			public String getDescription() {return "Nexus file (*.nex)";}
//		});
		fileChooser.setDialogTitle("Load Sequence");
		fileChooser.setMultiSelectionEnabled(true);
		int rval = fileChooser.showOpenDialog(null);

		if (rval == JFileChooser.APPROVE_OPTION) {

			File [] files = fileChooser.getSelectedFiles();
			for (int i = 0; i < files.length; i++) {
				String sFileName = files[i].getAbsolutePath();
				if (sFileName.lastIndexOf('/') > 0) {
					Beauti.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				if (sFileName.toLowerCase().endsWith(".nex") || sFileName.toLowerCase().endsWith(".nxs")) {
					NexusParser parser = new NexusParser();
					try {
						parser.parseFile(sFileName);
						if (parser.m_filteredAlignments.size() > 0) {
							for (Alignment data : parser.m_filteredAlignments) {
								selectedPlugins.add(data);
							}
						} else {
							selectedPlugins.add(parser.m_alignment);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Loading of " + sFileName + " failed: " + ex.getMessage());
						return null;
					}
				}
				if (sFileName.toLowerCase().endsWith(".xml")) {
					Plugin alignment =  getXMLData(sFileName);
					selectedPlugins.add(alignment);
				}
//				if (i < files.length - 1) {
//		            try {
//		           		m_input.setValue(plugin2, m_plugin);
//		            } catch (Exception ex) {
//		                System.err.println(ex.getClass().getName() + " " + ex.getMessage());
//		            }
//		            addSingleItem(plugin2);
//				} else {
//					return plugin2;
//				}
			}
			return selectedPlugins;
		}
		return null;
	} // loadDataFile
	
	static public Plugin getXMLData(String sFileName) {
		XMLParser parser = new XMLParser();
		try {
			String sXML = "";
    		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
    		while (fin.ready()) {
    			sXML += fin.readLine();
    		}
    		fin.close();
			Plugin runnable = parser.parseFragment(sXML, false);
			return getAlignment(runnable);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Loading of " + sFileName + " failed: " + ex.getMessage());
			return null;
		}
	}
	
	static Plugin getAlignment(Plugin plugin) throws IllegalArgumentException, IllegalAccessException {
		if (plugin instanceof Alignment) {
			return plugin;
		}
		for (Plugin plugin2 : plugin.listActivePlugins()) {
			plugin2 = getAlignment(plugin2);
			if (plugin2 != null) {
				return plugin2;
			}
		}
		return null;
	}


} // class AlignmentListInputEditor
