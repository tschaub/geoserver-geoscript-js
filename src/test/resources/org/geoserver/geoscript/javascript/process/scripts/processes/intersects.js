var Process = require("geoscript/process").Process;

var catalog = require("geoserver/catalog");

exports.process = new Process({
    title: "Intersection Test",
    description: "Determines whether a given geometry intersect target features.",
    inputs: {
        geometry: {
            type: "Geometry",
            title: "Input Geometry",
            description: "Input geometry that must intersect at least one target feature geometry."
        },
        featureType: {
            type: "String",
            title: "Target Feature Type",
            description: "The unqualified name of the target feature type."
        },
        namespace: {
            type: "String",
            title: "Target Namespace URI",
            description: "The namespace URI for the target feature type."
        },
        filter: {
            type: "String",
            title: "Target Filter",
            description: "CQL used to filter features from the target feature type (optional)."
        },
    },
    outputs: {
        intersects: {
            type: "Boolean",
            title: "Intersection Result",
            description: "The input geometry intersects at least one feature in the target feature set."
        }
    },
    run: function(inputs) {
        var intersects = false;
        var geometry = inputs.geometry;
        var layer = catalog.getFeatureType(inputs.namespace, inputs.featureType);
        var cursor = layer.query(inputs.filter);
        cursor.forEach(function(target) {
            intersects = geometry.intersects(target.geometry);
            return !intersects;
        });
        cursor.close(); // in case we stopped early
        return {intersects: intersects};
    }
});