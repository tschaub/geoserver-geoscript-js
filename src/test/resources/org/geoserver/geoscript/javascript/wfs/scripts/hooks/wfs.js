
exports.beforeCommit = function(details, request) {
    LOGGER.info("beforeCommit");
    var array;
    for (var key in details) {
        array = details[key];
        LOGGER.info("key: " + key + " count: " + array.length);
    }
//    return {
//        message: "beforeCommit exception message",
//        code: "234",
//        locator: "locatorId"
//    };
}

exports.afterTransaction = function(details, request) {
    LOGGER.info("afterTransaction");
    var array;
    for (var key in details) {
        array = details[key];
        LOGGER.info("key: " + key + " count: " + array.length);
    }
}

exports.priority = 10;