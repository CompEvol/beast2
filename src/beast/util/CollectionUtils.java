package beast.util;


import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * some useful methods
 */
public class CollectionUtils {

    // not use set because hard to get element given index
    public static <E> List<E> intersection(List<E> list1, List<E> list2) {
        list1.retainAll(list2);
        return list1;
    }

    public static <E> List<E> intersection(E[] array1, E[] array2) {
        return intersection(Arrays.asList(array1), Arrays.asList(array2));
    }

    public static <E> int indexof(E sLabel, E[] m_sLabels) {
        for (int i = 0; i < m_sLabels.length ; i++) {
            if (m_sLabels[i].equals(sLabel)) {
                return i;
            }
        }
        return -1;
    }

    /**
     *
     * @param array the array to be converted into list
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     * @return
     */
    public static <E> List<E> toList(E[] array, int fromIndex, int toIndex) {
        List<E> list = Arrays.asList(array);
        return list.subList(fromIndex, toIndex);
    }

    /**
     * very inefficient, but Java wonderful bitset has no subset op
     * perhaps using bit iterator would be faster, I can't br bothered.
     * @param x
     * @param y
     * @return
     */
    public static boolean isSubSet(BitSet x, BitSet y) {
        y = (BitSet) y.clone();
        y.and(x);
        return y.equals(x);
    }
}
