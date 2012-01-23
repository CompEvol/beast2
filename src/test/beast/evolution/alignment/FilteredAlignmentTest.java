package test.beast.evolution.alignment;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Sequence;
import junit.framework.TestCase;
import org.junit.Test;

public class FilteredAlignmentTest extends TestCase {

    static public Alignment getAlignment() throws Exception {
        Sequence human = new Sequence("human", "AAAACCCCGGGGTTTT");
        Sequence chimp = new Sequence("chimp", "ACGTACGTACGTACGT");

        Alignment data = new Alignment();
        data.initByName("sequence", human, "sequence", chimp,
                "dataType", "nucleotide"
        );
        return data;
    }

    @Test
    public void testRangeFiltered() throws Exception {
        Alignment data = getAlignment();
        FilteredAlignment data2 = new FilteredAlignment();
        data2.initByName("data", data, "filter", "1-9");
        assertEquals(9, data2.getSiteCount());
        assertEquals(9, data2.getPatternCount());

        data2.initByName("data", data, "filter", "2-9");
        assertEquals(8, data2.getSiteCount());
        assertEquals(8, data2.getPatternCount());

        data2.initByName("data", data, "filter", "10-");
        assertEquals(7, data2.getSiteCount());

        data2.initByName("data", data, "filter", "-10");
        assertEquals(10, data2.getSiteCount());

        data2.initByName("data", data, "filter", "-");
        assertEquals(16, data2.getSiteCount());

        data2.initByName("data", data, "filter", "2-5,7-10");
        assertEquals(8, data2.getSiteCount());
        assertEquals(8, data2.getPatternCount());
    }

    @Test
    public void testIteratorFiltered() throws Exception {
        Alignment data = getAlignment();
        FilteredAlignment data2 = new FilteredAlignment();
        data2.initByName("data", data, "filter", "1:16:2");
        assertEquals(8, data2.getSiteCount());
        assertEquals(8, data2.getPatternCount());

        int iPattern = data2.getPatternIndex(0);
        int[] pattern = data2.getPattern(iPattern);
        assertEquals(0, pattern[0]);
        assertEquals(0, pattern[1]);

        data2.initByName("data", data, "filter", "2:16:2");
        assertEquals(8, data2.getSiteCount());

        iPattern = data2.getPatternIndex(0);
        pattern = data2.getPattern(iPattern);
        assertEquals(0, pattern[0]);
        assertEquals(1, pattern[1]);

        data2.initByName("data", data, "filter", "1:10:2");
        assertEquals(5, data2.getSiteCount());

        data2.initByName("data", data, "filter", "1::3");
        assertEquals(6, data2.getSiteCount());

        data2.initByName("data", data, "filter", "2::3");
        assertEquals(5, data2.getSiteCount());

        data2.initByName("data", data, "filter", "::");
        assertEquals(16, data2.getSiteCount());

        data2.initByName("data", data, "filter", "2:5:");
        assertEquals(4, data2.getSiteCount());

        data2.initByName("data", data, "filter", ":5:");
        assertEquals(5, data2.getSiteCount());

        data2.initByName("data", data, "filter", "1::3,2::3");
        assertEquals(11, data2.getSiteCount());

//        System.out.println(alignmentToString(data2, 1));

        data2.initByName("data", data, "filter", "3::3");
        assertEquals(5, data2.getSiteCount());

//        System.out.println(alignmentToString(data2, 1));
    }

    String alignmentToString(Alignment data, int iTaxon) throws Exception {
        int[] nStates = new int[data.getSiteCount()];
        for (int i = 0; i < data.getSiteCount(); i++) {
            int iPattern = data.getPatternIndex(i);
            int[] sitePattern = data.getPattern(iPattern);
            nStates[i] = sitePattern[iTaxon];
        }
        return data.getDataType().state2string(nStates);
    }
}
