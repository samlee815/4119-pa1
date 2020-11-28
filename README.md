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

