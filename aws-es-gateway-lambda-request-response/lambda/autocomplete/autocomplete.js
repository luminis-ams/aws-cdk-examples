const AWS = require('aws-sdk');
const esb = require("elastic-builder");

const region = 'eu-west-1';
const domain = 'search-demo-search-kog26hrflbperlmbeubxx37xgq.eu-west-1.es.amazonaws.com';

exports.handler = async function (event) {
    let requestBodySearch = esb.requestBodySearch();

    let boolQuery = esb.boolQuery()
    let searchString = event.searchString;
    if (searchString.includes(" ")) {
        let terms = searchString.split(" ");
        for (const singleTerm of terms) {
            boolQuery.must(buildSingleTermQuery(singleTerm))
        }
    } else {
        boolQuery.must(buildSingleTermQuery(searchString))
    }

    requestBodySearch.query(boolQuery)
    requestBodySearch.size(100)
    requestBodySearch.highlight(esb.highlight("*").type("fvh"))

    console.log(requestBodySearch.toJSON())

    return await new Promise((resolve, reject) => {
        const endpoint = new AWS.Endpoint(domain);
        const request = new AWS.HttpRequest(endpoint, region);

        request.path += "jurkjes/_search";
        request.method = "POST";
        request.body = JSON.stringify(requestBodySearch.toJSON());
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

    function buildSingleTermQuery(string) {
        return esb.boolQuery()
            .should(esb.matchQuery("autocomplete_field.prefix", string).operator("and").analyzer("standard").boost(2))
            .should(esb.matchQuery("autocomplete_field", string).operator("and").analyzer("standard"));
    }
};
