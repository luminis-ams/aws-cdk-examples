const AWS = require('aws-sdk')
AWS.config.update({ region: process.env.REGION || 'eu-west-1' })
const ddb = new AWS.DynamoDB()

module.exports.flushToDynamoDB = async (params) => {
    const batchParams = {
        RequestItems: {
            'crawler': params
        }
    }

    console.log('flushToDynamoDB: ', batchParams)

    return new Promise((resolve, reject) => {
        ddb.batchWriteItem(batchParams, function (err, data) {
            if (err) {
                console.error('flushToDynamoDB', err)
                reject(err)
            } else {
                console.log('flushToDynamoDB: ', data)
                resolve(data)
            }
        })
    })
}