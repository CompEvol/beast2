package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.Binomial;
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexei Drummond
 */
@Description("A tree generated randomly from the structured coalescent process, with the given population sizes, migration rates and per-deme sample sizes.")
public class StructuredCoalescentTree extends Tree {

    public Input<RealParameter> popSizesMigrationRates = new Input<RealParameter>("popSizesMigrationRates", "A matrix of migration rates and population sizes. Population sizes occupy the diagonal and migration rates occupy the off-diagonals");
    public Input<IntegerParameter> sampleSizes = new Input<IntegerParameter>("sampleSizes", "The sample sizes for each population");

    public StructuredCoalescentTree() {
    }

    public StructuredCoalescentTree(RealParameter popSizesMigrationRatesParameter, IntegerParameter sampleSizesParameter) throws Exception {
        popSizesMigrationRates.setValue(popSizesMigrationRatesParameter, this);
        sampleSizes.setValue(sampleSizesParameter, this);
        initAndValidate();
    }

    enum EventType {coalescent, migration}

    public void initAndValidate() {

        int count = 0;
        List<List<Node>> nodes = new ArrayList<List<Node>>();
        for (int i = 0; i < sampleSizes.get().getDimension(); i++) {
            nodes.add(new ArrayList<Node>());
            for (int j = 0; j < sampleSizes.get().getValue(i); j++) {
                Node node = new Node();
                node.setNr(count);
                node.setID(count + "");
                node.setMetaData("deme", i);
                node.setHeight(0);
                nodes.get(i).add(node);
                count += 1;
            }
        }
        setRoot(simulateStructuredCoalescentForest(nodes, popSizesMigrationRates.get(), Double.POSITIVE_INFINITY).get(0));
        initArrays();
    }

    private List<Node> simulateStructuredCoalescentForest(List<List<Node>> nodes, RealParameter popSizesMigrationRates, double stopTime) {

        //diagonals are coalescent rates, off-diagonals are migration rates
        double[][] rates = new double[nodes.size()][nodes.size()];
        double totalRate = populateRateMatrix(nodes, popSizesMigrationRates, rates);

        double time = 0.0;

        int nodeNumber = getTotalNodeCount(nodes);

        while (time < stopTime && getTotalNodeCount(nodes) > 1) {

            SCEvent event = selectRandomEvent(rates, totalRate, time);
            if (event.type == EventType.coalescent) {
                // coalescent
                Node node1 = selectRandomNode(nodes.get(event.pop));
                Node node2 = selectRandomNode(nodes.get(event.pop));

                if (node1.getMetaData("deme") != node2.getMetaData("deme")) {
                    throw new RuntimeException("demes must match for coalescing nodes!");
                }

                Node parent = new Node();
                parent.setNr(nodeNumber);
                parent.setHeight(event.time);
                parent.setMetaData("deme", node1.getMetaData("deme"));
                parent.addChild(node1);
                parent.addChild(node2);

                time = event.time;

                nodes.get(event.pop).remove(node1);
                nodes.get(event.pop).remove(node1);
                nodes.get(event.pop).add(parent);

            } else {
                // migration

                Node migrant = selectRandomNode(nodes.get(event.pop));

                Node migrantsParent = new Node();
                migrantsParent.setNr(nodeNumber);
                migrantsParent.setHeight(event.time);
                migrantsParent.setMetaData("deme", event.toPop);

                migrantsParent.addChild(migrant);

                time = event.time;

                nodes.get(event.pop).remove(migrant);
                nodes.get(event.toPop).add(migrantsParent);
            }
            totalRate = populateRateMatrix(nodes, popSizesMigrationRates, rates);
            nodeNumber += 1;
        }

        List<Node> rootNodes = new ArrayList<Node>();
        for (List<Node> nodeList : nodes) {
            rootNodes.addAll(nodeList);
        }

        //System.out.println(rootNodes.size() + " root nodes remain");
        //System.out.println(" rootNodes.get(0).getNodeCount() == " + rootNodes.get(0).getNodeCount());


        return rootNodes;
    }

    private int getTotalNodeCount(List<List<Node>> nodes) {
        int count = 0;
        for (List<Node> nodeList : nodes) {
            count += nodeList.size();
        }
        return count;
    }

    private Node selectRandomNode(List<Node> nodes) {
        int index = Randomizer.nextInt(nodes.size());
        Node node = nodes.remove(index);
        return node;
    }

    private SCEvent selectRandomEvent(double[][] rates, double totalRate, double time) {

        double U = Randomizer.nextDouble() * totalRate;

        double cumulativeRate = 0.0;
        for (int i = 0; i < rates.length; i++) {
            for (int j = 0; j < rates.length; j++) {
                if (U > rates[i][j]) {
                    U -= rates[i][j];
                } else {
                    SCEvent event = new SCEvent();
                    event.pop = i;
                    event.toPop = j;
                    if (i == j) {
                        event.type = EventType.coalescent;
                    } else {
                        event.type = EventType.migration;
                    }

                    double V = U / rates[i][j];

                    event.time = time + (-Math.log(V) / totalRate);

                    return event;
                }
            }
        }
        throw new RuntimeException();
    }

    private double populateRateMatrix(List<List<Node>> nodes, RealParameter popSizesMigrationRates, double[][] rates) {

        double totalRate = 0;

        // coalescent rates
        for (int i = 0; i < rates.length; i++) {
            for (int j = 0; j < rates.length; j++) {
                double popSizej = popSizesMigrationRates.getMatrixValue(j, j);
                if (i == j) {
                    rates[i][i] = Binomial.choose2(nodes.get(i).size()) / popSizej;
                } else {
                    rates[i][j] = popSizesMigrationRates.getMatrixValue(i, j) * popSizej * nodes.get(i).size();
                }
                totalRate += rates[i][j];
            }
        }
        return totalRate;
    }

    private class SCEvent {

        int pop;

        // if the event is a migration this is the population the parent node is in 
        // (i.e. the deme that the lineage migrates to when going backwards in time)
        int toPop;

        EventType type;
        double time;

    }

    public static void main(String[] args) throws Exception {

        //List<Tree> trees = new ArrayList<Tree>();

        int reps = 10000;

        double[] popSize1 = new double[]{1, 1, 1, 1, 1};
        double[] popSize2 = new double[]{1, 2, 4, 8, 16};


        for (double m = 0.125; m < 32; m *= 2) {
            for (int i = 0; i < popSize1.length; i++) {
                int count = 0;
                for (int j = 0; j < reps; j++) {

                    Tree tree = new StructuredCoalescentTree(
                            new RealParameter(new Double[]{popSize1[i], m, popSize2[i], m}),
                            new IntegerParameter(new Integer[]{2, 2})
                    );

                    //trees.add(tree);

                    if ((Integer) tree.getRoot().getMetaData("deme") == 0) count += 1;

                }
                System.out.println(popSize1[i] + "\t" + popSize2[i] + "\t" + m + "\t" + ((double) count / (double) reps));
            }
        }

    }
}
