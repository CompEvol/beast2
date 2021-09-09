package beast.evolution.substitutionmodel;

import beast.base.Description;
import beast.evolution.datatype.Aminoacid;
import beast.evolution.datatype.DataType;

@Description(" CPREV 45 model of amino acid evolution " +
        " Adachi, J., P.J. Waddell, W. Martin, and M. Hasegawa. 2000. JME 50:348-358")
public class CPREV extends EmpiricalSubstitutionModel {


    @Override
	public
    double[][] getEmpiricalRates() {
        double[][] rate = new double[20][20];

        // Q matrix from Beast 1
        rate[0][1] = 105;
        rate[0][2] = 227;
        rate[0][3] = 175;
        rate[0][4] = 669;
        rate[0][5] = 157;
        rate[0][6] = 499;
        rate[0][7] = 665;
        rate[0][8] = 66;
        rate[0][9] = 145;
        rate[0][10] = 197;
        rate[0][11] = 236;
        rate[0][12] = 185;
        rate[0][13] = 68;
        rate[0][14] = 490;
        rate[0][15] = 2440;
        rate[0][16] = 1340;
        rate[0][17] = 14;
        rate[0][18] = 56;
        rate[0][19] = 968;

        rate[1][2] = 357;
        rate[1][3] = 43;
        rate[1][4] = 823;
        rate[1][5] = 1745;
        rate[1][6] = 152;
        rate[1][7] = 243;
        rate[1][8] = 715;
        rate[1][9] = 136;
        rate[1][10] = 203;
        rate[1][11] = 4482;
        rate[1][12] = 125;
        rate[1][13] = 53;
        rate[1][14] = 87;
        rate[1][15] = 385;
        rate[1][16] = 314;
        rate[1][17] = 230;
        rate[1][18] = 323;
        rate[1][19] = 92;

        rate[2][3] = 4435;
        rate[2][4] = 538;
        rate[2][5] = 768;
        rate[2][6] = 1055;
        rate[2][7] = 653;
        rate[2][8] = 1405;
        rate[2][9] = 168;
        rate[2][10] = 113;
        rate[2][11] = 2430;
        rate[2][12] = 61;
        rate[2][13] = 97;
        rate[2][14] = 173;
        rate[2][15] = 2085;
        rate[2][16] = 1393;
        rate[2][17] = 40;
        rate[2][18] = 754;
        rate[2][19] = 83;

        rate[3][4] = 10;
        rate[3][5] = 400;
        rate[3][6] = 3691;
        rate[3][7] = 431;
        rate[3][8] = 331;
        rate[3][9] = 10;
        rate[3][10] = 10;
        rate[3][11] = 412;
        rate[3][12] = 47;
        rate[3][13] = 22;
        rate[3][14] = 170;
        rate[3][15] = 590;
        rate[3][16] = 266;
        rate[3][17] = 18;
        rate[3][18] = 281;
        rate[3][19] = 75;

        rate[4][5] = 10;
        rate[4][6] = 10;
        rate[4][7] = 303;
        rate[4][8] = 441;
        rate[4][9] = 280;
        rate[4][10] = 396;
        rate[4][11] = 48;
        rate[4][12] = 159;
        rate[4][13] = 726;
        rate[4][14] = 285;
        rate[4][15] = 2331;
        rate[4][16] = 576;
        rate[4][17] = 435;
        rate[4][18] = 1466;
        rate[4][19] = 592;

        rate[5][6] = 3122;
        rate[5][7] = 133;
        rate[5][8] = 1269;
        rate[5][9] = 92;
        rate[5][10] = 286;
        rate[5][11] = 3313;
        rate[5][12] = 202;
        rate[5][13] = 10;
        rate[5][14] = 323;
        rate[5][15] = 396;
        rate[5][16] = 241;
        rate[5][17] = 53;
        rate[5][18] = 391;
        rate[5][19] = 54;

        rate[6][7] = 379;
        rate[6][8] = 162;
        rate[6][9] = 148;
        rate[6][10] = 82;
        rate[6][11] = 2629;
        rate[6][12] = 113;
        rate[6][13] = 145;
        rate[6][14] = 185;
        rate[6][15] = 568;
        rate[6][16] = 369;
        rate[6][17] = 63;
        rate[6][18] = 142;
        rate[6][19] = 200;

        rate[7][8] = 19;
        rate[7][9] = 40;
        rate[7][10] = 20;
        rate[7][11] = 263;
        rate[7][12] = 21;
        rate[7][13] = 25;
        rate[7][14] = 28;
        rate[7][15] = 691;
        rate[7][16] = 92;
        rate[7][17] = 82;
        rate[7][18] = 10;
        rate[7][19] = 91;

        rate[8][9] = 29;
        rate[8][10] = 66;
        rate[8][11] = 305;
        rate[8][12] = 10;
        rate[8][13] = 127;
        rate[8][14] = 152;
        rate[8][15] = 303;
        rate[8][16] = 32;
        rate[8][17] = 69;
        rate[8][18] = 1971;
        rate[8][19] = 25;

        rate[9][10] = 1745;
        rate[9][11] = 345;
        rate[9][12] = 1772;
        rate[9][13] = 454;
        rate[9][14] = 117;
        rate[9][15] = 216;
        rate[9][16] = 1040;
        rate[9][17] = 42;
        rate[9][18] = 89;
        rate[9][19] = 4797;

        rate[10][11] = 218;
        rate[10][12] = 1351;
        rate[10][13] = 1268;
        rate[10][14] = 219;
        rate[10][15] = 516;
        rate[10][16] = 156;
        rate[10][17] = 159;
        rate[10][18] = 189;
        rate[10][19] = 865;

        rate[11][12] = 193;
        rate[11][13] = 72;
        rate[11][14] = 302;
        rate[11][15] = 868;
        rate[11][16] = 918;
        rate[11][17] = 10;
        rate[11][18] = 247;
        rate[11][19] = 249;

        rate[12][13] = 327;
        rate[12][14] = 100;
        rate[12][15] = 93;
        rate[12][16] = 645;
        rate[12][17] = 86;
        rate[12][18] = 215;
        rate[12][19] = 475;

        rate[13][14] = 43;
        rate[13][15] = 487;
        rate[13][16] = 148;
        rate[13][17] = 468;
        rate[13][18] = 2370;
        rate[13][19] = 317;

        rate[14][15] = 1202;
        rate[14][16] = 260;
        rate[14][17] = 49;
        rate[14][18] = 97;
        rate[14][19] = 122;

        rate[15][16] = 2151;
        rate[15][17] = 73;
        rate[15][18] = 522;
        rate[15][19] = 167;

        rate[16][17] = 29;
        rate[16][18] = 71;
        rate[16][19] = 760;

        rate[17][18] = 346;
        rate[17][19] = 10;

        rate[18][19] = 119;

        return rate;
    }

    @Override
    public double[] getEmpiricalFrequencies() {
        double[] f = new double[20];
        f[0] = 0.076;
        f[1] = 0.062;
        f[2] = 0.041;
        f[3] = 0.037;
        f[4] = 0.009;
        f[5] = 0.038;
        f[6] = 0.049;
        f[7] = 0.084;
        f[8] = 0.025;
        f[9] = 0.081;
        f[10] = 0.101;
        f[11] = 0.050;
        f[12] = 0.022;
        f[13] = 0.051;
        f[14] = 0.043;
        f[15] = 0.062;
        f[16] = 0.054;
        f[17] = 0.018;
        f[18] = 0.031;
        f[19] = 0.066;
        return f;
    }

    @Override
    public int[] getEncodingOrder() {
        Aminoacid dataType = new Aminoacid();
        String codeMap = dataType.getCodeMap();
        int[] codeMapNrs = new int[dataType.getStateCount()];
        String encoding = "ARNDCQEGHILKMFPSTWYV";
        for (int i = 0; i < dataType.getStateCount(); i++) {
            codeMapNrs[i] = encoding.indexOf(codeMap.charAt(i));
        }
        return codeMapNrs;
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Aminoacid;
    }
} // class WAG
