# Note on migrating from BEAST v2.6 to v2.7


## tl;dr

1. Make sure your code, example XML and BEAUti template are compatible with the v2.7 API and updated libraries. The `beast2/script/migrate.pl` script can assist with that.
2. Make sure to list your services in `version.xml`. Run `beastfx.app.tools.PackageHealthChecker` to get suggestions for services to add in `version.xml`


## Migrating java code

Java classes have been organised somewhat differently. A perl script that recurses through all files is available that helps convert java code and xml files. It takes one argument: a directory name. To help resolve imports, from your package directory, run

```
perl ../beast2/script/migrate.pl src
```

Not all imports may be resolved, and some APIs changed slightly, so some manual clean up is probably necessary to make the code work again. API changes are mostly of the form of making member variables private and providing getters and setters to access them.

## Update java

BEAST now uses Java 17 instead of Java 8, which might give a small performance boost for some models and allows access to Java 17 features.

It is recommended to install the JDK from Azul, since it allows painless integration of JavaFX. It can be downloaded from here: https://www.azul.com/downloads/?package=jdk

* scroll to the bottom
* select `Java 17 LTS`
* select your operating system
* select `JDK FX` ** make sure to include FX and not just select JDK without FX **
* download the file and install according to instructions under the `How to install` link.

In the IDE that you are using, make sure that the JDK points to Java 17.

It is convenient to make this JDK the default. For Linux, this can be done by adding to your `~/.bashrc` file the following two lines:

```
export JAVA_HOME=/path/to/zulu17.34.19-ca-fx-jdk17.0.3-linux_x64
export PATH=${JAVA_HOME}/bin:${PATH}
```



## Update libraries

Some library versions have been updated, so any references in your `build.xml` file need to be updated.

* commons-math => commons-math3-3.6.1.jar		
* antl => antlr-runtime-4.10.1.jar	
* fest.jar => assertj-core-3.20.2.jar, assertj-swing-junit-3.17.1.jar, assertj-swing-3.17.1.jar
* junit-4.8.2.jar => junit/junit-platform-console-standalone-1.8.2.jar

## Update `version.xml`


Since the BEAST package is now split into `BEAST.base` and `BEAST.app` the dependencies need to be updated: replace
``` xml
<depends on='beast2' atleast='2.6.7'/>
```

with

``` xml
        <depends on='BEAST.base' atleast='2.7.0'/>
```

or, if you also depend on `BEAST.app`, with

``` xml
        <depends on='BEAST.base' atleast='2.7.0'/>
        <depends on='BEAST.app' atleast='2.7.0'/>
```


The `version.xml` file allows you to specify which services are available for external packages, the XML parser, etc.. A service is specified by adding a `service` element *inside* the `package` element, e.g. like so:

``` xml
<service type="beast.base.core.BEASTInterface">
      <provider classname="beast.base.core.VirtualBEASTObject"/>
      <provider classname="beast.base.evolution.RateStatistic"/>
      <provider classname="beast.base.evolution.Sum"/>
</service>      
```

A service has a `type` and contains one or more `providers` which indicate which classes provide the service. The following services are recognised by `beast.base`:


* `beast.base.core.BEASTInterface` should be used for any BEAST object that can be created through XML files.
* `beast.base.evolution.datatype.DataType` to be used when new data types are made available in a package.
* `beast.app.inputeditor.InputEditor` for any input editor providing GUI components for BEAUti. **Note**, packages should not put input editors in `beast.app.beauti` or `beast.app.draw` any more.
* `beast.app.inputeditor.AlignmentImporter` for classes that help with importing alignments from files.
* `beast.app.beauti.BeautiHelpAction` for actions that extend the Help menu in BEAUti.
* `beast.app.beauti.PriorProvider` for actions that extend the `Add priors` button in the prior panel in BEAUti.
* `beast.base.inference.ModelLogger`

The `PackageHealthChecker` tool (details below) that comes with BEAST can be used to give suggestions on what to include, which is inferred from the type of classes.


## Migrating example XML

Run `migrate.pl` on the examples directory:

```
perl ../beast2/script/migrate.pl examples
```

Make sure things are fine by running BEAST on each of the example files.




## Migrating BEAUti template XML

Run `migrate.pl` on the templates directory:

```
perl ../beast2/script/migrate.pl templates
```

Make sure things are fine by running BEAUti -- see [BEAUti debugging tips](http://www.beast2.org/2022/02/01/debugging-beauti-templates.html) for details on how to debug BEAUti templates.


BEAUti templates are now picked up from the `fxtemplates` directory instead of the `templates` directory, so 
* rename `templates` to `fxtemplates`
* in your `build.xml` file, replace `<mkdir dir="${Add_on_dir}/templates" />` with `<mkdir dir="${Add_on_dir}/fxtemplates" />`
* also in `build.xml`, replace `<copy todir="${Add_on_dir}/templates">` with `<copy todir="${Add_on_dir}/fxtemplates">`




## Run the `PackageHealthChecker` tool

The `PackageHealthChecker` checks installed packages only: be sure to have your package installed ([install by hand](https://www.beast2.org/managing-packages/#Install_by_hand)).

The `PackageHealthChecker` takes as arguments:

* `package` name of the  BEAST package (required)
* `namespace` only classes inside this package name will be listed (required)
* `output` output-file where report is stored. Use stdout if not specified.
* `verbose` flag to show info and error messages when parsing XML. 


The `PackageHealthChecker` tool comes with BEAST v2.7 is best run through an IDE through the `beastfx.app.tools.PackageHealthChecker` class. It performs some checks on the packages, including suggestions on what services to put in the `version.xml` file.


