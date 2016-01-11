all: ebsp2-tut

ebsp2-tut: ebsp2-tut.pdf
	mkdir ebsp2-tut
	cp -R ebsp2-tut.pdf scripts mystery-mammal ebsp2-tut
	zip -r ebsp2-tut.zip ebsp2-tut
	rm -rf ebsp2-tut

ebsp2-tut.pdf: ebsp2-tut.tex
	pdflatex ebsp2-tut
	bibtex ebsp2-tut
	pdflatex ebsp2-tut
	pdflatex ebsp2-tut

clean:
	rm -f ebsp2-tut.aux ebsp2-tut.bbl ebsp2-tut.blg ebsp2-tut.log ebsp2-tut.pdf ebsp2-tut.zip
