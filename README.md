# HTTP_TCP_UDP_ComputerNetwork
The goal of this program is transferring the file in an unreliable network and it includes two classes: RSendUDP and RReceiveUDP
These classes have methods and different threads to handle and transfer a file from client to server through two different techniques: “stop-and-wait” or “sliding window” which are ARQ algorithm.
In order to do that, a packet is designed to deliver the message to receiver, this packet includes: 
1.	mode is 1 byte, 0 dedicates stop-and-wait and 1 dedicates sliding-window,
2.	sequence number is 4 bytes,
3.	payload length is 2 bytes,
4.	end of file flag is 1 byte, 0 represents this is not last packet and 1 represents it is.
5.	and payload is 8bytes.
Program includes methods and relevant message to display the errors, IP address, port number, Ack number, also name and mode number of algorithm is used.
Algorithm work with different files size and gives the relevant messages.
For testing the program I used runner.java and runnerS.java in which two classes are instantiated.
