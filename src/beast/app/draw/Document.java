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
import beast.core.MCMC;
import beast.core.Plugin;
import beast.core.Runnable;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;


public class Document {
    String m_sName = "not named";
    Runnable m_mcmc = null;
    public ArrayList<Shape> m_objects = new ArrayList<Shape>();
    List<UndoAction> m_undoStack = new ArrayList<UndoAction>();
    int m_nCurrentEditAction = -1;
    /**
     * action that the network is saved
     */
    int m_nSavedPointer = -1;

    public boolean m_bIsSaved = true;

    public void isSaved() {
        m_bIsSaved = true;
    }

    void iSChanged() {
        m_bIsSaved = false;
    }

//    public final static String[] IMPLEMENTATION_DIR = {"beast.core", "beast.evolution", "beast.util"};
    public final static String[] IMPLEMENTATION_DIR = {"beast"};


    /**
     * list of class names for constants and functions respectively to choose from *
     */
//	String [] m_sConstantNames;
    String[] m_sFunctionNames;


    public Document() {
        // load all parsers
        List<String> sFunctioNames = ClassDiscovery.find(beast.core.Plugin.class, IMPLEMENTATION_DIR);
        m_sFunctionNames = sFunctioNames.toArray(new String[0]);
    } // c'tor

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

    void adjustArrows() {
        adjustArrows(false, m_objects);
    }

    void adjustInputs() {
    	for (Shape shape : m_objects) {
    		if (shape instanceof PluginShape) {
    			((PluginShape) shape).adjustInputs();
    		}
    	}
    }
    
    void adjustArrows(boolean bResetID, List<Shape> objects) {
        for (int i = 0; i < objects.size(); i++) {
            Shape shape = (Shape) objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                arrow.adjustCoordinates(m_objects, bResetID);
            }
            if (shape instanceof Group) {
                Group group = (Group) shape;
                adjustArrows(bResetID, group.m_objects);
            }
        }
    } // adjustArrows


    void adjustArrows(List<Shape> objects, List<Shape> groupObjects) {
        for (int i = 0; i < objects.size(); i++) {
            Shape shape = (Shape) objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                arrow.resetIDs(groupObjects);
            }
            if (shape instanceof Group) {
                Group group = (Group) shape;
                adjustArrows(group.m_objects, groupObjects);
            }
        }
    } // adjustArrows

    void readjustArrows(List<Shape> objects) {
        for (int i = 0; i < objects.size(); i++) {
            Shape shape = (Shape) objects.get(i);
            if (shape instanceof Arrow) {
                Arrow arrow = (Arrow) shape;
                arrow.m_sHeadID = arrow.m_head.m_id;
                ;
                arrow.m_sTailID = arrow.m_tail.m_id;
                ;
            }
        }
    } // readjustArrows

    public void moveShape(int nX, int nY, int nToX, int nToY, int nPosition) {
        boolean bNeedsUndoAction = true;
        if (m_nCurrentEditAction == m_undoStack.size() - 1 && m_nCurrentEditAction >= 0) {
            UndoAction undoAction = (UndoAction) m_undoStack.get(m_nCurrentEditAction);
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
            UndoAction undoAction = (UndoAction) m_undoStack.get(m_nCurrentEditAction);
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
        for (int i = 0; i < objects.size(); i++) {
            Shape shape = (Shape) objects.get(i);
            if (shape.m_id.equals(sID)) {
                return true;
            }
            if (shape instanceof Group) {
                Group group = (Group) shape;
                if (containsID(sID, group.m_objects, tabulist)) {
                    return true;
                }
            }
        }
        if (tabulist == null) {
            return false;
        }
        for (int i = 0; i < tabulist.size(); i++) {
            if (tabulist.get(i).equals(sID)) {
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

    Shape getID(String sID) {
        for (Shape shape : m_objects) {
            if (shape.m_id.equals(sID)) {
                return shape;
            }
        }
        return null;
    }

    public void addNewShape(Shape shape) {
        if (shape.m_id == null || containsID(shape.m_id, m_objects, null)) {
            shape.m_id = getNewID(null);
        }
        m_objects.add(shape);
        if (shape instanceof PluginShape && ((PluginShape) shape).m_function instanceof MCMC) {
            m_mcmc = (Runnable) ((PluginShape) shape).m_function;
        }
        addUndoAction(new AddAction());
    } // addNewShape

//	public void deleteShape(int nPosition) {
//		addUndoAction(new DeleteAction(nPosition));
//		m_objects.removeElementAt(nPosition);
//	} // deleteShape


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

    public void deleteShapes(List<Integer> selection) {
        List<String> sIDs = new ArrayList<String>();
        for (int j = 0; j < selection.size(); j++) {
            sIDs.add(((Shape) m_objects.get(((Integer) selection.get(j)).intValue())).m_id);

        }
        selection = getConnectedArrows(sIDs, selection);
        DeleteAction action = new DeleteAction(selection);
        addUndoAction(action);
        action.redo();
    } // deleteShape

    void ensureUniqueID(Shape shape, List<String> tabulist) {
        if (shape.m_id == null || containsID(shape.m_id, m_objects, tabulist)) {
            shape.m_id = getNewID(tabulist);
        }
        tabulist.add(shape.m_id);
        if (shape instanceof Group) {
            Group group = (Group) shape;
            adjustArrows(group.m_objects, group.m_objects);
            for (int i = 0; i < group.m_objects.size(); i++) {
                ensureUniqueID((Shape) group.m_objects.get(i), tabulist);
            }
            readjustArrows(group.m_objects);
        }
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

    public void group(Selection selection) {
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
    } // group

    
    void findAffectedShapes(Shape shape, List<Integer> selection) {
    	if (shape instanceof Ellipse) {
    		findInputs((Ellipse)shape, selection);
    	} else {
    		for (Ellipse ellipse : ((PluginShape)shape).m_inputs) {
        		findInputs(ellipse, selection);
    		}
    	}
    }
	void findInputs(Ellipse ellipse, List<Integer> selection) {
		for (Shape shape : m_objects) {
			if (shape instanceof Arrow) {
				Arrow arrow = (Arrow) shape;
				if (arrow.m_sHeadID.equals(ellipse.m_id)) {
					String sTailID = arrow.m_sTailID;
					for (int i = 0; i < m_objects.size(); i++) {
						if (m_objects.get(i).m_id.equals(sTailID)) {
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
    } // setFillColor

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

    public void setLabel(String sLabel, int iObject) {
        addUndoAction(new UndoAction(iObject, UndoAction.SET_LABEL_ACTION));
        Shape shape = (Shape) m_objects.get(iObject);
        shape.setLabel(sLabel);
    }

    public void setURL(String sURL, int iObject) {
        addUndoAction(new UndoAction(iObject, UndoAction.SET_URL_ACTION));
        Shape shape = (Shape) m_objects.get(iObject);
        shape.setURL(sURL);
    }

    public void setImageSrc(String sSrc, int iObject) {
        addUndoAction(new UndoAction(iObject, UndoAction.SET_IMG_ACTION));
        Shape shape = (Shape) m_objects.get(iObject);
        shape.setImageSrc(sSrc);
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
     * center set of nodes half way between left and right most node in the list
     *
     * @param selection a selection
     */
    public void centerHorizontal(Selection selection) {
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
    } // centerHorizontal

    /**
     * center set of nodes half way between top and bottom most node in the list
     *
     * @param selection a selection
     */
    public void centerVertical(Selection selection) {
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
    } // centerVertical

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
        Group m_group;

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
            m_group = new Group(shapes);
        }

        void undo() {
            if (m_nActionType == DELETE_ACTION) {
                Shape shape = XML2Shape(m_sXML);
                m_objects.add(m_nPosition, shape);
            } else { // m_nActionType == DELETE_SET_ACTION
                for (int i = 0; i < m_nPositions.size(); i++) {
                    int iShape = ((Integer) m_nPositions.get(i)).intValue();
                    Shape shape = (Shape) m_group.m_objects.get(i);
                    m_objects.add(iShape, shape);
                }
            }
        }

        void redo() {
            if (m_nActionType == DELETE_ACTION) {
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
                doit();
                return;
            }
            for (int i = 0; i < m_nNrPrimePositions; i++) {
            	Shape shape = m_objects.get(m_nPositions.get(i));
            	if (shape instanceof Ellipse) {
            		moveInputShapes((Ellipse) shape);
            	} else {
               		for (Ellipse ellipse : ((PluginShape)shape).m_inputs) {
               			moveInputShapes(ellipse);
                	}
            	}
            }
        }
        
        void moveInputShapes(PluginShape shape) {
        }
    	void moveInputShapes(Ellipse ellipse) {
    		for (Shape shape : m_objects) {
    			if (shape instanceof Arrow) {
    				Arrow arrow = (Arrow) shape;
    				if (arrow.m_sHeadID.equals(ellipse.m_id)) {
    					String sTailID = arrow.m_sTailID;
    					for (int i = 0; i < m_objects.size(); i++) {
    						Shape shape2 = m_objects.get(i); 
    						if (shape2.m_id.equals(sTailID)) {
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

    class GroupAction extends DeleteAction {
        GroupAction(List<Integer> selection) {
            super(selection);
            m_group.m_id = getNewID(null);
        }

        void undo() {
            m_objects.remove(m_objects.size() - 1);
            for (int i = 0; i < m_nPositions.size(); i++) {
                int iShape = ((Integer) m_nPositions.get(i)).intValue();
                Shape shape = (Shape) m_group.m_objects.get(i);
                m_objects.add(iShape, shape);
            }
        }

        void redo() {
            for (int i = m_nPositions.size() - 1; i >= 0; i--) {
                int iShape = ((Integer) m_nPositions.get(i)).intValue();
                m_objects.remove(iShape);
            }
            m_objects.add(m_group);
        }

    } // class GroupAction

    class UngroupAction extends UndoAction {

        Group m_group;
        int m_nGroupSize;

        UngroupAction(Selection selection) {
            super();
            m_nPosition = selection.getSingleSelection();
            m_group = (Group) m_objects.get(m_nPosition);
        } // c'tor

        int getGroupSize() {
            return m_group.m_objects.size();
        }

        void undo() {
            m_objects.add(m_nPosition, m_group);
            for (int i = 0; i < m_group.m_objects.size(); i++) {
                m_objects.remove(m_objects.size() - 1);
            }
        }

        void redo() {
            for (int i = 0; i < m_group.m_objects.size(); i++) {
                Shape shape = (Shape) m_group.m_objects.get(i);
                m_objects.add(shape);
            }
            m_objects.remove(m_nPosition);
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
        if (m_nSavedPointer > m_nCurrentEditAction) {
            m_nSavedPointer = -2;
        }
        m_undoStack.add(action);
        //m_sXMLStack.addElement(toXMLBIF03());
        m_nCurrentEditAction++;
    } // addUndoAction

    /**
     * remove all actions from the undo stack
     */
    public void clearUndoStack() {
        m_undoStack = new ArrayList<UndoAction>();
        m_nCurrentEditAction = -1;
        m_nSavedPointer = -1;
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
        UndoAction undoAction = (UndoAction) m_undoStack.get(m_nCurrentEditAction);
        undoAction.undo();
        adjustInputs();
        adjustArrows(true, m_objects);
        m_nCurrentEditAction--;
    } // undo

    public void redo() {
        if (!canRedo()) {
            return;
        }
        m_nCurrentEditAction++;
        UndoAction undoAction = (UndoAction) m_undoStack.get(m_nCurrentEditAction);
        undoAction.redo();
        adjustInputs();
        adjustArrows(true, m_objects);

    } // redo

    @SuppressWarnings("unchecked")
    Object getInput(beast.core.Plugin plugin, String sName) throws IllegalArgumentException, IllegalAccessException {
        //Field [] fields = plugin.getClass().getDeclaredFields();
        Field[] fields = plugin.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input input = (Input) fields[i].get(plugin);
                if (input.getName().equals(sName)) {
                    return input.get();
                }
            }
        }
        return null;
    }

    PluginShape getObjectWithLabel(String sLabel) {
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape) {
                if (shape.getLabel() == null) {
                    int h = 4;
                    h++;
                }
                if (shape.getLabel() != null && shape.getLabel().equals(sLabel)) {
                    return (PluginShape) shape;
                }
            }
        }
        return null;
    }

    String createLabel(Plugin p) {
        if (p.getID() == null || p.getID().equals("")) {
            String sStr = p.getClass().getName();
            return sStr.substring(sStr.lastIndexOf('.') + 1);
        }
        return p.getID();
    }

    String createID(PluginShape shape) {
        if (containsID(shape.getLabel(), m_objects, null)) {
            return getNewID(null);
        }
        return shape.getLabel();
    }

    final static int DX = 120;

    void addInput(PluginShape shape, Object o2, int nDepth, String sInput) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (o2 instanceof Plugin) {
            PluginShape shape2 = getObjectWithLabel(((Plugin) o2).getID());
            if (shape2 == null) {
                shape2 = new PluginShape((Plugin) o2, this);
                shape2.m_x = nDepth * DX;
                shape2.m_w = 80;
                //Random random = new Random();
                //shape2.m_y = random.nextInt(800);
                //shape2.m_h = 50;
                shape2.m_function = (Plugin) o2;
                shape2.setLabel(createLabel(((Plugin) o2)));
                shape2.m_id = createID(shape2);
                m_objects.add(shape2);
            }
            Arrow arrow = new Arrow(shape2, shape, sInput);
            arrow.m_id = getNewID(null);
            m_objects.add(arrow);
            process(shape2, nDepth);
        }
    }

    @SuppressWarnings("unchecked")
    void process(PluginShape shape, int nDepth) throws IllegalArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Plugin plugin = shape.m_function;
        Input[] sInputs = plugin.listInputs();
        for (int i = 0; i < sInputs.length; i++) {
            Object o = getInput(plugin, sInputs[i].getName());
            if (o != null) {
                if (o instanceof List) {
                    for (Object o2 : (List) o) {
                        addInput(shape, o2, nDepth + 1, sInputs[i].getName());
                    }
                } else if (o instanceof Plugin) {
                    addInput(shape, o, nDepth + 1, sInputs[i].getName());
//					PluginShape shape2 = getObjectWithLabel(((Plugin) o).getID());
//					if (shape2 == null) {
//						shape2 = new PluginShape((Plugin) o, this);
//						shape2.m_function = (Plugin) o;
//						shape2.setLabel(createLabel((Plugin) o));
//						shape2.m_id = createID(shape2);
//						m_objects.add(shape2);
//						addNewShape(shape2);
//					}
//					Arrow arrow = new Arrow(shape2, shape);
//					arrow.m_id = getNewID(null);
//					m_objects.add(arrow);
//					process(shape2);
                } else {
                    // it is a primitive type
                    String sValue = o + "";
                    Shape input = shape.getInput(sInputs[i].getName());
                    if (input != null) {
                        String sLabel = input.getLabel();
                        if (sLabel == null) {
                            int h = 4;
                            h++;
                        }
                        if (sValue == null) {
                            int h = 4;
                            h++;
                        }
                        if (sLabel.indexOf('=') < 0) {
                            input.setLabel(sLabel + "=" + sValue);
                        }
                    }
                }
            }
        }
    }

    void layout() {
        // first, reverse left to right order
        int nMaxX = 0;
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape) {
                nMaxX = Math.max(shape.m_x, nMaxX);
            }
        }
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape) {
                shape.m_x = nMaxX + DX - shape.m_x;
            }
        }
        // next, optimise top down order
        for (int iX = DX; iX < nMaxX + DX; iX += DX) {
            List<Shape> shapes = new ArrayList<Shape>();
            for (Shape shape : m_objects) {
                if (shape instanceof PluginShape && shape.m_x == iX) {
                    shapes.add(shape);
                }
            }
            int k = 1;
            for (Shape shape : shapes) {
                shape.m_y = k * 80;
                k++;
            }
        }
        // align to bottom
        int nMaxY = 0;
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape) {
                nMaxY = Math.max(shape.m_y, nMaxY);
            }
        }
        for (Shape shape : m_objects) {
            if (shape instanceof PluginShape) {
                shape.m_y = nMaxY + 20 - shape.m_y;
                ((PluginShape) shape).adjustInputs();
            }
        }
    }


    public void loadFile(String sFileName) {
        XMLParser parser = new XMLParser();
        try {
            m_mcmc = parser.parseFile(sFileName);
            PluginShape shape = new PluginShape(m_mcmc, this);
            shape.m_function = m_mcmc;
            shape.setLabel(createLabel(m_mcmc));
            shape.m_id = createID(shape);
            m_objects.add(shape);
            process(shape, 1);
            shape.m_x = DX;
            shape.m_w = 100;
            Random random = new Random();
            shape.m_y = random.nextInt(800);
            shape.m_h = 50;
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }

        layout();

        clearUndoStack();
        //adjustArrows();

        // set inputs of functions by looking at where the arrows are
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = m_objects.get(i);
            if (shape instanceof Arrow) {
                try {
                    ((Arrow) shape).setFunctionInput(m_objects, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        adjustArrows();
        moveArrowsToBack();

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

    static Shape parseNode(Node node, Document doc) {
        Shape shape = null;
//        if (node.getNodeName().equals("roundrectangle")) {
//            shape = new RoundRectangle(node, doc);
//        } else 
        if (node.getNodeName().equals("ellipse")) {
            shape = new Ellipse(node, doc);
//        } else if (node.getNodeName().equals("line")) {
//            shape = new Line(node, doc);
//        } else if (node.getNodeName().equals("rect")) {
//            shape = new Rect(node, doc);
//        } else if (node.getNodeName().equals("poly")) {
//            shape = new Poly(node, doc);
        } else if (node.getNodeName().equals("arrow")) {
            shape = new Arrow(node, doc);
//        } else if (node.getNodeName().equals("picture")) {
//            shape = new Rect(node, doc);
//        } else if (node.getNodeName().equals("group")) {
//            shape = new Group(node, doc);
        } else if (node.getNodeName().equals("gdx:function")) {
            shape = new PluginShape(node, doc);
        }
        return shape;
    } // parseNode

    public String toXML() {
        XMLProducer xmlProducer = new XMLProducer();
        return xmlProducer.toXML(m_mcmc);
//		StringBuffer sXML = new StringBuffer();
//		sXML.append("<doc name='" + m_sName + "'>\n");
//		for (int i = 0; i < m_objects.size(); i++) {
//			Shape shape = (Shape) m_objects.get(i);
//			sXML.append(shape.getXML());
//			sXML.append("\n");
//		}
//		sXML.append("</doc>");
//		return sXML.toString();
    } // toXML

    public String getHTMLMap() {
        StringBuffer sMAP = new StringBuffer();
        sMAP.append("<html><head><title>" + m_sName + "</title></head>\n<body>\n");
        sMAP.append("<MAP NAME=\"" + m_sName + "\">\n");
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = (Shape) m_objects.get(i);
            sMAP.append(shape.getHTMLMap());
        }
        sMAP.append("</MAP>\n");
        sMAP.append("<IMG SRC=\"" + m_sName + ".png\" ALT=\"" + m_sName + "\" BORDER=0 USEMAP=\"#" + m_sName + "\">\n");
        sMAP.append("</body>\n</html>");
        return sMAP.toString();
    }

    public String getPostScript() {
        StringBuffer sPostScript = new StringBuffer();
        for (int i = 0; i < m_objects.size(); i++) {
            Shape shape = (Shape) m_objects.get(i);
            sPostScript.append(shape.getPostScript());
        }
        return sPostScript.toString();
    }

    Shape findObjectWithID(String sID) {
        for (int i = 0; i < m_objects.size(); i++) {
            if (m_objects.get(i).m_id.equals(sID)) {
                return m_objects.get(i);
            }
        }
        return null;
    }


    public void relax() {

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
                String sID = arrow.m_tail.m_id;
                if (arrow.m_head instanceof Ellipse) {
                    String sID2 = ((Ellipse) arrow.m_head).m_function.m_id;
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
                Shape source = arrow.m_tail;
                int p1x = source.m_x + source.m_w / 2;
                int p1y = source.m_y + source.m_h / 2;
                if (arrow.m_head instanceof Ellipse) {
                    Shape target = ((Ellipse) arrow.m_head).m_function;
                    int p2x = target.m_x + target.m_w / 2;
                    int p2y = target.m_y + target.m_h / 2;

                    double vx = p1x - p2x;
                    double vy = p1y - p2y;
                    double len = Math.sqrt(vx * vx + vy * vy);

                    double desiredLen = 150;

                    // round from zero, if needed [zero would be Bad.].
                    len = (len == 0) ? .0001 : len;

                    double f = 1.0 / 3.0 * (desiredLen - len) / len;

                    int nDegree1 = degreeMap.get(source.m_id);//((PluginShape) source).getNrInputs();
                    int nDegree2 = degreeMap.get(target.m_id);//((PluginShape) target).getNrInputs();


                    f = f * Math.pow(0.99, (nDegree1 + nDegree2 - 2));


                    // the actual movement distance 'dx' is the force multiplied by the
                    // distance to go.
                    double dx = Math.min(f * vx, 3);
                    double dy = Math.min(f * vy, 3);
                    if (vx > -200 && vx < 0) {
                        dx = -dx;
                        //f *= Math.abs((vx+200))/40;
                    }
                    source.m_x = (int) Math.max(100, source.m_x + dx);
                    source.m_y = (int) Math.max(10, source.m_y + dy);
                    target.m_x = (int) Math.max(100, target.m_x - dx);
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
//			            double dlen = dx * dx + dy * dy;
//			            if (dlen > 0) {
//			                dlen = Math.sqrt(dlen) / 2;
//			                svd.repulsiondx += dx / dlen;
//			                svd.repulsiondy += dy / dlen;
//			            }
                    }
                }
                shape1.m_x = (int) Math.min(800, Math.max(10, shape1.m_x + dx));
                shape1.m_y = (int) Math.min(800, Math.max(10, shape1.m_y + dy));
//				shape1.m_x += dx;
//				shape1.m_y += dy;
            }
        }

        // Step 3: move dependent objects in place
        for (Shape shape : objects) {
            if (shape instanceof PluginShape) {
                ((PluginShape) shape).adjustInputs();
            }
        }
        adjustArrows();
    }


//	/** return list of labels of objects that contain an index **/
//	String [] getIndices() {
//		List<String> sIndices = new ArrayList<String>();
//		for (int i = 0; i < m_objects.size(); i++) {
//			Shape shape = m_objects.get(i);
//			if (shape instanceof Constant && ((Constant) shape).m_constant instanceof imp.Index) {
//				sIndices.add(shape.getLabel());
//			}
//		}
//		return sIndices.toArray(new String[0]);
//	}
//	/** return index of object represented by particular name **/
//	Index getIndex(String sName) {
//		List<String> sIndices = new ArrayList<String>();
//		for (int i = 0; i < m_objects.size(); i++) {
//			Shape shape = m_objects.get(i);
//			if (shape instanceof Constant && ((Constant) shape).m_constant instanceof imp.Index) {
//				if (shape.getLabel().equals(sName)) {
//					return ((Index)((Constant) shape).m_constant);
//				}
//			}
//		}
//		return null;
//	}
//
//	/** return index of object represented by particular name **/
//	String [] getTypes() {
//	//String [] sTypes = {"BOOL", "INT", "REAL", "CATEGORIAL", "RANKED", "STRING"};
//		List<String> sTypes = new ArrayList<String>();
//		sTypes.addElement("BOOL");
//		sTypes.addElement("INT");
//		sTypes.addElement("REAL");
//		sTypes.addElement("STRING");
//		for (int i = 0; i < m_objects.size(); i++) {
//			Shape shape = m_objects.get(i);
//			if (shape instanceof Constant && ((Constant) shape).m_constant instanceof imp.Index) {
//				sTypes.add(shape.getLabel());
//			}
//		}
//		return sTypes.toArray(new String[0]);
//	}
//
//	/** parse constant **/
//	beast.core.Plugin getConstant(Node node) {
//		try {
//			NodeList children = node.getChildNodes();
//			for(int iChild = 0; iChild < children.getLength(); iChild++) {
//				Node child = children.item(iChild);
//				if (child.getNodeType() == Node.ELEMENT_NODE) {
//					Object object = XMLParserHelper.parseNode(child, m_parserMap, m_indices, null, null, false);
//					if (object != null && object instanceof Index) {
//						Index index = (Index) object;
//						if (m_indices.get(index.getID()) != null) {
//							throw new XMLParserException(child, "Index defined more than once: id='" + index.getID() + "'");
//						}
//						m_indices.put(index.getID(), index);
//					}
//					if (object instanceof beast.core.Plugin) {
//						return (beast.core.Plugin) object;
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return null;
//	}
//
} // class Document
