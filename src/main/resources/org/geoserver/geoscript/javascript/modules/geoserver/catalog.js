var Namespace = require("./namespace").Namespace;
var Layer = require("geoscript/layer").Layer;
var Workspace = require("geoscript/workspace").Workspace;

var geoserver = Packages.org.geoserver;
var _catalog = geoserver.platform.GeoServerExtensions.bean("catalog");
var _factory = _catalog.getFactory();

Object.defineProperty(exports, "namespaces", {
    get: function() {
        var _namespaces = _catalog.getNamespaces();
        var len = _namespaces.size();
        var namespaces = new Array(len);
        var _namespace;
        for (var i=0; i<len; ++i) {
            _namespace = _namespaces.get(i);
            namespaces[i] = new Namespace({
                alias: String(_namespace.getPrefix()),
                uri: String(_namespace.getURI())
            });
        }
        return namespaces;
    }
});

exports.addNamespace = function(namespace, setDefault) {
    if (!(namespace instanceof Namespace)) {
        namespace = new Namespace(namespace);
    }
    var _namespace = _factory.createNamespace();
    _namespace.setPrefix(namespace.alias);
    _namespace.setURI(namespace.uri);
    _catalog.add(_namespace);
    var _workspace = _factory.createWorkspace();
    _workspace.setName(namespace.alias);
    _catalog.add(_workspace);
    if (setDefault) {
        _catalog.setDefaultWorkspace(_workspace);
    }
    return namespace;
};

exports.getFeatureType = function(uri, name) {
    var featureType = null;
    var _featureTypeInfo = _catalog.getResourceByName(
        uri, name, geoserver.catalog.FeatureTypeInfo
    );
    if (_featureTypeInfo != null) {
        var _source = _featureTypeInfo.getFeatureSource(null, null);
        var _store = _source.getDataStore();
        var workspace = Workspace.from_(_store);
        featureType = Layer.from_(_source, workspace);
    }
    return featureType;
};


// TODO: remove this after debugging
exports._catalog = _catalog;
