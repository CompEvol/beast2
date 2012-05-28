package test.beast.app.beauti;


import static org.fest.assertions.Assertions.assertThat;
import static org.fest.swing.edt.GuiActionRunner.execute;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import org.fest.swing.annotation.RunsInEDT;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.junit.testcase.FestSwingJUnitTestCase;

import beast.app.beauti.Beauti;
import beast.app.beauti.BeautiDoc;
import beast.core.Distribution;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.Plugin;
import beast.core.State;
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;

/**
 * Basic test methods for Beauti  
 * 
 */
public class BeautiBase extends FestSwingJUnitTestCase {

	protected FrameFixture beautiFrame;
	protected Beauti beauti;
	protected BeautiDoc doc;

	protected void onSetUp() {
		beautiFrame = new FrameFixture(robot(), createNewEditor());
		beautiFrame.show();
		beautiFrame.resizeTo(new Dimension(1224, 786));
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		beauti = (Beauti) f.target;
		doc = beauti.doc;
	}

	@RunsInEDT
	private static JFrame createNewEditor() {
		return execute(new GuiQuery<JFrame>() {
			protected JFrame executeInEDT() throws Throwable {
				Beauti beauti = Beauti.main2(new String[] {});
				return beauti.frame;
			}
		});
	}

	String priorsAsString() {
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		List<Distribution> priors = prior.pDistributions.get();
		return "assertPriorsEqual" + pluginListAsString(priors);
	}
	
	String stateAsString() {
		State state = (State) doc.pluginmap.get("state");
		List<StateNode> stateNodes = state.stateNodeInput.get();
		return "assertStateEquals" + pluginListAsString(stateNodes);
	}

	String operatorsAsString() {
		MCMC mcmc = (MCMC) doc.mcmc.get();
		List<Operator> operators = mcmc.operatorsInput.get();
		return "assertOperatorsEqual" + pluginListAsString(operators);
	}
	
	private String pluginListAsString(List<?> list) {
		if (list.size() == 0) {
			return "";
		}
		StringBuffer bf = new StringBuffer();
		for (Object o : list) {
			Plugin plugin = (Plugin) o;
			bf.append('"');
			bf.append(plugin.getID());
			bf.append("\", ");
		}
		String str = bf.toString();
		return "(" + str.substring(0, str.length()-2) + ");";
	}

	void assertPriorsEqual(String... ids) {
		CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
		List<Distribution> priors = prior.pDistributions.get();
		for (String id : ids) {
			boolean found = false;
			for (Plugin node : priors) {
				if (node.getID().equals(id)) {
					found = true;
				}
			}
			assertThat(found);
		}
		assertThat(ids.length == priors.size());
	}

	void assertStateEquals(String... ids) {
		State state = (State) doc.pluginmap.get("state");
		List<StateNode> stateNodes = state.stateNodeInput.get();
		for (String id : ids) {
			boolean found = false;
			for (StateNode node : stateNodes) {
				if (node.getID().equals(id)) {
					found = true;
				}
			}
			assertThat(found).as("Could not find plugin with ID " + id).isEqualTo(true);
		}
		assertThat(ids.length).as("list of plugins do not match").isEqualTo(stateNodes.size());
	}

	void assertOperatorsEqual(String... ids) {
		MCMC mcmc = (MCMC) doc.mcmc.get();
		List<Operator> operators = mcmc.operatorsInput.get();
		for (String id : ids) {
			boolean found = false;
			for (Plugin node : operators) {
				if (node.getID().equals(id)) {
					found = true;
				}
			}
			assertThat(found);
		}
		assertThat(ids.length == operators.size());
	}

	void assertArrayEquals(Object [] o, String array) {
		String str = array.substring(1, array.length() - 1);
		String [] strs = str.split(", ");
		for (int i = 0; i < o.length && i < strs.length; i++) {
			assertThat(strs[i]).as("expected array value " + strs[i] + " instead of " + o[i].toString()).isEqualTo(o[i].toString());
		}
		assertThat(o.length).as("arrays do not match: different lengths").isEqualTo(strs.length);
	}
	
	void printBeautiState(JTabbedPaneFixture f) throws InterruptedException {
        doc.scrubAll(true, false);
		//f.selectTab("MCMC");
		System.err.println(stateAsString());
		System.err.println(operatorsAsString());
		System.err.println(priorsAsString());
	}

	void printTableContents(JTableFixture t) {
		String [][] contents = t.contents();
		for (int i = 0; i < contents.length; i++) {
			System.err.print("\"" + Arrays.toString(contents[i]));
			if (i < contents.length - 1) {
				System.err.print("*\" +");
			} else {
				System.err.print("\"");
			}
			System.err.println();
		}
	}
	
	void checkTableContents(JTableFixture t, String str) {
		String [][] contents = t.contents();
		String [] strs = str.split("\\*");
		assertThat(contents.length).as("tables do not match: different #rows").isEqualTo(strs.length);
		for (int i = 0; i < contents.length; i++) {
			assertArrayEquals(contents[i], strs[i]);
		}
	}

}