import * as cdk from '@aws-cdk/core';
import * as ec2 from "@aws-cdk/aws-ec2";
import * as ecs from "@aws-cdk/aws-ecs";
import * as apigateway from "@aws-cdk/aws-apigateway";
import * as lambda from "@aws-cdk/aws-lambda";
import * as ecs_patterns from "@aws-cdk/aws-ecs-patterns";


export class AwsFargateEcsCdkStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const start = new lambda.Function(this, 'StartHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'start.handler'
        });
        const shutdown = new lambda.Function(this, 'ShutdownHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'shutdown.handler'
        });

        const api = new apigateway.RestApi(this, 'ByronApi', {});

        const startMethod = api.root.addResource('start');
        startMethod.addMethod('GET', new apigateway.LambdaIntegration(start))
        const shutdownMethod = api.root.addResource('shutdown');
        shutdownMethod.addMethod('GET', new apigateway.LambdaIntegration(shutdown))

        const vpc = new ec2.Vpc(this, "ByronVpc", {
            maxAzs: 3
        });

        const cluster = new ecs.Cluster(this, "ByronCluster", {
            vpc: vpc
        });

        const taskDefinition = new ecs.FargateTaskDefinition(this, 'ByronTaskDef', {
            memoryLimitMiB: 512,
            cpu: 256,
        });
        taskDefinition.addContainer('ByronContainer', {
            image: ecs.ContainerImage.fromRegistry("norconex-java:latest"),
            portMappings: [{containerPort: 80}],
            logging: ecs.LogDrivers.awsLogs({streamPrefix: 'ByronFargateTask'})
        });

        new ecs.FargateService(this, "ByronFargateService", {
            cluster: cluster,
            taskDefinition: taskDefinition,
            desiredCount: 0
        });

    }

}
