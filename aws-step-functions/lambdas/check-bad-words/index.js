exports.handler = (event, context, callback) => {
    const sender = event.sender;
    const message = event.message;

    function hasBadWords(message) {
        return message.indexOf("bad") !== -1;
    }

    const valid = !hasBadWords(message);
    const result = {sender: sender, message: message, valid: valid};
    callback(null, result);
};