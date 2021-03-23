const AWS = require('aws-sdk');
const { SNSClient, PublishCommand } = require("@aws-sdk/client-sns");

exports.handler = async (event) => {
    const sns = new SNSClient(process.env.REGION)
    const jsonBody = JSON.parse(event.body);

    const publishCommand = new PublishCommand({
        Message: jsonBody.message,
        TopicArn: process.env.SNS_TOPIC_ARN,
    });

    await sns.send(publishCommand);

    return {
        "isBase64Encoded": false,
        "statusCode": 200,
        "headers": {
            "Access-Control-Allow-Origin": '*'
        },
        "body": JSON.stringify({
            "message": "OK"
        })
    };
}