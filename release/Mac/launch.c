#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <mach-o/dyld.h>

void normalise(char * in, char * path) {
		//char * s2 = strstr(argv[0], "BEAUti.app");
		//s2[0] = 0;
		//fprintf(fout, "argv0 = %s\n", in);
		int i = 0 ,j = 0;
		while (in[i] != 0 && i < 1024) {
			char c = in[i];
			if (c == ' ') {
				path[j++] = '\\';
			}
			path[j++] = c; 
			i++;
		}
}

int main(int argc, char *argv[]) {
	char s[1024];
	char buf[1024];
	char path[1024];
	uint32_t size = sizeof(buf);
	size = _NSGetExecutablePath(buf, &size);
	
    //FILE *fout;
    //fout=fopen("/tmp/b", "w");

//	fprintf(fout, "size=%lu %lu\n", strlen(buf) , strlen(argv[0]));
	buf[strlen(buf) - strlen(argv[0])] = 0;
	//fprintf(fout, "path = %s\n", buf);
	//fprintf(fout, "argv0 = %s\n", argv[0]);

	if (strstr(argv[0], "AppLauncher.app") != NULL) {
		strstr(argv[0], "AppLauncher.app")[0] = 0;
		normalise(argv[0], path);
		sprintf(s, "%sjre1.8.0_161/bin/java -Xmx4g -Dapple.laf.useScreenMenuBar=true -Djava.library.path=$JAVAROOT:/usr/local/lib -Duser.language=en -cp %slib/launcher.jar beast.app.tools.AppLauncherLauncher", path, path);
	} else if (strstr(argv[0], "BEAST.app") != NULL) {
		strstr(argv[0], "BEAST.app")[0] = 0;
		normalise(argv[0], path);
		sprintf(s, "%sjre1.8.0_161/bin/java -Xmx4g -Dapple.laf.useScreenMenuBar=true -Djava.library.path=$JAVAROOT:/usr/local/lib -Duser.language=en -cp %slib/launcher.jar beast.app.beastapp.BeastLauncher -window -options -working", path, path);
	} else if (strstr(argv[0], "DensiTree.app") != NULL) {
		strstr(argv[0], "DensiTree.app")[0] = 0;
		normalise(argv[0], path);
		sprintf(s, "%sjre1.8.0_161/bin/java -Xmx4g -Dapple.laf.useScreenMenuBar=true -Djava.library.path=$JAVAROOT:/usr/local/lib -Duser.language=en -jar %sDensiTree.app/Contents/Java/DensiTree.jar", path, path);
	} else if (strstr(argv[0], "LogCombiner.app") != NULL) {
		strstr(argv[0], "LogCombiner.app")[0] = 0;
		normalise(argv[0], path);
		sprintf(s, "%sjre1.8.0_161/bin/java -Xmx4g -Dapple.laf.useScreenMenuBar=true -Djava.library.path=$JAVAROOT:/usr/local/lib -Duser.language=en -cp %slib/launcher.jar beast.app.tools.LogCombinerLauncher", path, path);
	} else if (strstr(argv[0], "TreeAnnotator.app") != NULL) {
		strstr(argv[0], "TreeAnnotator.app")[0] = 0;
		normalise(argv[0], path);
		sprintf(s, "%sjre1.8.0_161/bin/java -Xmx4g -Dapple.laf.useScreenMenuBar=true -Djava.library.path=$JAVAROOT:/usr/local/lib -Duser.language=en -cp %slib/launcher.jar beast.app.treeannotator.TreeAnnotatorLauncher", path, path);
	} else if (strstr(argv[0], "BEAUti.app") != NULL) {
		strstr(argv[0], "BEAUti.app")[0] = 0;
//		fprintf(fout, "argv0 = %s\n", argv[0]);
		normalise(argv[0], path);
//		fprintf(fout, "path = %s\n", path);
		sprintf(s, "%sjre1.8.0_161/bin/java -Xmx4g -Dapple.laf.useScreenMenuBar=true -Djava.library.path=$JAVAROOT:/usr/local/lib -Duser.language=en -cp %slib/launcher.jar beast.app.beauti.BeautiLauncher -capture", path, path);
	}

	//fprintf(fout,"%s\n", s);

    FILE* output = popen(s, "r");
	while ( fgets(buf, 1023, output) ) {
    //		fprintf(fout, "%s", buf);
  	}
  	pclose(output);
    //fprintf(fout, "Done\n");
	//fclose(fout);
  
    return 0;
}