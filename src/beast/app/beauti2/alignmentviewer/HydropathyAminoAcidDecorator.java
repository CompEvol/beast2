package beast.app.beauti2.alignmentviewer;

import jebl.evolution.sequences.AminoAcids;

import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: HydropathyAminoAcidDecorator.java,v 1.1 2005/11/11 16:40:41 rambaut Exp $
 */
public class HydropathyAminoAcidDecorator implements StateDecorator {
    Paint[] paints = new Paint[AminoAcids.getStateCount()];

    public HydropathyAminoAcidDecorator() {
        // Hydropathic
        paints[AminoAcids.I_STATE.getIndex()] =
        paints[AminoAcids.V_STATE.getIndex()] =
        paints[AminoAcids.L_STATE.getIndex()] =
        paints[AminoAcids.F_STATE.getIndex()] =
        paints[AminoAcids.C_STATE.getIndex()] =
        paints[AminoAcids.M_STATE.getIndex()] =
        paints[AminoAcids.A_STATE.getIndex()] =
        paints[AminoAcids.W_STATE.getIndex()] = new Color(51, 102, 255);

        // Neutral
        paints[AminoAcids.G_STATE.getIndex()] =
        paints[AminoAcids.T_STATE.getIndex()] =
        paints[AminoAcids.S_STATE.getIndex()] =
        paints[AminoAcids.Y_STATE.getIndex()] =
        paints[AminoAcids.P_STATE.getIndex()] =
        paints[AminoAcids.H_STATE.getIndex()] = new Color(255, 255, 0);

        // Hydrophilic
        paints[AminoAcids.D_STATE.getIndex()] =
        paints[AminoAcids.E_STATE.getIndex()] =
        paints[AminoAcids.K_STATE.getIndex()] =
        paints[AminoAcids.N_STATE.getIndex()] =
        paints[AminoAcids.Q_STATE.getIndex()] =
        paints[AminoAcids.R_STATE.getIndex()] = new Color(230, 6, 6);

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
