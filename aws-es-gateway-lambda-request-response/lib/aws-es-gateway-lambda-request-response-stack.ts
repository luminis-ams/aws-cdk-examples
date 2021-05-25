import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import * as apigateway from '@aws-cdk/aws-apigateway';
import {JsonSchemaType, JsonSchemaVersion} from '@aws-cdk/aws-apigateway';
import {Role} from "@aws-cdk/aws-iam";

const fs = require('fs')
const path = require('path')

export class AwsEsGatewayLambdaRequestResponseStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const role = Role.fromRoleArn(this, 'Role', 'arn:aws:iam::044915237328:role/lambdaSearchRole', {
            mutable: false,
        });
        const search = new lambda.Function(this, 'LambdaSearchHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda/search'),
            handler: 'search.handler',
            role: role
        });
        const autocomplete = new lambda.Function(this, 'LambdaAutocompleteHandler', {
            runtime: lambda.Runtime.NODEJS_12_X,
            code: lambda.Code.fromAsset('lambda/autocomplete'),
            handler: 'autocomplete.handler',
            role: role
        });

        const api = new apigateway.RestApi(this, 'byron-aws-es-gateway-lambda', {
            defaultCorsPreflightOptions: {
                allowOrigins: apigateway.Cors.ALL_ORIGINS,
                allowMethods: apigateway.Cors.ALL_METHODS
            },
            deployOptions: {
                stageName: "v1"
            }
        });

        const searchResource = api.root.addResource('search');
        const autocompleteResource = api.root.addResource('autocomplete');

        let searchResponseTemplate = fs.readFileSync(path.resolve(__dirname, 'templates/search_response_template_mapping.vm'), 'utf8')
        const searchIntegration = new apigateway.LambdaIntegration(search, {
            proxy: false,
            integrationResponses: [
                {
                    statusCode: "200",
                    responseTemplates: {
                        'application/json': searchResponseTemplate
                    },
                    responseParameters: {
                        'method.response.header.Access-Control-Allow-Origin': "'*'"
                    }
                }
            ]
        });

        let autocompleteResponseTemplate = fs.readFileSync(path.resolve(__dirname, 'templates/autocomplete_response_template_mapping.vm'), 'utf8')
        const autocompleteIntegration = new apigateway.LambdaIntegration(autocomplete, {
            proxy: false,
            integrationResponses: [
                {
                    statusCode: "200",
                    responseTemplates: {
                        'application/json': autocompleteResponseTemplate
                    },
                    responseParameters: {
                        'method.response.header.Access-Control-Allow-Origin': "'*'"
                    }
                }
            ]
        })

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
                    },
                    sort: {
                        type: JsonSchemaType.STRING
                    },
                    userId: {
                        type: JsonSchemaType.STRING
                    },
                    variant: {
                        type: JsonSchemaType.STRING
                    },
                    filters: {
                        type: JsonSchemaType.OBJECT,
                        properties: {
                            name: {
                                type: JsonSchemaType.STRING
                            },
                            value: {
                                type: JsonSchemaType.STRING
                            }
                        }

                    }
                }
            }
        });

        searchResource.addMethod('POST', searchIntegration, {
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
                    }
                }
            ]
        })

        autocompleteResource.addMethod('POST', autocompleteIntegration, {
            methodResponses: [
                {
                    statusCode: "200",
                    responseParameters: {
                        'method.response.header.Content-Type': true,
                        'method.response.header.Access-Control-Allow-Origin': true,
                        'method.response.header.Access-Control-Allow-Credentials': true
                    }
                }
            ]
        })


    }
}
