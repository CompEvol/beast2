package beast.core.parameter;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.StateNode;

public interface Parameter<T> extends Function {

    public T getValue(int i);

    public T getValue();

    public void setValue(int i, T value);
    
    public void setValue(T fValue);

    public T getLower();

    public void setLower(final T fLower);

    public T getUpper();

    public void setUpper(final T fUpper);

    public T[] getValues();

    public String getID();

    public int getMinorDimension1();

    public int getMinorDimension2();

    public T getMatrixValue(int i, int j);

    /**
     * swap values of element i and j
     *
     * @param i
     * @param j
     */
    public void swap(int i, int j);

    @Description("A parameter represents a value in the state space that can be changed "
            + "by operators.")
    public abstract class Base<T> extends StateNode implements Parameter<T> {

        /**
         * value is a required input since it is very hard to ensure any
         * internal consistency when no value is specified. When	another class
         * wants to set the dimension, say, this will make it the responsibility
         * of the other class to maintain internal consistency of the parameter.
         */
        final public Input<List<T>> valuesInput = new Input<>("value", "start value(s) for this parameter. If multiple values are specified, they should be separated by whitespace.", new ArrayList<>(), beast.core.Input.Validate.REQUIRED, getMax().getClass());
        public final Input<java.lang.Integer> dimensionInput =
                new Input<>("dimension", "dimension of the parameter (default 1, i.e scalar)", 1);
        public final Input<Integer> minorDimensionInput = new Input<>("minordimension", "minor-dimension when the parameter is interpreted as a matrix (default 1)", 1);

        /**
         * constructors *
         */
        public Base() {
        }

        public Base(final T[] values) {
            this.values = values.clone();
            this.storedValues = values.clone();
            m_fUpper = getMax();
            m_fLower = getMin();
            m_bIsDirty = new boolean[values.length];
            for (T value : values) {
            	valuesInput.get().add(value);
            }
        }

        @SuppressWarnings("unchecked")
		@Override
        public void initAndValidate() throws Exception {
            T[] sValues = valuesInput.get().toArray((T[]) Array.newInstance(getMax().getClass(), 0));

            int nDimension = Math.max(dimensionInput.get(), sValues.length);
            dimensionInput.setValue(nDimension, this);
            values = (T[]) Array.newInstance(getMax().getClass(), nDimension);
            storedValues = (T[]) Array.newInstance(getMax().getClass(), nDimension);
            for (int i = 0; i < values.length; i++) {
                values[i] = sValues[i % sValues.length];
            }

            m_bIsDirty = new boolean[dimensionInput.get()];

            minorDimension = minorDimensionInput.get();
            if (minorDimension > 0 && dimensionInput.get() % minorDimension > 0) {
                throw new IllegalArgumentException("Dimension must be divisible by stride");
            }
            this.storedValues = values.clone();
        }
        /**
         * upper & lower bound These are located before the inputs (instead of
         * after the inputs, as usual) so that valuesInput can determines the
         * class
         */
        protected T m_fUpper;
        protected T m_fLower;

        abstract T getMax();

        abstract T getMin();
        /**
         * the actual values of this parameter
         */
        protected T[] values;
        protected T[] storedValues;
        /**
         * sub-dimension when parameter is considered a matrix
         */
        protected int minorDimension = 1;
        /**
         * isDirty flags for individual elements in high dimensional parameters
         */
        protected boolean[] m_bIsDirty;
        /**
         * last element to be changed *
         */
        protected int m_nLastDirty;

        /**
         * @param index dimension to check
         * @return true if the iParam-th element has changed
         */
        public boolean isDirty(final int index) {
            return m_bIsDirty[index];
        }

        /**
         * Returns index of entry that was changed last. Useful if it is known
         * only a single value has changed in the array. *
         */
        public int getLastDirty() {
            return m_nLastDirty;
        }

        @Override
        public void setEverythingDirty(final boolean isDirty) {
            setSomethingIsDirty(isDirty);
            Arrays.fill(m_bIsDirty, isDirty);
        }

        /*
         * various setters & getters *
         */
        @Override
        public int getDimension() {
            return values.length;
        }

        /**
         * Change the dimension of a parameter
         * <p/>
         * This should only be called from initAndValidate() when a parent
         * beastObject can easily calculate the dimension of a parameter, but it is
         * awkward to do this by hand.
         * <p/>
         * Values are sourced from the original parameter values.
         *
         * @param dimension
         */
        @SuppressWarnings("unchecked")
        public void setDimension(final int dimension) {
            if (getDimension() != dimension) {
                final T[] values2 = (T[]) Array.newInstance(getMax().getClass(), dimension);
                for (int i = 0; i < dimension; i++) {
                    values2[i] = values[i % getDimension()];
                }
                values = values2;
                //storedValues = (T[]) Array.newInstance(m_fUpper.getClass(), nDimension);
            }
            m_bIsDirty = new boolean[dimension];
            try {
                dimensionInput.setValue(dimension, this);
            } catch (Exception e) {
                // ignore
            }
        }

        public void setMinorDimension(final int dimension) throws Exception {
            minorDimension = dimension;
            if (minorDimension > 0 && dimensionInput.get() % minorDimension > 0) {
                throw new IllegalArgumentException("Dimension must be divisible by stride");
            }
        }

        @Override
        public T getValue() {
            return values[0];
        }

        @Override
        public T getLower() {
            return m_fLower;
        }

        @Override
        public void setLower(final T fLower) {
            m_fLower = fLower;
        }

        @Override
        public T getUpper() {
            return m_fUpper;
        }

        @Override
        public void setUpper(final T fUpper) {
            m_fUpper = fUpper;
        }

        @Override
        public T getValue(final int iParam) {
            return values[iParam];
        }

        @Override
        public T[] getValues() {
            return Arrays.copyOf(values, values.length);
        }

        public void setBounds(final T fLower, final T fUpper) {
            m_fLower = fLower;
            m_fUpper = fUpper;
        }

        @Override
        public void setValue(final T fValue) {
            startEditing(null);

            values[0] = fValue;
            m_bIsDirty[0] = true;
            m_nLastDirty = 0;
        }

        @Override
        public void setValue(final int iParam, final T fValue) {
            startEditing(null);

            values[iParam] = fValue;
            m_bIsDirty[iParam] = true;
            m_nLastDirty = iParam;

        }

        @Override
        public void swap(final int iLeft, final int iRight) {
            startEditing(null);
            final T tmp = values[iLeft];
            values[iLeft] = values[iRight];
            values[iRight] = tmp;
            m_bIsDirty[iLeft] = true;
            m_bIsDirty[iRight] = true;
        }

        /**
         * Note that changing toString means fromXML needs to be changed as
         * well, since it parses the output of toString back into a parameter.
         */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append(getID()).append("[").append(values.length);
            if (minorDimension > 0) {
                buf.append(" ").append(minorDimension);
            }
            buf.append("] ");
            buf.append("(").append(m_fLower).append(",").append(m_fUpper).append("): ");
            for (final T value : values) {
                buf.append(value).append(" ");
            }
            return buf.toString();
        }

        @Override
        public Base<T> copy() {
            try {
                @SuppressWarnings("unchecked")
                final Parameter.Base<T> copy = (Parameter.Base<T>) this.clone();
                copy.values = values.clone();//new Boolean[values.length];
                copy.m_bIsDirty = new boolean[values.length];
                return copy;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void assignTo(final StateNode other) {
            @SuppressWarnings("unchecked")
            final Parameter.Base<T> copy = (Parameter.Base<T>) other;
            copy.setID(getID());
            copy.index = index;
            copy.values = values.clone();
            //System.arraycopy(values, 0, copy.values, 0, values.length);
            copy.m_fLower = m_fLower;
            copy.m_fUpper = m_fUpper;
            copy.m_bIsDirty = new boolean[values.length];
        }

        @Override
        public void assignFrom(final StateNode other) {
            @SuppressWarnings("unchecked")
            final Parameter.Base<T> source = (Parameter.Base<T>) other;
            setID(source.getID());
            values = source.values.clone();
            storedValues = source.storedValues.clone();
            System.arraycopy(source.values, 0, values, 0, values.length);
            m_fLower = source.m_fLower;
            m_fUpper = source.m_fUpper;
            m_bIsDirty = new boolean[source.values.length];
        }

        @Override
        public void assignFromFragile(final StateNode other) {
            @SuppressWarnings("unchecked")
            final Parameter.Base<T> source = (Parameter.Base<T>) other;
            System.arraycopy(source.values, 0, values, 0, values.length);
            Arrays.fill(m_bIsDirty, false);
        }

        /**
         * Loggable interface implementation follows (partly, the actual logging
         * of values happens in derived classes) *
         */
        @Override
        public void init(final PrintStream out) throws Exception {
            final int valueCount = getDimension();
            if (valueCount == 1) {
                out.print(getID() + "\t");
            } else {
                for (int iValue = 0; iValue < valueCount; iValue++) {
                    out.print(getID() + (iValue + 1) + "\t");
                }
            }
        }

        @Override
        public void close(final PrintStream out) {
            // nothing to do
        }

        /**
         * StateNode implementation *
         */
        @Override
        public void fromXML(final Node node) {
            final NamedNodeMap atts = node.getAttributes();
            setID(atts.getNamedItem("id").getNodeValue());
            final String str = node.getTextContent();
            Pattern pattern = Pattern.compile(".*\\[(.*) (.*)\\].*\\((.*),(.*)\\): (.*) ");
            Matcher matcher = pattern.matcher(str);

            if (matcher.matches()) {
                final String dimension = matcher.group(1);
                final String stride = matcher.group(2);
                final String lower = matcher.group(3);
                final String upper = matcher.group(4);
                final String valuesAsString = matcher.group(5);
                final String[] values = valuesAsString.split(" ");
                minorDimension = Integer.parseInt(stride);
                fromXML(Integer.parseInt(dimension), lower, upper, values);
            } else {
                pattern = Pattern.compile(".*\\[(.*)\\].*\\((.*),(.*)\\): (.*) ");
                matcher = pattern.matcher(str);
                if (matcher.matches()) {
                    final String dimension = matcher.group(1);
                    final String lower = matcher.group(2);
                    final String upper = matcher.group(3);
                    final String valuesAsString = matcher.group(4);
                    final String[] values = valuesAsString.split(" ");
                    minorDimension = 0;
                    fromXML(Integer.parseInt(dimension), lower, upper, values);
                } else {
                    throw new RuntimeException("parameter could not be parsed");
                }
            }
        }

        /**
         * Restore a saved parameter from string representation. This cannot be
         * a template method since it requires creation of an array of T...
         *
         * @param dimension parameter dimension
         * @param lower lower bound
         * @param upper upper bound
         * @param values values
         */
        abstract void fromXML(int dimension, String lower, String upper, String[] values);

        /**
         * matrix implementation *
         */
        @Override
        public int getMinorDimension1() {
            return minorDimension;
        }

        @Override
        public int getMinorDimension2() {
            return getDimension() / minorDimension;
        }

        @Override
        public T getMatrixValue(final int i, final int j) {
            return values[i * minorDimension + j];
        }

        public void setMatrixValue(final int i, final int j, final T value) {
            setValue(i * minorDimension + j, value);
        }

        public void getMatrixValues1(final int i, final T[] row) {
            assert (row.length == minorDimension);
            System.arraycopy(values, i * minorDimension, row, 0, minorDimension);
        }

        public void getMatrixValues1(final int i, final double[] row) {
            assert (row.length == minorDimension);
            for (int j = 0; j < minorDimension; j++) {
                row[j] = getArrayValue(i * minorDimension + j);
            }
        }

        public void getMatrixValues2(final int j, final T[] col) {
            assert (col.length == getMinorDimension2());
            for (int i = 0; i < getMinorDimension2(); i++) {
                col[i] = values[i * minorDimension + j];
            }
        }

        public void getMatrixValues2(final int j, final double[] col) {
            assert (col.length == getMinorDimension2());
            for (int i = 0; i < getMinorDimension2(); i++) {
                col[i] = getArrayValue(i * minorDimension + j);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void store() {
            if (storedValues.length != values.length) {
                storedValues = (T[]) Array.newInstance(m_fUpper.getClass(), values.length);
            }
            System.arraycopy(values, 0, storedValues, 0, values.length);
        }

        @Override
        public void restore() {
            final T[] tmp = storedValues;
            storedValues = values;
            values = tmp;
            hasStartedEditing = false;
            if (m_bIsDirty.length != values.length) {
                m_bIsDirty = new boolean[values.length];
            }
        }
    } // class Parameter
}
