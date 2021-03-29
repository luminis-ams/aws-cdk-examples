import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';

export class CdkStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const quarkusMessageLambda = new lambda.Function(this, "QuarkusMessageLambda", {
      runtime: lambda.Runtime.PROVIDED_AL2,
      handler: "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest",
      code: lambda.Code.fromAsset('../target/function.zip'),
      environment: {
        quarkus_lambda_handler: "message"
      }
    });

    const quarkusHelloLambda = new lambda.Function(this, "QuarkusHelloLambda", {
      runtime: lambda.Runtime.PROVIDED_AL2,
      handler: "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest",
      code: lambda.Code.fromAsset('../target/function.zip'),
      environment: {
        quarkus_lambda_handler: "hello"
      }
    });

  }
}
