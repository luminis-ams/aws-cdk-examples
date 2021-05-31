import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as sns from '@aws-cdk/aws-sns';
import * as subs from '@aws-cdk/aws-sns-subscriptions';
import * as apigateway from '@aws-cdk/aws-apigateway';

import {CfnOutput} from "@aws-cdk/core";

export class AwsLambdasStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const helloLogsLambda = new lambda.Function(this, "LambdaDemo_HelloLogsLambda", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromInline(`
            exports.handler = async function (event, context) {
                console.log(event.say + " " + event.to + "!");
                return context.logStreamName;
            };
            `),
        });

        new CfnOutput(this, 'CreateLogLambdaOutput', {
            description: "Creates a new lambda, execute with the following name.",
            value: helloLogsLambda.functionName,
        });

        const snsTopic = new sns.Topic(this, 'LambdaDemo_SnsTopic', {
            displayName: 'Lambda SNS Topic to send email',
        });

        // snsTopic.addSubscription(new subs.EmailSubscription("jettro@coenradie.com"));

        const sendToSNSLambda = new lambda.Function(this, 'LambdaDemo_SendToSNSLambda', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('./lambdas/lambda-send-message'),
            handler: 'send-message.handler',
            environment: {
                SNS_TOPIC_ARN: snsTopic.topicArn,
                REGION: this.region,
            }
        });

        snsTopic.grantPublish(sendToSNSLambda);

        new CfnOutput(this, 'SendMessageLambdaOutput', {
            description: "Call the lambda that just sends a message to the SNS Topic",
            value: sendToSNSLambda.functionName,
        });

        const responseLayer = new lambda.LayerVersion(this, "ResponseLayer", {
            code: lambda.Code.fromAsset("./layers/response-layer"),
            compatibleRuntimes: [lambda.Runtime.NODEJS_12_X],
        });

        const gatewayLambda = new lambda.Function(this, 'LambdaDemo_GatewaySendToSNSLambda', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('./lambdas/lambda-gateway'),
            handler: 'index.handler',
            environment: {
                SNS_TOPIC_ARN: snsTopic.topicArn,
                REGION: this.region,
            },
            layers: [responseLayer],
        });

        snsTopic.grantPublish(gatewayLambda);

        new apigateway.LambdaRestApi(this, 'LambdaDemo_LambdaApi', {
            handler: gatewayLambda,
        });
    }
}
