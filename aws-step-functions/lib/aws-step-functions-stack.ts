import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as sfn from '@aws-cdk/aws-stepfunctions';
import * as tasks from '@aws-cdk/aws-stepfunctions-tasks';
import * as sns from '@aws-cdk/aws-sns';
import * as subs from '@aws-cdk/aws-sns-subscriptions';
import * as apigateway from '@aws-cdk/aws-apigateway';
import * as iam from '@aws-cdk/aws-iam';


export class AwsStepFunctionsStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const snsTopic = new sns.Topic(this, 'LambdaStepsSnsTopic', {
            displayName: 'Lambda SNS Topic to send email',
        });

        snsTopic.addSubscription(new subs.EmailSubscription("jettro@coenradie.com"));

        const parseRequestLambda = new lambda.Function(this, "StepOneParseRequest", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/parse-request'),
        });

        const checkBadWordsLambda = new lambda.Function(this, "StepTwoCheckBadWords", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/check-bad-words'),
        });

        const sendMessageLambda = new lambda.Function(this, "StepThreeCheckBadWords", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/send-message'),
            environment: {
                SNS_TOPIC_ARN: snsTopic.topicArn,
                REGION: this.region,
            }
        });
        snsTopic.grantPublish(sendMessageLambda);

        const parseRequest = new tasks.LambdaInvoke(this, "ParseRequest", {
            lambdaFunction: parseRequestLambda,
            outputPath: "$.Payload",
        });

        const checkBadWords = new tasks.LambdaInvoke(this, "CheckBadWords", {
            lambdaFunction: checkBadWordsLambda,
            outputPath: "$.Payload",
        });

        const sendMessage = new tasks.LambdaInvoke(this, "SendMessage", {
            lambdaFunction: sendMessageLambda,
            outputPath: "$.Payload",
        });

        const waitX = new sfn.Wait(this, 'Wait X Seconds', {
            time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
        });

        const sendMessageFailed = new sfn.Fail(this, 'SendMessageFailed', {
            cause: 'Not a valid message',
            error: 'Check message failed',
        });

        const definition = parseRequest
            .next(checkBadWords)
            .next(
                new sfn.Choice(this, 'No Bad Words')
                    .when(sfn.Condition.booleanEquals('$.valid', false), sendMessageFailed)
                    .when(sfn.Condition.booleanEquals('$.valid', true), sendMessage)
                    .otherwise(waitX)
            );

        const stateMachine = new sfn.StateMachine(this, "StateMachine", {
            definition,
            timeout: cdk.Duration.minutes(5),
        });

        const credentialsRole = new iam.Role(this, "getRole", {
            assumedBy: new iam.ServicePrincipal("apigateway.amazonaws.com"),
        });

        credentialsRole.attachInlinePolicy(
            new iam.Policy(this, "getPolicy", {
                statements: [
                    new iam.PolicyStatement({
                        actions: ["states:StartExecution"],
                        effect: iam.Effect.ALLOW,
                        resources: [stateMachine.stateMachineArn],
                    }),
                ],
            })
        );

        const api = new apigateway.RestApi(this, "JettroStepFunctionsAPI");

        api.root.addMethod(
            "POST",
            new apigateway.AwsIntegration({
                service: "states",
                action: "StartExecution",
                integrationHttpMethod: "POST",
                options: {
                    credentialsRole,
                    integrationResponses: [
                        {
                            statusCode: "200",
                        },
                    ],
                    requestTemplates: {
                        "application/json": `
                        {
                            "input": "$util.escapeJavaScript($input.json('$'))",
                            "stateMachineArn": "${stateMachine.stateMachineArn}"
                        }`,
                    },
                },
            }),
            {
                methodResponses: [{statusCode: "200"}],
            }
        );
    }
}
