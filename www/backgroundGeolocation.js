var exec = require('cordova/exec');

const TAG = "[backgroundGeolocation]";

const PLUGIN = "BackgroundGeolocation";

const METHODS = {
    CONFIGURE:"configure",
    START:"start",
    STOP:"stop",
    CHECK_PERMISSIONS:"checkPermissions",
    ON:"on",
    REQUEST_PERMISSIONS:"requestPermissions"
}

const backgroundGeolocation = {

    start() {
        return new Promise((resolve,reject)=>{
            exec(resolve,reject,PLUGIN,METHODS.START)
        })
    },
    stop() {
        return new Promise((resolve,reject)=>{
            exec(resolve,reject,PLUGIN,METHODS.STOP)
        })
    },
    configure(options){
        return new Promise((resolve,reject)=>{
            exec(resolve,reject,PLUGIN,METHODS.CONFIGURE,[options])
        })
    },
    requestPermissions(options){
        return new Promise((resolve,reject)=>{
            exec(resolve,reject,PLUGIN,METHODS.REQUEST_PERMISSIONS,[options])
        })
    },
    checkPermissions(options){
        return new Promise((resolve,reject)=>{
            exec(resolve,reject,PLUGIN,METHODS.CHECK_PERMISSIONS,[options])
        })
    },
    on(success,error){
        exec(success,error,PLUGIN,METHODS.ON)
    }
}




module.exports = backgroundGeolocation
