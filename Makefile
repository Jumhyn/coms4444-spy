all:
	java spy.sim.Simulator -Xms8192m -Xmx10240m -t 10000 --player g1 g2 g3 g4 g5 g6 g7 g8 -m default_map --gui --fps 5

compile:
	javac spy/sim/*.java

clean:
	rm spy/*/*.class

