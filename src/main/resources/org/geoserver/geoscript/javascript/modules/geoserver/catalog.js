var catalog = Packages.org.geoserver.platform.GeoServerExtensions.bean("catalog");

Object.defineProperty(exports, "workspaces", {
    get: function() {
        var _workspaces = catalog.getWorkspaces();
        var len = _workspaces.size();
        var workspaces = new Array(len);
        for (var i=0; i<len; ++i) {
            workspaces[i] = String(_workspaces.get(i).getName());
        }
        return workspaces;
    }
});

