function init() {
    registerHandler();
}

function registerHandler() {
    var eventbus = new EventBus('http://localhost:3001/eventbus');
    eventbus.onopen = function() {
        eventbus.registerHandler('output_feed', function(error, message) {
            document.getElementById('output').innerHTML += '' + JSON.parse(message.body).line + '<br>';
        });
    }
}
