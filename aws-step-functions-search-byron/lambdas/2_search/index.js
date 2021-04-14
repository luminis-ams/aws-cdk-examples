const AWS = require('aws-sdk');

const region = 'eu-west-1';
const domain = 'search-demo-search-kog26hrflbperlmbeubxx37xgq.eu-west-1.es.amazonaws.com';

exports.handler = async function (event) {

    return await new Promise((resolve, reject) => {
        //todo: Improve the way we build queries
        const data = {
            from: 0,
            size: 10,
            query: {
                multi_match: {
                    query: event.term,
                    type: "cross_fields",
                    operator: process.env.runAsOr = 'true' ? 'OR' : 'AND',
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
                resolve(JSON.parse(responseBody));
            });
        }, function (error) {
            reject(error);
        });
    });
};