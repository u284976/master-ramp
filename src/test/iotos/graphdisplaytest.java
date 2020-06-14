package test.iotos;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;

public class graphdisplaytest{
    public static void main(String[] args){
        Graph graph = new SingleGraph("Tutorial 1");

		graph.addNode("1");
		graph.addNode("2");
		graph.addNode("3");
		graph.addEdge("A", "1", "2");
		graph.addEdge("B", "2", "3");
		graph.addEdge("C", "C", "A");

		graph.display();
    }
}