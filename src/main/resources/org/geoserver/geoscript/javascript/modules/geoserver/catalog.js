var catalog = Packages.org.geoserver.platform.GeoServerExtensions.bean("catalog");

var Namespace = require("./namespace").Namespace;

Object.defineProperty(exports, "namespaces", {
    get: function() {
        var _namespaces = catalog.getNamespaces();
        var len = _namespaces.size();
        var namespaces = new Array(len);
        var _info;
        for (var i=0; i<len; ++i) {
            _info = _namespaces.get(i);
            namespaces[i] = new Namespace({
                alias: String(_info.getPrefix()),
                uri: String(_info.getURI())
            });
        }
        return namespaces;
    }
});

