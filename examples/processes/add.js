/**
 * Get metadata about this process with the following:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=DescribeProcess&Identifier=js:add
 *     
 * Execute with the following for a full response document:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=Execute&Identifier=js:add&DataInputs=rhs=1;lhs=2&ResponseDocument=sum
 *
 * Or for just the raw data output:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=Execute&Identifier=js:add&DataInputs=rhs=1;lhs=2&RawDataOutput=sum
 */

exports.metadata = {
    title: "Addition Example",
    description: "Example script that adds two numbers together",
    inputs: {
        lhs: java.lang.Integer,
        rhs: java.lang.Integer
    },
    outputs: {
        sum: java.lang.Integer
    }
};

exports.process = function(input) {
    return {
        sum: (input.lhs + input.rhs)
    };
};
