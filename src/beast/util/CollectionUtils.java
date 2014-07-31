package beast.util;


import java.util.Arrays;
import java.util.List;

/**
 * some useful methods
 *
 * @author Walter Xie
 */
public class CollectionUtils {

    // not use set because hard to get element given index
    public static <E> List<E> insect(List<E> list1, List<E> list2) {
        list1.retainAll(list2);
        return list1;
    }

    public static <E> List<E> insect(E[] array1, E[] array2) {
        return insect(Arrays.asList(array1), Arrays.asList(array2));
    }

    public static <E> int indexof(E sLabel, E[] m_sLabels) {
        for (int i = 0; i < m_sLabels.length ; i++) {
            if (m_sLabels[i].equals(sLabel)) {
                return i;
            }
        }
        return -1;
    }
}
