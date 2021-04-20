const AWS = require('aws-sdk');
let ecs = new AWS.ECS();

const cluster = process.env.cluster;

exports.handler = async function (events, context) {
    console.log("SHUTDOWN");

    return await new Promise((resolve, reject) => {
            ecs.listTasks({
                cluster: cluster,
            }, function (err, data) {
                if (!err) {
                    console.log(data)
                    if (data && data.taskArns && data.taskArns.length > 0) {
                        let task = data.taskArns[0];
                        ecs.stopTask({
                            task: task,
                            cluster: cluster,
                            reason: 'Stopping indexing task'
                        }, function (err, data) {
                            let response;
                            let statusCode;
                            if (err) {
                                console.log(err, err.stack);
                                response = "Error stopping task: " + err.stack
                                statusCode = 500
                            } else {
                                response = "Stopped the following task: " + task
                                statusCode = 200
                            }
                            buildResponse(resolve, statusCode, response)
                        })
                    } else {
                        buildResponse(resolve, 200, "Currently no tasks are running");
                    }
                } else {
                    buildResponse(resolve, 500, "Error retrieving current tasks: " + err.stack);
                }
            });

            function buildResponse(resolve, statusCode, message) {
                resolve({
                    "isBase64Encoded": false,
                    "statusCode": statusCode,
                    "headers": {
                        "Access-Control-Allow-Origin": '*'
                    },
                    "body": message
                });
            }
        }
    )

}
