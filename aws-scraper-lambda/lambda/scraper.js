const AWS = require('aws-sdk');

const { processUrl } = require('./processUrl')

exports.handler = async function (event) {

    console.log('Received event:', JSON.stringify(event, null, 2))

    await Promise.all(event.Records.map(async (record) => {
        // Only run for inserts
        if (record.eventName !== 'INSERT') return

        console.log('DynamoDB Record: ', record.dynamodb)
        const URL = record.dynamodb.NewImage.url.S
        console.log('Processing: ', URL)
        await processUrl(URL)
    }))

    console.log(`Processed ${event.Records.length} records.`)
    return { statusCode: 200 }
};