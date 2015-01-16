# Contains classes implementing possible responses.
class Response(object):
    resType = ""
    def __init__(self, resType):
        self.resType = resType
    def serialize(self):
        return "NOT_IMPLEMENTED"

# concrete responses are used if packets are returned
class ConcreteResponse(Response):
    flags = ''
    ack = 0
    seq = 0

    def __init__(self, flags, seq, ack):
        super(ConcreteResponse, self).__init__("CONCRETE")
        self.seq = seq
        self.ack = ack
        self.flags = flags

    def serialize(self):
        outputString = self.flags + " " + str(self.seq) + " " + str(self.ack)
        return outputString
    
    def equals(self, response):
        equ = False
        if response is not None and type(response) is ConcreteResponse:  
            equ = (self.flags == response.flags) and (self.ack == response.ack) 
            equ = equ and (self.seq == response.seq)
        return equ

# timeouts are used if no packets are returned
class Timeout(Response):
    def __init__(self):
        super(Timeout, self).__init__("TIMEOUT")
    def serialize(self):
        outputString = "timeout"
        return  outputString

# undefineds are used for malformed packets (not used currently) 
class Undefined(Response):
    def __init__(self):
        super(Undefined, self).__init__("UNDEFINED")
    def serialize(self):
        outputString = "undefined"
        return  outputString

if __name__ == "__main__":
    response = ConcreteResponse("ss",10,20)
    print response.serialize()
    timeout = Timeout()
    print timeout.serialize()