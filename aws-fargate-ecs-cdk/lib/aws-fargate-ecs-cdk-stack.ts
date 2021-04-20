import * as cdk from '@aws-cdk/core';
import * as ec2 from "@aws-cdk/aws-ec2";
import * as ecr from "@aws-cdk/aws-ecr";
import * as ecs from "@aws-cdk/aws-ecs";
import {Protocol} from "@aws-cdk/aws-ecs";
import * as logs from '@aws-cdk/aws-logs';
import * as apigateway from "@aws-cdk/aws-apigateway";
import * as lambda from "@aws-cdk/aws-lambda";
import * as iam from "@aws-cdk/aws-iam";

export class AwsFargateEcsCdkStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);


        const vpc = new ec2.Vpc(this, 'ByronVpc', {
            maxAzs: 1,

        });
        const cluster = new ecs.Cluster(this, 'ByronCluster', {vpc: vpc});
        cluster.addDefaultCloudMapNamespace({
            name: "services.local",
        })

        const taskDef = new ecs.FargateTaskDefinition(this, "ByronTask", {
            memoryLimitMiB: 512,
            cpu: 256,
        });

        taskDef.addContainer('ByronContainer', {
            image: ecs.ContainerImage.fromEcrRepository(ecr.Repository.fromRepositoryName(this, 'dockerRepo', 'norconex-java')),
            portMappings: [{
                containerPort: 80,
                hostPort: 80,
                protocol: Protocol.TCP,
            }],
            environment: {
                "LOCALDOMAIN": "service.local",
                'norconex.max-depth': '1',
                'norconex.name': 'Default',
                'norconex.start-urls': 'https://www.jettro.dev,https://www.jettro.dev/unknown.html',
                'norconex.elasticsearch-nodes': 'http://localhost:9200',
                'norconex.elasticsearch-index-name': 'norconex'
            },
            logging: ecs.LogDrivers.awsLogs({
                streamPrefix: 'ByronFargateTask',
                logRetention: logs.RetentionDays.ONE_DAY,
            })
        })
        new ecs.FargateService(this, 'ByronFargateService', {
            cluster: cluster,
            taskDefinition: taskDef,
        });
        const role = iam.Role.fromRoleArn(this, 'Role', 'arn:aws:iam::044915237328:role/ecsTaskExecutionRole', {
            mutable: false,
        });
        const start = new lambda.Function(this, 'StartHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'start.handler',
            role: role,
            environment: {
                subnets: vpc.publicSubnets.map(s => s.subnetId).join(','),
                security_group: vpc.vpcDefaultSecurityGroup,
                cluster: cluster.clusterName,
                taskDef: taskDef.taskDefinitionArn
            }
        });
        const shutdown = new lambda.Function(this, 'ShutdownHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'shutdown.handler',
            role: role,
            environment: {
                cluster: cluster.clusterName
            }
        });

        const api = new apigateway.RestApi(this, 'ByronApi', {});

        const startMethod = api.root.addResource('start');
        startMethod.addMethod('GET', new apigateway.LambdaIntegration(start))
        const shutdownMethod = api.root.addResource('shutdown');
        shutdownMethod.addMethod('GET', new apigateway.LambdaIntegration(shutdown))


    }

}
