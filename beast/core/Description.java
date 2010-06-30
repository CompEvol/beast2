/*
* File Description.java
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

package beast.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation that can be used to both
 * o document a class at the top of the code and
 * o create user documentation for XML Beast files.
 * <p/>
 * The idea is to add @Description("bla bla bla")
 * just before class declarations of plug-ins. Applications
 * like DocMaker then can pick it up through introspection.
 * <p/>
 * To indicate that the description applies to all derived
 * classes, the isInheritable flag is set true by default.
 * This does not always apply, for instance when the description
 * contains text like "Should not be used directly, but by
 * implementations".
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {

    /**
     * @return the description of the class
     */
    String value();

    /**
     * @return true to indicate this description applies to all its inherited classes as well, false otherwise
     */
    boolean isInheritable() default true;
}
