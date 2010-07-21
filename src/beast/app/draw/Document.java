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

import beast.core.Input;
import beast.core.Plugin;
import beast.core.Runnable;
import beast.util.ClassDiscovery;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.StringReader;
import java.util.*;
import java.util.List;

/**
 * The Document class is the Document part in the doc-view pattern of 
 * the Beast ModelBuilder application.
 */
public class Document {
    
    /** list of PluginShapes, InputShapes and connecting Arrows **/
    public List<Shape> m_objects = new ArrayList<Shape>();

    /** undo/redo related stuff **/
    List<UndoAction> m_undoStack = new ArrayList<UndoAction>();
    int m_nCurrentEditAction = -1;
    
    
    //int m_nSavedPointer = -1;
    public boolean m_bIsSaved = true;
    public void isSaved() {m_bIsSaved = true;}
    void iSChanged() {m_bIsSaved = false;}

    /** list of class names for plug-ins to choose from **/
    String[] m_sPlugInNames;


    public Document() {
        // load all parsers
        List<String> sPlugInNames = ClassDiscovery.find(beast.core.Plugin.class, ClassDiscovery.IMPLEMENTATION_DIR);
        m_sPlugInNames = sPlugInNames.toArray(new String[0]);
    } // c'tor

    /** change order of shapes to ensure arrows are drawn before the rest **/
    void moveArrowsToBack() {
        ArrayList<Shape> arrows = new ArrayList<Shape>();
        List<Shape> others = new ArrayList<Shape>();

        for (Shape shape : m_objects) {
            if (shape instanceof Arrow) {
                arrows.add(shape);
            } else {
                others.add(shape);
            }
        }
        arrows.addAll(others);
        m_objects = arrows;
    }

    /** adjust position of inputs to fit with the associated plug in **/
    void adjustInputs() {
    	for (Shape shape : m_objects) {
    		if (shape instanceof PluginShape) {
    			((PluginShape) shape).adjustInputs();
    		}
    	}
    }

    /** adjust position of tail and head of arrows to the 
     * Plug in and input shapes they are attached to 
     * **/
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

    /** edit actions on shapes **/
    public void moveShape(int nX, int nY, int nToX, int nToY, int nPosition) {
        boolean bNeedsUndoAction = true;
        if (m_nCurrentEditAction == m_undoStack.size() - 1 && m_nCurrentEditAction >= 0) {
            UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
            if (undoAction.m_nActionType == UndoAction.MOVE_ACTION && undoAction.m_nPosition == nPosition) {
                bNeedsUndoAction = false;
            }
        }
        if (bNeedsUndoAction) {
            addUndoAction(new UndoAction(nPosition, UndoAction.MOVE_ACTION));
        }
        Shape shape = (Shape) m_objects.get(nPosition);
        shape.movePosition(nX, nY, nToX, nToY);
        adjustArrows();
    } // moveShape

    public void movePoint(int nPoint, int nX, int nY, int nToX, int nToY, int nPosition) {
        boolean bNeedsUndoAction = true;
        if (m_nCurrentEditAction == m_undoStack.size() - 1 && m_nCurrentEditAction >= 0) {
            UndoAction undoAction = m_undoStack.get(m_nCurrentEditAction);
            if (undoAction.m_nActionType == UndoAction.RESHAPE_ACTION && undoAction.m_nPosition == nPosition) {
                bNeedsUndoAction = false;
            }
        }
        if (bNeedsUndoAction) {
            addUndoAction(new UndoAction(nPosition, UndoAction.RESHAPE_ACTION));
        }
        Shape shape = (Shape) m_objects.get(nPosition);
        shape.movePoint(nPoint, nX, nY, nToX, nToY);
        adjustArrows();
    } // movePoint

    boolean containsID(String sID, List<Shape> objects, List<String> tabulist) {
    	for (Shape shape : m_objects) {
    		if (shape.getID()==null) {
    			int h = 3;
    			h++;
    		}
            if (shape.getID().equals(sID)) {
                return true;
            }
//            if (shape instanceof Group) {
//                Group group = (Group) shape;
//                if (containsID(sID, group.m_objects, tabulist)) {
//                    return true;
//                }
//            }
        }
        if (tabulist == null) {
            return false;
        }
        for (String sTabuID :  tabulist) {
            if (sTabuID.equals(sID)) {
                return true;
            }
        }
        return false;
    }

    String getNewID(List<String> tabulist) {
        int nID = m_objects.size();
        String sID = "id" + nID;
        while (containsID(sID, m_objects, tabulist)) {
            nID++;
            sID = "id" + nID;
        }
        return sID;
    }
    void setPluginID(PluginShape shape) {
    	if (shape.m_plugin.getID() != null && shape.m_plugin.getID().length() > 0) {
    		return;
    	}
		Plugin plugin = shape.m_plugin;
		String sBase = plugin.getClass().getName().replaceAll(".*\\.", "");
		int nID = 0;
        while (containsID(sBase + nID, m_objects, null)) {
        	nID++;
        }
        plugin.setID(sBase+nID);
    }
    
    Shape getShapeByID(String sID) {
        for (Shape shape : m_objects) {
            if (shape.getID().equals(sID)) {
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
        		((Arrow)shape).setID(getNewID(null));
        	}
        	if (shape instanceof PluginShape) {
        		setPluginID((PluginShape) shape);
        	}
        }
        m_objects.add(shape);
        addUndoAction(new AddAction());
    } // addNewShape

    List<Integer> getConnectedArrows(List<String> sIDs, List<Integer> selection) {
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = (Shape) m_objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                for (int j = 0; j < sIDs.size(); j++) {
                    if (arrow.m_sHeadID.equals(sIDs.get(j)) || arrow.m_sTailID.equals(sIDs.get(j))) {
                        if (!selection.contains(new Integer(i))) {
                            selection.add(new Integer(i));
                        }
                    }
                }
            }
        }
        return selection;
    }

    List<String> getIncomingArrows(List<String> sIDs) {
    	List<String> selection = new ArrayList<String>();
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = (Shape) m_objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                for (int j = 0; j < sIDs.size(); j++) {
                    if (arrow.m_sHeadID.equals(sIDs.get(j))) {
                        if (!selection.contains(arrow.m_sTailID)) {
                            selection.add(arrow.m_sTailID);
                        }
                    }
                }
            }
        }
        return selection;
    }
    
    List<String> getOutgoingArrows(List<String> sIDs) {
    	List<String> selection = new ArrayList<String>();
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = (Shape) m_objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                for (int j = 0; j < sIDs.size(); j++) {
                    if (arrow.m_sTailID.equals(sIDs.get(j))) {
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
        List<String> sIDs = new ArrayList<String>();
        for (int j = 0; j < selection.size(); j++) {
            sIDs.add(((Shape) m_objects.get(((Integer) selection.get(j)).intValue())).getID());

        }
        selection = getConnectedArrows(sIDs, selection);
        DeleteAction action = new DeleteAction(selection);
        addUndoAction(action);
        action.redo();
    } // deleteShape

    void ensureUniqueID(Shape shape, List<String> tabulist) {
        if (shape.getID() == null || 
        	shape.getID().equals("") ||
        	containsID(shape.getID(), m_objects, tabulist)) {
        	if (shape instanceof Arrow) {
        		((Arrow)shape).setID(getNewID(tabulist));
        	}
        	if (shape instanceof PluginShape) {
        		setPluginID((PluginShape) shape);
        	}
        }
        tabulist.add(shape.getID());
    } // ensureUniqueID

    public void pasteShape(String sXML) {
        Shape shape = XML2Shape(sXML);
        if (shape == null) {
            return;
        }
        ensureUniqueID(shape, new ArrayList<String>());
        m_objects.add(shape);
        addUndoAction(new AddAction());
    } // addNewShape

    /** move all plug ins connected with selection **/
    public void collapse(Selection selection) {
        // don't group arrows
        for (int i = selection.m_Selection.size() - 1; i >= 0; i--) {
            if ((Shape) m_objects.get(((Integer) selection.m_Selection.get(i)).intValue()) instanceof Arrow) {
                selection.m_Selection.remove(i);
            }
        }
        int nNrOfPrimePositions = selection.m_Selection.size();
        if (nNrOfPrimePositions == 0) {
            return;
        }
        for (int i = 0; i < nNrOfPrimePositions; i++) {
        	Shape shape = m_objects.get(((Integer) selection.m_Selection.get(i)).intValue());
        	findAffectedShapes(shape, selection.m_Selection);
        }
        if (selection.m_Selection.size() == nNrOfPrimePositions) {
        	// nothing to collapse
        	return;
        }
        
        UndoAction action = new UndoGroupAction(selection.m_Selection, nNrOfPrimePositions);
        addUndoAction(action);
        action.redo();
        selection.clear();
        selection.m_Selection.add(new Integer(m_objects.size() - 1));
        adjustInputs();
        adjustArrows();
    } // collapse

    
    /** Find inputs to collapse, i.e. PlugInShapes connected to
     * any input of a given PluginShape. 
     * Shape IDs are recorded in selection. **/
    void findAffectedShapes(Shape shape, List<Integer> selection) {
    	if (shape instanceof InputShape) {
    		findInputs((InputShape)shape, selection);
    	} else {
    		for (InputShape ellipse : ((PluginShape)shape).m_inputs) {
        		findInputs(ellipse, selection);
    		}
    	}
    }
    /** Find inputs to collapse, i.e. PlugInShapes connected to
     * given InputShape. Shape IDs are recorded in selection. **/
	void findInputs(InputShape ellipse, List<Integer> selection) {
		for (Shape shape : m_objects) {
			if (shape instanceof Arrow) {
				Arrow arrow = (Arrow) shape;
				if (arrow.m_sHeadID.equals(ellipse.getID())) {
					String sTailID = arrow.m_sTailID;
					for (int i = 0; i < m_objects.size(); i++) {
						if (m_objects.get(i).getID().equals(sTailID)) {
							selection.add(i);
						}
					}					
				}
			}
		}
	}
    
    
    
    public void ungroup(Selection selection) {
        UngroupAction action = new UngroupAction(selection);
        addUndoAction(action);
        action.redo();
        int nSize = action.getGroupSize();
        selection.clear();
        for (int i = 0; i < nSize; i++) {
            selection.m_Selection.add(new Integer(m_objects.size() - i - 1));
        }
    } // ungroup

    public void setFillColor(Color color, Selection selection) {
        if (selection.isSingleSelection()) {
            addUndoAction(new UndoAction(selection.getSingleSelection(), UndoAction.FILL_COLOR_ACTION));
        } else {
            addUndoGroupAction(selection);
        }
        for (int i = 0; i < selection.m_Selection.size(); i++) {
            int iSelection = ((Integer) selection.m_Selection.get(i)).intValue();
            Shape shape = (Shape) m_objects.get(iSelection);
            shape.setFillColor(color);
        }
    } // setFillColor

    public void setPenColor(Color color, Selection selection) {
        if (selection.isSingleSelection()) {
            addUndoAction(new UndoAction(selection.getSingleSelection(), UndoAction.PEN_COLOR_ACTION));
        } else {
            addUndoGroupAction(selection);
        }
        for (int i = 0; i < selection.m_Selection.size(); i++) {
            int iSelection = ((Integer) selection.m_Selection.get(i)).intValue();
            Shape shape = (Shape) m_objects.get(iSelection);
            shape.setPenColor(color);
        }
    } // setPenColor

    public void addUndoGroupAction(Selection selection) {
        addUndoAction(new UndoGroupAction(selection.m_Selection));
    }

    int getPositionX(int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        return shape.getX();
    }

    int getPositionY(int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        return shape.getY();
    }

    int getPositionX2(int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        return shape.getX2();
    }

    int getPositionY2(int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        return shape.getY2();
    }

    void setPositionX(int nX, int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        shape.setX(nX);
    }

    void setPositionY(int nY, int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        shape.setY(nY);
    }

    void setPositionX2(int nX, int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        shape.setX2(nX);
    }

    void setPositionY2(int nY, int iShape) {
        Shape shape = (Shape) m_objects.get(iShape);
        shape.setY2(nY);
    }

    public void setID(String sID, int iObject) {
        addUndoAction(new UndoAction(iObject, UndoAction.SET_LABEL_ACTION));
        Shape shape = (Shape) m_objects.get(iObject);
        ((PluginShape)shape).m_plugin.setID(sID);
    }

    public void toggleFilled(int iObject) {
        addUndoAction(new UndoAction(iObject, UndoAction.TOGGLE_FILL_ACTION));
        Shape shape = (Shape) m_objects.get(iObject);
        shape.toggleFilled();
    }

    public void setPenWidth(int nPenWidth, int iObject) {
        addUndoAction(new UndoAction(iObject, UndoAction.SET_URL_ACTION));
        Shape shape = (Shape) m_objects.get(iObject);
        shape.setPenWidth(nPenWidth);
    }

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
            order[i] = ((Integer) selection.m_Selection.get(i)).intValue();
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
            oldOrder[i] = ((Integer) selection.m_Selection.get(i)).intValue();
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
            order[i] = ((Integer) selection.m_Selection.get(i)).intValue();
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
            oldOrder[i] = ((Integer) selection.m_Selection.get(i)).intValue();
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
        addUndoGroupAction(selection);

        List<Integer> nodes = selection.m_Selection;
        int nMinX = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nX = getPositionX(((Integer) nodes.get(iNode)).intValue());
            if (nX < nMinX || iNode == 0) {
                nMinX = nX;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            setPositionX(nMinX, nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMaxX = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nX = getPositionX2(((Integer) nodes.get(iNode)).intValue());
            if (nX > nMaxX || iNode == 0) {
                nMaxX = nX;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            int dX = getPositionX2(nNode) - getPositionX(nNode);
            setPositionX(nMaxX - dX, nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMinY = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nY = getPositionY(((Integer) nodes.get(iNode)).intValue());
            if (nY < nMinY || iNode == 0) {
                nMinY = nY;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            setPositionY(nMinY, nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMaxY = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nY = getPositionY2(((Integer) nodes.get(iNode)).intValue());
            if (nY > nMaxY || iNode == 0) {
                nMaxY = nY;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            int dY = getPositionY2(nNode) - getPositionY(nNode);
            setPositionY(nMaxY - dY, nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMinY = -1;
        int nMaxY = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nY = (getPositionY(((Integer) nodes.get(iNode)).intValue()) +
                    getPositionY2(((Integer) nodes.get(iNode)).intValue())) / 2;
            if (nY < nMinY || iNode == 0) {
                nMinY = nY;
            }
            if (nY > nMaxY || iNode == 0) {
                nMaxY = nY;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            int dY = (getPositionY2(nNode) - getPositionY(nNode)) / 2;
            setPositionY((nMinY + nMaxY) / 2 - dY, nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMinX = -1;
        int nMaxX = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nX = (getPositionX(((Integer) nodes.get(iNode)).intValue()) +
                    getPositionX2(((Integer) nodes.get(iNode)).intValue())) / 2;
            if (nX < nMinX || iNode == 0) {
                nMinX = nX;
            }
            if (nX > nMaxX || iNode == 0) {
                nMaxX = nX;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            int dX = (getPositionX2(nNode) - getPositionX(nNode)) / 2;
            setPositionX((nMinX + nMaxX) / 2 - dX, nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMinX = -1;
        int nMaxX = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nX = getPositionX(((Integer) nodes.get(iNode)).intValue());
            if (nX < nMinX || iNode == 0) {
                nMinX = nX;
            }
            if (nX > nMaxX || iNode == 0) {
                nMaxX = nX;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            setPositionX((int) (nMinX + iNode * (nMaxX - nMinX) / (nodes.size() - 1.0)), nNode);
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
        addUndoGroupAction(selection);
        List<Integer> nodes = selection.m_Selection;
        int nMinY = -1;
        int nMaxY = -1;
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nY = getPositionY(((Integer) nodes.get(iNode)).intValue());
            if (nY < nMinY || iNode == 0) {
                nMinY = nY;
            }
            if (nY > nMaxY || iNode == 0) {
                nMaxY = nY;
            }
        }
        for (int iNode = 0; iNode < nodes.size(); iNode++) {
            int nNode = ((Integer) nodes.get(iNode)).intValue();
            setPositionY((int) (nMinY + iNode * (nMaxY - nMinY) / (nodes.size() - 1.0)), nNode);
        }
        adjustArrows();
    } // spaceVertical

    class UndoAction {
        final static int UNDO_ACTION = 0;
        final static int MOVE_ACTION = 1;
        final static int RESHAPE_ACTION = 2;
        final static int ADD_ACTION = 3;
        final static int DELETE_ACTION = 4;
        final static int DELETE_SET_ACTION = 11;
        final static int FILL_COLOR_ACTION = 5;
        final static int PEN_COLOR_ACTION = 6;
        final static int SET_LABEL_ACTION = 7;
        final static int SET_URL_ACTION = 8;
        final static int SET_IMG_ACTION = 9;
        final static int TOGGLE_FILL_ACTION = 10;


        int m_nActionType = UNDO_ACTION;

        String m_sXML;
        int m_nPosition;

        UndoAction() {
        }

        UndoAction(String sXML, int nPosition, int nType) {
            m_sXML = sXML;
            m_nPosition = nPosition;
            m_nActionType = nType;
        }

        UndoAction(int nPosition, int nType) {
            m_sXML = ((Shape) m_objects.get(nPosition)).getXML();
            m_nPosition = nPosition;
            m_nActionType = nType;
        }

        void undo() {
            Shape originalShape = XML2Shape(m_sXML);
            Shape currentShape = (Shape) m_objects.get(m_nPosition);
            m_objects.set(m_nPosition, originalShape);
            m_sXML = currentShape.getXML();
        } // undo

        void redo() {
            Shape newShape = XML2Shape(m_sXML);
            Shape currentShape = (Shape) m_objects.get(m_nPosition);
            m_objects.set(m_nPosition, newShape);
            m_sXML = currentShape.getXML();
        } // redo
    } // class UndoAction

    class AddAction extends UndoAction {
        public AddAction() {
            super(m_objects.size() - 1, ADD_ACTION);
        }

        void undo() {
            m_objects.remove(m_nPosition);
        }

        void redo() {
            Shape shape = XML2Shape(m_sXML);
            m_objects.add(shape);
        }
    } // class AddAction

    class DeleteAction extends UndoAction {
        public DeleteAction(int nPosition) {
            super(nPosition, DELETE_ACTION);
        }

        List<Integer> m_nPositions;
        List<Shape> m_shapes;

        public DeleteAction(List<Integer> selection) {
            super();
            m_nActionType = DELETE_SET_ACTION;
            m_nPositions = new ArrayList<Integer>();
            int[] selection2 = new int[selection.size()];
            for (int i = 0; i < selection2.length; i++) {
                selection2[i] = ((Integer) selection.get(i)).intValue();
            }
            Arrays.sort(selection2);
            for (int i = 0; i < selection2.length; i++) {
                m_nPositions.add(new Integer(selection2[i]));
            }
            List<Shape> shapes = new ArrayList<Shape>();
            for (int i = 0; i < selection.size(); i++) {
                shapes.add(m_objects.get(((Integer) selection.get(i)).intValue()));
            }
            m_shapes = shapes;
        }

        void undo() {
            if (m_nActionType == DELETE_ACTION) {
                Shape shape = XML2Shape(m_sXML);
                m_objects.add(m_nPosition, shape);
            } else { // m_nActionType == DELETE_SET_ACTION
                for (int i = 0; i < m_nPositions.size(); i++) {
                    int iShape = ((Integer) m_nPositions.get(i)).intValue();
                    Shape shape = (Shape) m_shapes.get(i);
                    m_objects.add(iShape, shape);
                }
            }
        }

        void redo() {
            if (m_nActionType == DELETE_ACTION) {
            	Shape shape = m_objects.get(m_nPosition);
            	if (shape instanceof Arrow) {
            		
            	} else if (shape instanceof Arrow) {
            		
            	}
                m_objects.remove(m_nPosition);
            } else { // m_nActionType == DELETE_SET_ACTION
                for (int i = m_nPositions.size() - 1; i >= 0; i--) {
                    int iShape = ((Integer) m_nPositions.get(i)).intValue();
                    m_objects.remove(iShape);
                }
            }
        }
    } // class DeleteAction

    public class UndoGroupAction extends UndoAction {
        List<Integer> m_nPositions;
        int m_nNrPrimePositions;
        
        public UndoGroupAction(List<Integer> selection) {
            this(selection, selection.size());
        }
        public UndoGroupAction(List<Integer> selection, int nNrPrimePositions) {
            super();
        	m_nNrPrimePositions = nNrPrimePositions;
            m_nPositions = new ArrayList<Integer>();
            for (int i = 0; i < selection.size(); i++) {
                m_nPositions.add(new Integer(((Integer) selection.get(i)).intValue()));
            }
            m_sXML = "<doc>";
            for (int i = 0; i < m_nPositions.size(); i++) {
                int iShape = ((Integer) m_nPositions.get(i)).intValue();
                Shape shape = (Shape) m_objects.get(iShape);
                m_sXML += shape.getXML();
            }
            m_sXML += "</doc>";
        }

        void undo() {
            doit();
        }

        void redo() {
            if (m_nNrPrimePositions == m_nPositions.size()) {
            	// it is a regular group move action
                doit();
                return;
            }
            // it is a collapse action
            for (int i = 0; i < m_nNrPrimePositions; i++) {
            	Shape shape = m_objects.get(m_nPositions.get(i));
            	if (shape instanceof InputShape) {
            		moveInputShapes((InputShape) shape);
            	} else {
               		for (InputShape ellipse : ((PluginShape)shape).m_inputs) {
               			moveInputShapes(ellipse);
                	}
            	}
            }
        }
        
    	void moveInputShapes(InputShape ellipse) {
    		for (Shape shape : m_objects) {
    			if (shape instanceof Arrow) {
    				Arrow arrow = (Arrow) shape;
    				if (arrow.m_sHeadID.equals(ellipse.getID())) {
    					String sTailID = arrow.m_sTailID;
    					for (int i = 0; i < m_objects.size(); i++) {
    						Shape shape2 = m_objects.get(i); 
    						if (shape2.getID().equals(sTailID)) {
    							shape2.m_x = ellipse.m_x;
    							shape2.m_y = ellipse.m_y;
    							shape2.m_w = 2;
    							shape2.m_h = 2;
    							shape2.m_bNeedsDrawing = false;
    						}
    					}					
    				}
    			}
    		}
    	}
        
        void doit() {
            String sXML = "<doc>";
            for (int i = 0; i < m_nPositions.size(); i++) {
                int iShape = ((Integer) m_nPositions.get(i)).intValue();
                Shape shape = (Shape) m_objects.get(iShape);
                sXML += shape.getXML();
            }
            sXML += "</doc>";
            List<Shape> shapes = XML2Shapes(m_sXML);
            for (int i = 0; i < m_nPositions.size(); i++) {
                int iShape = ((Integer) m_nPositions.get(i)).intValue();
                Shape shape = (Shape) shapes.get(i);
                m_objects.set(iShape, shape);
            }
            if (m_nNrPrimePositions == m_nPositions.size()) {
            	m_sXML = sXML;
            }
        }
    } // class UndoGroupAction

    class ReorderAction extends UndoAction {
        int[] m_oldOrder;
        int[] m_newOrder;

        ReorderAction(int[] oldOrder, int[] newOrder) {
            super();
            m_oldOrder = oldOrder;
            m_newOrder = newOrder;
        }

        void undo() {
            reorder(m_oldOrder, m_newOrder);
        }

        void redo() {
            for (int i = m_newOrder.length - 1; i >= 0; i--) {
                int iSelection = m_newOrder[i];
                Shape shape = (Shape) m_objects.get(iSelection);
                m_objects.remove(iSelection);
                m_objects.add(m_oldOrder[i], shape);
            }
        }

        void reorder(int[] oldOrder, int[] newOrder) {
            for (int i = 0; i < oldOrder.length; i++) {
                int iSelection = oldOrder[i];
                Shape shape = (Shape) m_objects.get(iSelection);
                m_objects.remove(iSelection);
                m_objects.add(newOrder[i], shape);
            }
        }
    } // class ReorderAction

//    class GroupAction extends DeleteAction {
//        GroupAction(List<Integer> selection) {
//            super(selection);
//            //m_group.getID() = getNewID(null);
//        }
//
//        void undo() {
//            m_objects.remove(m_objects.size() - 1);
//            for (int i = 0; i < m_nPositions.size(); i++) {
//                int iShape = ((Integer) m_nPositions.get(i)).intValue();
//                Shape shape = (Shape) m_group.m_objects.get(i);
//                m_objects.add(iShape, shape);
//            }
//        }
//
//        void redo() {
//            for (int i = m_nPositions.size() - 1; i >= 0; i--) {
//                int iShape = ((Integer) m_nPositions.get(i)).intValue();
//                m_objects.remove(iShape);
//            }
//            m_objects.add(m_group);
//        }
//
//    } // class GroupAction

    class UngroupAction extends UndoAction {

        //Group m_group;
        List<Shape> m_shapes;
        int m_nGroupSize;

        UngroupAction(Selection selection) {
            super();
            m_nPosition = selection.getSingleSelection();
            //m_group = (Group) m_objects.get(m_nPosition);
        } // c'tor

        int getGroupSize() {
            return m_shapes.size();//m_group.m_objects.size();
        }

        void undo() {
//            m_objects.add(m_nPosition, m_group);
//            for (int i = 0; i < m_group.m_objects.size(); i++) {
//                m_objects.remove(m_objects.size() - 1);
//            }
        }

        void redo() {
//            for (int i = 0; i < m_group.m_objects.size(); i++) {
//                Shape shape = (Shape) m_group.m_objects.get(i);
//                m_objects.add(shape);
//            }
//            m_objects.remove(m_nPosition);
        }

    } // class UngroupAction

    /**
     * add undo action to the undo stack.
     *
     * @param action operation that needs to be added to the undo stack
     */
    void addUndoAction(UndoAction action) {
        int iAction = m_undoStack.size() - 1;
        while (iAction > m_nCurrentEditAction) {
            m_undoStack.remove(iAction--);
        }
        m_undoStack.add(action);
        m_nCurrentEditAction++;
    } // addUndoAction

    /**
     * remove all actions from the undo stack
     */
    public void clearUndoStack() {
        m_undoStack = new ArrayList<UndoAction>();
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
        adjustArrows();

    } // redo


    PluginShape getObjectWithLabel(String sLabel) {
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape) {
                if (shape.getLabel() != null && shape.getLabel().equals(sLabel)) {
                    return (PluginShape) shape;
                }
            }
        }
        return null;
    }

    final static int DX = 120;
    final static int DY = 80;

    void addInput(PluginShape shape, Object o2, int nDepth, String sInput) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (o2 instanceof Plugin) {
            PluginShape shape2 = getObjectWithLabel(((Plugin) o2).getID());
            if (shape2 == null) {
                shape2 = new PluginShape((Plugin) o2, this);
                shape2.m_x = nDepth * DX;
                shape2.m_w = DY;
                shape2.m_plugin = (Plugin) o2;
                setPluginID(shape2);
                m_objects.add(shape2);
            }
            process(shape2, nDepth);
        }
    }

    void process(PluginShape shape, int nDepth) throws IllegalArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Plugin plugin = shape.m_plugin;
        List<Input<?>> sInputs = plugin.listInputs();
        for (Input<?> input_ : sInputs) {
            Object o = input_.get();
            if (o != null) {
                if (o instanceof List<?>) {
                    for (Object o2 : (List<?>) o) {
                        addInput(shape, o2, nDepth + 1, input_.getName());
                    }
                } else if (o instanceof Plugin) {
                    addInput(shape, o, nDepth + 1, input_.getName());
                // } else {
                	// it is a primitive type
                }
            }
        }
    }
    


    public void loadFile(String sFileName) {
    	m_objects.clear();
        XMLParser parser = new XMLParser();
        try {
            Plugin plugin0 = parser.parseFile(sFileName);
            
            if (plugin0 instanceof PluginSet) {
            	List<Plugin> set = ((PluginSet) plugin0).m_plugins.get();
            	if (set == null) {
            		return;
            	}
            	for (Plugin plugin : set) {
                    PluginShape shape = new PluginShape(plugin, this);
                    shape.m_plugin = plugin;
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
            PluginShape shape = new PluginShape(plugin0, this);
	            shape.m_plugin = plugin0;
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
    } // loadFile

    Shape XML2Shape(String sXML) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(sXML)));
            doc.normalize();
            Node node = doc.getDocumentElement();
            Shape shape = parseNode(node, this);
            return shape;
        } catch (Throwable t) {
        }
        return null;
    } // XML2Shape

    List<Shape> XML2Shapes(String sXML) {
        List<Shape> shapes = new ArrayList<Shape>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(sXML)));
            doc.normalize();
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int iNode = 0; iNode < nodes.getLength(); iNode++) {
                Node node = nodes.item(iNode);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    shapes.add(parseNode(node, this));
                }
            }
        } catch (Throwable t) {
        }
        return shapes;
    }

    /** parse XDL xml format **/
    static Shape parseNode(Node node, Document doc) {
        Shape shape = null;
        if (node.getNodeName().equals("ellipse")) {
            shape = new InputShape(node, doc);
        } else if (node.getNodeName().equals("arrow")) {
            shape = new Arrow(node, doc);
        } else if (node.getNodeName().equals("gdx:function")) {
            shape = new PluginShape(node, doc);
        }
        return shape;
    } // parseNode

    
    public String toXML() {
        XMLProducer xmlProducer = new XMLProducer();
        PluginSet pluginSet = calcPluginSet();
        if (pluginSet.m_plugins.get().size() == 1) {
            return xmlProducer.toXML(pluginSet.m_plugins.get().get(0));
        }
        return xmlProducer.toXML(pluginSet);
    } // toXML

    
    /** collect all objects and put all top-level plugins in a PluginSet */
    PluginSet calcPluginSet() {
    	// collect all plug-ins
    	Collection<Plugin> plugins = getPlugins();
    	// calc outputs
		HashMap<Plugin, List<Plugin>> outputs = PluginDialog.getOutputs(plugins); 
    	// put all plugins with no ouputs in the PluginSet
    	PluginSet pluginSet = new PluginSet();
		for (Plugin plugin : outputs.keySet()) {
			if (outputs.get(plugin).size() == 0) {
				try {
					pluginSet.setInputValue("plugin", plugin);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
    	return pluginSet;
    } // calcPluginSet
    
    /** convert m_objects in set of plugins **/
    Collection<Plugin> getPlugins() {
    	Collection<Plugin> plugins = new HashSet<Plugin>();
    	for (Shape shape : m_objects) {
    		if (shape instanceof PluginShape) {
    			plugins.add(((PluginShape)shape).m_plugin);
    		}
    	}
    	return plugins;
	}

    /** return true if source is ascendant of target **/
    boolean isAscendant(Plugin source, Plugin target) {
        Collection<Plugin> plugins = getPlugins();
    	List<Plugin> ascendants = PluginDialog.listAscendants(target, plugins);
    	return ascendants.contains(source);
    }
    
    Shape findObjectWithID(String sID) {
        for (int i = 0; i < m_objects.size(); i++) {
            if (m_objects.get(i).getID().equals(sID)) {
                return m_objects.get(i);
            }
        }
        return null;
    }


    /** simple layout algorithm for placing PluginShapes **/
    void layout() {
    	// first construct input map for ease of navigation
    	HashMap<PluginShape,List<PluginShape>> inputMap = new HashMap<PluginShape, List<PluginShape>>();
    	HashMap<PluginShape,List<PluginShape>> outputMap = new HashMap<PluginShape, List<PluginShape>>();
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape && shape.m_bNeedsDrawing) {
            	inputMap.put((PluginShape)shape, new ArrayList<PluginShape>());
            	outputMap.put((PluginShape)shape, new ArrayList<PluginShape>());
            }
        }
        for (Shape shape : m_objects) {
        	if (shape instanceof Arrow && shape.m_bNeedsDrawing) {
        		Shape headShape = ((Arrow) shape).m_headShape;
        		PluginShape pluginShape = ((InputShape) headShape).m_pluginShape;
        		PluginShape inputShape = ((Arrow) shape).m_tailShape;
        		inputMap.get(pluginShape).add(inputShape);
        		outputMap.get(inputShape).add(pluginShape);
        	}
        }
        // reset all x-coords to minimal x-value
        for (Shape shape : inputMap.keySet()) {
           	shape.m_x = DX;
        }
        // move inputs rightward till they exceed x-coord of their inputs
        boolean bProgress = true;
        while (bProgress) {
        	bProgress = false;
            for (Shape shape : inputMap.keySet()) {
            	int nMaxInputX = -DX;
            	for (Shape input : inputMap.get(shape)) {
            		nMaxInputX = Math.max(nMaxInputX, input.m_x);
            	}
            	if (shape.m_x < nMaxInputX + DX) {
            		shape.m_x = nMaxInputX + DX;
            		bProgress = true;
            	}
            }
        }
        // move inputs rightward till they are stopped by their outputs
        bProgress = true;
        while (bProgress) {
        	bProgress = false;
            for (Shape shape : outputMap.keySet()) {
            	int nMinOutputX = Integer.MAX_VALUE;
            	for (Shape input : outputMap.get(shape)) {
            		nMinOutputX = Math.min(nMinOutputX, input.m_x);
            	}
            	if (nMinOutputX < Integer.MAX_VALUE && shape.m_x < nMinOutputX - DX) {
            		shape.m_x = nMinOutputX - DX;
            		bProgress = true;
            	}
            }
        }
        
        
        layoutAdjustY(inputMap);
        // relax a bit
        System.err.print("Relax...");
        for (int i = 0; i < 250; i++) {
        	relax(false);
        }
        System.err.println("Done");
        layoutAdjustY(inputMap);

        adjustInputs();
    }
    
    /** Adjust y-coordinate of PluginShapes so they don't overlap
     * and are close to their inputs. Helper method for layout() **/
    void layoutAdjustY(HashMap<PluginShape,List<PluginShape>> inputMap) {
        // next, optimise top down order
        boolean bProgress = true;
        int iX = DX; 
        while (bProgress) {
            List<PluginShape> shapes = new ArrayList<PluginShape>();
            // find shapes with same x-coordinate
            for (PluginShape shape : inputMap.keySet()) {
                if (shape.m_x == iX) {
                    shapes.add(shape);
                }
            }
            int k = 1;
            HashMap<Integer, PluginShape> ycoordMap = new HashMap<Integer, PluginShape>();
            // set y-coordinate as mean of inputs
            // if there are no inputs, order them top to bottom at DY intervals
            for (PluginShape shape : shapes) {
            	List<PluginShape> inputs = inputMap.get(shape);
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
            int nPrevY = 0;
            ArrayList<Integer> yCoords = new ArrayList<Integer>();
            yCoords.addAll(ycoordMap.keySet());
            Collections.sort(yCoords);
            int dY = 0;
            for (Integer i : yCoords) {
            	PluginShape shape = ycoordMap.get(i);
            	if (shape.m_y < nPrevY + DY) {
            		dY = nPrevY + DY - shape.m_y;
            		shape.m_y = nPrevY + DY;
            	}
            	nPrevY = shape.m_y;
            }
            // upwards correction
            if (dY > 0) {
            	dY /= shapes.size();
                for (PluginShape shape : shapes) {
                	shape.m_y -= dY;
                }
            }
            
            
            bProgress = (shapes.size() > 0);
            iX += DX;
        }
    } // layoutAdjustY

    /** apply spring model algorithm to the placement of plug-in shapes **/
    public void relax(boolean bAllowXToMove) {
        List<Shape> objects = new ArrayList<Shape>();
        for (Shape shape : m_objects) {
            if (shape.m_bNeedsDrawing) {
                objects.add(shape);
            }
        }

        // Step 0: determine degrees
        HashMap<String, Integer> degreeMap = new HashMap<String, Integer>();
        for (Shape shape : objects) {

            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                String sID = arrow.m_tailShape.getID();
                if (arrow.m_headShape instanceof InputShape) {
                    String sID2 = ((InputShape) arrow.m_headShape).m_pluginShape.getID();
                    if (degreeMap.containsKey(sID)) {
                        degreeMap.put(sID, degreeMap.get(sID) + 1);
                    } else {
                        degreeMap.put(sID, 1);
                    }
                    if (degreeMap.containsKey(sID2)) {
                        degreeMap.put(sID2, degreeMap.get(sID2) + 1);
                    } else {
                        degreeMap.put(sID2, 1);
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
                    Shape target = ((InputShape) arrow.m_headShape).m_pluginShape;
                    int p2x = target.m_x + target.m_w / 2;
                    int p2y = target.m_y + target.m_h / 2;

                    double vx = p1x - p2x;
                    double vy = p1y - p2y;
                    double len = Math.sqrt(vx * vx + vy * vy);

                    double desiredLen = 150;

                    // round from zero, if needed [zero would be Bad.].
                    len = (len == 0) ? .0001 : len;

                    double f = 1.0 / 3.0 * (desiredLen - len) / len;

                    int nDegree1 = degreeMap.get(source.getID());
                    int nDegree2 = degreeMap.get(target.getID());


                    f = f * Math.pow(0.99, (nDegree1 + nDegree2 - 2));


                    // the actual movement distance 'dx' is the force multiplied by the
                    // distance to go.
                    double dx = Math.min(f * vx, 3);
                    double dy = Math.min(f * vy, 3);
                    if (vx > -200 && vx < 0) {
                        dx = -dx;
                        //f *= Math.abs((vx+200))/40;
                    }
                    if (bAllowXToMove) source.m_x = (int) Math.max(100, source.m_x + dx);
                    source.m_y = (int) Math.max(10, source.m_y + dy);
                    if (bAllowXToMove) target.m_x = (int) Math.max(100, target.m_x - dx);
                    target.m_y = (int) Math.max(10, target.m_y - dy);

                }
            }

        }
        // Step 2: repulse (pairwise)
        for (Shape shape1 : objects) {
            if (shape1 instanceof PluginShape) {
                int p1x = shape1.m_x + shape1.m_w / 2;
                int p1y = shape1.m_y + shape1.m_h / 2;
                double dx = 0, dy = 0;
                for (Shape shape2 : objects) {
                    if (shape2 instanceof PluginShape) {
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
                if (bAllowXToMove) shape1.m_x = (int) Math.min(800, Math.max(10, shape1.m_x + dx));
                shape1.m_y = (int) Math.min(800, Math.max(10, shape1.m_y + dy));
            }
        }

        // Step 3: move dependent objects in place
        for (Shape shape : objects) {
            if (shape instanceof PluginShape) {
                ((PluginShape) shape).adjustInputs();
            }
        }
        adjustArrows();
    } // relax


    /** sanity check: make sure there are no cycles and there is a Runnable 
     * plug in that is not an input of another plug in
     */
    final static int STATUS_OK = 0, STATUS_CYCLE = 1, STATUS_NOT_RUNNABLE = 2,
    STATUS_EMPTY_MODEL = 3, STATUS_ORPHANS_IN_MODEL = 4
    ;
    int isValidModel() {
    	PluginSet pluginSet = calcPluginSet();
    	if (pluginSet.m_plugins.get().size() == 0) {
    		return STATUS_EMPTY_MODEL;
    	}
    	if (pluginSet.m_plugins.get().size() > 1) {
    		return STATUS_ORPHANS_IN_MODEL;
    	}
    	boolean hasRunable = false;
    	for (Plugin plugin: pluginSet.m_plugins.get()) {
    		if (plugin instanceof Runnable) {
    			hasRunable = true;
    		}
    	}
    	if (!hasRunable) {
    		return STATUS_NOT_RUNNABLE;
    	}
    	return STATUS_OK;
    } // isValidModel

    /** remove all arrows, then add based on the plugin inputs **/
    void recalcArrows() {
    	// remove all arrows
    	for (int i = m_objects.size()-1; i >=0; i--) {
    		Shape shape = m_objects.get(i);
    		if (shape instanceof Arrow) {
    			m_objects.remove(i);
    		}
    	}
    	// build map for quick resolution of PluginShapes
    	HashMap<Plugin, PluginShape> map = new HashMap<Plugin, PluginShape>();
    	for (Shape shape : m_objects) {
    		if (shape instanceof PluginShape) {
    			map.put(((PluginShape) shape).m_plugin, (PluginShape)shape);
    		}
    	}
    	// re-insert arrows, if any
    	for (int i = m_objects.size()-1; i >=0; i--) {
    		Shape shape = m_objects.get(i);
    		if (shape instanceof PluginShape) {
    			PluginShape headShape = ((PluginShape) shape);
    			Plugin plugin = headShape.m_plugin;
    			try {
    			List<Input<?>> inputs = plugin.listInputs();
	    			for (Input<?> input : inputs) {
	    				if (input.get() != null) {
	    					if (input.get() instanceof Plugin) {
	    						PluginShape tailShape = map.get((Plugin)input.get());
	    						Arrow arrow = new Arrow(tailShape, headShape, input.getName());
	    						arrow.setID(getNewID(null));
	    						m_objects.add(arrow);
	    					}
	    					if (input.get() instanceof List<?>) {
	    						for (Object o : (List<?>) input.get()) {
	    							if (o != null && o instanceof Plugin) {
	    								PluginShape tailShape = map.get((Plugin)o);
	    	    						Arrow arrow = new Arrow(tailShape, headShape, input.getName());
	    	    						arrow.setID(getNewID(null));
	    	    						m_objects.add(arrow);
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
        moveArrowsToBack();
    } // recalcArrows
    
} // class Document
