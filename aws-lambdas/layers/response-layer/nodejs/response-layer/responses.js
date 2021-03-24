exports.createResponse = (message) => {
    return {
        "isBase64Encoded": false,
        "statusCode": 200,
        "headers": {
            "Access-Control-Allow-Origin": '*'
        },
        "body": JSON.stringify({
            "message": message
        })
    };
}