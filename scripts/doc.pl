#/usr/bin/perl
# Script that copies all files from src directory to src2 directory
# and extracts @Description and Input.description info into comments
# so that javadoc has access to them.

use Cwd;

mkdir("src2");


@list = `find src/|grep java|grep beast|grep -v "\.svn"|grep -v test|egrep -v BeerLikelihoodCore.+.java`;
print $#list;

# grab Description info for abstract classes
foreach $sFile (@list) {
    chomp($sFile);
    print "Processing $sFile\n";
    $sClass = $sFile;
    $sClass =~ s/src.//;
    $sClass =~ s/.java$//;
    $sClass =~ s/\//./g;
    open(FIN,$sFile) or die "Cannot open file $sFile for reading";
    while ($s = <FIN>) {
        if ($s =~ /^\s*\@Description/) {
            # extract Description text
            $d = $s;
            $d =~ s/.*\@Description\s*\(\s*"(.*)".*/$1/;
            while (($s !~ /\bpublic.+class\b/) && ($s !~ /\bpublic.+interface\b/) && ($s = <FIN>)) {
                if (($s !~ /\bpublic.+class\b/) && ($s !~ /\bpublic.+interface\b/)) {
                    $s =~ s/\s*"(.*)".*/$1/;
                    $d .= $s;
                }
            }
            $map{"$sClass \@description"} = $d;
        }
    }
    close FIN;
}


# grab description and input info from DocMaker
open (FIN,"java -cp build:lib/commons-math-2.0.jar beast.app.DocMaker -javadoc |");
while ($s = <FIN>) {
    $s =~ /^([^:]*):([^:]*):(.*)$/;
    $map{"$1 $2"} = $3;
}


# process files: copy each Java class from src to src2
foreach $sFile (@list) {
    chomp($sFile);
    print "Processing $sFile\n";
    $sClass = $sFile;
    $sClass =~ s/src.//;
    $sClass =~ s/.java$//;
    $sClass =~ s/\//./g;
    $sDir = $sFile;
    $sDir =~ s/src/src2/;
    $sDir =~ s/[^\/]*java$//;
    `mkdir -p $sDir`;
    open(FIN,$sFile) or die "Cannot open file $sFile for reading";
    $sFile2 = $sFile;
    $sFile2 =~ s/src/src2/;
    open(FOUT,">$sFile2") or die "Cannot open file $sFile2 for writing";
    while ($s = <FIN>) {
        if ($s =~ /^\s*\@Description/) {
            # extract Description text and insert as comment
            while (($s !~ /\bpublic.+class\b/) && ($s !~ /\bpublic.+interface\b/) && ($s = <FIN>)) {
            }
            if (length($map{"$sClass \@description"}) > 1) {
                print FOUT "\n/**\n * ".$map{"$sClass \@description"}." */\n";
            }
        }
        # insert input description
        if ($s =~ /new\s+Input<.*>\("([^"]*)"\s*,/) {
            print FOUT "/** ".$map{"$sClass $1"}." **/\n";
        }
        print FOUT $s;        
    }
    close FIN;
    close FOUT;
}
#print `cp src/beast/core/Plugin.java src2/beast/core`;
print `cp src/beast/core/Description.java src2/beast/core`;
