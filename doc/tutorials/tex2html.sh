#!/usr/bin/env bash
# input latex file name without extensions, not handle path
# for example: 
# cd doc/tutorials/STARBEAST
# ../tex2html StarBEAST_tutorial

TEX=$1

if [[ $string == *"."* ]]; then
	echo "input latex file name without extensions !"
	exit 0
fi

# compile latex
pdflatex $1
bibtex $1
pdflatex $1
pdflatex $1

# generate raw html
hevea $1

# modify html
sed -i.bak 's/style>/style><link rel=\"stylesheet\" type=\"text\/css\" href=\"..\/..\/beast2.css\">/g' $1.html

# TODO: how use parameter to replace \1 or $1?
sed -i.bak 's/<body >/<body ><div id=\"container\">/g' $1.html

sed -i.bak 's/<\/body>/<\/div><\/body>/g' $1.html
