package test.beast.evolution.alignment;


import java.util.List;

import org.junit.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import junit.framework.TestCase;

public class UncertainAlignmentTest extends TestCase {

	static public Tree getTreeB(Alignment data) throws Exception {
        TreeParser tree = new TreeParser();
        tree.initByName("taxa", data,
                "newick", "(seq1:2,(seq2:1,seq3:1):1);",
                "IsLabelledNewick", true);
        return tree;
    }
	
	static public Tree getTreeA(Alignment data) throws Exception {
        TreeParser tree = new TreeParser();
        tree.initByName("taxa", data,
                "newick", "((seq1:1,seq2:1):1,seq3:2);",
                "IsLabelledNewick", true);
        return tree;
    }
	static public Alignment getUncertainAlignment() throws Exception {
    	return getUncertainAlignment(false);
    }
    static public Alignment getUncertainAlignmentDoubled() throws Exception {
    	return getUncertainAlignment(true);
    }
    static public Alignment getUncertainAlignment(boolean duplicate) throws Exception {
	      
        String seq1Probs = new String("0.7,0.0,0.3,0.0; 0.0,0.3,0.0,0.7; 0.0,0.0,0.0,1.0;");
        String seq2Probs = new String("0.7,0.0,0.3,0.0; 0.0,0.3,0.0,0.7; 0.0,1.0,0.0,0.0;");
        String seq3Probs = new String("0.4,0.0,0.6,0.0; 0.0,0.6,0.0,0.4; 0.0,1.0,0.0,0.0;");
        
        if (duplicate) {
        	seq1Probs += seq1Probs;
        	seq2Probs += seq2Probs;
        	seq3Probs += seq3Probs;
        }
        Sequence seq1 = new Sequence();
		seq1.initByName("taxon","seq1","value",seq1Probs,"uncertain",true);
		Sequence seq2 = new Sequence();
		seq2.initByName("taxon","seq2","value",seq2Probs,"uncertain",true);
		Sequence seq3 = new Sequence();
		seq3.initByName("taxon","seq3","value",seq3Probs,"uncertain",true);
                
        Alignment data = new Alignment();
        
        data.initByName("sequence", seq1, "sequence", seq2, "sequence", seq3,
                "dataType", "nucleotide"                
        );

    	DataType dataType = data.getDataType();
        System.out.println("Most probable sequences:");
    	for (List<Integer> seq : data.getCounts()) {
    		System.out.println(dataType.encodingToString(seq));
    	}
    	
        return data;
    }
    
    static public Alignment getAlignment() throws Exception {
    	
    	// The sequences now denote the most likely annotation        
    	Sequence seq1 = new Sequence("seq1", "ATT");
        Sequence seq2 = new Sequence("seq2", "ATC");
        Sequence seq3 = new Sequence("seq3", "GCC");
        
        Alignment data = new Alignment();
        
        data.initByName("sequence", seq1, "sequence", seq2, "sequence", seq3,
                "dataType", "nucleotide"                
        );
    	
        return data;
    }

    @Test
    public void testUncertainAlignment() throws Exception {
    	Alignment data = getUncertainAlignment();    	
    	
    	DataType dataType = data.getDataType();
    	
       	System.out.println("Tip likelihoods:");
    	int sites = data.getCounts().get(0).size();
    	for (int taxon=0; taxon<data.getTaxonCount(); taxon++) {
    		for (int i=0; i<sites; i++) {
	    		double[] probs = data.getTipLikelihoods(taxon,i);
	    		for (int j=0; j<probs.length; j++) {
	        		System.out.print(probs[j]+" ");
	        	}
	    		System.out.print("; ");
    		}
    		System.out.println();
    	}
    	
    	System.out.println("Most likely sequences:");
    	for (List<Integer> seq : data.getCounts()) {
    		System.out.println(dataType.encodingToString(seq));
    	}
    	
    	Alignment data2 = getAlignment();
    	
    	for (int taxon=0; taxon<data.getTaxonCount(); taxon++) {
    		assertEquals(data.getCounts().get(taxon),data2.getCounts().get(taxon));
    	}    	        	    	            	    
    }  
}
