package beast.app.beauti2.alignmentviewer;

import jebl.evolution.sequences.AminoAcids;

import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: VolumeAminoAcidDecorator.java,v 1.1 2005/11/11 16:40:41 rambaut Exp $
 */
public class VolumeAminoAcidDecorator implements StateDecorator {
    Paint[] paints = new Paint[AminoAcids.getStateCount()];

    public VolumeAminoAcidDecorator() {
        // 60-90
        paints[AminoAcids.G_STATE.getIndex()] =
                paints[AminoAcids.A_STATE.getIndex()] =
                        paints[AminoAcids.S_STATE.getIndex()] =
                                new Color(255, 153, 153);

        // 108-117
        paints[AminoAcids.C_STATE.getIndex()] =
                paints[AminoAcids.D_STATE.getIndex()] =
                        paints[AminoAcids.P_STATE.getIndex()] =
                                paints[AminoAcids.N_STATE.getIndex()] =
                                        paints[AminoAcids.T_STATE.getIndex()] =
                                                new Color(230, 6, 6);

        // 138-154
        paints[AminoAcids.E_STATE.getIndex()] =
                paints[AminoAcids.V_STATE.getIndex()] =
                        paints[AminoAcids.Q_STATE.getIndex()] =
                                paints[AminoAcids.H_STATE.getIndex()] =
                                        new Color(255, 255, 0);

        // 162-174
        paints[AminoAcids.M_STATE.getIndex()] =
                paints[AminoAcids.I_STATE.getIndex()] =
                        paints[AminoAcids.L_STATE.getIndex()] =
                                paints[AminoAcids.K_STATE.getIndex()] =
                                        paints[AminoAcids.R_STATE.getIndex()] =
                                                new Color(153, 204, 255);
        // 189-228
        paints[AminoAcids.F_STATE.getIndex()] =
                paints[AminoAcids.Y_STATE.getIndex()] =
                        paints[AminoAcids.W_STATE.getIndex()] =
                                new Color(51, 102, 255);

        paints[AminoAcids.B_STATE.getIndex()] = Color.DARK_GRAY;
        paints[AminoAcids.Z_STATE.getIndex()] = Color.DARK_GRAY;
        paints[AminoAcids.X_STATE.getIndex()] = Color.GRAY;
        paints[AminoAcids.UNKNOWN_STATE.getIndex()] = Color.GRAY;
        paints[AminoAcids.STOP_STATE.getIndex()] = Color.GRAY;
        paints[AminoAcids.GAP_STATE.getIndex()] = Color.GRAY;
    };

    public Paint getStatePaint(int stateIndex) {
        return paints[stateIndex];
    }
}
