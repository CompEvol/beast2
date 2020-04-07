Run with BEAST using the -D and -DF options. 

The -D option specifies attribute-value pairs to be replaced in the XML, e.g., `-D arg1=10,arg2=20`
The -DF option is as -D, but has attribute-value pairs defined in file in JSON format

On OS X and Linux:

'''
/path/to/beast/bin/beast -D chainLength=1000000 -DF RSV2.json  RSV2.xml
'''

where `/path/to/beast` the path to where BEAST is installed.

On Windows:

'''
\path\to\beast\bat\beast.bat -D chainLength=1000000 -DF RSV2.json RSV2.xml
'''

where `\path\to\beast` the path to where BEAST is installed.

