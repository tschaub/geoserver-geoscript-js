
// returns a string summarizing the details provided to wfs hooks
function summarize(details) {
    var keys = ["PreInsert", "PreUpdate", "PostUpdate", "PostDelete", "natives"];
    var key, val, message = [];
    for (var i=0, ii=keys.length; i<ii; ++i) {
        key = keys[i];
        val = details[key];
        message[i] = key + ": " + (val && val.length || 0);
    }
    return message.join(" ");
}


/**
 *  This method is used to test that beforeCommit hooks can return an 
 *  object that results in a service exception.  This particular method will
 *  generate an exception if there is a native element in the transaction
 *  marked with vendorId="geoscript-js" with a value of "beforeCommit throws".
 */
exports.beforeCommit = function(details, request) {
    LOGGER.info("wfs.js beforeCommit called");
    var natives = details.natives;
    var nat, exception;
    for (var i=0, ii=natives.length; i<ii; ++i) {
        nat = natives[i];
        if (nat.vendorId === "geoscript-js" && nat.value === "beforeCommit throws") {
            exception = {
                locator: "beforeCommit",
                code: "234",
                message: summarize(details)
            };
            break;
        }
    }
    return exception;
};

/**
 *  This method is used to test that afterTransaction hooks can return an 
 *  object that results in a service exception.  This particular method will
 *  generate an exception if there is a native element in the transaction
 *  marked with vendorId="geoscript-js" with a value of "afterTransaction throws".
 */
exports.afterTransaction = function(details, request) {
    LOGGER.info("wfs.js afterTransaction called");
    var natives = details.natives;
    var nat, exception;
    for (var i=0, ii=natives.length; i<ii; ++i) {
        nat = natives[i];
        if (nat.vendorId === "geoscript-js" && nat.value === "afterTransaction throws") {
            exception = {
                locator: "afterTransaction",
                code: "234",
                message: summarize(details)
            };
            break;
        }
    }
    return exception;
};

exports.priority = 10;