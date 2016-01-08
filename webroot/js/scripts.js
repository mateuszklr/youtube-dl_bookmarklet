function init() {
    registerHandler();
}

function registerHandler() {
    var eventbus = new EventBus('http://localhost:3000/eventbus');
    eventbus.onopen = function() {
        eventbus.registerHandler('feed.output', function(error, message) {
            document.getElementById('output').innerHTML += '<span>' + JSON.parse(message.body).line + '</span><br>';
        });
        eventbus.registerHandler('feed.error', function(error, message) {
            document.getElementById('output').innerHTML += '<span class="red">' + JSON.parse(message.body).line + '</span><br>';
        });
    }
}
