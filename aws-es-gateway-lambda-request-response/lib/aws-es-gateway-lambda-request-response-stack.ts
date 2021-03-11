import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as apigateway from '@aws-cdk/aws-apigateway';
import {JsonSchemaType, JsonSchemaVersion} from '@aws-cdk/aws-apigateway';
import {Role} from "@aws-cdk/aws-iam";

export class AwsEsGatewayLambdaRequestResponseStack extends cdk.Stack {
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

        const api = new apigateway.RestApi(this, 'byron-aws-es-gateway-lambda', {});
        const resource = api.root.addResource('v1');

        let responseTemplate = '#set($inputRoot = $input.path(\'$\'))\n' +
            '{\n' +
            '  "totalHits" : "$inputRoot.took",\n' +
            '  "products": [\n' +
            '#foreach($elem in $inputRoot.hits.hits)\n' +
            '    {\n' +
            '      "title": "$elem.title"\n' +
            '    }#if($foreach.hasNext),#end\n' +
            '#end\n' +
            '  ],\n' +
            '  "body": $inputRoot\n' +
            '}'

        const integration = new apigateway.LambdaIntegration(search, {
            proxy: false,
            requestTemplates: {
                'application/json': "" +
                    "#set($inputRoot = $input.path('$')) \n" +
                    "{\n" +
                    "    \"searchString\":\"$inputRoot.searchString\",\n" +
                    "    \"page\":\"$inputRoot.page\",\n" +
                    "    \"size\":\"$inputRoot.size\"\n" +
                    "}"
            },
            integrationResponses: [
                {
                    statusCode: "200",
                    responseTemplates: {
                        'application/json': responseTemplate

                    }
                }
            ]
        });

        let requestModel = api.addModel('SearchRequest', {
            description: "Default SearchRequest",
            modelName: "SearchRequest",
            contentType: "application/json",
            schema: {
                schema: JsonSchemaVersion.DRAFT7,
                properties: {
                    searchString: {
                        type: JsonSchemaType.STRING
                    },
                    page: {
                        type: JsonSchemaType.INTEGER
                    },
                    size: {
                        type: JsonSchemaType.INTEGER
                    }
                }

            }
        });

        let responseModel = api.addModel('SearchResponse', {
            description: "Default SearchResponse",
            modelName: "SearchResponse",
            contentType: "application/json",
            schema: {
                schema: JsonSchemaVersion.DRAFT7,
                title: "ResponeModel",
                type: JsonSchemaType.OBJECT,
                properties: {
                    took: {
                        type: JsonSchemaType.INTEGER
                    },
                    totalHits: {
                        type: JsonSchemaType.INTEGER
                    },
                    products: {
                        type: JsonSchemaType.OBJECT,
                        properties: {
                            title: {
                                type: JsonSchemaType.STRING
                            }
                        }
                    }
                }
            }
        });

        resource.addMethod('POST', integration, {
            requestValidatorOptions: {
                requestValidatorName: 'test-validator',
                validateRequestBody: true,
                validateRequestParameters: false,
            },
            requestModels: {
                "application/json": requestModel
            },
            methodResponses: [
                {
                    statusCode: "200",
                    responseParameters: {
                        'method.response.header.Content-Type': true,
                        'method.response.header.Access-Control-Allow-Origin': true,
                        'method.response.header.Access-Control-Allow-Credentials': true
                    },
                    responseModels: {
                        "application/json": responseModel
                    }
                }

            ]
        })


    }
}
