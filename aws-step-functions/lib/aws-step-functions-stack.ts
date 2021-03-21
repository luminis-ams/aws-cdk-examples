import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as sfn from '@aws-cdk/aws-stepfunctions';
import * as tasks from '@aws-cdk/aws-stepfunctions-tasks';

export class AwsStepFunctionsStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const functionGenerateID = new lambda.Function(this, "GenerateID", {
      runtime: lambda.Runtime.NODEJS_12_X,
      handler: "index.handler",
      code: lambda.Code.fromInline(`
    const generate = () => Math.random().toString(36).substring(7);

    exports.handler = async () => ({"value": generate()});
  `),
    });

    const functionReverseID = new lambda.Function(this, "ReverseID", {
      runtime: lambda.Runtime.NODEJS_12_X,
      handler: "index.handler",
      code: lambda.Code.fromInline(`
    const reverse = (str) => (str === '') ? '' : reverse(str.substr(1)) + str.charAt(0);

    exports.handler = async (state) => ({"value": reverse(state.value)});
  `),
    });

    const definition = new tasks.LambdaInvoke(this, "Generate ID", {
      lambdaFunction: functionGenerateID,
      outputPath: "$.Payload",
    })
        .next(
            new sfn.Wait(this, "Wait 1 Second", {
              time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
            })
        )
        .next(
            new tasks.LambdaInvoke(this, "Reverse ID", {
              lambdaFunction: functionReverseID,
              outputPath: "$.Payload",
            })
        );

    const stateMachine = new sfn.StateMachine(this, "StateMachine", {
      definition,
      timeout: cdk.Duration.minutes(5),
    });
  }
}
