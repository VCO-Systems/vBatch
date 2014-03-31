Deployment
=========

First, do a clean build from Eclipse. Make sure there are no errors or warnings.

To build, open a terminal and cd to the project root.

cd build
build > ant [ minimal | deploy | dev ]

The build/staging directory will be emptied on each build, and filled with the results of the build.

There are three types of builds.  Every build creates the following zip file:  build/vbatch_[date]_[time].zip  The contents of the zip file depend on the build type:

* vbatch minimal    JAR file only
* vbatch deploy     JAR file, /config folder with ini files
* vbatch dev        JAR file, /config folder (with ini files), /data folder (shell scripts, DDL and sample data SQL)
