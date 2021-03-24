const { SNSClient, PublishCommand } = require("@aws-sdk/client-sns");

exports.handler = async (event, context) => {
    const sns = new SNSClient(process.env.REGION)

    const publishCommand = new PublishCommand({
        Message: event.message,
        TopicArn: process.env.SNS_TOPIC_ARN,
    });

    await sns.send(publishCommand);

    console.log("Send a message to the SNS Topic: " + process.env.SNS_TOPIC_ARN);
    return context.logStreamName;
}