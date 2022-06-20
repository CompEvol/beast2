package test.beast.evolution.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import beast.base.evolution.datatype.Aminoacid;
import beast.base.evolution.datatype.Binary;
import beast.base.evolution.datatype.IntegerData;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.evolution.datatype.TwoStateCovarion;
import beast.base.evolution.datatype.DataType.Base;

public class DataTypeDeEncodeTest {

	@Test
	public void testDataTypeDeEncode() {
		Base dAa = new Aminoacid();
		Base dBi = new Binary();
		Base dIn = new IntegerData();
		Base dNt = new Nucleotide();
		Base d2C = new TwoStateCovarion();

		for (Object [] o : Arrays.asList(new Object[][] {
				{ dIn, "1,2,14,23,?", Arrays.asList(new Integer[] { 1, 2, 14, 23, -1 }) },
				{ dNt, "ACGTURYMWSKBDHVNX", Arrays.asList(
						new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }) },
				{ dBi, "01-?10", Arrays.asList(new Integer[] { 0, 1, 2, 3, 1, 0 }) },
				{ d2C, "01ABCD-?", Arrays.asList(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 }) },
				{ dAa, "ACDEFGHIKLMNPQRSTVWY", Arrays.asList(
						new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }) }
			})) {
				Base d = (Base) o[0];
				String s = (String) o[1];
				List<Integer> c = (List<Integer>) o[2];
				// o.testStingToEncoding();
				assertEquals(c, d.stringToEncoding(s));
				//o.testRoundTrip();
				assertEquals(s, d.encodingToString(d.stringToEncoding(s)));
		}
	}

}
