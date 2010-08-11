package deadcode.evolution.tree;

import beast.core.StateNode;
import beast.evolution.tree.Node;

/**
 * @author Alexei Drummond
 */
public abstract class TreeTrait<T> extends StateNode {

    T[] values;

    public final T getValue(Node node) {

        return values[node.getNr()];
    }

//    public class Double extends TreeTrait<java.lang.Double> {
//
//        public Double(int size) {
//            values = new java.lang.Double[size];
//        }
//
//        @Override
//        public StateNode copy() {
//            TreeTrait copy = new TreeTrait.Double(values.length);
//            System.arraycopy(values, 0, copy.values, 0, values.length);
//            return copy;
//        }
//    }
//
//    public class Integer extends TreeTrait<java.lang.Integer> {
//
//        public Integer(int size) {
//            values = new java.lang.Integer[size];
//        }
//
//        @Override
//        public StateNode copy() {
//            TreeTrait copy = new TreeTrait.Integer(values.length);
//            System.arraycopy(values, 0, copy.values, 0, values.length);
//            return copy;
//        }
//    }
}
