var GEOM = require("geoscript/geom");

exports.execute = function(process, inputs) {
    for (var key in inputs) {
        if (process.inputs[key].type in GEOM) {
            inputs[key] = GEOM.Geometry.from_(inputs[key]);
        }
    }
    var outputs = process.run(inputs);
    // convert outputs
    for (var key in outputs) {
        if (process.outputs[key].type in GEOM) {
            outputs[key] = outputs[key]._geometry;
        }
    }
    return outputs;
};
