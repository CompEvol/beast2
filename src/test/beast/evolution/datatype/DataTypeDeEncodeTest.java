package test.beast.evolution.datatype;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import beast.evolution.datatype.Aminoacid;
import beast.evolution.datatype.Binary;
import beast.evolution.datatype.DataType.Base;
import beast.evolution.datatype.IntegerData;
import beast.evolution.datatype.Nucleotide;
import beast.evolution.datatype.TwoStateCovarion;

@RunWith(Parameterized.class)
public class DataTypeDeEncodeTest {
	private Base d;
	private String s;
	private List<Integer> c;

	public DataTypeDeEncodeTest(Base dataType, String sequence, List<Integer> codes) {
		d = dataType;
		s = sequence;
		c = codes;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> typestrings() {
		Base dAa = new Aminoacid();
		Base dBi = new Binary();
		Base dIn = new IntegerData();
		Base dNt = new Nucleotide();
		Base d2C = new TwoStateCovarion();

		return Arrays.asList(new Object[][] {
				{ dIn, "1,2,14,23,?", Arrays.asList(new Integer[] { 1, 2, 14, 23, -1 }) },
				{ dNt, "ACGTURYMWSKBDHVNX", Arrays.asList(
						new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }) },
				{ dBi, "01-?10", Arrays.asList(new Integer[] { 0, 1, 2, 3, 1, 0 }) },
				{ d2C, "01ABCD-?", Arrays.asList(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 }) },
				{ dAa, "ACDEFGHIKLMNPQRSTVWY", Arrays.asList(
						new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }) }
			});
	}

	@Test
	public void testStingToEncoding() {
		assertEquals(c, d.stringToEncoding(s));
	}

	@Test
	public void testRoundTrip() {
		assertEquals(s, d.encodingToString(d.stringToEncoding(s)));
	}

}
