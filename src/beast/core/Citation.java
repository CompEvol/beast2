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

import java.lang.annotation.*;

/**
 * This is an annotation that can be used to add a reference
 * to a class.
 * <p/>
 * Example: @Citation("Darwin & Wallace (1858) 'On the Tendency
 * of Species to form Varieties and on the Perpetuation of Varieties
 * and Species by Natural Means of Selection.' Linnean Society")
 * just before class declarations of plug-ins. Applications
 * like DocMaker then can pick it up through introspection.
 * <p/>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Citation {

    /**
     * @return the citation for the class
     */
    String value();

    String DOI() default "";

}