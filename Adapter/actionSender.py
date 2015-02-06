#from socketAdapter import SocketAdapter
import socket

# extends sender functionality with higher level commands
class ActionSender:
    cmdSocket = None
    sender = None
    actions = ["listen", "accept", "closeconnection", "closeserver", "exit"]
    def __init__(self, cmdIp = "10.0.2.2", cmdPort=5000, sender = None):
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        self.sender = sender
        
    def __str__(self):
        ret =  "ActionSender with parameters: " + str(self.__dict__)
        if self.sender is not None:
            ret  = ret + "\n" + str(self.sender)
        return ret
        
    # returns a new socket to the mapper/learner
    def setUpSocket(self):
        if self.cmdSocket is None:
            cmdSocket = socket.create_connection((self.cmdIp, self.cmdPort))
            cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            cmdSocket.settimeout(60)
            print "python connected to server Adapter at " + self.cmdIp + " " + (str(self.cmdPort))
            #self.cmdSocket = SocketAdapter(cmdSocket)
            self.cmdSocket = cmdSocket
        self.listenForServerPort()
        
    
    def closeSockets(self):
        try:
            print(self.cmdSocket)
            if self.cmdSocket is not None:
                print "Telling server adapter to end session"
                self.cmdSocket.send("exit\n")
                print "Closing server adapter command socket"
                self.cmdSocket.close()
        except IOError as e:
            print "Error closing ActionSender " + e
            raise e
    
    # fetches a server port
    def listenForServerPort(self):
        newPortFound = False
        newPortString = ""
        while True:
            newPortString = self.cmdSocket.recv(1024)
            print "received " + newPortString
            for word in newPortString.split(): # TODO check if this really always works with a stream
                if newPortFound:
                    self.serverPort = int(word)
                    print "next server port: " + word
                    return
                if word == "port":
                    newPortFound = True
                    
    def sendReset(self):
        if self.cmdSocket is None:
            self.setUpSocket()
        print "reset"
        print "********** reset **********"
        self.cmdSocket.send("reset\n")
        self.listenForServerPort()
        self.sender.setServerPort(self.serverPort)
    
    def captureResponse(self):
        response = None
        if self.sender is not None:
            response = self.captureResponse()
        return response
    
    def isFlags(self, inputString):
        isFlags = False
        if self.sender is not None:
            isFlags = self.sender.isFlags(inputString)
        return isFlags
    
    def isAction(self, inputString):
        return inputString in self.actions
    
    def sendAction(self, inputString):
        if self.isAction(inputString):
            self.cmdSocket.send(inputString + "\n") # TODO race-condition here, might go wrong: 
            response = self.sender.captureResponse() # response might arrive before sender is ready
            #cmdResponse = self.cmdSocket.recv(1024)
            #print "server adapter response: " + cmdResponse
        else:
            print inputString + " not a valid action ( it is not one of: " + str(self.actions) + ")"
        return response
                
    def sendInput(self, input1, seqNr, ackNr):
        return self.sender.sendInput(input1, seqNr, ackNr)
    
    def shutdown(self):
        self.closeSockets()
        if self.sender is not None:
            self.sender.shutdown()


    