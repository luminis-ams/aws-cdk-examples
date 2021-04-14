exports.handler = (event, context, callback) => {
    const term = event.term;

    if (!term) {
        callback(Error("Term is empty"))
    }

    const result = {term: term};
    callback(null, result);
};