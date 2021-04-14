import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as sfn from '@aws-cdk/aws-stepfunctions';
import * as tasks from '@aws-cdk/aws-stepfunctions-tasks';
import * as apigateway from '@aws-cdk/aws-apigateway';
import * as iam from '@aws-cdk/aws-iam';

export class AwsStepFunctionsSearchByronStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const role = iam.Role.fromRoleArn(this, 'Role', 'arn:aws:iam::044915237328:role/lambdaSearchRole', {
            mutable: false,
        });

        const parseRequestLambda = new lambda.Function(this, "StepOneParseRequest", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/1_parse-requests'),
        });
        const executeSearchLambda = new lambda.Function(this, "StepTwoExecuteSearch", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/2_search'),
            environment: {
                runAsOr : 'false'
            },
            role: role
        });
        const executeSearchAgainLambda = new lambda.Function(this, "StepTwoExecuteSearchAgain", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/2_search'),
            environment: {
                runAsOr : 'true'
            },
            role: role
        });
        const buildResponseLambda = new lambda.Function(this, "StepFourBuildResponse", {
            runtime: lambda.Runtime.NODEJS_12_X,
            handler: "index.handler",
            code: lambda.Code.fromAsset('./lambdas/3_response'),
        });

        const parseRequest = new tasks.LambdaInvoke(this, "ParseRequest", {
            lambdaFunction: parseRequestLambda,
            outputPath: "$.Payload",
        });
        const executeSearch = new tasks.LambdaInvoke(this, "ExecuteSearch", {
            lambdaFunction: executeSearchLambda,
            outputPath: "$.Payload",
        });
        const executeSearchAgain = new tasks.LambdaInvoke(this, "ExecuteSearchAgain", {
            lambdaFunction: executeSearchAgainLambda,
            outputPath: "$.Payload",
        });
        const buildResponse = new tasks.LambdaInvoke(this, "BuildResponse", {
            lambdaFunction: buildResponseLambda,
            outputPath: "$.Payload",
        });

        let condition = sfn.Condition.numberEquals("$.hits.total.value", 0);
        let choice = new sfn.Choice(this, 'Zero hits?')
            .when(condition, executeSearchAgain)
            .when(condition, new sfn.Choice(this, ''))
            .when(sfn.Condition.numberGreaterThan("$.hits.total.value", 0), buildResponse)
            .otherwise(buildResponse);


        const definition = parseRequest
            .next(executeSearch)
            .next(
                choice
            );

        const stateMachine = new sfn.StateMachine(this, "ByronStateMachine", {
            definition,
            stateMachineType: sfn.StateMachineType.EXPRESS,
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

        const api = new apigateway.RestApi(this, "ByronStepFunctionsAPI");

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
