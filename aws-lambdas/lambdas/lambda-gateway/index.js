const AWS = require('aws-sdk');
const { SNSClient, PublishCommand } = require("@aws-sdk/client-sns");
const { createResponse } = require("/opt/nodejs/response-layer/responses");

exports.handler = async (event) => {
    const sns = new SNSClient(process.env.REGION)
    const jsonBody = JSON.parse(event.body);

    const publishCommand = new PublishCommand({
        Message: jsonBody.message,
        TopicArn: process.env.SNS_TOPIC_ARN,
    });

    await sns.send(publishCommand);

    return createResponse("Fine by me");
}