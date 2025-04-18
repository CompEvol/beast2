package beast.base.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Interface for dealing with citations, in particular Citation annotations
 * This used to be part of BEASTInterface, but now can be used for non-BEASTInterface classes
 * as well (e.g. TopologySettingService implementations for TreeAnnotator).
 */
public interface Citable {

    /**
     * Deprecated: use getCitationList() instead to allow multiple citations, not just the first one
     */
	@Deprecated
    default Citation getCitation() {
        final Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
                return (Citation) annotation;
            }
            if (annotation instanceof Citation.Citations) {
                return ((Citation.Citations) annotation).value()[0];
                // TODO: this ignores other citations
            }
        }
        return null;
    } // getCitation

    /**
     * @return array of @Citation annotations for this class
     * or empty list if there are no citations
     **/
    default List<Citation> getCitationList() {
        final Annotation[] classAnnotations = this.getClass().getAnnotations();
        List<Citation> citations = new ArrayList<>();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
            	citations.add((Citation) annotation);
            }
            if (annotation instanceof Citation.Citations) {
            	for (Citation citation : ((Citation.Citations) annotation).value()) {
            		citations.add(citation);
            	}
            }
        }
       	return citations;
    } // getCitationList

    /**
     * @return references for this object based on Citation annotations
     */
    default String getCitations() {
        final StringBuilder buf = new StringBuilder();
    	for (Citation citation : getCitationList()) {
            // there is actually a citation to add
            buf.append("\n");
            buf.append(citation.value());
            buf.append("\n");
        }
        return buf.toString();
    } // getCitations


}
