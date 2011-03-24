package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.Aminoacid;
import beast.evolution.datatype.DataType;

@Description("WAG model of amino acid evolution by " +
		"S. Whelan and N. Goldman. 2000. Bioinformatics ?.")
public class WAG extends EmpiricalSubstitutionModel { 

	@Override
	double [][] getEmpiricalRates() {
		double[][] rate = new double[20][20];
		
		// Q matrix
		rate[0][1] = 0.610810; rate[0][2] = 0.569079; 
		rate[0][3] = 0.821500; rate[0][4] = 1.141050; 
		rate[0][5] = 1.011980; rate[0][6] = 1.756410; 
		rate[0][7] = 1.572160; rate[0][8] = 0.354813; 
		rate[0][9] = 0.219023; rate[0][10] = 0.443935; 
		rate[0][11] = 1.005440; rate[0][12] = 0.989475; 
		rate[0][13] = 0.233492; rate[0][14] = 1.594890; 
		rate[0][15] = 3.733380; rate[0][16] = 2.349220; 
		rate[0][17] = 0.125227; rate[0][18] = 0.268987; 
		rate[0][19] = 2.221870; 

		rate[1][2] = 0.711690; rate[1][3] = 0.165074; 
		rate[1][4] = 0.585809; rate[1][5] = 3.360330; 
		rate[1][6] = 0.488649; rate[1][7] = 0.650469; 
		rate[1][8] = 2.362040; rate[1][9] = 0.206722; 
		rate[1][10] = 0.551450; rate[1][11] = 5.925170; 
		rate[1][12] = 0.758446; rate[1][13] = 0.116821; 
		rate[1][14] = 0.753467; rate[1][15] = 1.357640; 
		rate[1][16] = 0.613776; rate[1][17] = 1.294610; 
		rate[1][18] = 0.423612; rate[1][19] = 0.280336; 

		rate[2][3] = 6.013660; rate[2][4] = 0.296524; 
		rate[2][5] = 1.716740; rate[2][6] = 1.056790; 
		rate[2][7] = 1.253910; rate[2][8] = 4.378930; 
		rate[2][9] = 0.615636; rate[2][10] = 0.147156; 
		rate[2][11] = 3.334390; rate[2][12] = 0.224747; 
		rate[2][13] = 0.110793; rate[2][14] = 0.217538; 
		rate[2][15] = 4.394450; rate[2][16] = 2.257930; 
		rate[2][17] = 0.078463; rate[2][18] = 1.208560; 
		rate[2][19] = 0.221176; 

		rate[3][4] = 0.033379; rate[3][5] = 0.691268; 
		rate[3][6] = 6.833400; rate[3][7] = 0.961142; 
		rate[3][8] = 1.032910; rate[3][9] = 0.043523; 
		rate[3][10] = 0.093930; rate[3][11] = 0.533362; 
		rate[3][12] = 0.116813; rate[3][13] = 0.052004; 
		rate[3][14] = 0.472601; rate[3][15] = 1.192810; 
		rate[3][16] = 0.417372; rate[3][17] = 0.146348; 
		rate[3][18] = 0.363243; rate[3][19] = 0.169417; 

		rate[4][5] = 0.109261; rate[4][6] = 0.023920; 
		rate[4][7] = 0.341086; rate[4][8] = 0.275403; 
		rate[4][9] = 0.189890; rate[4][10] = 0.428414; 
		rate[4][11] = 0.083649; rate[4][12] = 0.437393; 
		rate[4][13] = 0.441300; rate[4][14] = 0.122303; 
		rate[4][15] = 1.560590; rate[4][16] = 0.570186; 
		rate[4][17] = 0.795736; rate[4][18] = 0.604634; 
		rate[4][19] = 1.114570; 

		rate[5][6] = 6.048790; rate[5][7] = 0.366510; 
		rate[5][8] = 4.749460; rate[5][9] = 0.131046; 
		rate[5][10] = 0.964886; rate[5][11] = 4.308310; 
		rate[5][12] = 1.705070; rate[5][13] = 0.110744; 
		rate[5][14] = 1.036370; rate[5][15] = 1.141210; 
		rate[5][16] = 0.954144; rate[5][17] = 0.243615; 
		rate[5][18] = 0.252457; rate[5][19] = 0.333890; 

		rate[6][7] = 0.630832; rate[6][8] = 0.635025; 
		rate[6][9] = 0.141320; rate[6][10] = 0.172579; 
		rate[6][11] = 2.867580; rate[6][12] = 0.353912; 
		rate[6][13] = 0.092310; rate[6][14] = 0.755791; 
		rate[6][15] = 0.782467; rate[6][16] = 0.914814; 
		rate[6][17] = 0.172682; rate[6][18] = 0.217549; 
		rate[6][19] = 0.655045; 

		rate[7][8] = 0.276379; rate[7][9] = 0.034151; 
		rate[7][10] = 0.068651; rate[7][11] = 0.415992; 
		rate[7][12] = 0.194220; rate[7][13] = 0.055288; 
		rate[7][14] = 0.273149; rate[7][15] = 1.486700; 
		rate[7][16] = 0.251477; rate[7][17] = 0.374321; 
		rate[7][18] = 0.114187; rate[7][19] = 0.209108; 
		
		rate[8][9] = 0.152215; rate[8][10] = 0.555096; 
		rate[8][11] = 0.992083; rate[8][12] = 0.450867; 
		rate[8][13] = 0.756080; rate[8][14] = 0.771387; 
		rate[8][15] = 0.822459; rate[8][16] = 0.525511; 
		rate[8][17] = 0.289998; rate[8][18] = 4.290350; 
		rate[8][19] = 0.131869; 

		rate[9][10] = 3.517820; rate[9][11] = 0.360574; 
		rate[9][12] = 4.714220; rate[9][13] = 1.177640; 
		rate[9][14] = 0.111502; rate[9][15] = 0.353443; 
		rate[9][16] = 1.615050; rate[9][17] = 0.234326; 
		rate[9][18] = 0.468951; rate[9][19] = 8.659740; 
		
		rate[10][11] = 0.287583; rate[10][12] = 5.375250; 
		rate[10][13] = 2.348200; rate[10][14] = 0.462018; 
		rate[10][15] = 0.382421; rate[10][16] = 0.364222; 
		rate[10][17] = 0.740259; rate[10][18] = 0.443205; 
		rate[10][19] = 1.997370; 
		
		rate[11][12] = 1.032220; rate[11][13] = 0.098843; 
		rate[11][14] = 0.619503; rate[11][15] = 1.073780; 
		rate[11][16] = 1.537920; rate[11][17] = 0.152232; 
		rate[11][18] = 0.147411; rate[11][19] = 0.342012; 
		
		rate[12][13] = 1.320870; rate[12][14] = 0.194864; 
		rate[12][15] = 0.556353; rate[12][16] = 1.681970; 
		rate[12][17] = 0.570369; rate[12][18] = 0.473810; 
		rate[12][19] = 2.282020; 
		
		rate[13][14] = 0.179896; rate[13][15] = 0.606814; 
		rate[13][16] = 0.191467; rate[13][17] = 1.699780; 
		rate[13][18] = 7.154480; rate[13][19] = 0.725096; 
		
		rate[14][15] = 1.786490; rate[14][16] = 0.885349; 
		rate[14][17] = 0.156619; rate[14][18] = 0.239607; 
		rate[14][19] = 0.351250; 
		
		rate[15][16] = 4.847130; rate[15][17] = 0.578784; 
		rate[15][18] = 0.872519; rate[15][19] = 0.258861; 
		
		rate[16][17] = 0.126678; rate[16][18] = 0.325490; 
		rate[16][19] = 1.547670; 
		
		rate[17][18] = 2.763540; rate[17][19] = 0.409817; 
		
		rate[18][19] = 0.347826;
		
		return rate;
	}
	
	@Override
	public double [] getEmpiricalFrequencies() {
		double[] f = new double[20];
		f[0] = 0.0866;
		f[1] = 0.0440;
		f[2] = 0.0391;
		f[3] = 0.0570;
		f[4] = 0.0193;
		f[5] = 0.0367;
		f[6] = 0.0581;
		f[7] = 0.0833;
		f[8] = 0.0244;
		f[9] = 0.0485;
		f[10] = 0.0862;
		f[11] = 0.0620;
		f[12] = 0.0195;
		f[13] = 0.0384;
		f[14] = 0.0458;
		f[15] = 0.0695;
		f[16] = 0.0610;
		f[17] = 0.0144;
		f[18] = 0.0353;
		f[19] = 0.0709;
		return f;
	}

	@Override
	public int [] getEncodingOrder() {
		Aminoacid dataType = new Aminoacid();
		String sCodeMap = dataType.getCodeMap();
		int [] nCodeMap = new int[dataType.getStateCount()];
		String sEncoding = "ARNDCQEGHILKMFPSTWYV";
		for (int i = 0; i < dataType.getStateCount(); i++) {
			nCodeMap[i] = sEncoding.indexOf(sCodeMap.charAt(i));
		}
		return nCodeMap;
	}

	@Override
	public boolean canHandleDataType(DataType dataType) throws Exception {
		if (dataType instanceof Aminoacid) {
			return true;
		}
		throw new Exception("Can only handle amino acid data");
	}
} // class WAG
