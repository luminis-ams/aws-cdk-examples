const AWS = require('aws-sdk');
const esb = require("elastic-builder");

const region = 'eu-west-1';
const domain = 'search-demo-search-kog26hrflbperlmbeubxx37xgq.eu-west-1.es.amazonaws.com';

const aggregations = {
    "byType": "type",
    "byTags": "tags",
    "byColor": "color.keyword",
    "byMaterial": "material.keyword",
    "byRating": "rating",
    "byVendor": "vendor"
}

exports.handler = async function (event) {
    console.log(event);

    function getAggs() {
        return [
            esb.termsAggregation("byType", "type"),
            esb.termsAggregation("byTags", "tags"),
            esb.termsAggregation("byColor", "color.keyword"),
            esb.termsAggregation("byMaterial", "material.keyword"),
            esb.termsAggregation("byRating", "rating"),
            esb.termsAggregation("byVendor", "vendor")
        ];
    }

    return await new Promise((resolve, reject) => {
        let searchRequest = esb.requestBodySearch();

        let query = esb.multiMatchQuery(["description", "vendor", "title"], event.searchString)
            .type("cross_fields")
            .operator("and");

        if (event.filters) {
            let filters = [];
            for (const filter of event.filters) {
                let fieldName = aggregations.get(filter.name);
                console.log(fieldName)
                filters.push(esb.termsQuery(fieldName, filter.value))
            }
            query = esb.boolQuery()
                .must(query)
                .filter(filters);
        }

        searchRequest.query(query)
        searchRequest.size(event.size)
        searchRequest.from(event.from)
        searchRequest.suggestText(event.searchString)
        searchRequest.suggest(esb.termSuggester("DESCRIPTION_TERM", "description"))
        searchRequest.suggest(esb.termSuggester("TITLE_TERM", "title"))
        searchRequest.aggs(getAggs())

        const endpoint = new AWS.Endpoint(domain);
        const request = new AWS.HttpRequest(endpoint, region);

        request.path += "jurkjes/_search";
        request.method = "POST";
        request.body = JSON.stringify(searchRequest.toJSON());
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
