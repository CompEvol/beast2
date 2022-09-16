# perl script to help migrate packages from BEAST v2.6.X to v2.7.0


$s='beast.core beast.pkgmgmt:beast.base.core:beast.base.inference
beast.core.parameter beast.base.inference.parameter
beast.core.util beast.base.util:beast.base.math
beast.evolution.alignment beast.base.evolution.alignment
beast.evolution.branchratemodel beast.base.evolution.branchratemodel
beast.evolution.likelihood beast.base.evolution.likelihood
beast.evolution.nuc
beast.evolution.operators beast.base.evolution.operator:beast.base.inference.operator
beast.evolution.sitemodel beast.base.evolution.sitemodel
beast.evolution.speciation beast.base.evolution.speciation
beast.evolution.substitutionmodel beast.base.evolution.substitutionmodel
beast.evolution.tree beast.base.evolution.tree
beast.evolution.tree.coalescent beast.base.evolution.tree.coalescent
beast.math beast.base.math
beast.math.distributions beast.base.inference.distribution
beast.util beast.base.util:beast.base.math';
@s = split('\n',$s);
foreach $s (@s) {
	$s =~ /(.*) (.*)/;
	$namespacemap{$1}=$2;
}

$s = 'beast.app.BEASTVersion beast.pkgmgmt.BEASTVersion
beast.app.BEASTVersion2 beast.base.core.BEASTVersion2
beast.app.BeastMCMC beastfx.app.beast.BeastMCMC
beast.app.DocMaker beastfx.app.tools.DocMaker
beast.app.beastapp.BeastLauncher beast.pkgmgmt.launcher.BeastLauncher
beast.app.beauti.AlignmentImporter beastfx.app.inputeditor.AlignmentImporter
beast.app.beauti.AlignmentListInputEditor beastfx.app.inputeditor.AlignmentListInputEditor
beast.app.beauti.AlignmentViewer beastfx.app.inputeditor.AlignmentViewer
beast.app.beauti.BeautiAlignmentProvider beastfx.app.inputeditor.BeautiAlignmentProvider
beast.app.beauti.BeautiConfig beastfx.app.inputeditor.BeautiConfig
beast.app.beauti.BeautiConnector beastfx.app.inputeditor.BeautiConnector
beast.app.beauti.BeautiDoc beastfx.app.inputeditor.BeautiDoc
beast.app.beauti.BeautiDocListener beastfx.app.inputeditor.BeautiDocListener
beast.app.beauti.BeautiLauncher beast.pkgmgmt.launcher.BeautiLauncher
beast.app.beauti.BeautiPanelConfig beastfx.app.inputeditor.BeautiPanelConfig
beast.app.beauti.BeautiSubTemplate beastfx.app.inputeditor.BeautiSubTemplate
beast.app.beauti.FileInputEditor beastfx.app.inputeditor.FileInputEditor
beast.app.beauti.FileListInputEditor beastfx.app.inputeditor.FileListInputEditor
beast.app.beauti.GuessPatternDialog beastfx.app.inputeditor.GuessPatternDialog
beast.app.beauti.LogFileInputEditor beastfx.app.inputeditor.LogFileInputEditor
beast.app.beauti.LogFileListInputEditor beastfx.app.inputeditor.LogFileListInputEditor
beast.app.beauti.LoggerListInputEditor beastfx.app.inputeditor.LoggerListInputEditor
beast.app.beauti.MRCAPriorInputEditor beastfx.app.inputeditor.MRCAPriorInputEditor
beast.app.beauti.OutFileInputEditor beastfx.app.inputeditor.OutFileInputEditor
beast.app.beauti.OutFileListInputEditor beastfx.app.inputeditor.OutFileListInputEditor
beast.app.beauti.ParametricDistributionInputEditor beastfx.app.inputeditor.ParametricDistributionInputEditor
beast.app.beauti.PartitionContext beast.base.parser.PartitionContext
beast.app.beauti.SiteModelInputEditor beastfx.app.inputeditor.SiteModelInputEditor
beast.app.beauti.TaxonSetDialog beastfx.app.inputeditor.TaxonSetDialog
beast.app.beauti.TipDatesInputEditor beastfx.app.inputeditor.TipDatesInputEditor
beast.app.beauti.TreeFileInputEditor beastfx.app.inputeditor.TreeFileInputEditor
beast.app.beauti.TreeFileListInputEditor beastfx.app.inputeditor.TreeFileListInputEditor
beast.app.beauti.WrappedOptionPane beastfx.app.inputeditor.WrappedOptionPane
beast.app.beauti.XMLFileInputEditor beastfx.app.inputeditor.XMLFileInputEditor
beast.app.beauti.XMLFileListInputEditor beastfx.app.inputeditor.XMLFileListInputEditor
beast.app.beauti.guiutil.S11InitialSelection beastfx.app.beauti.S11InitialSelection
beast.app.draw.BEASTObjectDialog beastfx.app.inputeditor.BEASTObjectDialog
beast.app.draw.BEASTObjectInputEditor beastfx.app.inputeditor.BEASTObjectInputEditor
beast.app.draw.BEASTObjectPanel beastfx.app.inputeditor.BEASTObjectPanel
beast.app.draw.BooleanInputEditor beastfx.app.inputeditor.BooleanInputEditor
beast.app.draw.DoubleInputEditor beastfx.app.inputeditor.DoubleInputEditor
beast.app.draw.DoubleListInputEditor beastfx.app.inputeditor.DoubleListInputEditor
beast.app.draw.EnumInputEditor beastfx.app.inputeditor.EnumInputEditor
beast.app.draw.ExtensionFileFilter beastfx.app.util.ExtensionFileFilter
beast.app.draw.HelpBrowser beastfx.app.tools.HelpBrowser
beast.app.draw.InputEditor beastfx.app.inputeditor.InputEditor
beast.app.draw.InputEditorFactory beastfx.app.inputeditor.InputEditorFactory
beast.app.draw.IntegerInputEditor beastfx.app.inputeditor.IntegerInputEditor
beast.app.draw.IntegerListInputEditor beastfx.app.inputeditor.IntegerListInputEditor
beast.app.draw.ListInputEditor beastfx.app.inputeditor.ListInputEditor
beast.app.draw.LongInputEditor beastfx.app.inputeditor.LongInputEditor
beast.app.draw.ModelBuilder beastfx.app.tools.ModelBuilder
beast.app.draw.MyAction beastfx.app.inputeditor.MyAction
beast.app.draw.ParameterInputEditor beastfx.app.inputeditor.ParameterInputEditor
beast.app.draw.SmallButton beastfx.app.inputeditor.SmallButton
beast.app.draw.SmallLabel beastfx.app.inputeditor.SmallLabel
beast.app.draw.StringInputEditor beastfx.app.inputeditor.StringInputEditor
beast.app.draw.TaxonSetInputEditor beastfx.app.inputeditor.TaxonSetInputEditor
beast.app.tools.AppLauncherLauncher beast.pkgmgmt.launcher.AppLauncherLauncher
beast.app.tools.LogCombinerLauncher beast.pkgmgmt.launcher.LogCombinerLauncher
beast.app.treeannotator.FileDrop beastfx.app.util.FileDrop
beast.app.treeannotator.TreeAnnotatorLauncher beast.pkgmgmt.launcher.TreeAnnotatorLauncher
beast.app.util.Application beastfx.app.tools.Application
beast.app.util.Arguments beast.pkgmgmt.Arguments
beast.app.util.Utils6 beast.pkgmgmt.Utils6
beast.app.util.Version beast.pkgmgmt.Version
beast.app.util.WholeNumberField beastfx.app.beastapp.WholeNumberField
beast.core.BEASTInterface beast.base.core.BEASTInterface
beast.core.BEASTObject beast.base.core.BEASTObject
beast.core.BEASTObjectStore beast.base.core.BEASTObjectStore
beast.core.CalculationNode beast.base.inference.CalculationNode
beast.core.Citation beast.base.core.Citation
beast.core.Description beast.base.core.Description
beast.core.DirectSimulator beast.base.inference.DirectSimulator
beast.core.Evaluator beast.base.inference.Evaluator
beast.core.Function beast.base.core.Function
beast.core.Input beast.base.core.Input
beast.core.InputForAnnotatedConstructor beast.base.core.InputForAnnotatedConstructor
beast.core.Loggable beast.base.core.Loggable
beast.core.Logger beast.base.inference.Logger
beast.core.MCMC beast.base.inference.MCMC
beast.core.Operator beast.base.inference.Operator
beast.core.OperatorSchedule beast.base.inference.OperatorSchedule
beast.core.Param beast.base.core.Param
beast.core.Runnable beast.base.inference.Runnable
beast.core.State beast.base.inference.State
beast.core.StateNode beast.base.inference.StateNode
beast.core.StateNodeInitialiser beast.base.inference.StateNodeInitialiser
beast.core.VirtualBEASTObject beast.base.core.VirtualBEASTObject
beast.core.parameter.BooleanParameter beast.base.inference.parameter.BooleanParameter
beast.core.parameter.BooleanParameterList beast.base.inference.parameter.BooleanParameterList
beast.core.parameter.CompoundRealParameter beast.base.inference.parameter.CompoundRealParameter
beast.core.parameter.CompoundValuable beast.base.inference.parameter.CompoundValuable
beast.core.parameter.GeneralParameterList beast.base.inference.parameter.GeneralParameterList
beast.core.parameter.IntegerParameter beast.base.inference.parameter.IntegerParameter
beast.core.parameter.IntegerParameterList beast.base.inference.parameter.IntegerParameterList
beast.core.parameter.Map beast.base.inference.parameter.Map
beast.core.parameter.Parameter beast.base.inference.parameter.Parameter
beast.core.parameter.RealParameter beast.base.inference.parameter.RealParameter
beast.core.parameter.RealParameterList beast.base.inference.parameter.RealParameterList
beast.core.util.CompoundDistribution beast.base.inference.CompoundDistribution
beast.core.util.ESS beast.base.inference.util.ESS
beast.core.util.Log beast.base.core.Log
beast.core.util.Sum beast.base.evolution.Sum
beast.evolution.alignment.Alignment beast.base.evolution.alignment.Alignment
beast.evolution.alignment.AscertainedAlignment beast.base.evolution.alignment.AscertainedAlignment
beast.evolution.alignment.FilteredAlignment beast.base.evolution.alignment.FilteredAlignment
beast.evolution.alignment.Sequence beast.base.evolution.alignment.Sequence
beast.evolution.alignment.TaxonSet beast.base.evolution.alignment.TaxonSet
beast.evolution.alignment.distance.Distance beast.base.evolution.distance.Distance
beast.evolution.alignment.distance.F84Distance beast.base.evolution.distance.F84Distance
beast.evolution.alignment.distance.HammingDistance beast.base.evolution.distance.HammingDistance
beast.evolution.alignment.distance.JukesCantorDistance beast.base.evolution.distance.JukesCantorDistance
beast.evolution.alignment.distance.SMMDistance beast.base.evolution.distance.SMMDistance
beast.evolution.branchratemodel.BranchRateModel beast.base.evolution.branchratemodel.BranchRateModel
beast.evolution.branchratemodel.RandomLocalClockModel beast.base.evolution.branchratemodel.RandomLocalClockModel
beast.evolution.branchratemodel.RateStatistic beast.base.evolution.RateStatistic
beast.evolution.branchratemodel.StrictClockModel beast.base.evolution.branchratemodel.StrictClockModel
beast.evolution.branchratemodel.UCRelaxedClockModel beast.base.evolution.branchratemodel.UCRelaxedClockModel
beast.evolution.datatype.Aminoacid beast.base.evolution.datatype.Aminoacid
beast.evolution.datatype.Binary beast.base.evolution.datatype.Binary
beast.evolution.datatype.DataType beast.base.evolution.datatype.DataType
beast.evolution.datatype.IntegerData beast.base.evolution.datatype.IntegerData
beast.evolution.datatype.Nucleotide beast.base.evolution.datatype.Nucleotide
beast.evolution.datatype.StandardData beast.base.evolution.datatype.StandardData
beast.evolution.datatype.TwoStateCovarion beast.base.evolution.datatype.TwoStateCovarion
beast.evolution.datatype.UserDataType beast.base.evolution.datatype.UserDataType
beast.evolution.likelihood.BeagleTreeLikelihood beast.base.evolution.likelihood.BeagleTreeLikelihood
beast.evolution.likelihood.BeerLikelihoodCore beast.base.evolution.likelihood.BeerLikelihoodCore
beast.evolution.likelihood.BeerLikelihoodCore4 beast.base.evolution.likelihood.BeerLikelihoodCore4
beast.evolution.likelihood.GenericTreeLikelihood beast.base.evolution.likelihood.GenericTreeLikelihood
beast.evolution.likelihood.LikelihoodCore beast.base.evolution.likelihood.LikelihoodCore
beast.evolution.likelihood.ThreadedBeerLikelihoodCore beast.base.evolution.likelihood.ThreadedBeerLikelihoodCore
beast.evolution.likelihood.ThreadedBeerLikelihoodCore4 beast.base.evolution.likelihood.ThreadedBeerLikelihoodCore4
beast.evolution.likelihood.ThreadedLikelihoodCore beast.base.evolution.likelihood.ThreadedLikelihoodCore
beast.evolution.likelihood.ThreadedTreeLikelihood beast.base.evolution.likelihood.ThreadedTreeLikelihood
beast.evolution.likelihood.TreeLikelihood beast.base.evolution.likelihood.TreeLikelihood
beast.evolution.operators.AdaptableVarianceMultivariateNormalOperator beast.base.evolution.operator.kernel.AdaptableVarianceMultivariateNormalOperator
beast.evolution.operators.BactrianDeltaExchangeOperator beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator
beast.evolution.operators.BactrianIntervalOperator beast.base.inference.operator.kernel.BactrianIntervalOperator
beast.evolution.operators.BactrianNodeOperator beast.base.evolution.operator.kernel.BactrianNodeOperator
beast.evolution.operators.BactrianOperatorSchedule beast.base.evolution.operator.kernel.BactrianOperatorSchedule
beast.evolution.operators.BactrianRandomWalkOperator beast.base.inference.operator.kernel.BactrianRandomWalkOperator
beast.evolution.operators.BactrianScaleOperator beast.base.evolution.operator.kernel.BactrianScaleOperator
beast.evolution.operators.BactrianSubtreeSlide beast.base.evolution.operator.kernel.BactrianSubtreeSlide
beast.evolution.operators.BactrianTipDatesRandomWalker beast.base.evolution.operator.kernel.BactrianTipDatesRandomWalker
beast.evolution.operators.BactrianUpDownOperator beast.base.inference.operator.kernel.BactrianUpDownOperator
beast.evolution.operators.BitFlipOperator beast.base.inference.operator.BitFlipOperator
beast.evolution.operators.CompoundParameterHelper beast.base.inference.operator.CompoundParameterHelper
beast.evolution.operators.DeltaExchangeOperator beast.base.inference.operator.DeltaExchangeOperator
beast.evolution.operators.Exchange beast.base.evolution.operator.Exchange
beast.evolution.operators.IntRandomWalkOperator beast.base.inference.operator.IntRandomWalkOperator
beast.evolution.operators.IntUniformOperator beast.base.inference.operator.IntUniformOperator
beast.evolution.operators.JointOperator beast.base.inference.operator.JointOperator
beast.evolution.operators.KernelDistribution beast.base.inference.operator.kernel.KernelDistribution
beast.evolution.operators.KernelOperator beast.base.inference.operator.kernel.KernelOperator
beast.evolution.operators.NodeReheight beast.base.evolution.operator.NodeReheight
beast.evolution.operators.RealRandomWalkOperator beast.base.inference.operator.RealRandomWalkOperator
beast.evolution.operators.ScaleOperator beast.base.evolution.operator.ScaleOperator
beast.evolution.operators.SliceOperator beast.base.inference.operator.SliceOperator
beast.evolution.operators.SwapOperator beast.base.inference.operator.SwapOperator
beast.evolution.operators.TipDatesRandomWalker beast.base.evolution.operator.TipDatesRandomWalker
beast.evolution.operators.TipDatesScaler beast.base.evolution.operator.TipDatesScaler
beast.evolution.operators.UniformOperator beast.base.inference.operator.UniformOperator
beast.evolution.operators.Uniform beast.base.evolution.operator.Uniform
beast.evolution.operators.UpDownOperator beast.base.inference.operator.UpDownOperator
beast.evolution.sitemodel.SiteModel beast.base.evolution.sitemodel.SiteModel
beast.evolution.sitemodel.SiteModelInterface beast.base.evolution.sitemodel.SiteModelInterface
beast.evolution.speciation.BirthDeathGernhard08Model beast.base.evolution.speciation.BirthDeathGernhard08Model
beast.evolution.speciation.CalibratedBirthDeathModel beast.base.evolution.speciation.CalibratedBirthDeathModel
beast.evolution.speciation.CalibratedYuleInitialTree beast.base.evolution.speciation.CalibratedYuleInitialTree
beast.evolution.speciation.CalibratedYuleModel beast.base.evolution.speciation.CalibratedYuleModel
beast.evolution.speciation.CalibrationLineagesIterator beast.base.evolution.speciation.CalibrationLineagesIterator
beast.evolution.speciation.CalibrationPoint beast.base.evolution.speciation.CalibrationPoint
beast.evolution.speciation.GeneTreeForSpeciesTreeDistribution beast.base.evolution.speciation.GeneTreeForSpeciesTreeDistribution
beast.evolution.speciation.RandomGeneTree beast.base.evolution.speciation.RandomGeneTree
beast.evolution.speciation.SpeciesTreeDistribution beast.base.evolution.speciation.SpeciesTreeDistribution
beast.evolution.speciation.SpeciesTreeLogger beast.base.evolution.speciation.SpeciesTreeLogger
beast.evolution.speciation.SpeciesTreePopFunction beast.base.evolution.speciation.SpeciesTreePopFunction
beast.evolution.speciation.SpeciesTreePrior beast.base.evolution.speciation.SpeciesTreePrior
beast.evolution.speciation.StarBeastStartState beast.base.evolution.speciation.StarBeastStartState
beast.evolution.speciation.TreeTopFinder beast.base.evolution.speciation.TreeTopFinder
beast.evolution.speciation.YuleModel beast.base.evolution.speciation.YuleModel
beast.evolution.substitutionmodel.BinaryCovarion beast.base.evolution.substitutionmodel.BinaryCovarion
beast.evolution.substitutionmodel.Blosum62 beast.base.evolution.substitutionmodel.Blosum62
beast.evolution.substitutionmodel.CPREV beast.base.evolution.substitutionmodel.CPREV
beast.evolution.substitutionmodel.Dayhoff beast.base.evolution.substitutionmodel.Dayhoff
beast.evolution.substitutionmodel.DefaultEigenSystem beast.base.evolution.substitutionmodel.DefaultEigenSystem
beast.evolution.substitutionmodel.EigenDecomposition beast.base.evolution.substitutionmodel.EigenDecomposition
beast.evolution.substitutionmodel.EigenSystem beast.base.evolution.substitutionmodel.EigenSystem
beast.evolution.substitutionmodel.EmpiricalSubstitutionModel beast.base.evolution.substitutionmodel.EmpiricalSubstitutionModel
beast.evolution.substitutionmodel.Frequencies beast.base.evolution.substitutionmodel.Frequencies
beast.evolution.substitutionmodel.GTR beast.base.evolution.substitutionmodel.GTR
beast.evolution.substitutionmodel.GeneralSubstitutionModel beast.base.evolution.substitutionmodel.GeneralSubstitutionModel
beast.evolution.substitutionmodel.HKY beast.base.evolution.substitutionmodel.HKY
beast.evolution.substitutionmodel.JTT beast.base.evolution.substitutionmodel.JTT
beast.evolution.substitutionmodel.JukesCantor beast.base.evolution.substitutionmodel.JukesCantor
beast.evolution.substitutionmodel.MTREV beast.base.evolution.substitutionmodel.MTREV
beast.evolution.substitutionmodel.MutationDeathModel beast.base.evolution.substitutionmodel.MutationDeathModel
beast.evolution.substitutionmodel.SYM beast.base.evolution.substitutionmodel.SYM
beast.evolution.substitutionmodel.SubstitutionModel beast.base.evolution.substitutionmodel.SubstitutionModel
beast.evolution.substitutionmodel.TIM beast.base.evolution.substitutionmodel.TIM
beast.evolution.substitutionmodel.TN93 beast.base.evolution.substitutionmodel.TN93
beast.evolution.substitutionmodel.TVM beast.base.evolution.substitutionmodel.TVM
beast.evolution.substitutionmodel.WAG beast.base.evolution.substitutionmodel.WAG
beast.evolution.taxonomy.Taxon beast.base.evolution.alignment.Taxon
beast.evolution.tree.CladeSet beast.base.evolution.tree.CladeSet
beast.evolution.tree.Node beast.base.evolution.tree.Node
beast.evolution.tree.RandomTree beast.base.evolution.tree.coalescent.RandomTree
beast.evolution.tree.TraitSet beast.base.evolution.tree.TraitSet
beast.evolution.tree.TreeDistribution beast.base.evolution.tree.TreeDistribution
beast.evolution.tree.TreeHeightLogger beast.base.evolution.tree.TreeHeightLogger
beast.evolution.tree.TreeInterface beast.base.evolution.tree.TreeInterface
beast.evolution.tree.TreeMetric beast.base.evolution.tree.TreeMetric
beast.evolution.tree.TreeStatLogger beast.base.evolution.tree.TreeStatLogger
beast.evolution.tree.TreeTraceAnalysis beastfx.app.tools.TreeTraceAnalysis
beast.evolution.tree.TreeUtils beast.base.evolution.tree.TreeUtils
beast.evolution.tree.TreeWithMetaDataLogger beast.base.evolution.TreeWithMetaDataLogger
beast.evolution.tree.Tree beast.base.evolution.tree.Tree
beast.evolution.tree.coalescent.BayesianSkyline beast.base.evolution.tree.coalescent.BayesianSkyline
beast.evolution.tree.coalescent.Coalescent beast.base.evolution.tree.coalescent.Coalescent
beast.evolution.tree.coalescent.CompoundPopulationFunction beast.base.evolution.tree.coalescent.CompoundPopulationFunction
beast.evolution.tree.coalescent.ConstantPopulation beast.base.evolution.tree.coalescent.ConstantPopulation
beast.evolution.tree.coalescent.ExponentialGrowth beast.base.evolution.tree.coalescent.ExponentialGrowth
beast.evolution.tree.coalescent.IntervalList beast.base.evolution.tree.IntervalList
beast.evolution.tree.coalescent.IntervalType beast.base.evolution.tree.IntervalType
beast.evolution.tree.coalescent.PopulationFunction beast.base.evolution.tree.coalescent.PopulationFunction
beast.evolution.tree.coalescent.SampleOffValues beast.base.evolution.tree.coalescent.SampleOffValues
beast.evolution.tree.coalescent.ScaledPopulationFunction beast.base.evolution.tree.coalescent.ScaledPopulationFunction
beast.evolution.tree.coalescent.TreeIntervals beast.base.evolution.tree.TreeIntervals
beast.math.Binomial beast.base.util.Binomial
beast.math.GammaFunction beast.base.util.GammaFunction
beast.math.LogTricks beast.base.math.LogTricks
beast.math.MachineAccuracy beast.base.util.MachineAccuracy
beast.math.distributions.ChiSquare beast.base.inference.distribution.ChiSquare
beast.math.distributions.Dirichlet beast.base.inference.distribution.Dirichlet
beast.math.distributions.Exponential beast.base.inference.distribution.Exponential
beast.math.distributions.Gamma beast.base.inference.distribution.Gamma
beast.math.distributions.InverseGamma beast.base.inference.distribution.InverseGamma
beast.math.distributions.LaplaceDistribution beast.base.inference.distribution.LaplaceDistribution
beast.math.distributions.LogNormalDistributionModel beast.base.inference.distribution.LogNormalDistributionModel
beast.math.distributions.MRCAPrior beast.base.evolution.tree.MRCAPrior
beast.math.distributions.MarkovChainDistribution beast.base.inference.distribution.MarkovChainDistribution
beast.math.distributions.Normal beast.base.inference.distribution.Normal
beast.math.distributions.OneOnX beast.base.inference.distribution.OneOnX
beast.math.distributions.ParametricDistribution beast.base.inference.distribution.ParametricDistribution
beast.math.distributions.Poisson beast.base.inference.distribution.Poisson
beast.math.matrixalgebra.CholeskyDecomposition beast.base.math.matrixalgebra.CholeskyDecomposition
beast.math.matrixalgebra.IllegalDimension beast.base.math.matrixalgebra.IllegalDimension
beast.math.matrixalgebra.LUPDecomposition beast.base.math.matrixalgebra.LUPDecomposition
beast.math.matrixalgebra.LinearEquations beast.base.math.matrixalgebra.LinearEquations
beast.math.matrixalgebra.Matrix beast.base.math.matrixalgebra.Matrix
beast.math.matrixalgebra.NonSymmetricComponents beast.base.math.matrixalgebra.NonSymmetricComponents
beast.math.matrixalgebra.RobustEigenDecomposition beast.base.math.matrixalgebra.RobustEigenDecomposition
beast.math.matrixalgebra.RobustSingularValueDecomposition beast.base.math.matrixalgebra.RobustSingularValueDecomposition
beast.math.matrixalgebra.SymmetricMatrix beast.base.math.matrixalgebra.SymmetricMatrix
beast.math.matrixalgebra.Vector beast.base.math.matrixalgebra.Vector
beast.math.statistic.RPNcalculator beast.base.inference.util.RPNcalculator
beast.math.statistic.RPNexpressionCalculator beast.base.inference.util.RPNexpressionCalculator
beast.math.util.MathUtils beast.base.math.MathUtils
beast.prevalence.SubtreeSlide beast.base.evolution.operator.SubtreeSlide
beast.prevalence.TreeOperator beast.base.evolution.operator.TreeOperator
beast.prevalence.WilsonBalding beast.base.evolution.operator.WilsonBalding
beast.util.BEASTClassLoader beast.pkgmgmt.BEASTClassLoader
beast.util.ClassToPackageMap beast.base.parser.ClassToPackageMap
beast.util.ClusterTree beast.base.evolution.tree.ClusterTree
beast.util.CollectionUtils beast.base.util.CollectionUtils
beast.util.CredibleSet beast.base.util.CredibleSet
beast.util.FrequencySet beast.base.util.FrequencySet
beast.util.HeapSort beast.base.util.HeapSort
beast.util.InputType beast.base.parser.InputType
beast.util.JSONParser beast.base.parser.JSONParser
beast.util.JSONParserException beast.base.parser.JSONParserException
beast.util.JSONProducer beast.base.parser.JSONProducer
beast.util.LogComparator beastfx.app.tools.LogComparator
beast.util.MersenneTwisterFast beast.base.util.MersenneTwisterFast
beast.util.NexusParser beast.base.parser.NexusParser
beast.util.NexusParserListener beast.base.parser.NexusParserListener
beast.util.OutputUtils beast.base.parser.OutputUtils
beast.util.Package beast.pkgmgmt.Package
beast.util.PackageDependency beast.pkgmgmt.PackageDependency
beast.util.PackageManager beast.pkgmgmt.PackageManager
beast.util.PackageVersion beast.pkgmgmt.PackageVersion
beast.util.Randomizer beast.base.util.Randomizer
beast.util.Transform beast.base.inference.operator.kernel.Transform
beast.util.TreeParser beast.base.evolution.tree.TreeParser
beast.util.XMLParser beast.base.parser.XMLParser
beast.util.XMLParserException beast.base.parser.XMLParserException
beast.util.XMLParserUtils beast.base.parser.XMLParserUtils
beast.util.XMLProducer beast.base.parser.XMLProducer
beast.util.treeparser.NewickLexer beast.base.evolution.tree.treeparser.NewickLexer
beast.util.treeparser.NewickParser beast.base.evolution.tree.treeparser.NewickParser
beast.util.treeparser.NewickParserBaseListener beast.base.evolution.tree.treeparser.NewickParserBaseListener
beast.util.treeparser.NewickParserBaseVisitor beast.base.evolution.tree.treeparser.NewickParserBaseVisitor
beast.util.treeparser.NewickParserListener beast.base.evolution.tree.treeparser.NewickParserListener
beast.util.treeparser.NewickParserVisitor beast.base.evolution.tree.treeparser.NewickParserVisitor
biceps.operators.EpochFlexOperator beast.base.evolution.operator.EpochFlexOperator
biceps.operators.TreeStretchOperator beast.base.evolution.operator.TreeStretchOperator
orc.operators.AdaptableOperatorSampler beast.base.evolution.operator.AdaptableOperatorSampler
beast.core.Distribution beast.base.inference.Distribution
beast.math.distributions.Beta beast.base.inference.distribution.Beta
beast.util.LogAnalyser beastfx.app.tools.LogAnalyser
beast.math.distributions.Prior beast.base.inference.distribution.Prior
beast.math.distributions.Uniform beast.base.inference.distribution.Uniform
beast.core.Distribution beast.base.inference.Distribution
beast.evolution.alignment.Taxon beast.base.evolution.alignment.Taxon
beast.evolution.tree.TreeTraceAnalysis beastfx.app.tools.TreeTraceAnalysis
beast.evolution.operators.TreeOperator beast.base.evolution.operator.TreeOperator
beast.core.util.Evaluator beast.base.inference.Evaluator
beast.math.statistic.DiscreteStatistics beast.base.util.DiscreteStatistics
Beauti.g_sDir ProgramStatus.g_sDir
Beauti.setCurrentDir ProgramStatus.setCurrentDir
BeastMCMC.m_nThreads ProgramStatus.m_nThreads
BeastMCMC.g_exec ProgramStatus.g_exec
org.apache.commons.math.distribution.Distribution beast.base.inference.Distribution
beast.core: beast.base.core:beast.base.inference:
beast.core.util: beast.base.util:
beast.math.distributions.WeibullDistribution beastlabs.math.distributions.WeibullDistribution
"TreeIntervals" "beast.base.evolution.tree.TreeIntervals"
"util.CompoundDistribution" "beast.base.inference.CompoundDistribution"
"BactrianDeltaExchangeOperator" "beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator"
"BactrianIntervalOperator" "beast.base.inference.operator.kernel.BactrianIntervalOperator"
"BactrianNodeOperator" "beast.base.evolution.operator.kernel.BactrianNodeOperator"
"BactrianOperatorSchedule" "beast.base.evolution.operator.kernel.BactrianOperatorSchedule"
"BactrianRandomWalkOperator" "beast.base.inference.operator.kernel.BactrianRandomWalkOperator"
"BactrianScaleOperator" "beast.base.evolution.operator.kernel.BactrianScaleOperator"
"BactrianSubtreeSlide" "beast.base.evolution.operator.kernel.BactrianSubtreeSlide"
"BactrianTipDatesRandomWalker" "beast.base.evolution.operator.kernel.BactrianTipDatesRandomWalker"
"BactrianUpDownOperator" "beast.base.inference.operator.kernel.BactrianUpDownOperator"
"LG" "beastclassic.evolution.substitutionmodel.LG"
beast.inference.PathSampler modelselection.inference.PathSampler
starbeast3.SpeciesTree starbeast3.tree.SpeciesTree
starbeast3.SpeciesTreePrior starbeast3.evolution.speciation.SpeciesTreePrior
starbeast3.GeneTreeForSpeciesTreeDistribution starbeast3.evolution.speciation.GeneTreeForSpeciesTreeDistribution
starbeast3.StarBeast3Clock starbeast3.evolution.branchratemodel.StarBeast3Clock
starbeast3.StarBeastStartState starbeast3.core.StarBeastStartState
starbeast3.SpeciesTreeLogger starbeast3.core.SpeciesTreeLogger
starbeast3.GeneTreeLogger beast.base.evolution.TreeWithMetaDataLogger
beast.base.inference.OperatorScheduleRecalculator starbeast3.core.OperatorScheduleRecalculator
beast.evolution.tree.RNNIMetric beastlabs.evolution.tree.RNNIMetric
beast.base.evolution.tree.TreeDistanceLogger beastlabs.evolution.tree.TreeDistanceLogger
;

if ($#ARGV < 0) {
	print "Usage: perl migrate.pl <directory or file>\n";
	print "If a directory is specified, it recursively attempts to convert java class + xml files from BEAST v2.6.x to v2.7.0\n";
	print "If a file is specified, only that file is converted.";
	exit(0);
}
@s = split('\n',$s);
foreach $s (@s) {
	$s =~/(.*) (.*)/;
	$from = $1;
	$to = $2;
	$from =~ s/"/["']/g;
	$map{$from}=$to;
	
}

$dir = $ARGV[0];

if (-d $dir) {
	open(FIN0,"find $dir|egrep \"java|xml\"|");
	while ($s = <FIN0>) {
		$s =~ s/\n//;
		process($s);
	}
	close FIN0;
} elsif (-e $dir) {
	process($dir);
} else {
	print STDER "File or directory $dir could not be found.";
}

exit(0);

sub process {
	$file = shift;
	print STDERR "Processing $file\n";
	$text = '';
	open (FIN,$file);
	while ($s = <FIN>) {
		if ($s =~ /namespace=/) {
			foreach $x (keys(%namespacemap)) {
				$s =~ s/$x/$namespacemap{$x}/g;
			}
		}
		$text .= $s;
	}
	close FIN;
	foreach $s (keys(%map)) {
		$text =~ s/$s/$map{$s}/g;
	}
	$text =~ s/beast.base.evolution.tree.TreeWithMetaDataLogger/beast.base.evolution.TreeWithMetaDataLogger/g;
	open(FOUT,">$file");
	print FOUT $text;
	close FOUT;
	
}
