import {APIGatewayProxyEvent} from 'aws-lambda';

import * as AWS from 'aws-sdk';

const ddb = new AWS.DynamoDB.DocumentClient({
    apiVersion: 'latest',
    region: process.env.AWS_REGION
});

export const handler = async (event: APIGatewayProxyEvent) => {
    const tableName = process.env.TABLE_NAME;

    if (!tableName) {
        throw new Error('tableName not specified in process.env.TABLE_NAME');
    }

    const deleteParams = {
        TableName: tableName,
        Key: {
            connectionId: event.requestContext.connectionId,
        },
    };

    try {
        await ddb.delete(deleteParams).promise();
    } catch (err) {
        return {statusCode: 500, body: 'Failed to disconnect: ' + JSON.stringify(err)};
    }

    return {statusCode: 200, body: 'Disconnected.'};
};
