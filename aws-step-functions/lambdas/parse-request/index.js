exports.handler = (event, context, callback) => {
    // Create the message to be send
    // TODO can we throw an error here if one of the two fields is not present?
    const sender = event.sender;
    const message = event.message;
    const result = {sender: sender, message: message};
    callback(null, result);
};