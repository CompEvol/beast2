********* Developer Guide to Generate Tutorials in Html ********

The html version of tutorials are in the group SVN compevol/website/beast2org/tutorials. 
Please do not check in to the Git. 

To generate html: 
1) you need to copy *.tex, *.bib and style file if there is any to the working tutorial 
folder, such as compevol/website/beast2org/tutorials/DivergenceDating;

2) run script tex2html given the latex file name without extensions, 
such as ../tex2html StarBEAST_tutorial;

3) check the generated html, and copy it to the VM to update website.
  