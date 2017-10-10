                    BEAST v2.4.7 2017
                 Beast 2 development team 2011-2017

Last updated: July 2017

Contents:
1) INTRODUCTION
2) INSTALLING BEAST
3) CONVERTING SEQUENCES
4) RUNNING BEAST
5) ANALYZING RESULTS
6) NATIVE LIBRARIES
7) UNINSTALLING OLDER BEAST 2.x.x VERSIONS
8) SUPPORT & LINKS
9) ACKNOWLEDGMENTS 

___________________________________________________________________________
1) INTRODUCTION

BEAST (Bayesian evolutionary analysis sampling trees) is package for
evolutionary inference from molecular sequences.

BEAST uses a complex and powerful input format (specified in XML) to
describe the evolutionary model. This has advantages in terms of
flexibility in that the developers of BEAST do not have to try and predict
every analysis that researchers may wish to perform and explicitly provide
an option for doing it. However, this flexibility means it is possible to
construct models that don't perform well under the Markov chain Monte Carlo
(MCMC) inference framework used. We cannot test every possible model that
can be used in BEAST. There are two solutions to this: Firstly, we  supply
a range of recipes for commonly performed analyses that we know should work
in BEAST and provide example input files for these (although, the actual
data can also produce unexpected behavour). Secondly, we provide advice and
tools for the diagnosis of problems and suggestions on how to fix them:

<http://beast2.cs.auckland.ac.nz/>

BEAST is not a black-box into which you can put your data and expect an
easily interpretable answer. It requires careful inspection of the output
to check that it has performed correctly and usually will need tweaking,
adjustment and a number of runs to get a valid answer. Sorry.
___________________________________________________________________________
2) INSTALLING BEAST

BEAST requires a Java Virtual Machine to run. Many systems will already
have this installed. It requires at least version 1.8 of Java to run. The
latest versions of Java can be downloaded from:

<http://java.com/>

If in doubt type "java -version" to see what version of java is installed
(or if it is installed at all).

Mac OS X will already have a suitable version of Java installed.

Within the BEAST package will be the following directories:
Directory       Contents
doc/            Documentation of BEAST
examples/       Some NEXUS and XML files
lib/            Java & native libraries used by BEAST 
bin/            Scripts of the corresponding OS
templates/      Templates to initiate BEAUti

___________________________________________________________________________
3) CONVERTING SEQUENCES

A program called "BEAUti" will import data in NEXUS format, allow you to
select various models and options and generate an XML file ready for use in
BEAST.

To run BEAUti simply double-click the "BEAUti.exe" file in the BEAST
folder. If this doesn't work then you may not have Java installed correctly. 
Try opening an MS-DOS window and typing:

	java -cp lib/beast.jar beast.app.beauti.Beauti

__________________________________________________________________________
4) RUNNING BEAST

To run BEAST simply double-click the "BEAST.exe" file in the BEAST
folder. You will be asked to select a BEAST XML input file.

Alternatively open a Command window and type:
	
	java -jar lib/beast.jar input.xml

Where "input.xml" is the name of a BEAST XML format file. This file can
either be created from scratch using a text editor or be created by the
BEAUti program from a NEXUS format file. 

For documentation on creating and tuning the input files look at the
documentation and tutorials on-line at:

Help -      <http://beast2.org/>
FAQ -       <http://beast2.org/faq/>
Tutorials - <http://beast2.org/tutorials/>

BEAST arguments:
    -window Provide a console window
    -options Display an options dialog
    -working Change working directory to input file's directory
    -seed Specify a random number generator seed
    -prefix Specify a prefix for all output log filenames
    -statefile Specify the filename for storing/restoring the state
    -overwrite Allow overwriting of log files
    -resume Allow appending of log files
    -validate Parse the XML, but do not run -- useful for debugging XML
    -errors Specify maximum number of numerical errors before stopping
    -threads The number of computational threads to use (default auto)
    -java Use Java only, no native implementations
    -noerr Suppress all output to standard error
    -loglevel error,warning,info,debug,trace
    -beagle Use beagle library if available
    -beagle_info BEAGLE: show information on available resources
    -beagle_order BEAGLE: set order of resource use
    -beagle_instances BEAGLE: divide site patterns amongst instances
    -beagle_CPU BEAGLE: use CPU instance
    -beagle_GPU BEAGLE: use GPU instance if available
    -beagle_SSE BEAGLE: use SSE extensions if available
    -beagle_single BEAGLE: use single precision if available
    -beagle_double BEAGLE: use double precision if available
    -beagle_scaling BEAGLE: specify scaling scheme to use
    -help Print this information and stop
    -version Print version and stop
    -strictversions Use only package versions as specified in the 'required' attribute

For example:

     java -jar lib/beast.jar -seed 123456 -overwrite input.xml

On Linux, there is a bin directory inside the beast directory that has 
a number of scripts for starting programs, including beast, and beauti, 
so to start beast (or beauti) from a terminal, you can use

/path/to/beast/bin/beast

with one of the arguments above.
_________________________________________________________________
5) ANALYZING RESULTS

We have produced a powerful graphical program for analysing MCMC log files
(it can also analyse output from MrBayes and other MCMCs). This is called
'Tracer' and is available from the Tracer web site:

<http://tree.bio.ed.ac.uk/software/tracer>

Additionally, two new programs are distributed as part of the BEAST
package: LogCombiner & TreeAnnotator. LogCombiner can combine log or tree
files from multiple runs of BEAST into a single combined results file
(after removing appropriate burn-ins). TreeAnnotator can summarize a sample
of trees from BEAST using a single target tree, annotating it with
posterior probabilities, HPD node heights and rates. This tree can then be
viewed in a new program called 'FigTree' which is available from:

<http://tree.bio.ed.ac.uk/software/figtree>

or 'DensiTree' available from BEAST package.

___________________________________________________________________________
6) NATIVE LIBRARIES

We recommend that you install the BEAGLE library. BEAST attempts to use 
BEAGLE by default and this can speed up running BEAST considerably. The 
BEAGLE library needs to be installed separately from BEAST, and can be 
obtained from:

https://github.com/beagle-dev/beagle-lib/blob/master/README.md


___________________________________________________________________________
7) UNINSTALLING OLDER BEAST 2.x.x VERSIONS

If you installed BEAST version 2.x.x, you do no longer need to remove the 
packages (add-ons) installed with that version of BEAST. But if you want to
uninstall these packages, the easiest way to do this is in BEAUti
version 2.x.x, using the add-on manager under the menu File/Mangage add-ons.

Alternatively, delete the package directory:
C:\Users\<yourusername>\BEAST                            on Windows
/Users/<yourusername>/Library/Application Support/BEAST  on Mac
/home/<yourusername>/.beast                              on Linux

___________________________________________________________________________
8) SUPPORT & LINKS

BEAST is an extremely complex program and as such will inevitably have
bugs. Please email us to discuss any problems:

<remco@cs.auckland.ac.nz>
<alexei@cs.auckland.ac.nz>
<a.rambaut@ed.ac.uk>
<msuchard@ucla.edu>

The BEAST users' mailing-list 
<https://groups.google.com/forum/#!forum/beast-users>

The website for beast is here:

<http://beast2.org/>

Source code distributed under the GNU Lesser General Public License:

<https://github.com/CompEvol/beast2>

___________________________________________________________________________
9) ACKNOWLEDGMENTS

Thanks for supplying code or assisting with the creation
or testing of Beast 2 development team.



