const AWS = require('aws-sdk');

const region = 'eu-west-1';
const domain = 'search-demo-search-kog26hrflbperlmbeubxx37xgq.eu-west-1.es.amazonaws.com';

exports.handler = async function (event) {
    console.log(event);

    return await new Promise((resolve, reject) => {
        const {queryStringParameters} = event;

        const data = {
            size: 25,
            query: {
                multi_match: {
                    query: queryStringParameters.q,
                    fields: [
                        'description',
                        'vendor',
                        'title'
                    ]
                }
            }
        }

        const endpoint = new AWS.Endpoint(domain);
        const request = new AWS.HttpRequest(endpoint, region);

        request.path += "jurkjes/_search";
        request.method = "POST";
        request.body = JSON.stringify(data);
        request.headers['Content-Type'] = 'application/json';
        request.headers['host'] = domain;

        const credentials = new AWS.EnvironmentCredentials('AWS');
        const signer = new AWS.Signers.V4(request, 'es');
        signer.addAuthorization(credentials, new Date());

        const client = new AWS.HttpClient();
        client.handleRequest(request, null, function (response) {
            let responseBody = '';
            response.on('data', function (chunk) {
                responseBody += chunk;
            });
            response.on('end', function (_chunk) {
                resolve({
                    "isBase64Encoded": false,
                    "statusCode": response.statusCode,
                    "headers": {
                        "Access-Control-Allow-Origin": '*'
                    },
                    "body": responseBody
                });
            });
        }, function (error) {
            reject(error);
        });
    });
};