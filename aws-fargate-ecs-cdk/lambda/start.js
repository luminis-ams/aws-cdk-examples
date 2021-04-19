const AWS = require('aws-sdk');
let ecs = new AWS.ECS();

exports.handler = async function (events, context) {
    console.log("START");

    let params = {
        taskDefinition: 'ByronTaskDef',
        cluster: 'ByronCluster',
        count: 1
    }

    ecs.runTask(params, function (err, data) {
        if (err) {
            console.log(err, err.stack);
        } else {
            console.log(data)
        }
        context.done(err, data)
    })

}
