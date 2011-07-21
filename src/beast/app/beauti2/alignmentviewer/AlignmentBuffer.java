package beast.app.beauti2.alignmentviewer;

/**
 * @author Andrew Rambaut
 * @version $Id: AlignmentBuffer.java,v 1.1 2005/11/01 23:52:04 rambaut Exp $
 */
public interface AlignmentBuffer {

    int getSequenceCount();
    int getSiteCount();

    String getTaxonLabel(int i);

    String[] getStateTable();

    void getStates(int sequenceIndex, int fromSite, int toSite, byte[] states);

    void addAlignmentBufferListener(AlignmentBufferListener listener);

}
