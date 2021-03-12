const AWS = require('aws-sdk');

const region = 'eu-west-1';
const domain = 'search-demo-search-kog26hrflbperlmbeubxx37xgq.eu-west-1.es.amazonaws.com';

exports.handler = async function (event) {
    console.log(event);

    return await new Promise((resolve, reject) => {
        //todo: Improve the way we build queries
        const data = {
            from: event.page,
            size: event.size,
            query: {
                multi_match: {
                    query: event.searchString,
                    type: "cross_fields",
                    operator: "AND",
                    fields: [
                        'description',
                        'vendor',
                        'title'
                    ]
                }
            },
            suggest: {
                text: event.searchString,
                "DESCRIPTION_TERM": {
                    term: {
                        field: "description",
                        size: 1,
                        suggest_mode: "ALWAYS",
                        prefix_length: 1,
                        min_word_length: 3
                    }
                },
                "TITLE_TERM": {
                    term: {
                        field: "title",
                        size: 1,
                        suggest_mode: "ALWAYS",
                        prefix_length: 1,
                        min_word_length: 3
                    }
                }
            },
            aggregations: {
                "byType": {
                    terms: {
                        field: "type",
                        size: 10
                    }
                },
                "byTags": {
                    terms: {
                        field: "tags",
                        size: 10
                    }
                },
                "byColor": {
                    terms: {
                        field: "color.keyword",
                        size: 10
                    }
                },
                "byMaterial": {
                    terms: {
                        field: "material.keyword",
                        size: 10
                    }
                },
                "byRating": {
                    terms: {
                        field: "rating",
                        size: 10
                    }
                },
                "byVendor": {
                    terms: {
                        field: "vendor",
                        size: 10,
                        order: [
                            {
                                "_count": "desc"
                            },
                            {
                                "_key": "asc"
                            }
                        ]
                    }
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
                resolve(responseBody);
            });
        }, function (error) {
            reject(error);
        });
    });
};