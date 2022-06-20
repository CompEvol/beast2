Run with BEAST using the -D and -DF options. 

The -D option specifies attribute-value pairs to be replaced in the XML, e.g., `-D arg1=10,arg2=20`
The -DF option is as -D, but has attribute-value pairs defined in file in JSON format

Note that the `$(filebase)` definition is automatically replaced by the base name of the XML file.


To run the RSV2.xml example in this directory with a chain length of 1000000 and alignment and date trait defined in RSV2.json, on OS X and Linux:

'''
/path/to/beast/bin/beast -D chainLength=1000000 -DF RSV2.json RSV2.xml
'''

where `/path/to/beast` the path to where BEAST is installed.

To run on Windows:

'''
\path\to\beast\bat\beast.bat -D chainLength=1000000 -DF RSV2.json RSV2.xml
'''

where `\path\to\beast` the path to where BEAST is installed.

