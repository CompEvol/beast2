/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package beast.util;

import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import java.io.PrintStream;
import java.util.List;

/**
 * Class for producing NEXUS files.
 * 
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class NexusWriter {
    
    /**
     * Write an alignment and/or one or more trees to the provided print stream
     * in Nexus format.
     * 
     * @param alignment Alignment to write (may be null)
     * @param trees Zero or more trees with taxa corresponding to alignment. (May be null)
     * @param pstream Print stream where output is sent
     * @throws java.lang.Exception
     */
    public static void write(Alignment alignment, List<Tree> trees,
            PrintStream pstream) throws Exception {
        
        pstream.println("#NEXUS");

        List<String> taxaNames = null;
        if (alignment != null) {
            taxaNames = alignment.getTaxaNames();
        } else {
            if (trees != null && !trees.isEmpty())
                taxaNames = trees.get(0).getTaxonset().asStringList();
        }
        
        if (taxaNames == null)
            return;
        
        // Construct space-delimited list of taxon names
        StringBuilder sb = new StringBuilder();
        boolean first=true;
        for (String taxonName : taxaNames) {
            if (first)
                first = false;
            else
                sb.append(" ");
            sb.append(taxonName);
        }
        String taxaNamesString = sb.toString();
        
        // Taxa block
        pstream.println("Begin taxa;");
        pstream.format("\tdimensions ntax=%d;\n", taxaNames.size());
        pstream.format("\ttaxLabels %s;\n", taxaNamesString);
        pstream.println("End;\n");

        // Character block
        if (alignment != null) {
            pstream.println("Begin characters;");
            pstream.format("\tdimensions nchar=%d;\n",
                    alignment.getSiteCount());
            
            // Assumes BEAST sequence data types map directly
            // onto nexus data types.  No doubt a bad idea in general...
            pstream.format("\tformat datatype=%s;\n",
                    alignment.getDataType().getDescription());
            
            pstream.println("\tmatrix");
            for (int i=0; i<taxaNames.size(); i++) {
                String taxonName = alignment.getTaxaNames().get(i);
                String sequence = alignment.getDataType().state2string(
                        alignment.getCounts().get(i));
                pstream.format("\t\t%s %s", taxonName, sequence);
                if (i<taxaNames.size()-1)
                    pstream.println();
                else
                    pstream.println(";");
            }
            
            pstream.println("End;");
        }
        
        // Tree block
        if (trees != null) {
            pstream.println("Begin trees;");
            
            pstream.println("\ttranslate");
            for (int i=0; i<taxaNames.size(); i++) {
                pstream.format("\t\t%d %s", i, taxaNames.get(i));
                if (i<taxaNames.size()-1)
                    pstream.println(",");
                else
                    pstream.println(";");
            }
            
            for (int i=0; i<trees.size(); i++) {
                Tree tree = trees.get(i);
                pstream.format("\ttree TREE_%d = %s;\n", i+1, tree.toString());
            }
            
            pstream.println("End;");
        }
    }
    
}
