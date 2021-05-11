// Empty constructor
function ZettlePlugin() {}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'

ZettlePlugin.prototype.login = function(successCallback,errorCallback){
    var options = {};
    options.method = "login";
    cordova.exec(successCallback, errorCallback, 'ZettlePlugin', "login", [options])
}
ZettlePlugin.prototype.pay = function(amount,successCallback,errorCallback){
    cordova.exec(successCallback, errorCallback, 'ZettlePlugin', "pay", [amount,"DKK","msp@yourpay.io",70555678])
}
ZettlePlugin.prototype.payWithToken = function(successCallback,errorCallback){
    var options = {};
    options.method = "login";
    cordova.exec(successCallback, errorCallback, 'ZettlePlugin', "payWithToken", [options])
}
ZettlePlugin.prototype.settings = function(successCallback,errorCallback){
    var options = {};
    options.method = "login";
    cordova.exec(successCallback, errorCallback, 'ZettlePlugin', "settings", [options])
}
ZettlePlugin.prototype.logout = function(successCallback,errorCallback){
    var options = {};
    options.method = "login";
    cordova.exec(successCallback, errorCallback, 'ZettlePlugin', "logout", [options])
}

ZettlePlugin.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.ZettlePlugin = new ZettlePlugin();
    return window.plugins.ZettlePlugin;
};
cordova.addConstructor(ZettlePlugin.install);