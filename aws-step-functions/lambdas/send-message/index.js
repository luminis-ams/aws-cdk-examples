const { SNSClient, PublishCommand } = require("@aws-sdk/client-sns");

exports.handler = (event, context, callback) => {
    const sns = new SNSClient(process.env.REGION)
    const sender = event.sender;
    const message = event.message;

    const publishCommand = new PublishCommand({
        Message: event.message,
        TopicArn: process.env.SNS_TOPIC_ARN,
    });

    sns.send(publishCommand);

    const result = {sender: sender, message: message};
    callback(null, result);
};