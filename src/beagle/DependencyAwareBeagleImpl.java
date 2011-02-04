package beagle;

public class DependencyAwareBeagleImpl extends GeneralBeagleImpl {

    public DependencyAwareBeagleImpl(final int tipCount, final int partialsBufferCount, final int compactBufferCount, final int stateCount, final int patternCount, final int eigenBufferCount, final int matrixBufferCount, final int categoryCount, final int scaleBufferCount) {
        super(tipCount, partialsBufferCount, compactBufferCount, stateCount, patternCount, eigenBufferCount, matrixBufferCount, categoryCount, scaleBufferCount);
    }

//    private int[] dependencyCounts;
//
//    /**
//     * Constructor
//     *
//     * @param //stateCount number of states
//     */
//    public DependencyAwareBeagleImpl() {
//        super(4);
//        Logger.getLogger("beagle").info("Constructing dependency-aware 4-state Java BEAGLE implementation.");
//        pool = Executors.newFixedThreadPool(20);
//
//    }
//
//    public void calculatePartials(int[] operations, int[] dependencies, int operationCount, boolean rescale) {
//
//        if (dependencyCounts == null) {
//            // if it doesn't exist, construct a dependencyCount array that will be big enough
//            dependencyCounts = new int[nodeCount];
//        }
//
//        // zero the counts
//        for (int i = 0; i < operationCount; i++) {
//            dependencyCounts[i] = 0;
//        }
//
//        // set the counts based on the dependency array
//        for (int i = 0; i < operationCount; i++) {
//            if (dependencies[i] >= 0) {
//                dependencyCounts[dependencies[i]] ++;
//            }
//        }
//
//        // a dependency count of:
//        // >0 has 1 or 2 dependencies
//        // 0  independent and ready to go
//        // -1 scheduled for running
//        // -2 done and dusted
//
//        boolean done = false;
//
//        while (!done) {
//            // a set of threads for the pool
//            Set<Callable<Integer>> independent = new HashSet<Callable<Integer>>();
//
//            int x = 0;
//            for (int i = 0; i < operationCount; i++) {
//                if (dependencyCounts[i] == 0) {
//                    // has no dependencies so create a thread
//                    Caller caller = new Caller(operations[x], operations[x + 1], operations[x + 2]);
//                    independent.add(caller);
//
//                    // set the count as 'scheduled'
//                    dependencyCounts[i] = -1;
//                }
//                x += 3;
//            }
//
//            try {
//                // run all the independent threads
//                pool.invokeAll(independent);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            done = true;
//            for (int i = 0; i < operationCount; i++) {
//                // update the counts
//                if (dependencyCounts[i] == -1) {
//                    // this operation has finished
//                    if (dependencies[i] >= 0) {
//                        // so decrement the count of the operation that depends on it
//                        dependencyCounts[dependencies[i]] --;
//
//                        // we are definitely not done yet
//                        done = false;
//                    }
//                    dependencyCounts[i] = -2; // set it to -2 to mark it as done
//                }
//            }
//        }
//    }
//
//    class Caller implements Callable<Integer> {
//
//        Caller(final int nodeIndex1, final int nodeIndex2, final int nodeIndex3) {
//            this.nodeIndex1 = nodeIndex1;
//            this.nodeIndex2 = nodeIndex2;
//            this.nodeIndex3 = nodeIndex3;
//        }
//
//        public Integer call() throws Exception {
//            currentPartialsIndices[nodeIndex3] = 1 - currentPartialsIndices[nodeIndex3];
//
//            if (useTipPartials) {
//                updatePartialsPartials(nodeIndex1, nodeIndex2, nodeIndex3);
//            } else {
//                if (nodeIndex1 < tipCount) {
//                    if (nodeIndex2 < tipCount) {
//                        updateStatesStates(nodeIndex1, nodeIndex2, nodeIndex3);
//                    } else {
//                        updateStatesPartials(nodeIndex1, nodeIndex2, nodeIndex3);
//                    }
//                } else {
//                    if (nodeIndex2 < tipCount) {
//                        updateStatesPartials(nodeIndex2, nodeIndex1, nodeIndex3);
//                    } else {
//                        updatePartialsPartials(nodeIndex1, nodeIndex2, nodeIndex3);
//                    }
//                }
//            }
//
////            if (useScaling) {
////                scalePartials(nodeIndex3);
////            }
//            return 0;
//        }
//
//        private int nodeIndex1;
//        private int nodeIndex2;
//        private int nodeIndex3;
//    }
//
//    private final ExecutorService pool;
//
//    /**
//     * Calculates partial likelihoods at a node when both children have states.
//     */
//    protected void updateStatesStates(int nodeIndex1, int nodeIndex2, int nodeIndex3)
//    {
//        double[] matrices1 = matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1];
//        double[] matrices2 = matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2];
//
//        int[] states1 = tipStates[nodeIndex1];
//        int[] states2 = tipStates[nodeIndex2];
//
//        double[] partials3 = partials[currentPartialsIndices[nodeIndex3]][nodeIndex3];
//
//        // copied from NucleotideLikelihoodCore
//        int v = 0;
//        for (int j = 0; j < categoryCount; j++) {
//
//            for (int k = 0; k < patternCount; k++) {
//
//                int state1 = states1[k];
//                int state2 = states2[k];
//
//                int w = j * 20;
//
//                partials3[v] = matrices1[w + state1] * matrices2[w + state2];
//                v++;	w += 5;
//                partials3[v] = matrices1[w + state1] * matrices2[w + state2];
//                v++;	w += 5;
//                partials3[v] = matrices1[w + state1] * matrices2[w + state2];
//                v++;	w += 5;
//                partials3[v] = matrices1[w + state1] * matrices2[w + state2];
//                v++;	w += 5;
//            }
//        }
//    }
//
//    /**
//     * Calculates partial likelihoods at a node when one child has states and one has partials.
//     * @param nodeIndex1
//     * @param nodeIndex2
//     * @param nodeIndex3
//     */
//    protected void updateStatesPartials(int nodeIndex1, int nodeIndex2, int nodeIndex3)
//    {
//        double[] matrices1 = matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1];
//        double[] matrices2 = matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2];
//
//        int[] states1 = tipStates[nodeIndex1];
//        double[] partials2 = partials[currentPartialsIndices[nodeIndex2]][nodeIndex2];
//
//        double[] partials3 = partials[currentPartialsIndices[nodeIndex3]][nodeIndex3];
//
//        // copied from NucleotideLikelihoodCore
//        int u = 0;
//        int v = 0;
//
//        for (int l = 0; l < categoryCount; l++) {
//            for (int k = 0; k < patternCount; k++) {
//
//                int state1 = states1[k];
//
//                int w = l * 20;
//
//                partials3[u] = matrices1[w + state1];
//
//                double sum = matrices2[w] * partials2[v]; w++;
//                sum +=	matrices2[w] * partials2[v + 1]; w++;
//                sum +=	matrices2[w] * partials2[v + 2]; w++;
//                sum +=	matrices2[w] * partials2[v + 3]; w++;
//                w++; // increment for the extra column at the end
//                partials3[u] *= sum;	u++;
//
//                partials3[u] = matrices1[w + state1];
//
//                sum = matrices2[w] * partials2[v]; w++;
//                sum +=	matrices2[w] * partials2[v + 1]; w++;
//                sum +=	matrices2[w] * partials2[v + 2]; w++;
//                sum +=	matrices2[w] * partials2[v + 3]; w++;
//                w++; // increment for the extra column at the end
//                partials3[u] *= sum;	u++;
//
//                partials3[u] = matrices1[w + state1];
//
//                sum = matrices2[w] * partials2[v]; w++;
//                sum +=	matrices2[w] * partials2[v + 1]; w++;
//                sum +=	matrices2[w] * partials2[v + 2]; w++;
//                sum +=	matrices2[w] * partials2[v + 3]; w++;
//                w++; // increment for the extra column at the end
//                partials3[u] *= sum;	u++;
//
//                partials3[u] = matrices1[w + state1];
//
//                sum = matrices2[w] * partials2[v]; w++;
//                sum +=	matrices2[w] * partials2[v + 1]; w++;
//                sum +=	matrices2[w] * partials2[v + 2]; w++;
//                sum +=	matrices2[w] * partials2[v + 3];
//                partials3[u] *= sum;	u++;
//
//                v += 4;
//
//            }
//        }
//    }
//
//    protected void updatePartialsPartials(int nodeIndex1, int nodeIndex2, int nodeIndex3)
//    {
//        double[] matrices1 = matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1];
//        double[] matrices2 = matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2];
//
//        double[] partials1 = partials[currentPartialsIndices[nodeIndex1]][nodeIndex1];
//        double[] partials2 = partials[currentPartialsIndices[nodeIndex2]][nodeIndex2];
//
//        double[] partials3 = partials[currentPartialsIndices[nodeIndex3]][nodeIndex3];
//
//        // copied from NucleotideLikelihoodCore
//
//        double sum1, sum2;
//
//        int u = 0;
//        int v = 0;
//
//        for (int l = 0; l < categoryCount; l++) {
//            for (int k = 0; k < patternCount; k++) {
//
//                int w = l * 20;
//
//                sum1 = matrices1[w] * partials1[v];
//                sum2 = matrices2[w] * partials2[v]; w++;
//                sum1 += matrices1[w] * partials1[v + 1];
//                sum2 += matrices2[w] * partials2[v + 1]; w++;
//                sum1 += matrices1[w] * partials1[v + 2];
//                sum2 += matrices2[w] * partials2[v + 2]; w++;
//                sum1 += matrices1[w] * partials1[v + 3];
//                sum2 += matrices2[w] * partials2[v + 3]; w++;
//                w++; // increment for the extra column at the end
//                partials3[u] = sum1 * sum2; u++;
//
//                sum1 = matrices1[w] * partials1[v];
//                sum2 = matrices2[w] * partials2[v]; w++;
//                sum1 += matrices1[w] * partials1[v + 1];
//                sum2 += matrices2[w] * partials2[v + 1]; w++;
//                sum1 += matrices1[w] * partials1[v + 2];
//                sum2 += matrices2[w] * partials2[v + 2]; w++;
//                sum1 += matrices1[w] * partials1[v + 3];
//                sum2 += matrices2[w] * partials2[v + 3]; w++;
//                w++; // increment for the extra column at the end
//                partials3[u] = sum1 * sum2; u++;
//
//                sum1 = matrices1[w] * partials1[v];
//                sum2 = matrices2[w] * partials2[v]; w++;
//                sum1 += matrices1[w] * partials1[v + 1];
//                sum2 += matrices2[w] * partials2[v + 1]; w++;
//                sum1 += matrices1[w] * partials1[v + 2];
//                sum2 += matrices2[w] * partials2[v + 2]; w++;
//                sum1 += matrices1[w] * partials1[v + 3];
//                sum2 += matrices2[w] * partials2[v + 3]; w++;
//                w++; // increment for the extra column at the end
//                partials3[u] = sum1 * sum2; u++;
//
//                sum1 = matrices1[w] * partials1[v];
//                sum2 = matrices2[w] * partials2[v]; w++;
//                sum1 += matrices1[w] * partials1[v + 1];
//                sum2 += matrices2[w] * partials2[v + 1]; w++;
//                sum1 += matrices1[w] * partials1[v + 2];
//                sum2 += matrices2[w] * partials2[v + 2]; w++;
//                sum1 += matrices1[w] * partials1[v + 3];
//                sum2 += matrices2[w] * partials2[v + 3];
//                partials3[u] = sum1 * sum2; u++;
//
//                v += 4;
//            }
//        }
//    }
}