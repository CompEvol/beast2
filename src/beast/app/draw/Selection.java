/*
* File Selection.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
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
package beast.app.draw;

import java.util.ArrayList;
import java.util.List;

import beast.base.Log;

public class Selection {
    public List<TrackPoint> m_tracker = null;
    List<Integer> m_Selection;
    Document m_doc = null;

    public Selection() {
        m_Selection = new ArrayList<>(1);
    } // c'tor

    public void setDocument(Document doc) {
        m_doc = doc;
    }

    public boolean isSingleSelection() {
        return m_Selection.size() == 1;
    }

    boolean hasSelection() {
        return m_Selection.size() > 0;
    }

    public int getSingleSelection() {
        return m_Selection.get(0).intValue();
    }

    Shape getSingleSelectionShape() {
        return m_doc.m_objects.get(getSingleSelection());
    }

    void setSingleSelection(int selection) {
        m_Selection.removeAll(m_Selection);
        m_Selection.add(new Integer(selection));
        if (selection >= 0) {
            m_tracker = m_doc.m_objects.get(selection).getTracker();
        }
    }

    //    void setSingleSelection(int selection, Document doc) {
//    	m_Selection.removeAllElements();
//    	m_Selection.add(selection);
//    	m_tracker = ((Shape) doc.m_objects.get(selection)).getTracker();
//    }
    boolean contains(int selectionIndex) {
        for (int i = 0; i < m_Selection.size(); i++) {
            if (selectionIndex == m_Selection.get(i).intValue()) {
                return true;
            }
        }
        return false;
    } // contains

    void addToSelection(int selectionIndex) {
        if (contains(selectionIndex)) {
            return;
        }
        m_Selection.add(new Integer(selectionIndex));
        List<TrackPoint> tracker = m_doc.m_objects.get(selectionIndex).getTracker();
        if (m_tracker == null) {
            m_tracker = new ArrayList<>();
        }
        m_tracker.addAll(tracker);
    } // addToSelection

    void toggleSelection(int selectionIndex) {
        if (!contains(selectionIndex)) {
            addToSelection(selectionIndex);
        } else {
            m_tracker.removeAll(m_tracker);
            for (int i = 0; i < m_Selection.size(); i++) {
                if (selectionIndex == m_Selection.get(i).intValue()) {
                    m_Selection.remove(i);
                    i--;
                } else {
                    List<TrackPoint> tracker = m_doc.m_objects.get(m_Selection.get(i).intValue()).getTracker();
                    m_tracker.addAll(tracker);
                }
            }
        }
        for (int i = 0; i < m_Selection.size(); i++) {
        	Log.warning.print(m_Selection.get(i) + " ");
        }
        Log.warning.println();
    } // toggleSelection

    public void clear() {
        m_Selection.removeAll(m_Selection);
        m_tracker = null;
    } // clear

    boolean intersects(int x, int y) {
        for (int i = 0; i < m_Selection.size(); i++) {
            int shapeIndex = m_Selection.get(i).intValue();
            if (m_doc.m_objects.get(shapeIndex).intersects(x, y)) {
                return true;
            }
        }
        if (m_tracker != null) {
            for (int i = 0; i < m_tracker.size(); i++) {
                TrackPoint p = m_tracker.get(i);
                if (x > p.m_nX - 5 && x < p.m_nX + 5 && y > p.m_nY - 5 && y < p.m_nY + 5) {
                    return true;
                }
            }
        }
        return false;
    } // intersects

    void offset(int dX, int dY) {
        for (int i = 0; i < m_tracker.size(); i++) {
            TrackPoint p = m_tracker.get(i);
            p.m_nX += dX;
            p.m_nY += dY;
        }
        for (int i = 0; i < m_Selection.size(); i++) {
            int shapeIndex = m_Selection.get(i).intValue();
            Shape shape = m_doc.m_objects.get(shapeIndex);
            shape.offset(dX, dY);
        }
    }

    public void refreshTracker() {
        if (m_tracker == null) {
            m_tracker = new ArrayList<>();
        }
        m_tracker.removeAll(m_tracker);
        for (int i = 0; i < m_Selection.size(); i++) {
            int selectionIndex = m_Selection.get(i).intValue();
            List<TrackPoint> tracker = m_doc.m_objects.get(selectionIndex).getTracker();
            m_tracker.addAll(tracker);
        }
    }

    void setSelection(int[] selection) {
        m_Selection.removeAll(m_Selection);
        for (int i = 0; i < selection.length; i++) {
            m_Selection.add(new Integer(i));
        }

    }
} // class Selection
