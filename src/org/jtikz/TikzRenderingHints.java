package org.jtikz;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Alexei Drummond
 */
public class TikzRenderingHints extends RenderingHints {

    public TikzRenderingHints(Map<Key, ?> keyMap) {
        super(keyMap);
    }

    /**
     * This key determines how TikzGraphics2D anchors the nodes created by drawString method.
     */
    public static final Key KEY_NODE_ANCHOR ;

    static {
        KEY_NODE_ANCHOR = new Key(667) {
            @Override
            public boolean isCompatibleValue(Object o) {
                return Arrays.asList(VALUES_NODE_ANCHOR).contains(o);
            }
        };
    }

    public static final Object VALUE_CENTER = "center";
    public static final Object VALUE_MID = "mid";
    public static final Object VALUE_BASE = "base";
    public static final Object VALUE_NORTH = "north";
    public static final Object VALUE_SOUTH = "south";
    public static final Object VALUE_WEST = "west";
    public static final Object VALUE_EAST = "east";
    public static final Object VALUE_NORTH_WEST = "north west";
    public static final Object VALUE_NORTH_EAST = "north east";
    public static final Object VALUE_SOUTH_EAST = "south east";
    public static final Object VALUE_SOUTH_WEST = "south west";

    public static final Object[] VALUES_NODE_ANCHOR = {
        VALUE_CENTER, VALUE_MID, VALUE_BASE, VALUE_NORTH, VALUE_SOUTH, VALUE_WEST, VALUE_EAST, VALUE_NORTH_WEST, VALUE_NORTH_EAST, VALUE_SOUTH_EAST, VALUE_SOUTH_WEST
    };
    

}
