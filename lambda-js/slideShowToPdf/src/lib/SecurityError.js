module.exports = class SecurityError extends Error {

    constructor(message, httpCode) {
        super(message, httpCode)
        this.httpCode=httpCode;
        Error.captureStackTrace(this, SecurityError)
    }

}