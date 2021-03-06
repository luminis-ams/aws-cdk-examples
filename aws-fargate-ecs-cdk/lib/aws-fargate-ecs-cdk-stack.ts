import * as cdk from '@aws-cdk/core';
import * as ec2 from "@aws-cdk/aws-ec2";
import * as ecr from "@aws-cdk/aws-ecr";
import * as ecs from "@aws-cdk/aws-ecs";
import * as logs from '@aws-cdk/aws-logs';
import * as dynamodb from '@aws-cdk/aws-dynamodb';
import * as gateway from "@aws-cdk/aws-apigatewayv2";
import * as gatewayinteg from "@aws-cdk/aws-apigatewayv2-integrations";
import * as lambda from "@aws-cdk/aws-lambda";
import * as iam from "@aws-cdk/aws-iam";

export class AwsFargateEcsCdkStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        //todo - Fix better roles, more restrictive

        //Roles setup
        const fargateRole = new iam.Role(this, 'fargateRole', {
            assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
        });
        fargateRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonDynamoDBFullAccess')
        );
        fargateRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonESFullAccess')
        );

        const lambdaRole = new iam.Role(this, 'lambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
        });
        lambdaRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonDynamoDBFullAccess')
        );
        lambdaRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonECS_FullAccess')
        );

        //Fargate Cluster setup
        const vpc = new ec2.Vpc(this, 'ByronVpc', {
            maxAzs: 1,
        });
        const cluster = new ecs.Cluster(this, 'ByronCluster', {vpc: vpc});
        cluster.addDefaultCloudMapNamespace({
            name: "services.local",
        })

        const taskDef = new ecs.FargateTaskDefinition(this, "ByronTask", {
            memoryLimitMiB: 512,
            taskRole: fargateRole,
            cpu: 256,
        });

        taskDef.addContainer('ByronContainer', {
            image: ecs.ContainerImage.fromEcrRepository(ecr.Repository.fromRepositoryName(this, 'dockerRepo', 'norconex-java')),
            portMappings: [{
                containerPort: 8080,
                hostPort: 8080,
                protocol: ecs.Protocol.TCP,
            }],
            environment: {
                "LOCALDOMAIN": "service.local",
                'norconex.action': 'START',
                'norconex.elasticsearch-index-name': 'norconex',
                'norconex.elasticsearch-nodes': 'https://search-demo-search-kog26hrflbperlmbeubxx37xgq.eu-west-1.es.amazonaws.com',
                'norconex.max-depth': '1',
                'norconex.name': 'Norconex',
                'norconex.start-urls': 'https://www.jettro.dev,https://www.jettro.dev/unknown.html',
                'aws.use-local': 'false',
                'aws.region': 'eu-west-1',
                'aws.local-uri': 'http://localhost:8000',
                'aws.profile-name': 'local',
                'aws.table-prefix': 'SAAS-crawler'
            },
            logging: ecs.LogDrivers.awsLogs({
                streamPrefix: 'ByronFargateTask',
                logRetention: logs.RetentionDays.ONE_DAY,
            })
        })
        new ecs.FargateService(this, 'ByronFargateService', {
            cluster: cluster,
            taskDefinition: taskDef,
            desiredCount: 0
        });


        //Lambda setup
        const websocketConnectionsTable = new dynamodb.Table(this, 'WebsocketConnections', {
            partitionKey: {
                name: 'connectionId',
                type: dynamodb.AttributeType.STRING
            },
            billingMode: dynamodb.BillingMode.PAY_PER_REQUEST
        });
        const websocketConnectHandler = new lambda.Function(this, 'websocketConnectHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda/websocket'),
            handler: 'connect.handler'
        })
        const websocketDisconnectHandler = new lambda.Function(this, 'websocketDisconnectHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda/websocket'),
            handler: 'disconnect.handler'
        })
        websocketConnectionsTable.grantReadWriteData(websocketConnectHandler)

        const start = new lambda.Function(this, 'StartHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'start.handler',
            role: lambdaRole,
            environment: {
                subnets: vpc.publicSubnets.map(s => s.subnetId).join(','),
                security_group: vpc.vpcDefaultSecurityGroup,
                cluster: cluster.clusterName,
                taskDef: taskDef.taskDefinitionArn,
                containerName: 'ByronContainer',
                runProcess: 'START'
            }
        });
        const clean = new lambda.Function(this, 'CleanHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'start.handler',
            role: lambdaRole,
            environment: {
                subnets: vpc.publicSubnets.map(s => s.subnetId).join(','),
                security_group: vpc.vpcDefaultSecurityGroup,
                cluster: cluster.clusterName,
                taskDef: taskDef.taskDefinitionArn,
                containerName: 'ByronContainer',
                runProcess: 'CLEAN'
            }
        });
        const shutdown = new lambda.Function(this, 'ShutdownHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'shutdown.handler',
            role: lambdaRole,
            environment: {
                cluster: cluster.clusterName
            }
        });
        const status = new lambda.Function(this, 'StatusHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'status.handler',
            role: lambdaRole,
            environment: {
                tableName: 'SAAS-crawler--crawler-stats'
            }
        });


        const webSocketApi = new gateway.WebSocketApi(this, 'ByronWebsocketApi', {
            connectRouteOptions: {
                integration: new gatewayinteg.LambdaWebSocketIntegration({
                    handler: websocketConnectHandler
                })
            },
            disconnectRouteOptions: {
                integration: new gatewayinteg.LambdaWebSocketIntegration({
                    handler: websocketDisconnectHandler
                })
            },
        });

        const apiStage = new gateway.WebSocketStage(this, 'DevStage', {
            webSocketApi,
            stageName: 'dev',
            autoDeploy: true,
        });

        //Api Gateway

        const api = new gateway.HttpApi(this, "ByronApi");
        api.addRoutes(
            {
                path: '/start',
                methods: [gateway.HttpMethod.GET],
                integration: new gatewayinteg.LambdaProxyIntegration({
                    handler: start
                })
            }
        )
        api.addRoutes(
            {
                path: '/clean',
                methods: [gateway.HttpMethod.GET],
                integration: new gatewayinteg.LambdaProxyIntegration({
                    handler: clean
                })
            })
        api.addRoutes(
            {
                path: '/status',
                methods: [gateway.HttpMethod.GET],
                integration: new gatewayinteg.LambdaProxyIntegration({
                    handler: status
                })
            })
        api.addRoutes(
            {
                path: '/shutdown',
                methods: [gateway.HttpMethod.GET],
                integration: new gatewayinteg.LambdaProxyIntegration({
                    handler: shutdown
                })
            })
    }

}
