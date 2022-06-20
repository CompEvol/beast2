/**
 * 
 */
package test.beast.evolution.tree;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TraitSet;

/**
 * @author Gereon Kaiping <g.a.kaiping@hum.leidenuniv.nl>
 *
 */
public class TraitSetTest {
	
	public TaxonSet taxonSet(int Nleaves) {
    List<Sequence> seqList = new ArrayList<Sequence>();

    for (int i=0; i<Nleaves; i++) {
        String taxonID = "t" + i;
        seqList.add(new Sequence(taxonID, "?"));
    }

    Alignment alignment = new Alignment(seqList, "nucleotide");
    TaxonSet taxonSet = new TaxonSet(alignment);
    return taxonSet;}
	

	@Test
	public void testDateBackward() {
		int Nleaves = 2; 
	    TraitSet timeTrait = new TraitSet();

	    timeTrait.initByName(
	            "traitname", "date-backward",
	            "taxa", taxonSet(Nleaves),
	            "value", "t0=0, t1=10");
	    // The trait actually represents the age of the taxa relative to
	    // each other with arbitrary zero, so we test it like this.
	    assertEquals(-10.0, timeTrait.getValue("t0")-timeTrait.getValue("t1"), 1e-7);
	}

	@Test
	public void testDateForward() {
		int Nleaves = 2; 
	    TraitSet timeTrait = new TraitSet();

	    timeTrait.initByName(
	            "traitname", "date-forward",
	            "taxa", taxonSet(Nleaves),
	            "value", "t0=0, t1=10");
	    // The trait actually represents the age of the taxa relative to
	    // each other with arbitrary zero, so we test it like this.
	    assertEquals(10.0, timeTrait.getValue("t0")-timeTrait.getValue("t1"), 1e-7);
	}

	@Test
	public void testDateForwardUnspecified() {
		int Nleaves = 2; 
	    TraitSet timeTrait = new TraitSet();

	    timeTrait.initByName(
	            "traitname", "date-forward",
	            "taxa", taxonSet(Nleaves),
	            "value", "t1=10");
	    // The trait actually represents the age of the taxa relative to
	    // each other with arbitrary zero, so we test it like this.
	    assertEquals(0.0, timeTrait.getValue("t0")-timeTrait.getValue("t1"), 1e-7);
	}
}
