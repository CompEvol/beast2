# Mac Release

Last update 28-07-2020


## No JRE

1. `ant mac` and then check all messages.

This will build the disk image and do the notarization.

2. `cd ~/tmp` to find _BEAST.v2.?.?.dmg_. 

Note: this path is depend on the path of beast2 project, which is equivalent to `beast2/../../tmp`. 

3. `xcrun altool --notarization-info *-*-*-*-* -u username -p passwd`

4. `xcrun stapler staple BEAST.v2.?.?.dmg`

5. upload dmg to Github.


## With JRE

1. `ant macjre` and then check all messages.

2. `cd ~/tmp` to find _BEAST\_with\_JRE.v2.?.?.dmg_.  

3. `xcrun altool --notarization-info *-*-*-*-* -u username -p passwd`

4. `xcrun stapler staple BEAST_with_JRE.v2.?.?.dmg`

5. upload dmg to Github.


## TODO

1. the hard code of JRE path in `universalJavaApplicationJREStub` must match JRE path in the `build.xml`


