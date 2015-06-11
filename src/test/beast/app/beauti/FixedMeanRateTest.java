package test.beast.app.beauti;

import java.io.File;

import org.fest.swing.data.TableCell;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

import beast.evolution.operators.DeltaExchangeOperator;

/** test how the FixedMeanRate flag interact with link/unlink **/
public class FixedMeanRateTest extends BeautiBase {
	
	@Test
	public void testFixedMeanRate() throws Exception {

		importAlignment("examples/nexus", new File("26.nex"), new File("29.nex"));
		
		beautiFrame.menuItemWithPath("Mode", "Automatic set fix mean substitution rate flag").click();

		
		warning("Setting fixed mean rates");
		JTabbedPaneFixture f = beautiFrame.tabbedPane();
		f = f.selectTab("Site Model");		
		beautiFrame.checkBox("mutationRate.isEstimated").check();
		beautiFrame.checkBox("FixMeanMutationRate").check();

		warning("link/unlink site models");
		f.selectTab("Partitions");
		beautiFrame.table().selectCells(TableCell.row(0).column(0), TableCell.row(1).column(0));
		beautiFrame.button("Link Site Models").click();
		beautiFrame.button("Unlink Site Models").click();

		//saveFile("/Users/remcobouckaert/tmp", "x.xml");
		makeSureXMLParses();
		

		
		DeltaExchangeOperator operator = (DeltaExchangeOperator) beauti.doc.pluginmap.get("FixMeanMutationRatesOperator");
		int nrOfParameters = operator.parameterInput.get().size();
		if(nrOfParameters != 2) {
			throw new Exception("Expected 2 parameters for deltaExchangeOperator, not " + nrOfParameters);
		}
	}
	

}
