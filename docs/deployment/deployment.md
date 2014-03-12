Building
=========

First, do a clean build from Eclipse. Make sure there are no errors or warnings.

To build, open a terminal and cd to the project root.

cd build
build > ant

The build/staging directory will be emptied on each build, and filled with the results of the build.

There are two types of builds:

* minimal (the default)
** vbatch.jar
** config/*.ini  (the db connection files for dev, prod, etc)
* full
** Does a minimal build, and adds a /data folder with all sql files and scripts for setup, testing, etc

Lastly, the build's contents are put into a new zipfile:  build/vbatch_[current date and time].zip
