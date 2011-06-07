## Setup

These instructions assume you'll be running GeoServer from source with the GeoScript JS sources locally.

### Getting GeoServer

Create a place for the GeoServer sources and check out the trunk from Subversion.

    mkdir geoserver
    cd geoserver
    svn checkout http://svn.codehaus.org/geoserver/trunk
    cd trunk

### Getting the GeoScript JS Module

Next you'll clone the GeoScript JS Module source with Git.

    git clone git://github.com/tschaub/geoserver-geoscript-js.git src/community/geoscript-js

### Adding GeoScript JS as a Community Module

Using your checkout of GeoServer from Subversion, you need to modify a couple `pom.xml` files to configure GeoScript JS as a community module.

    patch -p0 < src/community/geoscript-js/etc/geoserver.patch
    
### Building

Use Maven to build GeoServer with the GeoScript JS Module.

    cd src
    mvn clean install -P geoscript-js,wps

### Running

The Maven Jetty plugin can be used to run web based modules:

    cd src/web/app
    mvn jetty:run -Pgeoscript-js,wps -DGEOSERVER_DATA_DIR=../../../data/release/

To run GeoServer from Eclipse, follow the GeoServer [Eclipse Guide](http://docs.geoserver.org/latest/en/developer/eclipse-guide/index.html).  When generating the Eclipse project, make sure to use the same profiles as for installing above:

    mvn -P geoscript-js,wps eclipse:eclipse

## Scripting WPS

To script a new process, you just need to place `.js` files in a `scripts/processes` directory in your data directory.  See the examples directory for some sample process scripts.
