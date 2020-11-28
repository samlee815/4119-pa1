Simple netchat client & server
programming assignment 1 for CSEE4119, Fall 2020

DESIGNS:

Implementing the UdpChat using java, including three files UdpChat.java, Entry.java and ClientIO.java
Entry.java basically implements a class to contain the  entry of client information table for both 
server and client and corresponding methods that can be utilized. ClientIO.java implements the runnable
interace and allow us to create a thread that takes stdin input and put the message in a synchronized 
list that can be used by the main thread.

In UdpChat.java, the main method do the error checking of command line arguments and start corresponding 
serverLoop and clientLoop. And in clientLoop and serverLoop we use HashTable to store client information
table.and for serverloop the server deals with different kinds of messages and send corresponding acks 
to them. And we design the registration and deregistration request to be reg|nickname|port and 
der|nicjname|port and their corresponding ack to be ack|reg, ack|der (and ack|dec to decline reg requests)
And we also have offline message sent to server in the format off|destination|sender|message and the ack
in the form ack|off|destination|sender|message. And we keep a hashmap to store offline messages sent to
server. and transmitTable method and transmitOffmsg is used when server is to broadcast table updates or
offline messages to clients.

In the clientLoop we have loop to cope with reg/dereg requests at the beginning, and compareLoop to cope
with sending message and receiving corresponding ack, the message in sent in form msg|destination|sender
|message. and the corresponding ack has form ack|msg|dest|sender|message. And when the message is not received
by sender. the offLoop takes care of the offlinemessage sending to server. And finnally the handle method does
a lazy timed receive on the socket and deal with the meassage received on the socket.

To act like a normal chat client, we only allow client to send dereg requests that matches its own nickname

Testing 1:
start server using:java UdpChat -s 2000
start client 1/2/3 using :java UdpChat -c client1/2/3 127.0.0.1 2000 2100/2200/2300

Server message: 
>>>Client client1 is online
>>>Client client2 is online
>>>Client client3 is online
>>>

Client 1 message:
>>>[Welcome, you are registered]
>>>[Client table updated]
[Client table updated]
[Client table updated]

Client 2 message:
>>>[Welcome, you are registered]
>>>[Client table updated]
[Client table updated]

Client 3 message:
>>>[Welcome, you are registered]
>>>[Client table updated]

sending hey from 1 to 2, 2 to 3, 3 to 1

Client 1 message:
send client2 hey
>>>[Message received by client2]
client3: hey

client 2 message:
client1: hey
send client3 hey
>>>[Message received by client3]

client 3 message:
client2: hey
send client1 hey
>>>[Message received by client1]

(We implement UdpChat in the way such a client could not send msg to itself, it just simply prints another prompt)

dereg client1 and send hey from client2,3 to client 1:

server message:
>>>Client client1 is offline

client 1 message:
>>>0
[You are offline, bye]

client 2 message:
[Client table updated]
send client1 hey
>>>No ack from client1,send message to server
[Message received by server and saved]

client 3 message:
[Client table updated]
send client1 hey
>>>No ack from client1,send message to server
[Message received by server and saved]

reregistratinf client 1:

server message:
>>>Client client1 is online

client 1 message:
>>>reg client1
>>>[Welcome, you are registered]
>>>[Client table updated]
[You have message]
Offline msg :<client2> 2020-11-29 06:14:43.737  hey
Offline msg :<client3> 2020-11-29 06:14:52.795  hey

client 2 message:
[Client table updated]

client 3 message:
[Client table updated]


Test session 1 ends

Testing 2:

Starting server and client1/2 using the sameport in testing 1
messages omitted

dereg client1:
server message:
>>>Client client1 is offline

client 1 message:
dereg client1
>>>[You are offline, bye]

client 2 message:
[Client table updated]

exiting server and send hey from client 1 to 2:

client 3 message:
send client1 hey
>>>No ack from client1,send message to server
[Server not responding]
[Exiting]



