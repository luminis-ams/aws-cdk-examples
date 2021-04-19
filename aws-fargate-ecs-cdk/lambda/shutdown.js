const AWS = require('aws-sdk');
let ecs = new AWS.ECS();

exports.handler = async function (events, context) {
    console.log("SHUTDOWN");

    let params = {
        task: 'ByronTaskDef',
        cluster: 'ByronCluster',
        reason: 'Stopping indexing task'
    }

    ecs.stopTask(params, function (err, data) {
        if (err) {
            console.log(err, err.stack);
        } else {
            console.log(data)
        }
        context.done(err, data)
    })

}
