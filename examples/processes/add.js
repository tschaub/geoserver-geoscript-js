/**
 * Get metadata about this process with the following:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=DescribeProcess&Identifier=js:add
 *     
 * Execute with the following for a full response document:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=Execute&Identifier=js:add&DataInputs=first=1;second=2&ResponseDocument=sum
 *
 * Or for just the raw data output:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=Execute&Identifier=js:add&DataInputs=first=1;second=2&RawDataOutput=sum
 */

var Process = require("geoscript/process").Process;

exports.process = new Process({
	title: "JavaScript Addition Process",
	description: "Adds two integers.",
	inputs: {
		first: {
			type: "Integer",
			description: "The first operand."
		},
		second: {
			type: "Integer",
			description: "The second operand."
		}
	},
	outputs: {
		sum: {
			type: "Integer",
			description: "The sum of the two inputs"
		}
	},
	run: function(inputs) {
		return {sum: inputs.first + inputs.second};
	}
});
