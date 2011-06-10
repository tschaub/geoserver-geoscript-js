
exports.beforeTransaction = function(request) {
    LOGGER.info("beforeTransaction");
}

exports.beforeCommit = function(request) {
    LOGGER.info("beforeCommit");
//    return {
//        message: "obj message",
//        code: "234",
//        locator: "locator here"
//    };
}

exports.afterTransaction = function(request) {
    LOGGER.info("afterTransaction");
}

exports.priority = 10;
