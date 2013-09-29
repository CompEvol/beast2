/*
 * Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>.
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
package beast.core.parameter;

import beast.core.Description;
import beast.core.StateNode;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("ParameterList state node.  Work in progress!")
public class ParameterList<T> extends StateNode {
    
    List<QuietParameter> pList, pListStored;

    protected boolean dirty;

    public ParameterList() { };
    
    @Override
    public void initAndValidate() {
        pList = new ArrayList<QuietParameter>();
        pListStored = new ArrayList<QuietParameter>();
        dirty = true;
    }
    
    @Override
    public void setEverythingDirty(boolean isDirty) {
        dirty = isDirty;
    }

    @Override
    public StateNode copy() {
        ParameterList<T> copy = new ParameterList<T>();
        copy.initAndValidate();
        for (QuietParameter param : pList) {
            QuietParameter paramCopy = param.copy();
            copy.pList.add(paramCopy);
        }
        
        return copy;
    }

    @Override
    public void assignTo(StateNode other) {
        if (!(other instanceof ParameterList))
            throw new RuntimeException("Incompatible statenodes in assignTo "
                    + "call.");
        
        ParameterList otherParamList = (ParameterList)other;
        
        otherParamList.pList.clear();
        for (QuietParameter param : pList)
            otherParamList.pList.add(param.copy());
        
        otherParamList.dirty = true;
    }

    @Override
    public void assignFrom(StateNode other) {
        if (!(other instanceof ParameterList))
            throw new RuntimeException("Incompatible statenodes in assignFrom "
                    + "call.");
        
        ParameterList otherParamList = (ParameterList)other;
        
        pList.clear();
        for (Object paramObj : otherParamList.pList)
            pList.add((QuietParameter)paramObj);

        dirty = true;
    }

    @Override
    public void assignFromFragile(StateNode other) {
        assignFrom(other);
    }

    @Override
    public void fromXML(Node node) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public int scale(double fScale) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void store() {
        pListStored.clear();
        for (QuietParameter param : pList)
            pListStored.add(param.copy());
    }

    @Override
    public void restore() {
        pList.clear();
        for (QuietParameter param: pListStored)
            pList.add(param.copy());
    }

    @Override
    public void init(PrintStream out) throws Exception {
        // TODO.
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void log(int nSample, PrintStream out) {
        // TODO.  Need to figure out how to log variable-dimension quantities...
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close(PrintStream out) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getDimension() {
        return pList.size();
    }

    @Override
    public double getArrayValue() {
        // TODO: How does this make sense?
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getArrayValue(int iDim) {
        // TODO: How does this make sense?
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Jessie's QuietParameter.  Objects of this class make sense
     * only in the context of ParameterLists.  They behave very much like
     * Parameter<T>.Base objects, but are not StateNodes.
     */
    public class QuietParameter implements Parameter<T> {

        List<T> values;
        T lower, upper;
        int dimMinor;
        
        QuietParameter() {
            values = new ArrayList<T>();
        }
        
        @Override
        public T getValue(int i) {
            return values.get(i);
        }

        @Override
        public T getValue() {
            return values.get(0);
        }

        @Override
        public void setValue(int i, T value) {
            values.set(i, value);
        }

        @Override
        public T getLower() {
            return lower;
        }

        @Override
        public void setLower(T lower) {
            this.lower = lower;
        }

        @Override
        public T getUpper() {
            return upper;
        }

        @Override
        public void setUpper(T upper) {
            this.upper = upper;
        }

        @Override
        public T[] getValues() {
            return (T[])values.toArray();
        }

        @Override
        public String getID() {
            // TODO
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getMinorDimension1() {
            return dimMinor;
        }
        
        @Override
        public int getMinorDimension2() {
            return values.size()/dimMinor;
        }

        @Override
        public T getMatrixValue(int i, int j) {
            return values.get(i*dimMinor+j);
        }

        @Override
        public void swap(int i, int j) {
            T tmp = values.get(i);
            values.set(i, values.get(j));
            values.set(j, tmp);
        }

        @Override
        public int getDimension() {
            return values.size();
        }

        @Override
        public double getArrayValue() {
            return (Double)values.get(0);
        }

        @Override
        public double getArrayValue(int i) {
            return (Double)values.get(i);
        }
        
        /**
         * @return deep copy of parameter.
         */
        public QuietParameter copy() {
            QuietParameter paramCopy = new QuietParameter();
            
            paramCopy.values.addAll(values);
            paramCopy.lower = lower;
            paramCopy.upper = upper;
            paramCopy.dimMinor = dimMinor;
            
            return paramCopy;
        }
    }
    
}
