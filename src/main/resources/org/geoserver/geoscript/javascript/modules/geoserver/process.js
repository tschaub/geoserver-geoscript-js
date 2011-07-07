
exports.execute = function(process, inputs) {
    var field;
    // convert inputs
    for (var key in inputs) {
        field = process.inputs[key];
        inputs[key] = field.valueFrom_(inputs[key]);
    }
    // execute
    var outputs = process.run(inputs);
    // convert outputs
    for (var key in outputs) {
        field = process.outputs[key];
        outputs[key] = field.valueTo_(outputs[key]);
    }
    return outputs;
};
