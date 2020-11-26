default:
	javac -d ./ Entry.java
	javac UdpChat.java

clean:
	rm -rf mypack
	rm -rf *.class
