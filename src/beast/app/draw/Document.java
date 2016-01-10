/*
* File Document.java
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



import java.awt.Color;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.util.Log;
import beast.util.AddOnManager;
import beast.util.XMLParser;
import beast.util.XMLProducer;

/**
 * The Document class is the Document part in the doc-view pattern of
 * the Beast ModelBuilder application.
 */
public class Document {

    /**
     * list of PluginShapes, InputShapes and connecting Arrows *
     */
    public List<Shape> m_objects = new ArrayList<>();
    public List<Shape> m_tmpobjects;

    /**
     * undo/redo related stuff *
     */
    List<UndoAction> m_undoStack = new ArrayList<>();
    int m_nCurrentEditAction = -1;


    //int m_nSavedPointer = -1;
    public boolean m_bIsSaved = true;
    
    Set<String> tabulist;

    public void isSaved() {
        m_bIsSaved = true;
    }

    void sChanged() {
        m_bIsSaved = false;
    }

    /**
     * list of class names for plug-ins to choose from *
     */
    String[] m_sPlugInNames;

    // if false, only non-null and non-default valued inputs are shown
    // Also inputs from the tabu list will be eliminated
    boolean m_bShowALlInputs = false;
    boolean showAllInputs() {
    	return m_bShowALlInputs;
    }
    boolean m_bSanitiseIDs = true;
    boolean sanitiseIDs() {
    	return m_bSanitiseIDs;
    }

    public Document() {
        // load all parsers
        List<String> plugInNames = AddOnManager.find(beast.core.BEASTInterface.class, AddOnManager.IMPLEMENTATION_DIR);
        m_sPlugInNames = plugInNames.toArray(new String[0]);
        tabulist = new HashSet<>();
        Properties properties = new Properties();
        try {
        	
        	properties.load(getClass().getResourceAsStream("/beast/app/draw/tabulist.properties")); 
            String list = properties.getProperty("tabulist");
            for (String str  : list.split("\\s+")) {
            	tabulist.add(str.trim());
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }


    } // c'tor

//    /** change order of shapes to ensure arrows are drawn before the rest **/
//    void moveArrowsToBack() {
//        ArrayList<Shape> arrows = new ArrayList<>();
//        List<Shape> others = new ArrayList<>();
//
//        for (Shape shape : m_objects) {
//            if (shape instanceof Arrow) {
//                arrows.add(shape);
//            } else {
//                others.add(shape);
//            }
//        }
//        arrows.addAll(others);
//        m_objects = arrows;
//    }

    /**
     * adjust position of inputs to fit with the associated plug in *
     */
    void adjustInputs() {
        for (Shape shape : m_objects) {
            if (shape instanceof BEASTObjectShape) {
                ((BEASTObjectShape) shape).adjustInputs();
            }
        }
    }

    /**
     * adjust position of tail and head of arrows to the
     * Plug in and input shapes they are attached to
     * *
     */
    void adjustArrows() {
//        adjustArrows(m_objects);
//    }
//
//    void adjustArrows(List<Shape> objects) {
        for (Shape shape : m_objects) {
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                arrow.adjustCoordinates();
            }
        }
    } // adjustArrows

    void readjustArrows(List<Shape> objects) {
        for (Shape shape : m_objects) {
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                arrow.m_sHeadID = arrow.m_headShape.getID();
                ;
                arrow.m_sTailID = arrow.m_tailShape.getID();
                ;
            }
        }
    } // readjustArrows

    /**
     * edit actions on shapes *
     */
    public void moveShape(int x, int y, int toX, int toY, int position) {
        boolean needsUndoAction = true;
        if (m_nCurrentEditAction == m_undoStack.size() - 1 && m_nCurrentEditAction >= 0) {
            UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
            if (undoAction.m_nActionType == UndoAction.MOVE_ACTION && undoAction.isSingleSelection(position)) {
                needsUndoAction = false;
            }
        }
        if (needsUndoAction) {
            addUndoAction(new UndoAction(position, UndoAction.MOVE_ACTION));
        }
        Shape shape = m_objects.get(position);
        shape.movePosition(x, y, toX, toY);
        adjustArrows();
    } // moveShape

    public void moveShapes(int dX, int dY, List<Integer> positions) {
        boolean needsUndoAction = true;
        if (m_nCurrentEditAction == m_undoStack.size() - 1 && m_nCurrentEditAction >= 0) {
            UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
            if (undoAction.m_nActionType == UndoAction.MOVE_ACTION && undoAction.isSelection(positions)) {
                needsUndoAction = false;
            }
        }
        if (needsUndoAction) {
            addUndoAction(new UndoAction(positions, UndoAction.MOVE_ACTION));
        }
    } // moveShape

    public void movePoint(int point, int x, int y, int toX, int toY, int position) {
        boolean needsUndoAction = true;
        if (m_nCurrentEditAction == m_undoStack.size() - 1 && m_nCurrentEditAction >= 0) {
            UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
            if (undoAction.m_nActionType == UndoAction.RESHAPE_ACTION && undoAction.isSingleSelection(position)) {
                needsUndoAction = false;
            }
        }
        if (needsUndoAction) {
            addUndoAction(new UndoAction(position, UndoAction.RESHAPE_ACTION));
        }
        Shape shape = m_objects.get(position);
        shape.movePoint(point, x, y, toX, toY);
        adjustArrows();
    } // movePoint

    boolean containsID(String id, List<Shape> objects, List<String> tabulist) {
        for (Shape shape : m_objects) {
            if (shape.getID().equals(id)) {
                return true;
            }
//            if (shape instanceof Group) {
//                Group group = (Group) shape;
//                if (containsID(id, group.m_objects, tabulist)) {
//                    return true;
//                }
//            }
        }
        if (tabulist == null) {
            return false;
        }
        for (String tabuID : tabulist) {
            if (tabuID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    String getNewID(List<String> tabulist) {
        int _id = m_objects.size();
        String id = "id" + _id;
        while (containsID(id, m_objects, tabulist)) {
            _id++;
            id = "id" + _id;
        }
        return id;
    }

    void setPluginID(BEASTObjectShape shape) {
        if (shape.m_beastObject.getID() != null && shape.m_beastObject.getID().length() > 0) {
            return;
        }
        BEASTInterface beastObject = shape.m_beastObject;
        String base = beastObject.getClass().getName().replaceAll(".*\\.", "");
        int _id = 0;
        while (containsID(base + _id, m_objects, null)) {
            _id++;
        }
        beastObject.setID(base + _id);
    }

    Shape getShapeByID(String id) {
        for (Shape shape : m_objects) {
            if (shape.getID().equals(id)) {
                return shape;
            }
        }
        return null;
    }

    public void addNewShape(Shape shape) {
        if (shape.getID() == null ||
                shape.getID().equals("") ||
                containsID(shape.getID(), m_objects, null)) {
            if (shape instanceof Arrow) {
                ((Arrow) shape).setID(getNewID(null));
            }
            if (shape instanceof BEASTObjectShape) {
                setPluginID((BEASTObjectShape) shape);
            }
        }
        m_objects.add(shape);
        if (shape instanceof BEASTObjectShape) {
            List<Integer> objects = new ArrayList<>();
            objects.add(m_objects.size() - 1);
            checkForOtherPluginShapes(objects, (BEASTObjectShape) shape);
            if (objects.size() == 1) {
                addUndoAction(new PluginAction(m_objects.size() - 1, UndoAction.ADD_PLUGIN_ACTION));
            } else {
                addUndoAction(new MultiObjectAction(objects, UndoAction.ADD_GROUP_ACTION));
            }
        } else if (shape instanceof Arrow) {
            addUndoAction(new ArrowAction(m_objects.size() - 1, UndoAction.ADD_ARROW_ACTION));
        }
    } // addNewShape

    void checkForOtherPluginShapes(List<Integer> objects, BEASTObjectShape shape) {
        // check whether we need to create any input beastObjects
        try {
            List<Input<?>> inputs = shape.m_beastObject.listInputs();
            for (Input<?> input : inputs) {
                if (input.get() instanceof BEASTInterface) {
                    BEASTInterface beastObject = (BEASTInterface) input.get();
                    BEASTObjectShape beastObjectShape = new BEASTObjectShape(beastObject, this);
                    beastObjectShape.m_x = Math.max(shape.m_x - DX, 0);
                    beastObjectShape.m_y = shape.m_y;
                    beastObjectShape.m_w = 100;
                    beastObjectShape.m_h = 80;
                    setPluginID(beastObjectShape);
                    m_objects.add(beastObjectShape);
                    objects.add(m_objects.size() - 1);
                    Arrow arrow = new Arrow(beastObjectShape, shape, input.getName());
                    m_objects.add(arrow);
                    objects.add(m_objects.size() - 1);
                    // recurse
                    checkForOtherPluginShapes(objects, beastObjectShape);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<Integer> getConnectedArrows(List<String> ids, List<Integer> selection) {
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = m_objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                for (int j = 0; j < ids.size(); j++) {
                    if (arrow.m_sHeadID.startsWith(ids.get(j)) || arrow.m_sTailID.equals(ids.get(j))) {
                        if (!selection.contains(new Integer(i))) {
                            selection.add(new Integer(i));
                        }
                    }
                }
            }
        }
        return selection;
    }

    List<String> getIncomingArrows(List<String> ids) {
        List<String> selection = new ArrayList<>();
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = m_objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                for (int j = 0; j < ids.size(); j++) {
                    if (arrow.m_sHeadID.equals(ids.get(j))) {
                        if (!selection.contains(arrow.m_sTailID)) {
                            selection.add(arrow.m_sTailID);
                        }
                    }
                }
            }
        }
        return selection;
    }

    List<String> getOutgoingArrows(List<String> ids) {
        List<String> selection = new ArrayList<>();
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = m_objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                for (int j = 0; j < ids.size(); j++) {
                    if (arrow.m_sTailID.equals(ids.get(j))) {
                        if (!selection.contains(arrow.m_sTailID)) {
                            selection.add(arrow.m_sTailID);
                        }
                    }
                }
            }
        }
        return selection;
    }

    public void deleteShapes(List<Integer> selection) {
        List<String> ids = new ArrayList<>();
        for (int j = 0; j < selection.size(); j++) {
            ids.add(m_objects.get(selection.get(j).intValue()).getID());

        }
        selection = getConnectedArrows(ids, selection);
        UndoAction action = new MultiObjectAction(selection, UndoAction.DEL_GROUP_ACTION);
        addUndoAction(action);
        action.redo();
    } // deleteShape

    void ensureUniqueID(Shape shape, List<String> tabulist) {
        if (shape.getID() == null ||
                shape.getID().equals("") ||
                containsID(shape.getID(), m_objects, tabulist)) {
            if (shape instanceof Arrow) {
                ((Arrow) shape).setID(getNewID(tabulist));
            }
            if (shape instanceof BEASTObjectShape) {
                setPluginID((BEASTObjectShape) shape);
            }
        }
        tabulist.add(shape.getID());
    } // ensureUniqueID

    public void pasteShape(String xml) {
        List<Shape> shapes = XML2Shapes(xml, true);
        if (shapes.size() == 0) {
            return;
        }
        List<Integer> positions = new ArrayList<>();
        for (Shape shape : shapes) {
            if (shape instanceof Arrow) {
                ((Arrow) shape).setID(getNewID(null));
            }
            if (shape instanceof BEASTObjectShape) {
                ((BEASTObjectShape) shape).m_beastObject.setID(null);
                setPluginID((BEASTObjectShape) shape);
                // ensure the new shape does not overlap exactly with an existing shape
                int offset = 0;
                boolean isMatch = false;
                do {
                    isMatch = false;
                    for (Shape shape2 : m_objects) {
                        if (shape2.m_x == shape.m_x + offset && shape2.m_y == shape.m_y + offset &&
                                shape2.m_w == shape.m_w && shape2.m_h == shape.m_h) {
                            isMatch = true;
                            offset += 10;
                        }
                    }
                } while (isMatch);
                shape.m_x += offset;
                shape.m_y += offset;
            }
            m_objects.add(shape);
            positions.add(m_objects.size() - 1);
        }
        addUndoAction(new MultiObjectAction(positions, UndoAction.ADD_GROUP_ACTION));
    } // pasteShape

    /**
     * move all plug ins connected with selection *
     */
    public void collapse(Selection selection) {
//        // don't group arrows
//        for (int i = selection.m_Selection.size() - 1; i >= 0; i--) {
//            if ((Shape) m_objects.get(((Integer) selection.m_Selection.get(i)).intValue()) instanceof Arrow) {
//                selection.m_Selection.remove(i);
//            }
//        }
//        int nrOfPrimePositions = selection.m_Selection.size();
//        if (nrOfPrimePositions == 0) {
//            return;
//        }
//        for (int i = 0; i < nrOfPrimePositions; i++) {
//        	Shape shape = m_objects.get(((Integer) selection.m_Selection.get(i)).intValue());
//        	findAffectedShapes(shape, selection.m_Selection);
//        }
//        if (selection.m_Selection.size() == nrOfPrimePositions) {
//        	// nothing to collapse
//        	return;
//        }
//        
//        UndoAction action = new UndoMultiSelectionAction(selection.m_Selection, nrOfPrimePositions);
//        addUndoAction(action);
//        action.redo();
//        selection.clear();
//        selection.m_Selection.add(new Integer(m_objects.size() - 1));
//        adjustInputs();
//        adjustArrows();
    } // collapse


    /**
     * Find inputs to collapse, i.e. PlugInShapes connected to
     * any input of a given PluginShape.
     * Shape IDs are recorded in selection. *
     */
    void findAffectedShapes(Shape shape, List<Integer> selection) {
        if (shape instanceof InputShape) {
            findInputs((InputShape) shape, selection);
        } else {
            for (InputShape ellipse : ((BEASTObjectShape) shape).m_inputs) {
                findInputs(ellipse, selection);
            }
        }
    }

    /**
     * Find inputs to collapse, i.e. PlugInShapes connected to
     * given InputShape. Shape IDs are recorded in selection. *
     */
    void findInputs(InputShape ellipse, List<Integer> selection) {
        for (Shape shape : m_objects) {
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                if (arrow.m_sHeadID.equals(ellipse.getID())) {
                    String tailID = arrow.m_sTailID;
                    for (int i = 0; i < m_objects.size(); i++) {
                        if (m_objects.get(i).getID().equals(tailID)) {
                            selection.add(i);
                        }
                    }
                }
            }
        }
    }

//    public void ungroup(Selection selection) {
//        UngroupAction action = new UngroupAction(selection);
//        addUndoAction(action);
//        action.redo();
//        int size = action.getGroupSize();
//        selection.clear();
//        for (int i = 0; i < size; i++) {
//            selection.m_Selection.add(new Integer(m_objects.size() - i - 1));
//        }
//    } // ungroup

    public void setFillColor(Color color, Selection selection) {
        if (selection.isSingleSelection()) {
            addUndoAction(new UndoAction(selection.getSingleSelection(), UndoAction.FILL_COLOR_ACTION));
        } else {
            addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        }
        for (int i = 0; i < selection.m_Selection.size(); i++) {
            int selectionIndex = selection.m_Selection.get(i).intValue();
            Shape shape = m_objects.get(selectionIndex);
            shape.setFillColor(color);
        }
    } // setFillColor

    public void setPenColor(Color color, Selection selection) {
        if (selection.isSingleSelection()) {
            addUndoAction(new UndoAction(selection.getSingleSelection(), UndoAction.PEN_COLOR_ACTION));
        } else {
            addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        }
        for (int i = 0; i < selection.m_Selection.size(); i++) {
            int selectionIndex = selection.m_Selection.get(i).intValue();
            Shape shape = m_objects.get(selectionIndex);
            shape.setPenColor(color);
        }
    } // setPenColor

//    public void addUndoGroupAction(Selection selection) {
//        addUndoAction(new UndoAction(selection.m_Selection));
//    }

    int getPositionX(int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        return shape.getX();
    }

    int getPositionY(int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        return shape.getY();
    }

    int getPositionX2(int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        return shape.getX2();
    }

    int getPositionY2(int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        return shape.getY2();
    }

    void setPositionX(int x, int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        shape.setX(x);
    }

    void setPositionY(int y, int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        shape.setY(y);
    }

    void setPositionX2(int x, int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        shape.setX2(x);
    }

    void setPositionY2(int y, int shapeIndex) {
        Shape shape = m_objects.get(shapeIndex);
        shape.setY2(y);
    }

    public void setID(String id, int object) {
        addUndoAction(new UndoAction(object, UndoAction.SET_LABEL_ACTION));
        Shape shape = m_objects.get(object);
        ((BEASTObjectShape) shape).m_beastObject.setID(id);
    }

    public void toggleFilled(int object) {
        addUndoAction(new UndoAction(object, UndoAction.TOGGLE_FILL_ACTION));
        Shape shape = m_objects.get(object);
        shape.toggleFilled();
    }

    void setInputValue(BEASTObjectShape beastObjectShape, String input, String valueString) throws Exception {
        addUndoAction(new SetInputAction(beastObjectShape, input, valueString));
        //beastObjectShape.m_beastObject.setInputValue(input, valueString);
    }

    /**
     * action representing assignment of value to a primitive input *
     */
    class SetInputAction extends UndoAction {
        BEASTObjectShape m_beastObjectShape;
        String m_sInput;
        String m_sValue;

        SetInputAction(BEASTObjectShape beastObjectShape, String input, String valueString) {
            m_beastObjectShape = beastObjectShape;
            m_sInput = input;
            m_sValue = valueString;
            doit();
        }

        @Override
		void redo() {
            doit();
        }

        @Override
		void undo() {
            doit();
        }

        @Override
		void doit() {
            try {
                String valueString = m_beastObjectShape.m_beastObject.getInput(m_sInput).get().toString();
                m_beastObjectShape.m_beastObject.setInputValue(m_sInput, m_sValue);
                m_sValue = valueString;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } // class SetInputAction

    public void toFront(Selection selection) {
        if (selection.m_Selection.size() == 0) {
            return;
        }
        int[] newOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < newOrder.length; i++) {
            newOrder[i] = m_objects.size() - 1 - i;
        }
        int[] order = new int[selection.m_Selection.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = selection.m_Selection.get(i).intValue();
        }
        Arrays.sort(order);
        int[] oldOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < oldOrder.length; i++) {
            oldOrder[i] = order[oldOrder.length - 1 - i];
        }
        ReorderAction reorderAction = new ReorderAction(oldOrder, newOrder);
        addUndoAction(reorderAction);
        reorderAction.reorder(oldOrder, newOrder);
        selection.setSelection(newOrder);
    } // toFront

    public void toBack(Selection selection) {
        if (selection.m_Selection.size() == 0) {
            return;
        }
        int[] newOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < selection.m_Selection.size(); i++) {
            newOrder[i] = i;
        }
        int[] oldOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < oldOrder.length; i++) {
            oldOrder[i] = selection.m_Selection.get(i).intValue();
        }
        Arrays.sort(oldOrder);
        ReorderAction reorderAction = new ReorderAction(oldOrder, newOrder);
        addUndoAction(reorderAction);
        reorderAction.reorder(oldOrder, newOrder);
        selection.setSelection(newOrder);
    } // toBack

    public void forward(Selection selection) {
        if (selection.m_Selection.size() == 0) {
            return;
        }
        int[] order = new int[selection.m_Selection.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = selection.m_Selection.get(i).intValue();
        }
        Arrays.sort(order);
        int[] oldOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < oldOrder.length; i++) {
            oldOrder[i] = order[oldOrder.length - 1 - i];
        }
        int[] newOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < newOrder.length; i++) {
            if (oldOrder[i] < m_objects.size() - 1) {
                newOrder[i] = oldOrder[i] + 1;
            } else {
                newOrder[i] = m_objects.size() - 1;
            }
        }
        ReorderAction reorderAction = new ReorderAction(oldOrder, newOrder);
        addUndoAction(reorderAction);
        reorderAction.reorder(oldOrder, newOrder);
        selection.setSelection(newOrder);
    } // forward

    public void backward(Selection selection) {
        if (selection.m_Selection.size() == 0) {
            return;
        }
        int[] oldOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < oldOrder.length; i++) {
            oldOrder[i] = selection.m_Selection.get(i).intValue();
        }
        Arrays.sort(oldOrder);
        int[] newOrder = new int[selection.m_Selection.size()];
        for (int i = 0; i < selection.m_Selection.size(); i++) {
            if (oldOrder[i] > 0) {
                newOrder[i] = oldOrder[i] - 1;
            }
        }
        ReorderAction reorderAction = new ReorderAction(oldOrder, newOrder);
        addUndoAction(reorderAction);
        reorderAction.reorder(oldOrder, newOrder);
        selection.setSelection(newOrder);
    } // backward

    /**
     * align set of nodes with the left most node in the list
     *
     * @param selection a selection
     */
    public void alignLeft(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));

        List<Integer> nodes = selection.m_Selection;
        int minX = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int x = getPositionX(nodes.get(nodeIndex).intValue());
            if (x < minX || nodeIndex == 0) {
                minX = x;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            setPositionX(minX, node);
        }
        adjustArrows();
    } // alignLeft

    /**
     * align set of nodes with the right most node in the list
     *
     * @param selection a selection
     */
    public void alignRight(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int maxX = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int x = getPositionX2(nodes.get(nodeIndex).intValue());
            if (x > maxX || nodeIndex == 0) {
                maxX = x;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            int dX = getPositionX2(node) - getPositionX(node);
            setPositionX(maxX - dX, node);
        }
        adjustArrows();
    } // alignRight

    /**
     * align set of nodes with the top most node in the list
     *
     * @param selection a selection
     */
    public void alignTop(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int minY = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int y = getPositionY(nodes.get(nodeIndex).intValue());
            if (y < minY || nodeIndex == 0) {
                minY = y;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            setPositionY(minY, node);
        }
        adjustArrows();
    } // alignTop

    /**
     * align set of nodes with the bottom most node in the list
     *
     * @param selection a selection
     */
    public void alignBottom(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int maxY = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int y = getPositionY2(nodes.get(nodeIndex).intValue());
            if (y > maxY || nodeIndex == 0) {
                maxY = y;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            int dY = getPositionY2(node) - getPositionY(node);
            setPositionY(maxY - dY, node);
        }
        adjustArrows();
    } // alignBottom

    /**
     * centre set of nodes half way between left and right most node in the list
     *
     * @param selection a selection
     */
    public void centreHorizontal(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int minY = -1;
        int maxY = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int y = (getPositionY(nodes.get(nodeIndex).intValue()) +
                    getPositionY2(nodes.get(nodeIndex).intValue())) / 2;
            if (y < minY || nodeIndex == 0) {
                minY = y;
            }
            if (y > maxY || nodeIndex == 0) {
                maxY = y;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            int dY = (getPositionY2(node) - getPositionY(node)) / 2;
            setPositionY((minY + maxY) / 2 - dY, node);
        }
        adjustArrows();
    } // centreHorizontal

    /**
     * centre set of nodes half way between top and bottom most node in the list
     *
     * @param selection a selection
     */
    public void centreVertical(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int minX = -1;
        int maxX = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int x = (getPositionX(nodes.get(nodeIndex).intValue()) +
                    getPositionX2(nodes.get(nodeIndex).intValue())) / 2;
            if (x < minX || nodeIndex == 0) {
                minX = x;
            }
            if (x > maxX || nodeIndex == 0) {
                maxX = x;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            int dX = (getPositionX2(node) - getPositionX(node)) / 2;
            setPositionX((minX + maxX) / 2 - dX, node);
        }
        adjustArrows();
    } // centreVertical

    /**
     * space out set of nodes evenly between left and right most node in the list
     *
     * @param selection a selection
     */
    public void spaceHorizontal(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int minX = -1;
        int maxX = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int x = getPositionX(nodes.get(nodeIndex).intValue());
            if (x < minX || nodeIndex == 0) {
                minX = x;
            }
            if (x > maxX || nodeIndex == 0) {
                maxX = x;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            setPositionX((int) (minX + nodeIndex * (maxX - minX) / (nodes.size() - 1.0)), node);
        }
        adjustArrows();
    } // spaceHorizontal

    /**
     * space out set of nodes evenly between top and bottom most node in the list
     *
     * @param selection a selection
     */
    public void spaceVertical(Selection selection) {
        // update undo stack
        addUndoAction(new UndoAction(selection.m_Selection, UndoAction.MOVE_ACTION));
        List<Integer> nodes = selection.m_Selection;
        int minY = -1;
        int maxY = -1;
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int y = getPositionY(nodes.get(nodeIndex).intValue());
            if (y < minY || nodeIndex == 0) {
                minY = y;
            }
            if (y > maxY || nodeIndex == 0) {
                maxY = y;
            }
        }
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            int node = nodes.get(nodeIndex).intValue();
            setPositionY((int) (minY + nodeIndex * (maxY - minY) / (nodes.size() - 1.0)), node);
        }
        adjustArrows();
    } // spaceVertical


    public class UndoAction {
        final static int UNDO_ACTION = 0;
        final static int MOVE_ACTION = 1;
        final static int RESHAPE_ACTION = 2;
        final static int ADD_PLUGIN_ACTION = 3;
        final static int DEL_PLUGIN_ACTION = 4;
        final static int ADD_ARROW_ACTION = 5;
        final static int DEL_ARROW_ACTION = 6;
        final static int FILL_COLOR_ACTION = 5;
        final static int PEN_COLOR_ACTION = 6;
        final static int SET_LABEL_ACTION = 7;
        final static int TOGGLE_FILL_ACTION = 10;
        final static int ADD_GROUP_ACTION = 20;
        final static int DEL_GROUP_ACTION = 21;

        int m_nActionType = UNDO_ACTION;

        String m_sXML;
        List<Integer> m_nPositions;

        /* single selection undo actions **/

        public UndoAction(int selection, int actionType) {
            if (!(m_objects.get(selection) instanceof BEASTObjectShape)) {
                return;
            }
            m_nActionType = actionType;
            m_nPositions = new ArrayList<>();
            m_nPositions.add(selection);
            init();
        }
        /* multiple selection undo actions **/

        public UndoAction(List<Integer> selection, int actionType) {
            m_nActionType = actionType;
            m_nPositions = new ArrayList<>();
            for (int i = 0; i < selection.size(); i++) {
                if (m_objects.get(selection.get(i)) instanceof BEASTObjectShape) {
                    m_nPositions.add(new Integer(selection.get(i).intValue()));
                }
            }
            init();
        }

        /* undo actions that don't need a selection **/

        public UndoAction() {
        }

        boolean isSingleSelection(int position) {
            return (m_nPositions.size() == 1 && m_nPositions.get(0) == position);
        }

        boolean isSelection(List<Integer> positions) {
            int matches = 0;
            for (Integer i : positions) {
                if (m_objects.get(i) instanceof BEASTObjectShape) {
                    if (m_nPositions.contains(i)) {
                        matches++;
                    } else {
                        return false;
                    }
                }
            }
            return matches == m_nPositions.size();
        }

        void init() {
            m_sXML = "<doc>";
            for (int i = 0; i < m_nPositions.size(); i++) {
                int shapeIndex = m_nPositions.get(i).intValue();
                Shape shape = m_objects.get(shapeIndex);
                m_sXML += shape.getXML();
            }
            m_sXML += "</doc>";
        }

        void undo() {
            doit();
        }

        void redo() {
            doit();
        }

        void doit() {
            String xml = "<doc>";
            for (int i = 0; i < m_nPositions.size(); i++) {
                int shapeIndex = m_nPositions.get(i).intValue();
                Shape shape = m_objects.get(shapeIndex);
                xml += shape.getXML();
            }
            xml += "</doc>";
            List<Shape> shapes = XML2Shapes(m_sXML, false);
            for (int i = 0; i < m_nPositions.size(); i++) {
                int shapeIndex = m_nPositions.get(i).intValue();
                Shape originalShape = m_objects.get(shapeIndex);
                Shape shape = shapes.get(i);
                ((BEASTObjectShape) shape).m_beastObject = ((BEASTObjectShape) originalShape).m_beastObject;
                originalShape.assignFrom(shape);
            }
            m_sXML = xml;
        }
    } // class UndoAction

    /**
     * action representing addition/deletion of a single beastObject.
     * This does not take connecting arrows in account.
     * Use MultiObjectAction to add/delete beastObject with its connecting arrows.
     */
    class PluginAction extends UndoAction {
        public PluginAction(int position, int actionType) {
            // assumes beastObjectShape + all its inputs has just been added
            m_nActionType = actionType;
            BEASTObjectShape beastObjectShape = (BEASTObjectShape) m_objects.get(position);
            m_nPositions = new ArrayList<>();
            m_nPositions.add(position);
            position--;
            while (position >= 0 &&
                    m_objects.get(position) instanceof InputShape &&
                    ((InputShape) m_objects.get(position)).getPluginShape() == beastObjectShape) {
                m_nPositions.add(0, position--);
            }
            // creat XML
            init();
        }

        @Override
        void undo() {
            switch (m_nActionType) {
                case ADD_PLUGIN_ACTION:
                    removePlugin();
                    return;
                case DEL_PLUGIN_ACTION:
                    addPlugin();
                    return;
            }
            Log.err.println("Error 101: action type not set properly");
        }

        @Override
        void redo() {
            switch (m_nActionType) {
                case ADD_PLUGIN_ACTION:
                    addPlugin();
                    return;
                case DEL_PLUGIN_ACTION:
                    removePlugin();
                    return;
            }
            Log.err.println("Error 102: action type not set properly");
        }

        void removePlugin() {
            for (int i = m_nPositions.size() - 1; i >= 0; i--) {
                m_objects.remove((int) m_nPositions.get(i));
            }
        }

        void addPlugin() {
            List<Shape> shapes = XML2Shapes(m_sXML, true);
            for (int i = 0; i < m_nPositions.size(); i++) {
                m_objects.add(m_nPositions.get(i), shapes.get(i));
            }
        }
    } // class AddPluginAction


    /**
     * action representing addition/deletion of a single arrow.
     */
    class ArrowAction extends UndoAction {
        public ArrowAction(int position, int arrowAction) {
            m_nActionType = arrowAction;
            m_nPositions = new ArrayList<>();
            m_nPositions.add(position);
            init();
        }

        @Override
        void undo() {
            switch (m_nActionType) {
                case ADD_ARROW_ACTION:
                    removeArrow();
                    return;
                case DEL_ARROW_ACTION:
                    addArrow();
                    return;
            }
            Log.err.println("Error 103: action type not set properly");
        }

        @Override
        void redo() {
            switch (m_nActionType) {
                case ADD_ARROW_ACTION:
                    addArrow();
                    return;
                case DEL_ARROW_ACTION:
                    removeArrow();
                    return;
            }
            Log.err.println("Error 104: action type not set properly");
        }

        void removeArrow() {
            Arrow arrow = (Arrow) m_objects.get(m_nPositions.get(0));
            m_objects.remove((int) m_nPositions.get(0));
            // unconnect plug-in and input
            final Input<?> input = arrow.m_headShape.m_input;
            if (input instanceof List<?>) {
                ((List<?>) input.get()).remove(arrow.m_tailShape.m_beastObject);
            } else {
                try {
                    input.setValue(null, arrow.m_headShape.getBEASTObject());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        void addArrow() {
            List<Shape> shapes = XML2Shapes(m_sXML, true);
            Arrow arrow = (Arrow) shapes.get(0);
            m_objects.add(m_nPositions.get(0), arrow);
            // reconnect plug-in with input
            arrow.m_tailShape = getPluginShapeWithLabel(arrow.m_sTailID);
            arrow.m_headShape = getInputShapeWithID(arrow.m_sHeadID);
            try {
                arrow.m_headShape.m_input.setValue(arrow.m_tailShape.m_beastObject, arrow.m_headShape.getBEASTObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } // class ArrowAction

    /**
     * action representing addition or deletion of multiple beastObjects/arrows
     */
    class MultiObjectAction extends UndoAction {
        List<UndoAction> m_actions;

        MultiObjectAction(List<Integer> positions, int actionType) {
            m_nActionType = actionType;
            m_actions = new ArrayList<>();
            // remove duplicates, if any
            Collections.sort(positions, (Integer o1, Integer o2) -> {
                    return (o2 - o1);
                }
            );
            for (int i = 1; i < positions.size(); i++) {
                if (positions.get(i) == positions.get(i - 1)) {
                    positions.remove(i);
                    i--;
                }
            }
            // split in beastObjects and arrows
            List<Integer> arrows = new ArrayList<>();
            List<Integer> pluginsShapes = new ArrayList<>();
            for (int i : positions) {
                Shape shape = m_objects.get(i);
                if (shape instanceof BEASTObjectShape) {
                    pluginsShapes.add(i);
                } else if (shape instanceof Arrow) {
                    arrows.add(i);
                }
            }
            // create appropriate set of undo actions
            switch (actionType) {
                case ADD_GROUP_ACTION:
                    for (int i : pluginsShapes) {
                        m_actions.add(new PluginAction(i, ADD_PLUGIN_ACTION));
                    }
                    for (int i : arrows) {
                        m_actions.add(new ArrowAction(i, ADD_ARROW_ACTION));
                    }
                    break;
                case DEL_GROUP_ACTION:
                    for (int i : arrows) {
                        m_actions.add(new ArrowAction(i, DEL_ARROW_ACTION));
                    }
                    for (int i : pluginsShapes) {
                        m_actions.add(new PluginAction(i, DEL_PLUGIN_ACTION));
                    }
                    break;
                default:
                    Log.err.println("Error 105: unrecognized action type");
            }
        }


        @Override
        void undo() {
            for (int i = m_actions.size() - 1; i >= 0; i--) {
                m_actions.get(i).undo();
            }
        }

        @Override
        void redo() {
            for (int i = 0; i < m_actions.size(); i++) {
                m_actions.get(i).redo();
            }
        }
    } // class MultiObjectAction

    class ReorderAction extends UndoAction {
        int[] m_oldOrder;
        int[] m_newOrder;

        ReorderAction(int[] oldOrder, int[] newOrder) {
            super();
            m_oldOrder = oldOrder;
            m_newOrder = newOrder;
        }

        @Override
		void undo() {
            reorder(m_oldOrder, m_newOrder);
        }

        @Override
		void redo() {
            for (int i = m_newOrder.length - 1; i >= 0; i--) {
                int selectionIndex = m_newOrder[i];
                Shape shape = m_objects.get(selectionIndex);
                m_objects.remove(selectionIndex);
                m_objects.add(m_oldOrder[i], shape);
            }
        }

        void reorder(int[] oldOrder, int[] newOrder) {
            for (int i = 0; i < oldOrder.length; i++) {
                int selectionIndex = oldOrder[i];
                Shape shape = m_objects.get(selectionIndex);
                m_objects.remove(selectionIndex);
                m_objects.add(newOrder[i], shape);
            }
        }
    } // class ReorderAction


    /**
     * add undo action to the undo stack.
     *
     * @param action operation that needs to be added to the undo stack
     */
    void addUndoAction(UndoAction action) {
        int actionIndex = m_undoStack.size() - 1;
        while (actionIndex > m_nCurrentEditAction) {
            m_undoStack.remove(actionIndex--);
        }
        m_undoStack.add(action);
        m_nCurrentEditAction++;
    } // addUndoAction

    /**
     * remove all actions from the undo stack
     */
    public void clearUndoStack() {
        m_undoStack = new ArrayList<>();
        m_nCurrentEditAction = -1;
    } // clearUndoStack

    public boolean canUndo() {
        return m_nCurrentEditAction > -1;
    } // canUndo

    public boolean canRedo() {
        return m_nCurrentEditAction < m_undoStack.size() - 1;
    } // canRedo

    public void undo() {
        if (!canUndo()) {
            return;
        }
        UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
        undoAction.undo();
        adjustInputs();
        recalcArrows();
        adjustArrows();
        m_nCurrentEditAction--;
    } // undo

    public void redo() {
        if (!canRedo()) {
            return;
        }
        m_nCurrentEditAction++;
        UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
        undoAction.redo();
        adjustInputs();
        recalcArrows();
        adjustArrows();
    } // redo


    BEASTObjectShape getPluginShapeWithLabel(String label) {
        for (Shape shape : m_objects) {
            if (shape instanceof BEASTObjectShape) {
                if (shape.getLabel() != null && shape.getLabel().equals(label)) {
                    return (BEASTObjectShape) shape;
                }
            }
        }
        return null;
    }

    InputShape getInputShapeWithID(String label) {
        for (Shape shape : m_objects) {
            if (shape instanceof InputShape) {
                if (shape.getID() != null && shape.getID().equals(label)) {
                    return (InputShape) shape;
                }
            }
        }
        return null;
    }

    final static int DX = 120;
    final static int DY = 80;

    void addInput(BEASTObjectShape shape, Object o2, int depth, String input) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (o2 instanceof BEASTInterface) {
            BEASTObjectShape shape2 = getPluginShapeWithLabel(((BEASTInterface) o2).getID());
            if (shape2 == null) {
                shape2 = new BEASTObjectShape((BEASTInterface) o2, this);
                shape2.m_x = depth * DX;
                shape2.m_w = DY;
                shape2.m_beastObject = (BEASTInterface) o2;
                setPluginID(shape2);
                m_objects.add(shape2);
            }
            process(shape2, depth);
        }
    }

    void process(BEASTObjectShape shape, int depth) throws IllegalArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        BEASTInterface beastObject = shape.m_beastObject;
        List<Input<?>> inputs = beastObject.listInputs();
        for (Input<?> input_ : inputs) {
            Object o = input_.get();
            if (o != null) {
                if (o instanceof List<?>) {
                    for (Object o2 : (List<?>) o) {
                        addInput(shape, o2, depth + 1, input_.getName());
                    }
                } else if (o instanceof BEASTInterface) {
                    addInput(shape, o, depth + 1, input_.getName());
                    // } else {
                    // it is a primitive type
                }
            }
        }
    }


    public void loadFile(String fileName) {
        m_objects.clear();
        XMLParser parser = new XMLParser();
        try {
            //fileName;
            StringBuilder xml = new StringBuilder();
            String NL = System.getProperty("line.separator");
            Scanner scanner = new Scanner(new File(fileName));
            try {
                while (scanner.hasNextLine()) {
                    xml.append(scanner.nextLine() + NL);
                }
            } finally {
                scanner.close();
            }
            BEASTInterface plugin0 = parser.parseBareFragment(xml.toString(), false);
            init(plugin0);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
    }
    
    void reinit() {
    	String xml = toXML();
        m_objects.clear();
        try {
            XMLParser parser = new XMLParser();
            BEASTInterface plugin0 = parser.parseBareFragment(xml, false);
            init(plugin0);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
    }

    public void init(BEASTInterface plugin0) {
        try {
            if (plugin0 instanceof BEASTObjectSet) {
                List<BEASTInterface> set = ((BEASTObjectSet) plugin0).m_plugins.get();
                if (set == null) {
                    return;
                }
                for (BEASTInterface beastObject : set) {
                    BEASTObjectShape shape = new BEASTObjectShape(beastObject, this);
                    shape.m_beastObject = beastObject;
                    setPluginID(shape);
                    m_objects.add(shape);
                    process(shape, 1);
                    shape.m_x = DX;
                    shape.m_w = 100;
                    Random random = new Random();
                    shape.m_y = random.nextInt(800);
                    shape.m_h = 50;
                }
            } else {
                BEASTObjectShape shape = new BEASTObjectShape(plugin0, this);
                shape.m_beastObject = plugin0;
                setPluginID(shape);
                m_objects.add(shape);
                process(shape, 1);
                shape.m_x = DX;
                shape.m_w = 100;
                Random random = new Random();
                shape.m_y = random.nextInt(800);
                shape.m_h = 50;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
        // clear undo stack after all items have been
        // added since the stack is affected by parsing...
        clearUndoStack();

        recalcArrows();
        layout();
        adjustArrows();
    } // init

    List<Shape> XML2Shapes(String xml, boolean reconstructBEASTObjects) {
        List<Shape> shapes = new ArrayList<>();
        m_tmpobjects = shapes;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml)));
            doc.normalize();
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int nodeIndex = 0; nodeIndex < nodes.getLength(); nodeIndex++) {
                Node node = nodes.item(nodeIndex);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    shapes.add(parseNode(node, this, reconstructBEASTObjects));
                }
            }
        } catch (Throwable t) {
        }
        m_tmpobjects = null;
        return shapes;
    }

    final static String PLUGIN_SHAPE_ELEMENT = "pluginshape";
    final static String INPUT_SHAPE_ELEMENT = "inputshape";
    final static String ARROW_ELEMENT = "arrow";

    /**
     * parse XDL xml format *
     */
    static Shape parseNode(Node node, Document doc, boolean reconstructBEASTObjects) {
        Shape shape = null;
        if (node.getNodeName().equals(INPUT_SHAPE_ELEMENT) && reconstructBEASTObjects) {
            shape = new InputShape(node, doc, reconstructBEASTObjects);
        } else if (node.getNodeName().equals(ARROW_ELEMENT) && reconstructBEASTObjects) {
            shape = new Arrow(node, doc, reconstructBEASTObjects);
        } else if (node.getNodeName().equals(PLUGIN_SHAPE_ELEMENT)) {
            shape = new BEASTObjectShape(node, doc, reconstructBEASTObjects);
        }
        return shape;
    } // parseNode

    public String toXML() {
        XMLProducer xmlProducer = new XMLProducer();
        BEASTObjectSet pluginSet = calcPluginSet();
        if (pluginSet.m_plugins.get().size() == 1) {
            return xmlProducer.toXML(pluginSet.m_plugins.get().get(0));
        }
        return xmlProducer.toXML(pluginSet);
    } // toXML


    /**
     * collect all objects and put all top-level beastObjects in a PluginSet
     */
    BEASTObjectSet calcPluginSet() {
        // collect all plug-ins
        Collection<BEASTInterface> beastObjects = getPlugins();
        // calc outputs
        HashMap<BEASTInterface, List<BEASTInterface>> outputs = BEASTObjectPanel.getOutputs(beastObjects);
        // put all beastObjects with no ouputs in the PluginSet
        BEASTObjectSet pluginSet = new BEASTObjectSet();
        for (BEASTInterface beastObject : outputs.keySet()) {
            if (outputs.get(beastObject).size() == 0) {
                try {
                    pluginSet.setInputValue("beastObject", beastObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return pluginSet;
    } // calcPluginSet

    /**
     * convert m_objects in set of beastObjects *
     */
    Collection<BEASTInterface> getPlugins() {
        Collection<BEASTInterface> beastObjects = new HashSet<>();
        for (Shape shape : m_objects) {
            if (shape instanceof BEASTObjectShape) {
                beastObjects.add(((BEASTObjectShape) shape).m_beastObject);
            }
        }
        return beastObjects;
    }

    /**
     * return true if source is ascendant of target *
     */
    boolean isAscendant(BEASTInterface source, BEASTInterface target) {
        Collection<BEASTInterface> beastObjects = getPlugins();
        List<BEASTInterface> ascendants = BEASTObjectPanel.listAscendants(target, beastObjects);
        return ascendants.contains(source);
    }

    Shape findObjectWithID(String id) {
        if (m_tmpobjects != null) {
            for (int i = 0; i < m_tmpobjects.size(); i++) {
                if (m_tmpobjects.get(i).getID().equals(id)) {
                    return m_tmpobjects.get(i);
                }
            }
        }
        for (int i = 0; i < m_objects.size(); i++) {
            if (m_objects.get(i).getID().equals(id)) {
                return m_objects.get(i);
            }
        }
        return null;
    }


    /**
     * simple layout algorithm for placing PluginShapes *
     */
    void layout() {
        // first construct input map for ease of navigation
        HashMap<BEASTObjectShape, List<BEASTObjectShape>> inputMap = new HashMap<>();
        HashMap<BEASTObjectShape, List<BEASTObjectShape>> outputMap = new HashMap<>();
        for (Shape shape : m_objects) {
            if (shape instanceof BEASTObjectShape && shape.m_bNeedsDrawing) {
                inputMap.put((BEASTObjectShape) shape, new ArrayList<>());
                outputMap.put((BEASTObjectShape) shape, new ArrayList<>());
            }
        }
        for (Shape shape : m_objects) {
            if (shape instanceof Arrow && shape.m_bNeedsDrawing) {
                Shape headShape = ((Arrow) shape).m_headShape;
                BEASTObjectShape beastObjectShape = ((InputShape) headShape).m_beastObjectShape;
                BEASTObjectShape inputShape = ((Arrow) shape).m_tailShape;
                inputMap.get(beastObjectShape).add(inputShape);
                outputMap.get(inputShape).add(beastObjectShape);
            }
        }
        // reset all x-coords to minimal x-value
        for (Shape shape : inputMap.keySet()) {
            shape.m_x = DX;
        }
        // move inputs rightward till they exceed x-coord of their inputs
        boolean progress = true;
        while (progress) {
            progress = false;
            for (Shape shape : inputMap.keySet()) {
                int maxInputX = -DX;
                for (Shape input : inputMap.get(shape)) {
                    maxInputX = Math.max(maxInputX, input.m_x);
                }
                if (shape.m_x < maxInputX + DX) {
                    shape.m_x = maxInputX + DX;
                    progress = true;
                }
            }
        }
        // move inputs rightward till they are stopped by their outputs
        progress = true;
        while (progress) {
            progress = false;
            for (Shape shape : outputMap.keySet()) {
                int minOutputX = Integer.MAX_VALUE;
                for (Shape input : outputMap.get(shape)) {
                    minOutputX = Math.min(minOutputX, input.m_x);
                }
                if (minOutputX < Integer.MAX_VALUE && shape.m_x < minOutputX - DX) {
                    shape.m_x = minOutputX - DX;
                    progress = true;
                }
            }
        }


        layoutAdjustY(inputMap);
        // relax a bit
        Log.warning.print("Relax...");
        for (int i = 0; i < 250; i++) {
            relax(false);
        }
        Log.warning.println("Done");
        layoutAdjustY(inputMap);

        adjustInputs();
    }

    /**
     * Adjust y-coordinate of PluginShapes so they don't overlap
     * and are close to their inputs. Helper method for layout() *
     */
    void layoutAdjustY(HashMap<BEASTObjectShape, List<BEASTObjectShape>> inputMap) {
        // next, optimise top down order
        boolean progress = true;
        int x = DX;
        while (progress) {
            List<BEASTObjectShape> shapes = new ArrayList<>();
            // find shapes with same x-coordinate
            for (BEASTObjectShape shape : inputMap.keySet()) {
                if (shape.m_x == x) {
                    shapes.add(shape);
                }
            }
            int k = 1;
            HashMap<Integer, BEASTObjectShape> ycoordMap = new HashMap<>();
            // set y-coordinate as mean of inputs
            // if there are no inputs, order them top to bottom at DY intervals
            for (BEASTObjectShape shape : shapes) {
                List<BEASTObjectShape> inputs = inputMap.get(shape);
                if (inputs.size() == 0) {
                    shape.m_y = k * DY;
                } else {
                    shape.m_y = 0;
                    for (Shape input : inputs) {
                        shape.m_y += input.m_y;
                    }
                    shape.m_y /= inputs.size();
                }
                while (ycoordMap.containsKey(shape.m_y)) {
                    shape.m_y++;
                }
                ycoordMap.put(shape.m_y, shape);
                k++;
            }
            // ensure shapes are sufficiently far apart - at least DY between them
            int prevY = 0;
            ArrayList<Integer> yCoords = new ArrayList<>();
            yCoords.addAll(ycoordMap.keySet());
            Collections.sort(yCoords);
            int dY = 0;
            for (Integer i : yCoords) {
                BEASTObjectShape shape = ycoordMap.get(i);
                if (shape.m_y < prevY + DY) {
                    dY = prevY + DY - shape.m_y;
                    shape.m_y = prevY + DY;
                }
                prevY = shape.m_y;
            }
            // upwards correction
            if (dY > 0) {
                dY /= shapes.size();
                for (BEASTObjectShape shape : shapes) {
                    shape.m_y -= dY;
                }
            }


            progress = (shapes.size() > 0);
            x += DX;
        }
    } // layoutAdjustY

    /**
     * apply spring model algorithm to the placement of plug-in shapes *
     */
    public void relax(boolean allowXToMove) {
        List<Shape> objects = new ArrayList<>();
        for (Shape shape : m_objects) {
            if (shape.m_bNeedsDrawing) {
                objects.add(shape);
            }
        }

        // Step 0: determine degrees
        HashMap<String, Integer> degreeMap = new HashMap<>();
        for (Shape shape : objects) {

            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                String id = arrow.m_tailShape.getID();
                if (arrow.m_headShape instanceof InputShape) {
                    String id2 = arrow.m_headShape.m_beastObjectShape.getID();
                    if (degreeMap.containsKey(id)) {
                        degreeMap.put(id, degreeMap.get(id) + 1);
                    } else {
                        degreeMap.put(id, 1);
                    }
                    if (degreeMap.containsKey(id2)) {
                        degreeMap.put(id2, degreeMap.get(id2) + 1);
                    } else {
                        degreeMap.put(id2, 1);
                    }
                }
            }
        }

        // Step 1: relax
        for (Shape shape : objects) {
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                Shape source = arrow.m_tailShape;
                int p1x = source.m_x + source.m_w / 2;
                int p1y = source.m_y + source.m_h / 2;
                if (arrow.m_headShape instanceof InputShape) {
                    Shape target = arrow.m_headShape.m_beastObjectShape;
                    int p2x = target.m_x + target.m_w / 2;
                    int p2y = target.m_y + target.m_h / 2;

                    double vx = p1x - p2x;
                    double vy = p1y - p2y;
                    double len = Math.sqrt(vx * vx + vy * vy);

                    double desiredLen = 150;

                    // round from zero, if needed [zero would be Bad.].
                    len = (len == 0) ? .0001 : len;

                    double f = 1.0 / 3.0 * (desiredLen - len) / len;

                    int degree1 = degreeMap.get(source.getID());
                    int degree2 = degreeMap.get(target.getID());


                    f = f * Math.pow(0.99, (degree1 + degree2 - 2));


                    // the actual movement distance 'dx' is the force multiplied by the
                    // distance to go.
                    double dx = Math.min(f * vx, 3);
                    double dy = Math.min(f * vy, 3);
                    if (vx > -200 && vx < 0) {
                        dx = -dx;
                        //f *= Math.abs((vx+200))/40;
                    }
                    if (allowXToMove) source.m_x = (int) Math.max(100, source.m_x + dx);
                    source.m_y = (int) Math.max(10, source.m_y + dy);
                    if (allowXToMove) target.m_x = (int) Math.max(100, target.m_x - dx);
                    target.m_y = (int) Math.max(10, target.m_y - dy);

                }
            }

        }
        // Step 2: repulse (pairwise)
        for (Shape shape1 : objects) {
            if (shape1 instanceof BEASTObjectShape) {
                int p1x = shape1.m_x + shape1.m_w / 2;
                int p1y = shape1.m_y + shape1.m_h / 2;
                double dx = 0, dy = 0;
                for (Shape shape2 : objects) {
                    if (shape2 instanceof BEASTObjectShape) {
                        int p2x = shape2.m_x + shape2.m_w / 2;
                        int p2y = shape2.m_y + shape2.m_h / 2;
                        double vx = p1x - p2x;
                        double vy = p1y - p2y;
                        double distanceSq = Math.sqrt(vx * vx + vy * vy);
                        if (distanceSq == 0) {
                            dx += Math.random() * 1;
                            dy += Math.random() * 5;
                        } else if (distanceSq < 300 /*repulsion_range_sq*/) {
                            double factor = 500;
                            dx += factor * vx / distanceSq / distanceSq;
                            dy += factor * vy / distanceSq / distanceSq;
                        }
                    }
                }
                if (allowXToMove) shape1.m_x = (int) Math.min(800, Math.max(10, shape1.m_x + dx));
                shape1.m_y = (int) Math.min(800, Math.max(10, shape1.m_y + dy));
            }
        }

        // Step 3: move dependent objects in place
        for (Shape shape : objects) {
            if (shape instanceof BEASTObjectShape) {
                ((BEASTObjectShape) shape).adjustInputs();
            }
        }
        adjustArrows();
    } // relax


    /**
     * sanity check: make sure there are no cycles and there is a Runnable
     * plug in that is not an input of another plug in
     */
    final static int STATUS_OK = 0, STATUS_CYCLE = 1, STATUS_NOT_RUNNABLE = 2,
            STATUS_EMPTY_MODEL = 3, STATUS_ORPHANS_IN_MODEL = 4;

    int isValidModel() {
        BEASTObjectSet pluginSet = calcPluginSet();
        if (pluginSet.m_plugins.get().size() == 0) {
            return STATUS_EMPTY_MODEL;
        }
        if (pluginSet.m_plugins.get().size() > 1) {
            return STATUS_ORPHANS_IN_MODEL;
        }
        boolean hasRunable = false;
        for (BEASTInterface beastObject : pluginSet.m_plugins.get()) {
            if (beastObject instanceof Runnable) {
                hasRunable = true;
            }
        }
        if (!hasRunable) {
            return STATUS_NOT_RUNNABLE;
        }
        return STATUS_OK;
    } // isValidModel

    /**
     * remove all arrows, then add based on the beastObject inputs *
     */
    void recalcArrows() {
        // remove all arrows
        for (int i = m_objects.size() - 1; i >= 0; i--) {
            Shape shape = m_objects.get(i);
            if (shape instanceof Arrow) {
                m_objects.remove(i);
            }
        }
        // build map for quick resolution of PluginShapes
        HashMap<BEASTInterface, BEASTObjectShape> map = new HashMap<>();
        for (Shape shape : m_objects) {
            if (shape instanceof BEASTObjectShape) {
                map.put(((BEASTObjectShape) shape).m_beastObject, (BEASTObjectShape) shape);
            }
        }
        // re-insert arrows, if any
        for (int i = m_objects.size() - 1; i >= 0; i--) {
            Shape shape = m_objects.get(i);
            if (shape instanceof BEASTObjectShape) {
                BEASTObjectShape headShape = ((BEASTObjectShape) shape);
                BEASTInterface beastObject = headShape.m_beastObject;
                try {
                    List<Input<?>> inputs = beastObject.listInputs();
                    for (Input<?> input : inputs) {
                        if (input.get() != null) {
                            if (input.get() instanceof BEASTInterface) {
                                BEASTObjectShape tailShape = map.get(input.get());
                                try {
	                                Arrow arrow = new Arrow(tailShape, headShape, input.getName());
	                                arrow.setID(getNewID(null));
	                                m_objects.add(arrow);
                                } catch (Exception e) {
									// ignore, can happen when not all inputs are to be shown
								}
                            }
                            if (input.get() instanceof List<?>) {
                                for (Object o : (List<?>) input.get()) {
                                    if (o != null && o instanceof BEASTInterface) {
                                        BEASTObjectShape tailShape = map.get(o);
                                        try {
	                                        Arrow arrow = new Arrow(tailShape, headShape, input.getName());
	                                        arrow.setID(getNewID(null));
	                                        m_objects.add(arrow);
                                        } catch (Exception e) {
        									// ignore, can happen when not all inputs are to be shown
        								}
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //moveArrowsToBack();
    } // recalcArrows

} // class Document
