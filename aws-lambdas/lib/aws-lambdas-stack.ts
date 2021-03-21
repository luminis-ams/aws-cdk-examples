import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import {CfnOutput} from "@aws-cdk/core";

export class AwsLambdasStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const helloLogsLambda = new lambda.Function(this, "JettroHelloLogsLambda", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromInline(`
            exports.handler = async function (event, context) {
                console.log(event.say + " " + event.to + "!");
                return context.logStreamName;
            };
      `),
        });

        new CfnOutput(this, 'createUserUrl', {
            description: "Creates a new lambda, execute with the following name.",
            value: helloLogsLambda.functionName,
        });
    }
}
