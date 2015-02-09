********* Developer Guide to Generate Tutorials in Html ********

The html version of tutorials are in the group SVN compevol/website/beast2org/tutorials. 
Please do not check in to the Git. 

To generate html: 
1) install hevea (Homebrew => Opam => Hevea) and pdflatex; 

2) make the latex version tutorial and figures ready;
add the following new command to the top of latex:
\newcommand{\includeimage}[2][]{%
%HEVEA\imgsrc{#2.hevea.png}%
%BEGIN LATEX
\includegraphics[#1]{#2}
%END LATEX
}

And replace all \includegraphics into \includeimage, such as 
\begin{figure}
\centering	
\includeimage[width=0.6\textwidth]{figures/BEAUti_GuessDates}
\label{fig:BEAUti_GuessDates}
\end{figure}
 
3) convert all figures to png, and add .hevea before .png, such as BEAUti_GuessDates.hevea.png

4) you need to work in the Git tutorial source folder, because the script requires *.tex, *.bib 
and style file if there is any to generate html, such as doc/tutorials/DivergenceDating;

5) run script tex2html given the latex file name without extensions, 
such as ../tex2html StarBEAST_tutorial;
Note: if hevea is installed but not appear, run "eval `opam config env`" in command line.

6) copy html to SVN compevol/website/beast2org/tutorials/DivergenceDating, and check what it looks;

7) if good, then commit to SVN, otherwise edit latex or figures and then repeat 5);

8) copy html or figures to the VM beast2.org to update website.
  