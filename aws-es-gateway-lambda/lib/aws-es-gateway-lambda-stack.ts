import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as apigw from '@aws-cdk/aws-apigateway';
import {ManagedPolicy, Role, ServicePrincipal} from "@aws-cdk/aws-iam";

export class AwsEsGatewayLambdaStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const role = Role.fromRoleArn(this, 'Role', 'arn:aws:iam::044915237328:role/lambdaSearchRole', {
            mutable: false,
        });
        const search = new lambda.Function(this, 'LambdaSearchHandler', {
            runtime: lambda.Runtime.NODEJS_10_X,
            code: lambda.Code.fromAsset('lambda'),
            handler: 'search.handler',
            role: role
        });

        let gateway = new apigw.LambdaRestApi(this, 'SearchLambdaEndpoint', {
            handler: search
        });
    }
}
