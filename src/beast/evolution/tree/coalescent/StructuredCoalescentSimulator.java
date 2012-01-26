package beast.evolution.tree.coalescent;

import beast.core.Input;
import beast.core.Plugin;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.Binomial;
import beast.util.Randomizer;

import java.util.*;

/**
 * @author Alexei Drummond
 */
public class StructuredCoalescentSimulator extends beast.core.Runnable {
    
    Input<RealParameter> popSizesMigrationRates = new Input<RealParameter>("popSizesMigrationRates", "A matrix of migration rates and population sizes. Population sizes occupy the diagonal and migration rates occupy the off-diagonals");
    Input<IntegerParameter> sampleSizes = new Input<IntegerParameter>("sampleSizes", "The sample sizes for each population");
    
    
    
    public Tree simulateTree() {
        
        int count = 0;
        List<List<Node>> nodes = new ArrayList<List<Node>>();
        for (int i = 0; i < sampleSizes.get().getDimension(); i++) {
            nodes.add(new ArrayList<Node>());
            for (int j = 0; j < sampleSizes.get().getValue(i); j++) {
                Node node = new Node();
                node.setID(count + "");
                node.setMetaData("deme", i);
                nodes.get(i).add(node);
                count += 1;
            }
        }
        
        return simulateStructuredCoalescentTree(nodes, popSizesMigrationRates.get());
    }

    private Tree simulateStructuredCoalescentTree(List<List<Node>> nodes, RealParameter popSizesMigrationRates) {
        
        //diagonals are coalescent rates, off-diagonals are migration rates
        double[][] rates = new double[nodes.size()][nodes.size()];
        double totalRate = getRates(nodes, popSizesMigrationRates, rates);
        
        int[] entry = selectRandomEntry(rates, totalRate); 
        if (entry[0] == entry[1]) {
            // coalescent
            Node node1 = selectRandomNode(nodes.get(entry[0]));
            Node node2 = selectRandomNode(nodes.get(entry[0]));

        } else {
            // migration
        }
        
        //TODO
        return null;
    }

    private Node selectRandomNode(List<Node> nodes) {
        int index = Randomizer.nextInt(nodes.size());
        Node node = nodes.remove(index);
        return node;
    }

    private int[] selectRandomEntry(double[][] rates, double totalRate) {

        double U = Randomizer.nextDouble() * totalRate;
        
        double cumulativeRate = 0.0;
        for (int i = 0; i < rates.length; i++) {
            for (int j = 0; j < rates.length; j++) {
                cumulativeRate += rates[i][j];
                if (cumulativeRate > U) return new int[] {i,j};
            }
        }
        throw new RuntimeException();
    }

    private double getRates(List<List<Node>> nodes, RealParameter popSizesMigrationRates, double[][] rates) {
        
        double totalRate = 0;
        
        // coalescent rates
        for (int i = 0; i < rates.length; i++) {
            for (int j = 0; j < rates.length; j++) {
                double popSizej = popSizesMigrationRates.getMatrixValue(j,j);
                if (i == j) {
                    rates[i][i] = Binomial.choose2(nodes.get(i).size()) * popSizej;
                } else {
                    rates[i][j] = popSizesMigrationRates.getMatrixValue(i,j) * popSizej;
                }
                totalRate += rates[i][j];
            }
        }
        return totalRate;
    }

    public void run() {
        
    }
    
}
