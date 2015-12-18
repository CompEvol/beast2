/*
 * FrequencySet.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * modified from BEAST 1 FrequencySet<T>
 * Stores a set of objects with frequencies
 *
 * @author Andrew Rambaut
 * @author Walter Xie
 */
public class FrequencySet<T> {
    public static final double DEFAULT_CRED_SET = 0.95;

    public FrequencySet() {
        setCredSetProbability(DEFAULT_CRED_SET);
    }

    // really need ?
    public FrequencySet(double credSetProbability) {
        setCredSetProbability(credSetProbability);
    }

    /**
     * get number of objects
     */
    public int size() {
        return frequencyMap.size();
    }

    /**
     * get object in frequency order
     */
    public T get(int i) {
        if (!sorted) {
            sortByFrequency();
        }

        return sortedList.get(i);
    }


    public Integer getFrequency(T obj) {
        if (!sorted) {
            sortByFrequency();
        }

        return frequencyMap.get(obj);
    }

    /**
     * get frequency of ith object
     */
    public int getFrequency(int i) {
        return getFrequency(sortedList.get(i));
    }

    /**
     * get sum of all frequencies
     */
    public int getSumFrequency() {
        if (!sorted) {
            sortByFrequency();
        }

        int sum = 0;
        for (int i = 0; i < size(); i++) {
            sum += getFrequency(i);
        }

        return sum;
    }

    /**
     * adds an object to the set
     */
    public void add(T object) {
        add(object, 1);
    }

    /**
     * adds an object to the set with an initial frequency, or if object already
     * in frequency set then frequency is incremented by given frequency.
     */
    public void add(T object, int frequency) {
        Integer freq = frequencyMap.get(object);
        if (freq != null) {
            freq += frequency;
            frequencyMap.put(object, freq);
        } else {
            frequencyMap.put(object, frequency);
            sortedList.add(object);
            sorted = false;
        }
    }

    /**
     * The frequencySets are equal if their inner sets are equal
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FrequencySet) && sortedList.equals(((FrequencySet) obj).sortedList);
    }

    public Map<T, Integer> getFrequencyMap() {
        return frequencyMap;
    }

    /**
     *
     * @param target     if null, then only return credibleSetList.credibleSetList
     * @return
     */
    public CredibleSet<T> getCredibleSet(T target) {
        CredibleSet<T> credibleSet = new CredibleSet<>(getCredSetProbability());
        credibleSet.setCredibleSetList(target, this);
        return credibleSet;
    }

    public CredibleSet<T> getCredibleSet() {
        return getCredibleSet(null);
    }


    public double getCredSetProbability() {
        if (credSetProbability == 0)
            setCredSetProbability(DEFAULT_CRED_SET);
        return credSetProbability;
    }

    public void setCredSetProbability(double credSetProbability) {
        this.credSetProbability = credSetProbability;
    }

    /**
     * sort by descending frequency
     */
    private void sortByFrequency() {
        sortedList.clear();
        sortedList.addAll(frequencyMap.keySet());
        Collections.sort(sortedList, frequencyComparator);
        sorted = true;
    }

    //
    // Private stuff
    //

    private List<T> sortedList = new ArrayList<>();
    private Map<T, Integer> frequencyMap = new HashMap<>();
    private boolean sorted = false;
    private double credSetProbability = 0;

    private Comparator<T> frequencyComparator = new Comparator<T>() {
        public int compare(T obj1, T obj2) {
            return frequencyMap.get(obj2) - frequencyMap.get(obj1);
        }
    };

}