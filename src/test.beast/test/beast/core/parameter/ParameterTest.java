package test.beast.core.parameter;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import beast.base.inference.parameter.RealParameter;

import java.util.Arrays;
import java.util.List;

public class ParameterTest extends TestCase {

    @Test
    public void testParamter() throws Exception {
        RealParameter parameter = new RealParameter();
        parameter.initByName("value", "1.27 1.9");
        assertEquals(parameter.getDimension(), 2);
        parameter.setDimension(5);
        assertEquals(parameter.getDimension(), 5);
        assertEquals(parameter.getValue(0), parameter.getValue(2));
        assertEquals(parameter.getValue(0), parameter.getValue(4));
        assertEquals(parameter.getValue(1), parameter.getValue(3));
        assertNotSame(parameter.getValue(0), parameter.getValue(1));
        try {
            parameter.setValue(2, 2.0); // this will throw an exception
            assertNotSame(parameter.getValue(0), parameter.getValue(2));
        } catch (Exception e) {
            // setValue is not allowed for StateNode not in State
        }
        Double[] x = {1.0, 2.0, 3.0, 2.0, 4.0, 5.5};
        parameter = new RealParameter(x);
        assertEquals(parameter.getDimension(), 6);
    }

    //*** test keys ***//

    @Test
    public void testGetKey() {
        RealParameter keyParam = new RealParameter();
        // pretend to be 1d array now
        keyParam.initByName("value", "3.0 2.0 1.0");

        // the i'th value
        assertEquals("1", keyParam.getKey(0));
        assertEquals("3", keyParam.getKey(2));

    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Test
    public void testGetKeyException() {
        RealParameter keyParam = new RealParameter();
        // pretend to be 1d array now
        keyParam.initByName("value", "3.0");

        // if (getDimension() == 1) return "0";
        assertEquals("0", keyParam.getKey(0));

        exception.expect(IllegalArgumentException.class);
        keyParam.getKey(2);
    }


    final String spNames = "sp1 sp2 sp3 sp4 sp5 sp6 sp7 sp8 sp9 sp10";
    final String spNames2 = "sp11 sp12 sp13 sp14 sp15 sp16 sp17 sp18 sp19 sp20";

    // each line is a species, each column a trait
    final List<Double> twoTraitsValues =  Arrays.asList(
            0.326278727608277, 1.8164550628074,
            -0.370085503473201, 0.665116986641999,
            1.17377224776421, 3.59440970719762,
            3.38137444987329, -0.187743059073837,
            -1.64759474375234, -2.19534387982435,
            -3.22668212260941, -1.71183724870188,
            1.81925405275285, -0.428821390843389,
            4.22298205455098, 1.51483058860744,
            3.63674837097173, 3.68456953445085,
            -0.743303344769609, 1.10602125889508
    );

    /*
     * This test checks whether we get all the trait values for two species.
     * For 2D matrix, keys must either have the same length as dimension or the number of rows.
     */
    @Test
    public void testKeysTwoColumns () {

        final int colCount = 2;
        RealParameter twoCols = new RealParameter();
        twoCols.initByName("value", twoTraitsValues, "keys", spNames, "minordimension", colCount);

        assertEquals(twoCols.getDimension()/colCount, twoCols.getKeysList().size());
        Assert.assertArrayEquals(twoCols.getRowValues("sp1"), new Double[] { 0.326278727608277, 1.8164550628074 });
        Assert.assertArrayEquals(twoCols.getRowValues("sp8"), new Double[] { 4.22298205455098, 1.51483058860744 });
    }

    /**
     * For 1D array, keys must have the same length as dimension
     */
    @Test
    public void testKeys1DArray () {

        RealParameter oneTraits = new RealParameter();
        // pretend to be 1d array now
        oneTraits.initByName("value", twoTraitsValues, "keys", spNames+" "+spNames2);

        assertEquals(oneTraits.getDimension(), oneTraits.getKeysList().size());
        // 1d array now, so values positions are diff
        Assert.assertArrayEquals(oneTraits.getRowValues("sp1"), new Double[] { 0.326278727608277 });
        Assert.assertArrayEquals(oneTraits.getRowValues("sp8"), new Double[] { -0.187743059073837 });
        Assert.assertArrayEquals(oneTraits.getRowValues("sp11"), new Double[] { -3.22668212260941 });
        Assert.assertArrayEquals(oneTraits.getRowValues("sp19"), new Double[] { -0.743303344769609 });
    }



}
