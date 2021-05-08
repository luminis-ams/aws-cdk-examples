import * as cdk from '@aws-cdk/core';
import * as es from '@aws-cdk/aws-elasticsearch';
import * as path from "path";
import * as lambda from '@aws-cdk/aws-lambda';

import {CfnJson, CustomResource, Duration, Tags} from "@aws-cdk/core";
import {CfnIdentityPool, CfnIdentityPoolRoleAttachment, CfnUserPoolGroup, UserPool} from "@aws-cdk/aws-cognito";
import {
    AnyPrincipal,
    Effect,
    FederatedPrincipal,
    ManagedPolicy,
    PolicyStatement,
    Role,
    ServicePrincipal
} from "@aws-cdk/aws-iam";
import {Domain} from "@aws-cdk/aws-elasticsearch";

import {AwsCustomResource, AwsCustomResourcePolicy, PhysicalResourceId, Provider} from '@aws-cdk/custom-resources'


export class AwsEsCdkStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const applicationPrefix = "saasjettro";
        const esDomainName = "saascluster"

        /**
         * Tags are added to all child components, using a tag we can easily detect what components belong to a stack
         */
        Tags.of(this).add('stack', 'sample-cluster', {
            applyToLaunchedInstances: true
        })

        /**
         * Configure Cognito User and Identity Pool
         */
        const userPool = this.createUserPool(applicationPrefix);
        const idPool = this.createIdentityPool(applicationPrefix);

        /**
         * Create the required roles:
         * limitedUserRole: Can read data of specific indexes only, can login to Kibana
         * adminUserRole: Can do everything with the cluster, can create indexes, can configure open distro
         * esServiceRole: Is only used to configure Cognito within the elasticsearch cluster
         * lambdaServiceRole: Used by the lambda that handles the elasticsearch requests to configure open distro and
         *      execute other elasticsearch requests. Can also be used to insert index templates and even data.
         */
        const esLimitedUserRole = this.createUserRole(idPool, "esLimitedUserRole");
        const esAdminUserRole = this.createUserRole(idPool, "esAdminUserRole");
        const esServiceRole = this.createServiceRole("esServiceRole", "es.amazonaws.com", "AmazonESCognitoAccess");
        const lambdaServiceRole = this.createServiceRole("lambdaServiceRole", 'lambda.amazonaws.com', "service-role/AWSLambdaBasicExecutionRole");

        /**
         * Create two user groups within the Cognito UserPool: es-admins and es-limited users
         */
        this.createAdminUserGroup(userPool.userPoolId, esAdminUserRole.roleArn);
        this.createLimitedUserGroup(userPool.userPoolId, esLimitedUserRole.roleArn);

        /**
         * Create the Elasticsearch domain
         */
        const esDomain = this.createESDomain(esDomainName, idPool, esServiceRole, esLimitedUserRole, lambdaServiceRole, userPool);

        /**
         * Add the esLimitedUserRole as the default role when a user gets authenticated.
         */
        this.configureIdentityPool(userPool, idPool, applicationPrefix, esDomain, esLimitedUserRole);

        /**
         * Execute a diversity in calls to configure the open distro in the running cluster
         */
        this.executeOpenDistroConfiguration(lambdaServiceRole, esDomain, esAdminUserRole, esLimitedUserRole);

    }

    private createUserPool(applicationPrefix: string) {
        const userPool = new UserPool(this, applicationPrefix + 'UserPool', {
            userPoolName: applicationPrefix + ' User Pool',
            userInvitation: {
                emailSubject: 'With this account you can use Kibana',
                emailBody: 'Hello {username}, you have been invited to join our awesome app! Your temporary password is {####}',
                smsMessage: 'Hi {username}, your temporary password for our awesome app is {####}',
            },
            signInAliases: {
                username: true,
                email: true,
            },
            autoVerify: {
                email: true,
            }
        });

        userPool.addDomain('cognitoDomain', {
            cognitoDomain: {
                domainPrefix: applicationPrefix
            }
        });
        return userPool;
    }

    private createIdentityPool(applicationPrefix: string) {
        return new CfnIdentityPool(this, applicationPrefix + "IdentityPool", {
            allowUnauthenticatedIdentities: false,
            cognitoIdentityProviders: []
        });
    }

    private createServiceRole(identifier: string, servicePrincipal: string, policyName: string) {
        return new Role(this, identifier, {
            assumedBy: new ServicePrincipal(servicePrincipal),
            managedPolicies: [ManagedPolicy.fromAwsManagedPolicyName(policyName)]
        });
    }

    private createUserRole(idPool: CfnIdentityPool, identifier: string) {
        return new Role(this, identifier, {
            assumedBy: new FederatedPrincipal('cognito-identity.amazonaws.com', {
                "StringEquals": {"cognito-identity.amazonaws.com:aud": idPool.ref},
                "ForAnyValue:StringLike": {
                    "cognito-identity.amazonaws.com:amr": "authenticated"
                }
            }, "sts:AssumeRoleWithWebIdentity")
        });
    }

    private createLimitedUserGroup(userPoolId: string, limitedUserRoleArn: string) {
        new CfnUserPoolGroup(this, "userPoolLimitedGroupPool", {
            userPoolId: userPoolId,
            groupName: "es-limited-users",
            roleArn: limitedUserRoleArn
        });
    }

    private createAdminUserGroup(userPoolId: string, adminUserRoleArn: string) {
        new CfnUserPoolGroup(this, "userPoolAdminGroupPool", {
            userPoolId: userPoolId,
            groupName: "es-admins",
            roleArn: adminUserRoleArn
        });
    }

    private createESDomain(domainName: string,
                           idPool: CfnIdentityPool,
                           esServiceRole: Role,
                           esLimitedUserRole: Role,
                           lambdaServiceRole: Role,
                           userPool: UserPool) {
        const domainArn = "arn:aws:es:" + this.region + ":" + this.account + ":domain/" + domainName + "/*"

        const domain = new es.Domain(this, 'Domain', {
            version: es.ElasticsearchVersion.V7_9,
            domainName: domainName,
            enableVersionUpgrade: true,
            capacity: {
                dataNodes: 1,
                dataNodeInstanceType: "t3.small.elasticsearch",
            },
            ebs: {
                volumeSize: 10
            },
            logging: {
                appLogEnabled: false,
                slowSearchLogEnabled: false,
                slowIndexLogEnabled: false,
            },
            nodeToNodeEncryption: true,
            encryptionAtRest: {
                enabled: true
            },
            enforceHttps: true,
            accessPolicies: [new PolicyStatement({
                effect: Effect.ALLOW,
                actions: ["es:ESHttp*"],
                principals: [new AnyPrincipal()],
                resources: [domainArn],
            }),
                new PolicyStatement({
                    effect: Effect.ALLOW,
                    actions: ["es:ESHttp*"],
                    principals: [new AnyPrincipal()],
                    resources: ["arn:aws:iam::044915237328:role/AwsFargateEcsCdkStack-fargateRole19726F4B-PUQG91H6LBIB"],
                })
            ],
            cognitoKibanaAuth: {
                identityPoolId: idPool.ref,
                role: esServiceRole,
                userPoolId: userPool.userPoolId
            },
            fineGrainedAccessControl: {
                masterUserArn: lambdaServiceRole.roleArn
            }
        });


        new ManagedPolicy(this, 'limitedUserPolicy', {
            roles: [esLimitedUserRole, lambdaServiceRole],
            statements: [
                new PolicyStatement({
                    effect: Effect.ALLOW,
                    resources: [domainArn],
                    actions: ['es:ESHttp*']
                })
            ]
        })

        return domain;
    }

    private configureIdentityPool(userPool: UserPool,
                                  identityPool: CfnIdentityPool,
                                  applicationPrefix: string,
                                  esDomain: Domain,
                                  esLimitedUserRole: Role) {
        /**
         * The goal here is to set the authenticated role for the IdentityPool
         * obtain a reference to a client for the UserPool. We use the provided
         * CognitoIdentityServiceProvider and call the method listUserPoolClients
         */
        const userPoolClients = new AwsCustomResource(this, 'clientIdResource', {
            policy: AwsCustomResourcePolicy.fromSdkCalls({resources: [userPool.userPoolArn]}),
            onCreate: {
                service: 'CognitoIdentityServiceProvider',
                action: 'listUserPoolClients',
                parameters: {
                    UserPoolId: userPool.userPoolId
                },
                physicalResourceId: PhysicalResourceId.of(`ClientId-${applicationPrefix}`)
            }
        });
        userPoolClients.node.addDependency(esDomain);

        const clientId = userPoolClients.getResponseField('UserPoolClients.0.ClientId');
        const providerName = `cognito-idp.${this.region}.amazonaws.com/${userPool.userPoolId}:${clientId}`

        new CfnIdentityPoolRoleAttachment(this, 'userPoolRoleAttachment', {
            identityPoolId: identityPool.ref,
            roles: {
                'authenticated': esLimitedUserRole.roleArn
            },
            roleMappings: new CfnJson(this, 'roleMappingsJson', {
                    value: {
                        [providerName]: {
                            Type: 'Token',
                            AmbiguousRoleResolution: 'AuthenticatedRole'
                        }
                    }
                }
            )
        });
    }


    private executeOpenDistroConfiguration(lambdaServiceRole: Role, esDomain: Domain, esAdminUserRole: Role, esLimitedUserRole: Role) {
        /**
         * Function implementing the requests to Amazon Elasticsearch Service
         * for the custom resource.
         */
        const esRequestsFn = new lambda.Function(this, 'esRequestsFn', {
            runtime: lambda.Runtime.NODEJS_10_X,
            handler: 'es-requests.handler',
            code: lambda.Code.fromAsset(path.join(__dirname, '..', 'functions/es-requests')),
            timeout: Duration.seconds(30),
            role: lambdaServiceRole,
            environment: {
                "DOMAIN": esDomain.domainEndpoint,
                "REGION": this.region
            }
        });

        const esRequestProvider = new Provider(this, 'esRequestProvider', {
            onEventHandler: esRequestsFn
        });

        new CustomResource(this, 'esRequestsResource', {
            serviceToken: esRequestProvider.serviceToken,
            properties: {
                requests: [
                    {
                        "method": "PUT",
                        "path": "_opendistro/_security/api/rolesmapping/all_access",
                        "body": {
                            "backend_roles": [
                                esAdminUserRole.roleArn,
                                lambdaServiceRole.roleArn,
                                "arn:aws:iam::044915237328:role/AwsFargateEcsCdkStack-fargateRole19726F4B-PUQG91H6LBIB"
                            ],
                            "hosts": [],
                            "users": []
                        }
                    },
                    {
                        "method": "PUT",
                        "path": "_opendistro/_security/api/rolesmapping/security_manager",
                        "body": {
                            "backend_roles": [
                                lambdaServiceRole.roleArn,
                                esAdminUserRole.roleArn,
                                "arn:aws:iam::044915237328:role/AwsFargateEcsCdkStack-fargateRole19726F4B-PUQG91H6LBIB"
                            ],
                            "hosts": [],
                            "users": []
                        }
                    },
                    {
                        "method": "PUT",
                        "path": "_opendistro/_security/api/roles/kibana_limited_role",
                        "body": {
                            "cluster_permissions": [
                                "cluster_composite_ops",
                                "indices_monitor"
                            ],
                            "index_permissions": [{
                                "index_patterns": [
                                    "test*"
                                ],
                                "dls": "",
                                "fls": [],
                                "masked_fields": [],
                                "allowed_actions": [
                                    "read"
                                ]
                            }],
                            "tenant_permissions": [{
                                "tenant_patterns": [
                                    "global"
                                ],
                                "allowed_actions": [
                                    "kibana_all_read"
                                ]
                            }]
                        }
                    },
                    {
                        "method": "PUT",
                        "path": "_opendistro/_security/api/rolesmapping/kibana_limited_role",
                        "body": {
                            "backend_roles": [
                                esLimitedUserRole.roleArn
                            ],
                            "hosts": [],
                            "users": []
                        }
                    }
                ]
            }
        });
    }

}
