const AWS = require("aws-sdk");
const dynamodb = new AWS.DynamoDB();

const tableName = process.env.tableName

exports.handler = async function (event, ctx, callback) {

    return await new Promise((resolve, reject) => {
        let params = {
            TableName: tableName,
        }

        dynamodb.scan(params, function (err, data) {
            if (err) {
                console.log(err, err.stack)
                buildResponse(resolve, 500, JSON.stringify(err.stack))
            } else {
                console.log(data)
                buildResponse(resolve, 200, JSON.stringify(data))
            }
        })
    })

    function buildResponse(resolve, statusCode, message) {
        resolve({
            "isBase64Encoded": false,
            "statusCode": statusCode,
            "headers": {
                "Access-Control-Allow-Origin": '*'
            },
            "body": message
        });
    }

}
