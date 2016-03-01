all:
	javac -cp "lib/*" -d class/ EcoSim/*.java

clean:
	rm -r class/*/*.class

