
exports.beforeTransaction = function(request) {
    LOGGER.info("beforeTransaction");
}

exports.beforeCommit = function(request) {
    LOGGER.info("beforeCommit");
//    return {
//        message: "beforeCommit exception message",
//        code: "234",
//        locator: "locatorId"
//    };
}

exports.afterTransaction = function(request, details) {
    LOGGER.info("afterTransaction");
    var array;
    for (var key in details) {
        LOGGER.info("key: " + key);
        array = details[key];
        LOGGER.info("type: " + (typeof array) + " length: " + array.length);
        for (var i=0, ii=array.length; i<ii; ++i) {
            LOGGER.info("item " + i + ": " + JSON.stringify(array[i]));
        }
    }
}

exports.priority = 10;
