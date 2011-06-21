

exports.beforeTransaction = function(details, request) {
    LOGGER.info("beforeTransaction");
    var inserts = details.inserts;
    var insert;
    for (var i=0, ii=inserts.length; i<ii; ++i) {
        insert = inserts[i];
        LOGGER.info(i + ": {" + insert.uri + "}" + insert.name + ": " + insert.feature);
    }
    
//    return {
//        message: "beforeTransaction exception message",
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
        for (var i=0, ii=array.length; i<ii; ++i) {
            LOGGER.info("item " + i + ": " + JSON.stringify(array[i]));
        }
    }
}

exports.priority = 10;
