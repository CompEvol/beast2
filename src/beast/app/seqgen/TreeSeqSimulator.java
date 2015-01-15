package beast.app.seqgen;

import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.tree.RandomTree;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.coalescent.ConstantPopulation;
import beast.util.XMLProducer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * given a population size to simulate a random tree,
 * and use SequenceSimulator to simulate sequence from that tree.
 * only work for constant coalescent
 *
 * @author Walter Xie
 */
public class TreeSeqSimulator {
    private final double popSize;
    private final String[] taxa;
    private final int[] dates;

    public TreeSeqSimulator(double popSize, String[] taxa, int[] dates) {
        this.popSize = popSize;
        this.taxa = taxa;
        this.dates = dates;
    }

    public Alignment getDummyAlignment(String[] taxa) throws Exception {
        List<Sequence> seqList = new ArrayList<Sequence>();

        for (int i=0; i<taxa.length; i++) {
            seqList.add(new Sequence(taxa[i], "?"));
        }

        return new Alignment(seqList, 4, "nucleotide");
    }

    public Tree getRandomTree(double popSize, Alignment dummyAlg, String[] taxa, int[] dates) throws Exception {
        TaxonSet taxonSet = new TaxonSet(dummyAlg);
        StringBuilder traitSB = new StringBuilder();
        for (int i=0; i<taxa.length; i++) {
            if (i>0)
                traitSB.append(",");
            traitSB.append(taxa[i]).append("=").append(dates[i]);
        }

        TraitSet timeTrait = new TraitSet();
        timeTrait.initByName(
                "traitname", "date",
                "taxa", taxonSet,
                "value", traitSB.toString());

        ConstantPopulation popFunc = new ConstantPopulation();
        popFunc.initByName("popSize", new RealParameter(Double.toString(popSize)));

        // Create RandomTree and TreeInterval instances
        RandomTree tree = new RandomTree();
//        TreeIntervals intervals = new TreeIntervals();

        tree.initByName(
                "taxa", dummyAlg,
                "populationModel", popFunc,
                "trait", timeTrait);

//        intervals.initByName("tree", tree);

        return tree;
    }

    public Alignment getSimulatedAlignment(Alignment dummyAlg, Tree tree, SiteModel siteModel, BranchRateModel branchRateModel) throws Exception {
        // feed to sequence simulator and generate leaves
        SequenceSimulator treeSimulator = new SequenceSimulator();
        treeSimulator.init(dummyAlg, tree, siteModel, branchRateModel);

        return treeSimulator.simulate();
    }

    // hard code
    public SiteModel getSiteModel(Alignment dummyAlg, double kappa, double gammaShape) throws Exception {
        Frequencies freqs = new Frequencies();
        freqs.initByName("data", dummyAlg);

        HKY hky = new HKY();
        hky.initByName("kappa", Double.toString(kappa), "frequencies", freqs);

        SiteModel siteModel = new SiteModel();
        siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
                "shape", Double.toString(gammaShape),
                "proportionInvariant", "0.0",
                "substModel", hky);

        return siteModel;
    }

    // hard code
    public BranchRateModel getBranchModel(double clockRate) throws Exception {
        StrictClockModel strictClockModel = new StrictClockModel();
        strictClockModel.initByName("clock.rate", Double.toString(clockRate));

        return strictClockModel;
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        //403gene6t0
//    double popSize = ;
//    String[] taxa = new String[]{"CA1.T401_1", "CA3.T415_1", "CB121212.12_1", "CB070117.03_1", "CB070121.08_1", "CB070121.16_1"};
//    int[] dates = new int[]{0, 0, 5388, 6220, 25600, 30000};

        //403gene6t716
        double popSize = 4.2299E5;
        String[] taxa = new String[]{"CB111229.15_1", "CB130106.26_1", "CB121212.12_1", "CB070117.03_1", "CB070121.08_1", "CB070121.16_1"};
        int[] dates = new int[]{716, 1937, 5388, 6220, 25600, 30000};
        double clockRate = 7.2028E-9;
        double kappa = 5.2931;
        double gammaShape = 2.4402E-2;

        String[] ids = new String[]{
                "Pad_R000231_Scaffold10","Pad_R000245_Scaffold10","Pad_R000311_Scaffold10","Pad_R000346_Scaffold10","Pad_R000382_Scaffold100","Pad_R000387_Scaffold100","Pad_R000476_Scaffold101","Pad_R000542_Scaffold103","Pad_R000544_Scaffold103","Pad_R000554_Scaffold103","Pad_R000587_Scaffold1041","Pad_R000592_Scaffold105","Pad_R000599_Scaffold105","Pad_R000611_Scaffold105","Pad_R000830_Scaffold109","Pad_R000995_Scaffold113","Pad_R001098_Scaffold113","Pad_R001280_Scaffold116","Pad_R001327_Scaffold117","Pad_R001370_Scaffold117","Pad_R001385_Scaffold118","Pad_R001410_Scaffold12","Pad_R001431_Scaffold120","Pad_R001485_Scaffold120","Pad_R001495_Scaffold120","Pad_R001499_Scaffold120","Pad_R001503_Scaffold120","Pad_R001509_Scaffold120","Pad_R001513_Scaffold120","Pad_R001518_Scaffold120","Pad_R001546_Scaffold120","Pad_R001556_Scaffold120","Pad_R001569_Scaffold120","Pad_R001583_Scaffold120","Pad_R001587_Scaffold120","Pad_R001597_Scaffold121","Pad_R001610_Scaffold121","Pad_R001615_Scaffold121","Pad_R001623_Scaffold121","Pad_R001678_Scaffold121","Pad_R001689_Scaffold121","Pad_R001694_Scaffold121","Pad_R001803_Scaffold123","Pad_R001806_Scaffold123","Pad_R001807_Scaffold123","Pad_R001815_Scaffold123","Pad_R001828_Scaffold124","Pad_R001831_Scaffold124","Pad_R001835_Scaffold124","Pad_R001846_Scaffold124","Pad_R001854_Scaffold124","Pad_R001868_Scaffold124","Pad_R001886_Scaffold124","Pad_R001892_Scaffold124","Pad_R001915_Scaffold126","Pad_R001936_Scaffold126","Pad_R002193_Scaffold130","Pad_R002335_Scaffold137","Pad_R002340_Scaffold137","Pad_R002387_Scaffold139","Pad_R002430_Scaffold14","Pad_R002440_Scaffold14","Pad_R002449_Scaffold14","Pad_R002466_Scaffold14","Pad_R002495_Scaffold14","Pad_R002648_Scaffold141","Pad_R002712_Scaffold144","Pad_R002727_Scaffold145","Pad_R002814_Scaffold149","Pad_R002833_Scaffold149","Pad_R002850_Scaffold15","Pad_R002976_Scaffold154","Pad_R003012_Scaffold154","Pad_R003030_Scaffold154","Pad_R003078_Scaffold155","Pad_R003091_Scaffold155","Pad_R003121_Scaffold156","Pad_R003125_Scaffold156","Pad_R003145_Scaffold156","Pad_R003263_Scaffold160","Pad_R003293_Scaffold160","Pad_R003302_Scaffold160","Pad_R003316_Scaffold160","Pad_R003322_Scaffold160","Pad_R003365_Scaffold160","Pad_R003411_Scaffold160","Pad_R003414_Scaffold160","Pad_R003450_Scaffold164","Pad_R003453_Scaffold164","Pad_R003486_Scaffold166","Pad_R003499_Scaffold166","Pad_R003501_Scaffold166","Pad_R003697_Scaffold170","Pad_R003853_Scaffold172","Pad_R003882_Scaffold172","Pad_R003903_Scaffold172","Pad_R003918_Scaffold173","Pad_R003945_Scaffold174","Pad_R003991_Scaffold174","Pad_R003996_Scaffold174","Pad_R004044_Scaffold175","Pad_R004052_Scaffold178","Pad_R004249_Scaffold183","Pad_R004305_Scaffold186","Pad_R004320_Scaffold186","Pad_R004357_Scaffold186","Pad_R004358_Scaffold186","Pad_R004533_Scaffold190","Pad_R004548_Scaffold190","Pad_R004564_Scaffold191","Pad_R004589_Scaffold194","Pad_R004638_Scaffold197","Pad_R004642_Scaffold197","Pad_R004658_Scaffold197","Pad_R004709_Scaffold197","Pad_R004765_Scaffold202","Pad_R004779_Scaffold202","Pad_R004802_Scaffold203","Pad_R004811_Scaffold203","Pad_R004812_Scaffold203","Pad_R004813_Scaffold203","Pad_R004872_Scaffold207","Pad_R004986_Scaffold21","Pad_R004990_Scaffold21","Pad_R004995_Scaffold21","Pad_R005021_Scaffold21","Pad_R005128_Scaffold215","Pad_R005159_Scaffold216","Pad_R005197_Scaffold218","Pad_R005217_Scaffold218","Pad_R005250_Scaffold218","Pad_R005263_Scaffold22","Pad_R005378_Scaffold222","Pad_R005382_Scaffold222","Pad_R005390_Scaffold222","Pad_R005402_Scaffold222","Pad_R005428_Scaffold223","Pad_R005475_Scaffold225","Pad_R005524_Scaffold226","Pad_R005538_Scaffold227","Pad_R005543_Scaffold227","Pad_R005573_Scaffold227","Pad_R005580_Scaffold227","Pad_R005584_Scaffold227","Pad_R005585_Scaffold227","Pad_R005606_Scaffold23","Pad_R005711_Scaffold238","Pad_R005761_Scaffold24","Pad_R005791_Scaffold24","Pad_R005798_Scaffold24","Pad_R005888_Scaffold242","Pad_R005892_Scaffold242","Pad_R005988_Scaffold244","Pad_R005995_Scaffold244","Pad_R006001_Scaffold244","Pad_R006024_Scaffold244","Pad_R006099_Scaffold25","Pad_R006104_Scaffold25","Pad_R006131_Scaffold25","Pad_R006132_Scaffold25","Pad_R006164_Scaffold252","Pad_R006169_Scaffold252","Pad_R006190_Scaffold256","Pad_R006210_Scaffold257","Pad_R006228_Scaffold259","Pad_R006355_Scaffold260","Pad_R006388_Scaffold262","Pad_R006415_Scaffold262","Pad_R006420_Scaffold262","Pad_R006429_Scaffold263","Pad_R006433_Scaffold263","Pad_R006440_Scaffold263","Pad_R006515_Scaffold267","Pad_R006519_Scaffold267","Pad_R006523_Scaffold267","Pad_R006534_Scaffold267","Pad_R006582_Scaffold268","Pad_R006645_Scaffold27","Pad_R006678_Scaffold27","Pad_R006705_Scaffold27","Pad_R006710_Scaffold27","Pad_R006718_Scaffold27","Pad_R006733_Scaffold27","Pad_R006793_Scaffold273","Pad_R006800_Scaffold273","Pad_R006942_Scaffold284","Pad_R006949_Scaffold284","Pad_R006980_Scaffold286","Pad_R006998_Scaffold286","Pad_R007105_Scaffold286","Pad_R007136_Scaffold288","Pad_R007144_Scaffold289","Pad_R007169_Scaffold29","Pad_R007175_Scaffold29","Pad_R007194_Scaffold290","Pad_R007201_Scaffold290","Pad_R007235_Scaffold293","Pad_R007256_Scaffold294","Pad_R007291_Scaffold294","Pad_R007302_Scaffold294","Pad_R007312_Scaffold294","Pad_R007366_Scaffold298","Pad_R007377_Scaffold298","Pad_R007391_Scaffold298","Pad_R007426_Scaffold3","Pad_R007445_Scaffold3","Pad_R007483_Scaffold30","Pad_R007508_Scaffold300","Pad_R007523_Scaffold300","Pad_R007533_Scaffold302","Pad_R007578_Scaffold304","Pad_R007579_Scaffold304","Pad_R007594_Scaffold304","Pad_R007618_Scaffold306","Pad_R007635_Scaffold306","Pad_R007640_Scaffold306","Pad_R007667_Scaffold308","Pad_R007768_Scaffold314","Pad_R007783_Scaffold314","Pad_R007795_Scaffold314","Pad_R007798_Scaffold314","Pad_R007960_Scaffold32","Pad_R008004_Scaffold32","Pad_R008208_Scaffold34","Pad_R008245_Scaffold34","Pad_R008253_Scaffold34","Pad_R008254_Scaffold34","Pad_R008481_Scaffold353","Pad_R008485_Scaffold353","Pad_R008504_Scaffold356","Pad_R008510_Scaffold356","Pad_R008533_Scaffold358","Pad_R008591_Scaffold358","Pad_R008621_Scaffold358","Pad_R008650_Scaffold359","Pad_R008724_Scaffold36","Pad_R008771_Scaffold36","Pad_R008820_Scaffold36","Pad_R008827_Scaffold36","Pad_R008829_Scaffold36","Pad_R008839_Scaffold36","Pad_R008869_Scaffold361","Pad_R008908_Scaffold365","Pad_R008912_Scaffold365","Pad_R008918_Scaffold365","Pad_R009028_Scaffold377","Pad_R009307_Scaffold383","Pad_R009348_Scaffold386","Pad_R009359_Scaffold386","Pad_R009362_Scaffold386","Pad_R009394_Scaffold387","Pad_R009434_Scaffold39","Pad_R009531_Scaffold394","Pad_R009556_Scaffold398","Pad_R009562_Scaffold399","Pad_R009563_Scaffold399","Pad_R009579_Scaffold399","Pad_R009592_Scaffold399","Pad_R009683_Scaffold4","Pad_R009733_Scaffold4","Pad_R009748_Scaffold4","Pad_R009752_Scaffold4","Pad_R009753_Scaffold4","Pad_R009767_Scaffold4","Pad_R009790_Scaffold4","Pad_R009810_Scaffold4","Pad_R009830_Scaffold4","Pad_R009858_Scaffold4","Pad_R010076_Scaffold403","Pad_R010137_Scaffold41","Pad_R010144_Scaffold41","Pad_R010147_Scaffold41","Pad_R010172_Scaffold410","Pad_R010195_Scaffold410","Pad_R010209_Scaffold411","Pad_R010214_Scaffold411","Pad_R010268_Scaffold413","Pad_R010270_Scaffold413","Pad_R010362_Scaffold42","Pad_R010409_Scaffold42","Pad_R010414_Scaffold42","Pad_R010442_Scaffold420","Pad_R010451_Scaffold420","Pad_R010472_Scaffold420","Pad_R010561_Scaffold427","Pad_R010573_Scaffold427","Pad_R010595_Scaffold429","Pad_R010773_Scaffold437","Pad_R010962_Scaffold45","Pad_R010965_Scaffold45","Pad_R011010_Scaffold45","Pad_R011014_Scaffold45","Pad_R011024_Scaffold45","Pad_R011027_Scaffold45","Pad_R011240_Scaffold463","Pad_R011246_Scaffold463","Pad_R011326_Scaffold47","Pad_R011332_Scaffold47","Pad_R011341_Scaffold47","Pad_R011358_Scaffold47","Pad_R011622_Scaffold5","Pad_R011686_Scaffold505","Pad_R011689_Scaffold505","Pad_R011713_Scaffold51","Pad_R011768_Scaffold52","Pad_R011884_Scaffold54","Pad_R011888_Scaffold54","Pad_R011922_Scaffold54","Pad_R011929_Scaffold54","Pad_R011943_Scaffold54","Pad_R012004_Scaffold547","Pad_R012006_Scaffold547","Pad_R012025_Scaffold55","Pad_R012036_Scaffold55","Pad_R012076_Scaffold554","Pad_R012134_Scaffold57","Pad_R012136_Scaffold57","Pad_R012179_Scaffold57","Pad_R012196_Scaffold57","Pad_R012239_Scaffold57","Pad_R012257_Scaffold57","Pad_R012261_Scaffold57","Pad_R012283_Scaffold571","Pad_R012352_Scaffold582","Pad_R012372_Scaffold582","Pad_R012545_Scaffold6","Pad_R012703_Scaffold608","Pad_R012822_Scaffold62","Pad_R012843_Scaffold62","Pad_R012850_Scaffold62","Pad_R012948_Scaffold63","Pad_R012970_Scaffold63","Pad_R012973_Scaffold63","Pad_R012974_Scaffold63","Pad_R012978_Scaffold63","Pad_R012987_Scaffold63","Pad_R012989_Scaffold63","Pad_R013000_Scaffold63","Pad_R013014_Scaffold63","Pad_R013031_Scaffold635","Pad_R013064_Scaffold65","Pad_R013290_Scaffold67","Pad_R013321_Scaffold67","Pad_R013341_Scaffold67","Pad_R013359_Scaffold67","Pad_R013397_Scaffold68","Pad_R013419_Scaffold68","Pad_R013596_Scaffold71","Pad_R013605_Scaffold71","Pad_R013610_Scaffold71","Pad_R013617_Scaffold71","Pad_R013618_Scaffold71","Pad_R013627_Scaffold71","Pad_R013642_Scaffold71","Pad_R013647_Scaffold71","Pad_R013684_Scaffold71","Pad_R013781_Scaffold72","Pad_R013852_Scaffold74","Pad_R013868_Scaffold75","Pad_R013878_Scaffold75","Pad_R013880_Scaffold75","Pad_R013897_Scaffold75","Pad_R013915_Scaffold75","Pad_R013954_Scaffold75","Pad_R013979_Scaffold75","Pad_R014044_Scaffold77","Pad_R014099_Scaffold77","Pad_R014121_Scaffold77","Pad_R014125_Scaffold77","Pad_R014133_Scaffold771","Pad_R014268_Scaffold79","Pad_R014284_Scaffold79","Pad_R014302_Scaffold79","Pad_R014346_Scaffold80","Pad_R014428_Scaffold83","Pad_R014457_Scaffold838","Pad_R014474_Scaffold84","Pad_R014479_Scaffold84","Pad_R014482_Scaffold84","Pad_R014494_Scaffold84","Pad_R014499_Scaffold841","Pad_R014659_Scaffold86","Pad_R014700_Scaffold87","Pad_R014710_Scaffold87","Pad_R014722_Scaffold87","Pad_R014768_Scaffold88","Pad_R014770_Scaffold88","Pad_R014775_Scaffold88","Pad_R014780_Scaffold88","Pad_R014851_Scaffold9","Pad_R014852_Scaffold9","Pad_R014860_Scaffold9","Pad_R014906_Scaffold911","Pad_R014910_Scaffold913","Pad_R014938_Scaffold92","Pad_R014941_Scaffold92","Pad_R014942_Scaffold92","Pad_R014952_Scaffold92","Pad_R014980_Scaffold93","Pad_R014982_Scaffold93","Pad_R015185_Scaffold98","Pad_R015214_Scaffold98","Pad_R015225_Scaffold98"
        };
        out.println("Total " + ids.length + " genes");

        for (String id : ids) {
            TreeSeqSimulator treeSeqSimulator = new TreeSeqSimulator(popSize, taxa, dates);
            try {
                Alignment dummyAlg = treeSeqSimulator.getDummyAlignment(taxa);
                Tree tree = treeSeqSimulator.getRandomTree(popSize, dummyAlg, taxa, dates);

                out.println("Tree (" + id + ") is " + tree.toString());

                SiteModel siteModel = treeSeqSimulator.getSiteModel(dummyAlg, kappa, gammaShape);
                BranchRateModel branchRateModel = treeSeqSimulator.getBranchModel(clockRate);

                Alignment alignment = treeSeqSimulator.getSimulatedAlignment(dummyAlg, tree, siteModel, branchRateModel);

                XMLProducer producer = new XMLProducer();
                String sXML = producer.toRawXML(alignment);
//            out.println("<beast version='2.0'>");
                out.println(sXML);
//            out.println("</beast>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } // main
}
