
exports.beforeTransaction = function(request) {
    LOGGER.info("beforeTransaction");
}

exports.beforeCommit = function(request) {
    LOGGER.info("beforeCommit");
    return {
        message: "beforeCommit exception message",
        code: "234",
        locator: "locatorId"
    };
}

exports.afterTransaction = function(request) {
    LOGGER.info("afterTransaction");
}

exports.priority = 10;
