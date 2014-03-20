/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package test.beast.math.statistic;

import beast.core.parameter.RealParameter;
import beast.math.statistic.ExpCalculator;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class ExpCalculatorTest {
    
    @Test
    public void hello() throws Exception {
        RealParameter iparam = new RealParameter("1.0 2.0 3.0");
        iparam.setID("I");

        RealParameter jparam = new RealParameter("27.0 13.5");
        jparam.setID("J");

        
        ExpCalculator instance = new ExpCalculator();
        instance.initByName(
                "expression", "-(J[0]/J + log(exp(I))*-3 - 1.5 + 1.5 + 1 + -3)",
                "parameter", iparam,
                "parameter", jparam);
        
        assertEquals(instance.getDimension(), 3);
        assertTrue(Math.abs(instance.getArrayValue(0)-4.0)<1e-15);
        assertTrue(Math.abs(instance.getArrayValue(1)-6.0)<1e-15);
        assertTrue(Math.abs(instance.getArrayValue(2)-10.0)<1e-15);
    }
}
