import * as cdk from '@aws-cdk/core';
import * as lambda from '@aws-cdk/aws-lambda';
import {StartingPosition} from '@aws-cdk/aws-lambda';
import * as dynamodb from '@aws-cdk/aws-dynamodb';
import {DynamoEventSource} from '@aws-cdk/aws-lambda-event-sources';
import {Duration} from "@aws-cdk/core";

export class AwsScraperLambdaStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const table = new dynamodb.Table(this, 'crawler', {
            partitionKey: {
                name: 'PartitionKey',
                type: dynamodb.AttributeType.STRING
            },
            stream: dynamodb.StreamViewType.NEW_IMAGE,
            tableName: "crawler",
            readCapacity: 1,
            writeCapacity: 1,
        });

        const scraper = new lambda.Function(this, 'LambdaScraper', {
            runtime: lambda.Runtime.NODEJS_10_X,
            code: lambda.Code.fromAsset('lambda'),
            timeout: Duration.seconds(1),
            handler: 'scraper.handler'
        });

        scraper.addEventSource(new DynamoEventSource(table, {
            startingPosition: StartingPosition.TRIM_HORIZON,
            batchSize: 1
        }));

        table.grantReadWriteData(scraper);

    }
}
