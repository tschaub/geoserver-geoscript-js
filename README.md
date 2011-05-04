cd rhino
mvn install:install-file -Dfile=build/rhino1_7R3/js.jar -Dpackaging=jar -DgroupId=org.opengeo.rhino -DartifactId=rhino -Dversion=1.7R3

cd geoserver/src/web/app
mvn jetty:run -Pgeoscript-js,wps -DGEOSERVER_DATA_DIR=../../../data/release/