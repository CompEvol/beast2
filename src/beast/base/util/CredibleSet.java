package beast.base.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Credible Set results
 *
 * @author Walter Xie
 */
public class CredibleSet<T> {

    public List<T> credibleSetList;

    final double credSetProbability;

    public int sumFrequency = 0;

    public int targetIndex = -1;
    public double targetProb = 0.0;
    public double targetCum = 1.0;

    public CredibleSet(double credSetProbability) {
        credibleSetList = new ArrayList<>();
        this.credSetProbability = credSetProbability;
    }

    public void setCredibleSetList(T target, FrequencySet<T> frequencySet) {
        int total = frequencySet.getSumFrequency();

        for (int i = 0; i < frequencySet.size(); i++) {
            final int freq = frequencySet.getFrequency(i);
            final double prop = ((double) freq) / total;

            sumFrequency += freq;
            final double sumProp = ((double) sumFrequency) / (double)total;

            T obj = frequencySet.get(i);
            credibleSetList.add(obj);
            if (target != null && obj.equals(target)) {
                targetIndex = i + 1;
                targetProb = prop;
                targetCum = sumProp;
            }

            if (sumProp >= credSetProbability) {
                break;
            }
        }
    }

    /**
     * get frequency of ith object
     */
    public int getFrequency(int i, FrequencySet<T> frequencySet) {
        return frequencySet.getFrequency(credibleSetList.get(i));
    }
}