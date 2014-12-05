
import socket

# extends sender functionality with higher level commands
class ActionSender:
    cmdSocket = None
    sender = None
    actions = ["listen", "accept", "closeconnection", "closeserver"]
    def __init__(self, cmdIp = "192.168.56.1", cmdPort=5000, sender = None):
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        self.sender = sender
        
    # returns a new socket to the mapper/learner
    def setUpSocket(self):
        if self.cmdSocket is None:
            cmdSocket = socket.create_connection((self.cmdIp, self.cmdPort))
            cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            print "python connected to server Adapter at " + cmdIp + " " + (str(cmdPort))
            self.cmdSocket = SocketAdapter(cmdSocket)
            self.listenForServerPort()
        
    
    def closeSockets(self):
        try:
            print(self.cmdSocket)
            if self.cmdSocket is not None:
                print "Closing gateway server command socket"
                self.cmdSocket.send("exit\n")
                self.cmdSocket.close()
        except IOError:
            print "Error closing ActionSender " + IOError.message
    
    # fetches a server port
    def listenForServerPort(self):
        newPortFound = False
        newPortString = ""
        while True:
            newPortString = self.cmdSocket.recv(1024)
            for word in newPortString.split(): # TODO check if this really always works with a stream
                print "recvd " + word
                if newPortFound:
                    self.serverPort = int(word)
                    print "next server port: " + word
                    return
                if word == "port":
                    newPortFound = True
                    
    def sendReset(self):
         print "reset"
         print "********** reset **********"
         self.sender.refreshNetworkPort()
         self.cmdSocket.send("reset\n")
         self.listenForServerPort()
         self.sender.setServerPort(self.serverPort)
    
    def isAction(self, input):
        return input in self.actions
    
    def sendCommand(self, input):
        if self.cmdSocket is None:
            self.setUpSocket()
        response = None
        if self.isAction(input):
            self.cmdSocket.send(input + "\n") # TODO race-condition here, might go wrong: 
            response = sender.captureResponse() # response might arrive before sender is ready
        else:
            print input + " not a valid action ( it is not one of: " + str(self.actions) + ")"
        return response
                
    def sendInput(self, input1, seqNr, ackNr):
        return sender.sendInput(input1, seqNr, ackNr)
    


    