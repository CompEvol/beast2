package test.beast.evolution.likelihood;


import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;

/** This test mimics the testLikelihood.xml file from Beast 1, which compares Beast 1 results to PAUP results. 
 * So, it these tests succeed, then Beast II calculates the same for these simple models as Beast 1 and PAUP.
 * **/
public class TreeLikelihoodTest extends TestCase {
	final static double PRECISION = 1e-7;
	
	public TreeLikelihoodTest() {
		super();
	}
	
	protected TreeLikelihood newTreeLikelihood() {
		return new TreeLikelihood();
	}
	
	
	Alignment getAlignment() throws Exception {
		List<Sequence>  sequences = new ArrayList<Sequence>();
		sequences.add(new Sequence("human", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA"));
		sequences.add(new Sequence("chimp","AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA"));
		sequences.add(new Sequence("bonobo","AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTTAAATCCCCTTATTTCTACTAGGACTATGAGAGTCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCTCTCAGTAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAGC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTTGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCCCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCAACACCTCTTTACAGTGA"));
		sequences.add(new Sequence("gorilla","AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGAGGTTTAAACCCCCTTATTTCTACTAGGACTATGAGAATTGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTGTCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTCACATCCTTCCCGTACTAAGAAATTTAGGTTAAACATAGACCAAGAGCCTTCAAAGCCCTTAGTAAGTTA-CAACACTTAATTTCTGTAAGGACTGCAAAACCCTACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCAATGGGACTCAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAT-TCACCTCGGAGCTTGGTAAAAAGAGGCCCAGCCTCTGTCTTTAGATTTACAGTCCAATGCCTTA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGACCTTCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAGGTTAAATTACGGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCGTTAACCTTTTAAGTTAAAGATTAAGAGTATCGGCACCTCTTTGCAGTGA"));
		sequences.add(new Sequence("orangutan","AGAAATATGTCTGACAAAAGAGTTACTTTGATAGAGTAAAAAATAGAGGTCTAAATCCCCTTATTTCTACTAGGACTATGGGAATTGAACCCACCCCTGAGAATCCAAAATTCTCCGTGCCACCCATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTA--CACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTCA-CAGCACTTAATTTCTGTAAGGACTGCAAAACCCCACTTTGCATCAACTGAGCGCAAATCAGCCACTTTAATTAAGCTAAGCCCTCCTAGACCGATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAT-TGGCTTCAGTCCAAAGCCCCGGCAGGCCTTAAAGCTGCTCCTTCGAATTTGCAATTCAACATGACAA-TCACCTCAGGGCTTGGTAAAAAGAGGTCTGACCCCTGTTCTTAGATTTACAGCCTAATGCCTTAACTCGGCCATTTTACCGCAAAAAAGGAAGGAATCGAACCTCCTAAAGCTGGTTTCAAGCCAACCCCATAACCCCCATGACTTTTTCAAAAGGTACTAGAAAAACCATTTCGTAACTTTGTCAAAGTTAAATTACAGGTC-AGACCCTGTGTATCTTA-CATTGCAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACCAGCCTCTCTTTGCAATGA"));
		sequences.add(new Sequence("siamang","AGAAATACGTCTGACGAAAGAGTTACTTTGATAGAGTAAATAACAGGGGTTTAAATCCCCTTATTTCTACTAGAACCATAGGAGTCGAACCCATCCTTGAGAATCCAAAACTCTCCGTGCCACCCGTCGCACCCTGTTCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCATACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTAACAAAACTTAATTTCTGCAAGGGCTGCAAAACCCTACTTTGCATCAACCGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCGATGGGACTTAAACCCATAAAAATTTAGTTAACAGCTAAACACCCTAAACAACCTGGCTTCAATCTAAAGCCCCGGCAGA-GTTGAAGCTGCTTCTTTGAACTTGCAATTCAACGTGAAAAATCACTTCGGAGCTTGGCAAAAAGAGGTTTCACCTCTGTCCTTAGATTTACAGTCTAATGCTTTA-CTCAGCCACTTTACCACAAAAAAGGAAGGAATCGAACCCTCTAAAACCGGTTTCAAGCCAGCCCCATAACCTTTATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATCACAGGTCCAAACCCCGTATATCTTATCACTGTAGAGCTAGACCAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACTACCGCCTCTTTACAGTGA"));
		Alignment data = null;
		data = new Alignment(sequences, 4, "nucleotide");
		return data;
	}
	
	Tree getTree(Alignment data) throws Exception {
		TreeParser tree = new TreeParser();
		tree.init(data, "((((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035,gorilla:0.036038):0.033087000000000005,orangutan:0.069125):0.030456999999999998,siamang:0.099582);");
		return tree;
	}

	@Test
	public void testJC69Likelihood() throws Exception {
		// Set up JC69 model: uniform freqs, kappa = 1, 0 gamma categories	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data, false);

		HKY hky = new HKY();
		hky.init("1.0", freqs);

		SiteModel siteModel = new SiteModel();
//		RealParameter fMu = new RealParameter(1.0,0.0,1000.0,1);
//		RealParameter nShape = new RealParameter(0.5, 0.0, 1000.0, 1);
//		RealParameter fInvarProportion = new RealParameter(0.0, 0.0, 1000.0, 1);
//		siteModel.init(fMu, 1, nShape, fInvarProportion, hky, freqs);
		siteModel.init("1.0", 1, null, null, hky);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1992.2056440317247, PRECISION);
	}

	@Test
	public void testK80Likelihood() throws Exception {
		// Set up K80 model: uniform freqs, kappa = 27.402591, 0 gamma categories	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data, false);

		HKY hky = new HKY();
		hky.init("27.402591", freqs);

		SiteModel siteModel = new SiteModel();
//		RealParameter fMu = new RealParameter(1.0,0.0,1000.0,1);
//		RealParameter nShape = new RealParameter(0.5, 0.0, 1000.0, 1);
//		RealParameter fInvarProportion = new RealParameter(0.0, 0.0, 1000.0, 1);
//		siteModel.init(fMu, 1, nShape, fInvarProportion, hky, freqs);
		siteModel.init("1.0", 1, null, null, hky);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1856.303048876734, PRECISION);
	}
	
	@Test
	public void testHKY85Likelihood() throws Exception {
		// Set up HKY85 model: estimated freqs, kappa = 29.739445, 0 gamma categories	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		HKY hky = new HKY();
		hky.init("29.739445", freqs);

		SiteModel siteModel = new SiteModel();
//		RealParameter fMu = new RealParameter(1.0,0.0,1000.0,1);
//		RealParameter nShape = new RealParameter(0.5, 0.0, 1000.0, 1);
//		RealParameter fInvarProportion = new RealParameter(0.0, 0.0, 1000.0, 1);
//		siteModel.init(fMu, 1, nShape, fInvarProportion, hky, freqs);
		siteModel.init("1.0", 1, null, null, hky);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1825.2131708068507, PRECISION);
	}
	
	
	@Test
	public void testHKY85GLikelihood() throws Exception {
		// Set up HKY85+G model: estimated freqs, kappa = 38.82974, 4 gamma categories, shape = 0.137064	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		HKY hky = new HKY();
		hky.init("38.82974", freqs);

		SiteModel siteModel = new SiteModel();
//		RealParameter fMu = new RealParameter(1.0,0.0,1000.0,1);
//		RealParameter nShape = new RealParameter(0.137064, 0.0, 1000.0, 1);
//		RealParameter fInvarProportion = new RealParameter(0.0, 0.0, 1000.0, 1);
//		siteModel.init(fMu, 4, nShape, fInvarProportion, hky, freqs);
		siteModel.init("1.0", 4, "0.137064", "0.0", hky);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		System.err.println(fLogP - -1789.7593576610134);
		assertEquals(fLogP, -1789.7593576610134, PRECISION);
	}

	@Test
	public void testHKY85ILikelihood() throws Exception {
		// Set up HKY85+I model: estimated freqs, kappa = 38.564672, 0 gamma categories, prop invariant = 0.701211	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		HKY hky = new HKY();
		hky.init("38.564672", freqs);

		SiteModel siteModel = new SiteModel();
//		RealParameter fMu = new RealParameter(1.0,0.0,1000.0,1);
//		RealParameter nShape = new RealParameter(0.137064, 0.0, 1000.0, 1);
//		RealParameter fInvarProportion = new RealParameter(0.701211, 0.0, 1000.0, 1);
//		siteModel.init(fMu, 1, nShape, fInvarProportion, hky, freqs);
		siteModel.init("1.0", 1, "0.137064", "0.701211", hky);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1789.912401996943, PRECISION);
	}

	@Test
	public void testHKY85GILikelihood() throws Exception {
		// Set up HKY85+G+I model: estimated freqs, kappa = 39.464538, 4 gamma categories, shape = 0.587649, prop invariant = 0.486548	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		HKY hky = new HKY();
		hky.init("39.464538", freqs);

		SiteModel siteModel = new SiteModel();
//		RealParameter fMu = new RealParameter(1.0,0.0,1000.0,1);
//		RealParameter nShape = new RealParameter(0.587649, 0.0, 1000.0, 1);
//		RealParameter fInvarProportion = new RealParameter(0.486548, 0.0, 1000.0, 1);
		siteModel.init("1.0", 4, "0.587649", "0.486548", hky);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1789.639227747059, PRECISION);
	}


	@Test
	public void testGTRLikelihood() throws Exception {
		// Set up GTR model: no gamma categories, no proportion invariant 	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.init("1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.init("1.0", 1, null, null, gsm);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1969.145839307625, PRECISION);
	}

	@Test
	public void testGTRILikelihood() throws Exception {
		// Set up GTR model: prop invariant = 0.5
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.init("1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.init("1.0", 1, null, "0.5", gsm);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1948.8417455357564, PRECISION);
	}
	
	@Test
	public void testGTRGLikelihood() throws Exception {
		// Set up GTR model: 4 gamma categories, gamma shape = 0.5
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.init("1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.init("1.0", 4, "0.5", null, gsm);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1949.0360143622, PRECISION);
	}
	
	@Test
	public void testGTRGILikelihood() throws Exception {
		// Set up GTR model: 4 gamma categories, gamma shape = 0.5, prop invariant = 0.5
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.init(data);

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.init("1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.init("1.0", 4, "0.5", "0.5", gsm);

		TreeLikelihood likelihood = newTreeLikelihood();
		likelihood.init(data, tree, siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1947.5829396144961, PRECISION);
	}

} // class TreeLikelihoodTest
