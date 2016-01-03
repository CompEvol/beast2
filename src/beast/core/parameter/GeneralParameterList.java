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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.util.Log;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 * @param <T> Type of parameters in list.
 */
@Description("State node representing a list of parameter objects, used for "
        + "model selection problems. The parameters involved are not instances "
        + "of Parameter.Base, but are instead instances of a local class "
        + "QuietParameter which is not itself a StateNode.  All constituent "
        + "parameters must have identical dimensions and bounds.")
public abstract class GeneralParameterList<T> extends StateNode {
    
    final public Input<List<Parameter.Base<T>>> initialParamsInput = new Input<>(
            "initialParam",
            "Parameter whose value will initially be in parameter list.",
            new ArrayList<>());
    
    final public Input<Integer> dimensionInput = new Input<>("dimension",
            "Dimension of individual parameters in list.  Default 1.", 1);
    
    final public Input<Integer> minorDimensionInput = new Input<>("minordimension",
            "Minor dimension of individual parameters in list. Default 1.", 1);
    

    
    protected List<QuietParameter> pList, pListStored;
    
    protected TreeSet<Integer> deallocatedKeys, deallocatedKeysStored;
    protected int nextUnallocatedKey, nextUnallocatedKeyStored;
    
    protected int dimension, minorDimension;
    protected T lowerBound, upperBound;

    public GeneralParameterList() { };
    
    @Override
    public void initAndValidate() {
        pList = new ArrayList<>();
        pListStored = new ArrayList<>();
        deallocatedKeys = new TreeSet<>();
        deallocatedKeysStored = new TreeSet<>();
        nextUnallocatedKey = 0;
        nextUnallocatedKeyStored = 0;
        
        dimension = dimensionInput.get();
        minorDimension = minorDimensionInput.get();
        
        for (Parameter<?> param : initialParamsInput.get()) {
            if (param.getDimension() != dimension)
                throw new IllegalArgumentException("Parameter dimension does not equal"
                        + " dimension specified in enclosing ParameterList.");
            QuietParameter qParam = new QuietParameter(param);
            allocateKey(qParam);
            pList.add(qParam);
        }

        store();
        setSomethingIsDirty(false);
    }
   
    /**
     * Retrieve number of parameters in parameter list.
     * 
     * @return size of parameter list.
     */
    public int size() {
        return pList.size();
    }
    
    /**
     * Retrieve parameter from list.
     * 
     * @param index index of parameter to retrieve
     * @return parameter
     */
    public QuietParameter get(int index) {
        return pList.get(index);
    }
    
    /**
     * Assign parameter to position in list.
     * 
     * @param index
     * @param param 
     */
    public void set(int index, QuietParameter param) {
        startEditing(null);
        pList.set(index, param);
    }
    
    /**
     * Append parameter to end of list.
     * 
     * @param param 
     */
    public void add(QuietParameter param) {
        startEditing(null);
        pList.add(param);
    }
    
    /**
     * Insert parameter at position index in list, incrementing the index of
     * all parameters already at and to the right of that position.
     * 
     * @param index
     * @param param 
     */
    public void add(int index, QuietParameter param) {
        startEditing(null);
        pList.add(index, param);
    }
    
    /**
     * Remove parameter from list.
     * 
     * @param param 
     */
    public void remove(QuietParameter param) {
        startEditing(null);
        deallocatedKeys.add(param.key);
        pList.remove(param);
    }
    
    /**
     * Remove parameter at index from list.
     * 
     * @param index 
     */
    public void remove(int index) {
        startEditing(null);
        deallocatedKeys.add(pList.get(index).key);
        pList.remove(index);
    }
    
    /**
     * Create new parameter, without appending it to the list.  This only
     * makes sense if the parameter is eventually added to the list.  This
     * call does not itself affect the ParameterList's dirty status (it
     * will be marked as dirty when/if add() is called).
     * 
     * @return New parameter.
     */
    public QuietParameter createNewParam() {
        QuietParameter param = new QuietParameter();
        allocateKey(param);
        return param;
    }
    
    /**
     * Create new parameter from existing Parameter, without appending it to
     * the list.  This only makes sense if the parameter is eventually added
     * to the list.  This call does not itself affect the ParameterList's
     * dirty status (it will be marked as dirty when/if add() is called).
     * 
     * @param otherParam
     * @return New parameter.
     */
    public QuietParameter createNewParam(Parameter<?> otherParam) {
        QuietParameter param = new QuietParameter(otherParam);
        allocateKey(param);
        return param;
    }

    /**
     * Create new parameter and append to list.
     * 
     * @return New parameter.
     */
    public QuietParameter addNewParam() {
        startEditing(null);
        QuietParameter param = new QuietParameter();
        allocateKey(param);
        pList.add(param);
        return param;
    }
    
    /**
     * Create new parameter from existing Parameter and append to list.
     * 
     * @param otherParam
     * @return New parameter.
     */
    public QuietParameter addNewParam(Parameter<?> otherParam) {
        startEditing(null);
        QuietParameter param = new QuietParameter(otherParam);
        allocateKey(param);
        pList.add(param);
        return param;
    }
    
    /**
     * Assign unique ID to this parameter.
     * @param param 
     */
    private void allocateKey(QuietParameter param) {
        if (deallocatedKeys.size()>0) {
            param.key = deallocatedKeys.first();
            deallocatedKeys.remove(param.key);
        } else {
            param.key = nextUnallocatedKey;
            nextUnallocatedKey += 1;
        }
    }
    
    @Override
    public StateNode copy() {

        try {
            @SuppressWarnings("unchecked")
			GeneralParameterList<T> copy = (GeneralParameterList<T>) this.clone();
            copy.initAndValidate();
                    
            copy.pList.clear();
            for (QuietParameter param : pList) {
                QuietParameter paramCopy = param.copy();
                copy.pList.add(paramCopy);
            }
        
            copy.dimension = dimension;
            copy.minorDimension = minorDimension;
            copy.lowerBound = lowerBound;
            copy.upperBound = upperBound;
            copy.deallocatedKeys.addAll(deallocatedKeys);
            copy.nextUnallocatedKey = nextUnallocatedKey;
            
            return copy;
        
        } catch (CloneNotSupportedException ex) {
            Log.err(ex.getMessage());
        }
        
        return null;
    }

    @Override
    public void assignTo(StateNode other) {
        if (!(other instanceof GeneralParameterList))
            throw new RuntimeException("Incompatible statenodes in assignTo "
                    + "call.");
        
        @SuppressWarnings("unchecked")
		GeneralParameterList<T> otherParamList = (GeneralParameterList<T>)other;
        
        otherParamList.pList.clear();
        for (QuietParameter param : pList)
            otherParamList.pList.add(param.copy());
        
        otherParamList.dimension = dimension;
        otherParamList.minorDimension = minorDimension;
        otherParamList.lowerBound = lowerBound;
        otherParamList.upperBound = upperBound;
        otherParamList.deallocatedKeys = new TreeSet<>(deallocatedKeys);
        otherParamList.nextUnallocatedKey = nextUnallocatedKey;
    }

    @SuppressWarnings("unchecked")
	@Override
    public void assignFrom(StateNode other) {
        if (!(other instanceof GeneralParameterList))
            throw new RuntimeException("Incompatible statenodes in assignFrom "
                    + "call.");
        
		GeneralParameterList<T> otherParamList = (GeneralParameterList<T>)other;
        
        pList.clear();
        for (Object paramObj : otherParamList.pList)
            pList.add((QuietParameter) paramObj);
        
        dimension = otherParamList.dimension;
        minorDimension = otherParamList.minorDimension;
        lowerBound = otherParamList.lowerBound;
        upperBound = otherParamList.upperBound;
        deallocatedKeys = new TreeSet<>(otherParamList.deallocatedKeys);
        nextUnallocatedKey = otherParamList.nextUnallocatedKey;
    }

    @Override
    public void assignFromFragile(StateNode other) {
        assignFrom(other);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("Dimension: [%d, %d], Bounds: [%s,%s], ",
                dimension,
                minorDimension,
                String.valueOf(lowerBound),
                String.valueOf(upperBound)));
        
        sb.append("AvailableKeys: [");
        boolean first = true;
        for (int key : deallocatedKeys) {
            if (!first)
                sb.append(",");
            else
                first = false;
            
            sb.append(key);
        }
        sb.append("], ");
        
        sb.append("NextKey: ").append(nextUnallocatedKey).append(", ");
        
        sb.append("Parameters: [");
        for (int i=0; i<pList.size(); i++) {
            if (i>0)
                sb.append(",");
            sb.append(pList.get(i));
        }
        sb.append("], ");
        
        sb.append("ParameterKeys: [");
        for (int i=0; i<pList.size(); i++) {
            if (i>0)
                sb.append(",");
            sb.append(pList.get(i).key);
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    
    @Override
    public void fromXML(Node node) {
        String str = node.getTextContent();
        
        Pattern pattern = Pattern.compile("^"
                + " *Dimension: *\\[([^]]*)] *,"
                + " *Bounds: *\\[([^]]*)] *,"
                + " *AvailableKeys: *\\[([^]]*)] *,"
                + " *NextKey: *([^, ]*) *,"
                + " *Parameters: *\\[(.*)] *,"
                + " *ParameterKeys: *\\[(.*)] *$");
        Matcher matcher = pattern.matcher(str);
        
        if (!matcher.find())
            throw new RuntimeException("Error parsing ParameterList state string.");
        
        // Parse dimension strings
        String [] dimStr = matcher.group(1).split(",");
        dimension = Integer.parseInt(dimStr[0].trim());
        minorDimension = Integer.parseInt(dimStr[1].trim());
        
        // Parse dealocated key strings
        deallocatedKeys.clear();
        for (String keyStr : matcher.group(3).trim().split(",") ) {
            if (keyStr.trim().length()>0)
                deallocatedKeys.add(Integer.parseInt(keyStr));
        }
        
        // Parse next allocated key string
        nextUnallocatedKey = Integer.parseInt(matcher.group(4));
        
        // Prepare bounds and parameter value strings for parsing by methods in
        // non-abstract classes (where type T is known).
        String [] boundsStr = matcher.group(2).split(",");
        
        List<String[]> parameterValueStrings = new ArrayList<>();
        String parameterListString = matcher.group(5).trim();
        
        pattern = Pattern.compile("\\[([^]]*)]");
        Matcher parameterMatcher = pattern.matcher(parameterListString);
        
        while(parameterMatcher.find())
            parameterValueStrings.add(parameterMatcher.group(1).split(","));
        
        // Parse key strings:
        List<Integer> keys = new ArrayList<>();
        for (String keyString : matcher.group(6).split(","))
            keys.add(Integer.parseInt(keyString.trim()));
        
        readStateFromString(boundsStr, parameterValueStrings, keys);
    }
    
    /**
     * Reads upper and lower parameter element bounds and parameter values from
     * strings and uses these to populate the corresponding GeneralParameterList
     * fields.
     * 
     * @param boundsStrings Two-element array containing lower and upper bounds.
     * @param parameterValueStrings List of arrays of reps of parameter values
     * @param keys List of keys to assign to parameters
     */
    protected abstract void readStateFromString(String [] boundsStrings,
            List<String[]> parameterValueStrings,
            List<Integer> keys);

    @Override
    public int scale(double fScale) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void store() {
        pListStored.clear();
        for (QuietParameter param : pList)
            pListStored.add(param.copy());

        deallocatedKeysStored.clear();
        deallocatedKeysStored.addAll(deallocatedKeys);
        nextUnallocatedKeyStored = nextUnallocatedKey;
    }

    @Override
    public void restore() {
        pList.clear();
        for (QuietParameter param: pListStored)
            pList.add(param.copy());
        
        deallocatedKeys.clear();
        deallocatedKeys.addAll(deallocatedKeysStored);
        nextUnallocatedKey = nextUnallocatedKeyStored;
        
        hasStartedEditing = false;
    }
    
    
    @Override
    public void setEverythingDirty(boolean isDirty) {
        setSomethingIsDirty(isDirty);
    }

    /*
    * The following methods are here because Functions are Loggable.  This
    * doesn't seem to make sense for ParameterLists though, so at the moment
    * these methods just log the ParameterLists's size.
    */
    
    @Override
    public void init(PrintStream out) throws Exception {
        out.print(getID() + ".size\t");
    }

    @Override
    public void log(int nSample, PrintStream out) {
        out.print(pList.size() + "\t");
    }

    @Override
    public void close(PrintStream out) { }

    /*
     * The following methods are here because all StateNodes are Functions.
     * They don't seem to make sense for ParameterLists though, so at the
     * moment these methods just probe the ParameterList's size.
     */
    
    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return pList.size();
    }

    @Override
    public double getArrayValue(int i) {
        if (i==0)
            return pList.size();
        else
            return Double.NaN;
    }

    

    /**
     * Jessie's QuietParameter.  Objects of this class make sense
     * only in the context of ParameterLists.  They behave very much like
     * Parameter<T>.Base objects, but are not StateNodes.
     */
    public class QuietParameter implements Parameter<T> {

        Object[] values;
        int key = -1;
        
        /**
         * Construct a new QuietParameter.
         */
        QuietParameter() {
            values = new Object[dimension];
        }

        /**
         * Create new QuietParameter from existing parameter.
         * 
         * @param param 
         */
        QuietParameter(Parameter<?> param) {
            if (param.getDimension() != dimension)
                throw new IllegalArgumentException("Cannot construct "
                        + "ParameterList parameter with a dimension not equal "
                        + "to that specified in the enclosing list.");
            
            values = new Object[dimension];
            for (int i=0; i<param.getValues().length; i++) {
                values[i] = param.getValue(i);
            }
            
        }
        
        public int getKey() {
            return key;
        }
        
        @SuppressWarnings("unchecked")
		@Override
        public T getValue(int i) {
            return (T)values[i];
        }

        @SuppressWarnings("unchecked")
		@Override
        public T getValue() {
            return (T)values[0];
        }

        @Override
        public void setValue(int i, T value) {
            startEditing(null); // ParameterList's startEditing()
            values[i] = value;
        }
        
        @Override
        public void setValue(T value) {
            startEditing(null); // ParameterList's startEditing()
            values[0] = value;
        }

        @Override
        public T getLower() {
            return lowerBound;
        }

        @Override
        public void setLower(T lower) {
            lowerBound = lower;
        }

        @Override
        public T getUpper() {
            return upperBound;
        }

        @Override
        public void setUpper(T upper) {
            upperBound = upper;
        }

        @SuppressWarnings("unchecked")
		@Override
        public T[] getValues() {
            return (T[])values;
        }

        @Override
        public String getID() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getMinorDimension1() {
            return minorDimension;
        }
        
        @Override
        public int getMinorDimension2() {
            return dimension/minorDimension;
        }
        
        
        @SuppressWarnings("unchecked")
		@Override
        public T getMatrixValue(int i, int j) {
            return (T)values[i*minorDimension+j];
        }

        @Override
        public void swap(int i, int j) {
            startEditing(null);
            Object tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }

        @Override
        public int getDimension() {
            return values.length;
        }

        @Override
        public double getArrayValue() {
            return (Double)values[0];
        }

        @Override
        public double getArrayValue(int i) {
            return (Double)values[0];
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (int i=0; i<values.length; i++) {
                if (i>0)
                    sb.append(",");
                sb.append(values[i]);
            }            
            sb.append("]");
            
            return sb.toString();
        }
        
        /**
         * @return deep copy of parameter.
         */
        public QuietParameter copy() {
            QuietParameter copy = new QuietParameter(this);
            copy.key = this.key;
            return copy;
        }

    }
    
}
