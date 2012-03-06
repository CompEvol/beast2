package beast.evolution.alignment;

import java.util.Arrays;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;

@Description("Alignemnt based on a filter operation on another alignment")
public class FilteredAlignment extends Alignment {
    public Input<String> m_sFilterInput = new Input<String>("filter", "specifies which of the sites in the input alignment should be selected " +
            "First site is 1." +
            "Filter specs are comma separated, either a range [from]-[to] or iteration [from]:[to]:[step]; " +
            "1-100 defines a range, " +
            "1:100:3 defines every third in range 1-100, " +
            "1::3,2::3 removes every third site. " +
            "Default for range [1]-[last site], default for iterator [1]:[last site]:[1]", Validate.REQUIRED);
    public Input<Alignment> m_alignmentInput = new Input<Alignment>("data", "alignment to be filtered", Validate.REQUIRED);

    // these triples specify a range for(i=From; i <= To; i += Step)
    int[] m_iFrom;
    int[] m_iTo;
    int[] m_iStep;
    /**
     * list of indices filtered from input alignment *
     */
    int[] m_iFilter;

    public FilteredAlignment() {
        m_pSequences.setRule(Validate.OPTIONAL);
    }

    @Override
    public void initAndValidate() throws Exception {
        parseFilterSpec();
        calcFilter();
        Alignment data = m_alignmentInput.get();
        m_dataType = data.m_dataType;
        m_counts = data.m_counts;
        m_sTaxaNames = data.m_sTaxaNames;
        m_nStateCounts = data.m_nStateCounts;

        calcPatterns();
    }

    private void parseFilterSpec() throws Exception {
        // parse filter specification
        String sFilter = m_sFilterInput.get();
        String[] sFilters = sFilter.split(",");
        m_iFrom = new int[sFilters.length];
        m_iTo = new int[sFilters.length];
        m_iStep = new int[sFilters.length];
        for (int i = 0; i < sFilters.length; i++) {
            sFilter = " " + sFilters[i] + " ";
            if (sFilter.matches(".*-.*")) {
                // range, e.g. 1-100
                m_iStep[i] = 1;
                String[] sStrs = sFilter.split("-");
                m_iFrom[i] = parseInt(sStrs[0], 1) - 1;
                m_iTo[i] = parseInt(sStrs[1], m_alignmentInput.get().getSiteCount()) - 1;
            } else if (sFilter.matches(".*:.*:.+")) {
                // iterator, e.g. 1:100:3
                String[] sStrs = sFilter.split(":");
                m_iFrom[i] = parseInt(sStrs[0], 1) - 1;
                m_iTo[i] = parseInt(sStrs[1], m_alignmentInput.get().getSiteCount()) - 1;
                m_iStep[i] = parseInt(sStrs[2], 1);

            } else {
                throw new Exception("Don't know how to parse filter " + sFilter);
            }
        }
    }

    int parseInt(String sStr, int nDefault) {
        sStr = sStr.replaceAll("\\s+", "");
        try {
            return Integer.parseInt(sStr);
        } catch (Exception e) {
            return nDefault;
        }
    }

    private void calcFilter() {
        boolean[] bUsed = new boolean[m_alignmentInput.get().getSiteCount()];
        for (int i = 0; i < m_iTo.length; i++) {
            for (int k = m_iFrom[i]; k <= m_iTo[i]; k += m_iStep[i]) {
                bUsed[k] = true;
            }
        }
        // count
        int k = 0;
        for (int i = 0; i < bUsed.length; i++) {
            if (bUsed[i]) {
                k++;
            }
        }
        // set up index set
        m_iFilter = new int[k];
        k = 0;
        for (int i = 0; i < bUsed.length; i++) {
            if (bUsed[i]) {
                m_iFilter[k++] = i;
            }
        }
    }

    @Override
    protected void calcPatterns() {
        int nTaxa = m_counts.size();
        int nSites = m_iFilter.length;

        // convert data to transposed int array
        int[][] nData = new int[nSites][nTaxa];
        for (int i = 0; i < nTaxa; i++) {
            List<Integer> sites = m_counts.get(i);
            for (int j = 0; j < nSites; j++) {
                nData[j][i] = sites.get(m_iFilter[j]);
            }
        }

        // sort data
        SiteComparator comparator = new SiteComparator();
        Arrays.sort(nData, comparator);

        // count patterns in sorted data
        int nPatterns = 1;
        int[] weights = new int[nSites];
        weights[0] = 1;
        for (int i = 1; i < nSites; i++) {
            if (comparator.compare(nData[i - 1], nData[i]) != 0) {
                nPatterns++;
                nData[nPatterns - 1] = nData[i];
            }
            weights[nPatterns - 1]++;
        }

        // reserve memory for patterns
        m_nWeight = new int[nPatterns];
        m_nPatterns = new int[nPatterns][nTaxa];
        for (int i = 0; i < nPatterns; i++) {
            m_nWeight[i] = weights[i];
            m_nPatterns[i] = nData[i];
        }

        // find patterns for the sites
        m_nPatternIndex = new int[nSites];
        for (int i = 0; i < nSites; i++) {
            int[] sites = new int[nTaxa];
            for (int j = 0; j < nTaxa; j++) {
                sites[j] = m_counts.get(j).get(m_iFilter[i]);
            }
            m_nPatternIndex[i] = Arrays.binarySearch(m_nPatterns, sites, comparator);
        }

        // determine maximum state count
        // Usually, the state count is equal for all sites,
        // though for SnAP analysis, this is typically not the case.
        m_nMaxStateCount = 0;
        for (int m_nStateCount1 : m_nStateCounts) {
            m_nMaxStateCount = Math.max(m_nMaxStateCount, m_nStateCount1);
        }
        // report some statistics
        //for (int i = 0; i < m_sTaxaNames.size(); i++) {
        //    System.err.println(m_sTaxaNames.get(i) + ": " + m_counts.get(i).size() + " " + m_nStateCounts.get(i));
        //}
        System.err.println("Filter " + m_sFilterInput.get());
        System.err.println(getNrTaxa() + " taxa");
        System.err.println(getSiteCount() + " sites");
        System.err.println(getPatternCount() + " patterns");
    }
}
