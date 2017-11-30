
var nextId = Date.now();
var clients = [];
var http = require('http');
var server = http.createServer(function (request, response) {});
server.listen(1234, function () {
    console.log((new Date()) + "Server is listening on port 1234");
});
var WebSocketServer = require('websocket').server;
wsServer = new WebSocketServer({
    httpServer: server
});

wsServer.on('request', function (r) {
    console.log((new Date()) + 'Connection accepted ');
    console.log(r.requestedProtocols);
    var connection = r.accept('', r.origin);
    connection.clientId = nextId;
    nextId++;
    clients.push(connection);

    var msg = {
        type: 'id',
        clientId: connection.clientId
    };
    connection.sendUTF(JSON.stringify(msg));
    connection.on('message', function (msg) {
        console.log("receive from client: " + JSON.stringify(msg));

        if (msg.type == 'utf8') {
            var data = JSON.parse(msg.utf8Data);
            //注册消息
            if (data.type == 'register') {
                var name = data.name;
                console.log("new client register: " + name);
                for (var i = 0; i < clients.length; ++i) {
                    if (clients[i].clientId == data.clientId) {
                        clients[i].name = name;
                        break;
                    }
                }
                sendUserListToAll();
            } else if (data.type == 'offer') {
                console.log("offer from client");
                sendMessageToClient(data.target, data);

            } else if (data.type == 'answer') {
                console.log("answer from client");
                sendMessageToClient(data.target, data);
            } else if (data.type == "icecandidate") {
                console.log("icecandidate from client " + data.target);
                sendMessageToClient(data.target, data);
            } else if (data.type == "removecandidate") {
                console.log("remove ice from client: " + data.target);
                sendMessageToClient(data.target, data);
            }
        }
    });

    connection.on('close', function (reasonCode, description) {
       clients = clients.filter(function (p1, p2, p3) {
           return p1.connected;

        });

       sendUserListToAll();
       console.log("connection closed: " + connection.name + " " + reasonCode + " " + description);

    });

})

function sendUserListToAll(){
    var msg = generateUserList();
    for (var i = 0; i < clients.length; ++ i) {
        clients[i].sendUTF(JSON.stringify(msg));
    }
}

function generateUserList() {
    var msg = {
        type:'users',
        list:[]
    }
    for (var i = 0; i < clients.length; ++i) {
        msg.list.push(clients[i].name);
    }

    return msg;
}

function sendMessageToClient(clientName, data) {

    for (var i = 0; i < clients.length; ++i) {
        if (clients[i].name == clientName) {
            clients[i].sendUTF(JSON.stringify(data));
            break;
        }
    }
}
