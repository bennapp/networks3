import java.net.*;
import java.lang.*;
import java.sql.Timestamp;
import java.util.*;
import java.io.*;


public class BFClient implements Runnable{
	public static final int INFINITY = 1000000000; //max weight 1,147,483,647
	public String threadType;
	public static long timeout;
	public static int itimeout;
	public static long time;
	public static long prevTime;
	public static int localPort;
	public static InetAddress ip;
	public static Hashtable<Node, Integer> routingTable = new Hashtable<Node, Integer>();
	public static Hashtable<Node, Integer> nodeDownTable = new Hashtable<Node, Integer>();
	public static Hashtable<Node, Integer> nodeDownTable2 = new Hashtable<Node, Integer>();
	public static Hashtable<Node, Integer> nodeUpTable = new Hashtable<Node, Integer>();
	public static Integer messageCount = 1;
	public static Boolean update;
	public static Hashtable<Node, Long> timeoutTable = new Hashtable<Node, Long>();
	public static Hashtable<Edge, Integer> graph = new Hashtable<Edge, Integer>();
	
	public BFClient(String threadType){
		this.threadType = threadType;
	}

	public static void main(String[] args){
		if(args.length < 4){
			System.err.println("Wrong nuber of arguments try");
			System.err.println("%> ./bfclient localport timeout [localhost port1 weight1 ...]");
			System.exit(0);
		}
		if(((args.length - 2) % 3) != 0){
			System.err.println("Wrong nuber of arguments try");
			System.err.println("%> ./bfclient localport timeout [ipaddress1 port1 weight1 ...]");
			System.exit(0);
		}
		
		try {
			ip = getRawIP("localhost");
			localPort = Integer.parseInt(args[0]);
			timeout = Long.parseLong(args[1]) * 1000;
			itimeout = Integer.parseInt(args[1]);
			update = true;
			prevTime = System.currentTimeMillis();
		} catch (Exception e){
			e.printStackTrace();
		}

		// add the nodes to the routingTable
		int index = args.length -1;
		try{
			while(index > 2){
				Node node = new Node(getRawIP(args[index-2]), Integer.parseInt(args[index-1]));
				routingTable.put(node, Integer.parseInt(args[index]));
				timeoutTable.put(node, System.currentTimeMillis());
				index = index - 3;
			}
		} catch (Exception e){
			e.printStackTrace();
		}

		//add initial routingTable to graph
		Node me = new Node(ip, localPort);
		for (Enumeration<Node> e = routingTable.keys(); e.hasMoreElements();){
			Node to = e.nextElement();
			graph.put(new Edge(me, to), routingTable.get(to));
		}

		//p(routingTable);

		new Thread(new BFClient("listen")).start();
		new Thread(new BFClient("send")).start();
		new Thread(new BFClient("algorithm")).start();
		//new Thread(new BFClient("timer")).start();
		//new Thread(new BFClient("time")).start();

		//handle commands
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		String input;
		try{
			//LINKUP localhost 9002
			//LINKDOWN localhost 9002
			while((input = stdIn.readLine()) != null ){
				if(input.startsWith("LINKUP")){
					String[] inputs = input.split(" ");
					try{
						Node nodeUp = new Node(getRawIP(inputs[1]), Integer.parseInt(inputs[2]));
						if(nodeDownTable.get(nodeUp) != null){
							messageCount = 1;
							p("LINKingUP " + inputs[1] + " " +  inputs[2]);
							p(nodeDownTable2.get(nodeUp));
							graph.put(new Edge(me, nodeUp), nodeDownTable2.get(nodeUp));
							nodeUpTable.put(nodeUp, routingTable.get(nodeUp));
							nodeDownTable.remove(nodeUp);
							nodeDownTable2.remove(nodeUp);
						} else {
							System.err.println("Client with ip " + inputs[1] + " and port " + inputs[2] + " has not been set using LINKDOWN");
						}

					} catch (Exception e){
						e.printStackTrace();
						System.err.println("Incorrect LINKUP args");
					}
				}
				if(input.startsWith("LINKDOWN")){
					String[] inputs = input.split(" ");
					try{
						Node nodeDown = new Node(getRawIP(inputs[1]), Integer.parseInt(inputs[2]));
						p("LINKingDOWN " + inputs[1] + " " +  inputs[2]);
						if (routingTable.get(nodeDown) != null) {
							messageCount = 1;
							if(nodeUpTable.get(nodeDown) != null){
								nodeUpTable.remove(nodeDown);
							}
							int temp = 0;
							temp = routingTable.get(nodeDown);
							nodeDownTable.put(nodeDown, temp);
							nodeDownTable2.put(nodeDown, temp);
							p(nodeDownTable);
							graph.remove(new Edge(me, nodeDown));
							graph.put(new Edge(me, nodeDown), INFINITY);
						} else {
							System.err.println("Client with ip " + inputs[1] + " and port " + inputs[2] + " is not known");
						}
					} catch (Exception e){
						e.printStackTrace();
						System.err.println("Incorrect LINKDOWN args");
					}
					
				}
				if(input.equals("SHOWRT")){
					p("<Current Time>Distance vector list is:");
					String output = "";
					for (Enumeration<Node> e = routingTable.keys(); e.hasMoreElements();){
						Node to = e.nextElement();
						if(to.link != null){
							output += "Destination = " + to.ip.toString().substring(1) + ":" + to.port + ", Cost = " + routingTable.get(to) + ", Link = (" + to.link.ip.toString().substring(1) + ":" + to.link.port + ")\n";
						} else {
							output += "Destination = " + to.ip.toString().substring(1) + ":" + to.port + ", Cost = " + routingTable.get(to) + ", Link = (" + to.ip.toString().substring(1) + ":" + to.port + ")\n";
						}
					}
					p(output);
				}
				if(input.equals("CLOSE")){
					p("Closing client");
					System.exit(0);
				}else{

				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	//LINKDOWN localhost 9002
	//LINKUP localhost 9002
	public void run(){
		if(threadType.equals("send")){
			while(true){
				p("sending");
				for (Enumeration<Node> e = routingTable.keys(); e.hasMoreElements();){
					Node to = e.nextElement();
					Node me = new Node(ip, localPort);
					if(!to.equals(me)){
						byte[] bytes = new byte[2400];//suitable for a network ~ 100, increase for a large network
						byte[] ipbytes = ip.getAddress();
						for(int i = 0; i<4; i++){
							bytes[i] = ipbytes[i];
						}
						setInt(bytes, 4, localPort);
						try{
							if(nodeDownTable.get(to) == null && nodeUpTable.get(to) == null){
								byte[] tablebytes = serialize(routingTable);
								for(int i = 0; i<tablebytes.length; i++){
			  						 bytes[i+8] = tablebytes[i];
			  					}
							}
							p(to);
							if(messageCount == 1){
								p("node down table" + nodeDownTable);
								if(nodeDownTable.get(to) != null){
									p("sending down code!");
									setInt(bytes, 2395, 1337);
								}
								p("node up table" + nodeUpTable);
								if(nodeUpTable.get(to) != null){
									p("sending up code!");
									nodeUpTable.remove(to);
									setInt(bytes, 2395, 9779);
								}
								messageCount--;
							}
							
							if(routingTable.get(to) != INFINITY){
								DatagramPacket packet = new DatagramPacket(bytes, bytes.length, to.ip, to.port);
								DatagramSocket datagramSocket = new DatagramSocket();
								datagramSocket.send(packet);
							} else if(nodeDownTable.get(to) != null || nodeUpTable.get(to) != null) {
								DatagramPacket packet = new DatagramPacket(bytes, bytes.length, to.ip, to.port);
								DatagramSocket datagramSocket = new DatagramSocket();
								datagramSocket.send(packet);
							} else{
								//LINKDOWN localhost 9002
								//LINKUP localhost 9002
							}
						} catch (Exception errorSend){
							errorSend.printStackTrace();
						}
					}

				}
				update = false;
				wait(itimeout);
				update = true;
				//check if update
			}
		}
		if(threadType.equals("listen")){
			try{
				DatagramSocket listenSocket = new DatagramSocket(localPort); 
  				byte[] receiveData = new byte[2400]; //suitable for a network ~ 100, increase for a larger network 
  				while(true){
  					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
  					listenSocket.receive(receivePacket);
  					byte[] bytes = receivePacket.getData();
  					byte[] tablebytes = new byte[1024];
  					//byte[] ipbytes = new byte[4];
  					byte[] portbytes = new byte[4];
  					for(int i = 0; i< 4; i++){
  					//	ipbytes[i] = bytes[i];
  						portbytes[i] = bytes[i+4];
  					}
  					for(int i = 0; i<tablebytes.length; i++){
  						tablebytes[i] = bytes[i+8];
  					}
  					InetAddress ipRec = receivePacket.getAddress();//InetAddress.getByAddress(ipbytes);
  					int portRec = getInt(portbytes, 0); //receivePacket.getPort(); 
					Node from = new Node(ipRec, portRec);
					Node me = new Node(ip, localPort);
					timeoutTable.put(from, System.currentTimeMillis());
  					if(getInt(bytes, 2395) == 1337){
  						if (routingTable.get(from) != null) {
							if(nodeUpTable.get(from) == null){
								nodeDownTable.put(from, routingTable.get(from));
								graph.remove(new Edge(me, from));
								graph.put(new Edge(me, from), INFINITY);
							}
						}
  					} else if(getInt(bytes, 2395) == 9779){
  						p("here is the down table " + nodeDownTable);
  						p("here is from" + from);
  						p("recieved code");
  						graph.put(new Edge(me, from), nodeDownTable.get(from));
  						nodeDownTable.remove(from);

  					} else {
  						if(nodeDownTable.get(from) != null){
  							//
  						}else{
  							if(nodeUpTable.contains(from)){
  								nodeUpTable.remove(from);
  							}
		  					@SuppressWarnings("unchecked")
							Hashtable<Node, Integer> receivedTable = ((Hashtable<Node, Integer>)deserialize(tablebytes));
							//p("this is te receivedTable");
							//p(receivedTable);
							//p("graph before" + graph);
							p("portRec " + portRec);
							// p("receivedTable" + receivedTable);
							for (Enumeration<Node> e = receivedTable.keys(); e.hasMoreElements();){
								Node to = e.nextElement();

								Edge toFrom = null;
								if(to.link == null){
									toFrom = new Edge(to, from);
								} else {
									toFrom = new Edge(to, from, to.link);
								}
								//p("to.port looking for 9002" + to.port);
								//p("to.port value = " + receivedTable.get(toFrom));

								if(to.link != null && graph.get(to) != null){
									if(graph.get(to) == INFINITY && to.link.equals(me)){
									} else {
										graph.put(toFrom, receivedTable.get(to));
									}
								} else {
									graph.put(toFrom, receivedTable.get(to));
								}
								//check if any links went to infinity
								if(receivedTable.get(to) == INFINITY){
									for (Enumeration<Edge> e2 = graph.keys(); e2.hasMoreElements();){
										Edge edge = e2.nextElement();
										if(edge.hasLink()){
											if(edge.link == to){
												graph.put(edge, INFINITY);
											}
										}
							 		}
								}
								//check if neighbors link to eachother to a node that is infinity
								for (Enumeration<Node> e2 = routingTable.keys(); e2.hasMoreElements();){
									Node to2 = e2.nextElement();
									if(to2.link != null){
										if(toFrom.hasLink()){
											if(to.equals(to2)){
												p("to = " + to);
												p("to2 = " + to2);
												if(to.link.equals(me) && to2.link.equals(from)){
													p("test3");
													graph.remove(new Edge(me, to));
													graph.remove(new Edge(from, to));
													graph.put(new Edge(me, to), INFINITY);
													graph.put(new Edge(from, to), INFINITY);
												}
											}
										}
									}
								}
							}
  						}
					// p("graph now = " + graph);
  					} //else for linkdown
  				}
  			} catch (Exception e){
  				//e.printStackTrace();
  			}
		}
		if(threadType.equals("algorithm")){
			while(true){
				wait(1);

				if(!update){
					Hashtable<Edge, Integer> tempGraph = graphDeepCopy(graph);
					Hashtable<Node, Integer> tempTable = routingDeepCopy(routingTable);
					Hashtable<Node, Integer> oldTable = routingDeepCopy(routingTable);
					// p("before" + tempGraph);
					tempGraph = bellmanFord(tempGraph);
					// p("after " + tempGraph);
					
					// p("this is tempGraph" + tempGraph);
					// p("this is graph" + graph);
					tempTable = updateRoutingTable(tempGraph);
					// p("tempTable" + tempTable);
					
					//p("");
					//p("oldTable" + oldTable);
					p("routingTable" + routingTable);

					//p("tables equal? = " + tempTable.equals(oldTable));
					if(!tempTable.equals(oldTable)){
						routingTable = routingDeepCopy(tempTable);
						update = true;
					}
					//p("timeout table " + timeoutTable);
					if(checkTimeOuts()){
						p("hello");
						update = true;
					}
				}
			}
		}
		// if(threadType.equals("timer")){
		// 	while(update == 0){
		// 		if(checkTimeOuts()){
		// 			p("hello");
		// 			update++;
		// 		}
		// 		wait(3);
		// 		p(timeoutTable);
		// 	}
		// }
		
	}

	public static Hashtable<Edge, Integer> bellmanFord(Hashtable<Edge, Integer> graph){
		Hashtable<Edge, Integer> tempGraph = new Hashtable<Edge, Integer>();
		//init
		HashSet<Node> nodes = new HashSet<Node>();
		for (Enumeration<Edge> e = graph.keys(); e.hasMoreElements();){
			Edge to = e.nextElement();
			nodes.add(to.a);
			nodes.add(to.b);
		}
		Iterator<Node> nodesIteratorA = nodes.iterator();
		while(nodesIteratorA.hasNext()){
			Node a = nodesIteratorA.next();
			Iterator<Node> nodesIteratorB = nodes.iterator();
			while(nodesIteratorB.hasNext()){
				Node b = nodesIteratorB.next();
				Edge edge = new Edge(a, b);
				if(a.equals(b)){
					tempGraph.put(edge, 0);
				} else if(graph.get(edge) != null){
					tempGraph.put(edge, graph.get(edge));
				} else {
					tempGraph.put(edge, INFINITY);
				}
			}
		}
		//relax
		nodesIteratorA = nodes.iterator();
		while(nodesIteratorA.hasNext()){
			Node a = nodesIteratorA.next();
			for (Enumeration<Edge> e = graph.keys(); e.hasMoreElements();){
				Edge to = e.nextElement();
				Node b = to.a; Node c = to.b;
				Edge aToB = new Edge(a, b); Edge aToC = new Edge(a, c); 
				Edge aToCLinkB = new Edge(a, c, b);
				Edge aToBLinkC = new Edge(a, b, c);
				if(graph.get(aToB) != null){
					if(graph.get(aToB) + graph.get(to) < tempGraph.get(aToC)){
						tempGraph.remove(aToCLinkB);
						tempGraph.put(aToCLinkB, graph.get(aToB) + graph.get(to));
					}
				}
				//p("has link" + aToBLinkC.hasLink());
				//p(aToBLinkC);
				if(graph.get(aToC) != null){
					if(graph.get(aToC) + graph.get(to) < tempGraph.get(aToB)){
						tempGraph.remove(aToBLinkC);
						tempGraph.put(aToBLinkC, graph.get(aToC) + graph.get(to));
					}
				}
			}

		}
		//check for negatives
		for (Enumeration<Edge> e = tempGraph.keys(); e.hasMoreElements();){
			Edge to = e.nextElement();
			if(tempGraph.get(to) < 0){
				System.err.println("Graph contains negatives");
			}
		}
		return tempGraph;
	}

	public static boolean checkTimeOuts(){
		boolean timeouts = false;
		Node me = new Node(ip, localPort);
		for (Enumeration<Node> e = timeoutTable.keys(); e.hasMoreElements();){
			Node node = e.nextElement();
			if(nodeTimeOut(timeoutTable.get(node))){
				timeouts = true;
				routingTable.put(node, INFINITY);
				p("node" + node.port);
				p("me" + me.port);
				// graph.remove(new Edge(node, me));
				graph.put(new Edge(node, me), INFINITY);
			}
		}
		return timeouts;
	}

	public static boolean nodeTimeOut(long nodeTime){
		long timeNow = System.currentTimeMillis();
		if((timeNow - nodeTime) > (timeout * 3)){
			return true;
		}
		return false;
	}

	public static Hashtable<Node, Integer> updateRoutingTable(Hashtable<Edge, Integer> graph){
		Hashtable<Node, Integer> tempTable = new Hashtable<Node, Integer>();
		Node me = new Node(ip, localPort);
		for (Enumeration<Edge> e = graph.keys(); e.hasMoreElements();){
			Edge to = e.nextElement();
			if(to.hasNode(me)){
				Node from = to.otherNode(me);
				//p("from port = " + from.port);
				//p(graph.get(to));
				if(to.hasLink()){
					tempTable.put(new Node(from, to.link), graph.get(to));
				} else {
					tempTable.put(from, graph.get(to));
				}
			}
		}
		return tempTable;
	}
	public static Hashtable<Node, Integer> routingDeepCopy(Hashtable<Node, Integer> graph){
		Hashtable<Node, Integer> tempTable = new Hashtable<Node, Integer>();
		for (Enumeration<Node> e = graph.keys(); e.hasMoreElements();){
						Node to = e.nextElement();
						tempTable.put(to, graph.get(to));
		}
		return tempTable;
	}
	public static Hashtable<Edge, Integer> graphDeepCopy(Hashtable<Edge, Integer> graph){
		Hashtable<Edge, Integer> tempGraph = new Hashtable<Edge, Integer>();
		for (Enumeration<Edge> e = graph.keys(); e.hasMoreElements();){
			Edge to = e.nextElement();
			tempGraph.put(to, graph.get(to));
		}
		return tempGraph;
	}
	public static InetAddress getRawIP(String ip){
		InetAddress rawIp = null;
		try{
			rawIp = InetAddress.getByAddress(InetAddress.getByName(ip).getAddress());
		} catch(Exception e){
			e.printStackTrace();
		}
		return rawIp;
	}

	//credit is due where credit is deserved
	//http://stackoverflow.com/questions/3736058/java-object-to-byte-and-byte-to-object-converter-for-tokyo-cabinet
	//Thomas Mueller
	public static byte[] serialize(Object obj) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	ObjectOutputStream os = new ObjectOutputStream(out);
    	os.writeObject(obj);
    return out.toByteArray();
	}
	//http://stackoverflow.com/questions/3736058/java-object-to-byte-and-byte-to-object-converter-for-tokyo-cabinet
	//Thomas Mueller
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return is.readObject();
	}
	public static int getInt(byte[] bytes, int i){
 		return (int)(((bytes[i]&0xFF)<<24) | ((bytes[i+1]&0xFF) << 16) | ((bytes[i+2]&0xFF)<<8) | (bytes[i+3]&0xFF));
	}
	public static void setInt(byte[] bytes, int i, int num){
		bytes[i] 	 = (byte) (num >> 24);
		bytes[i + 1] = (byte) (num >> 16);
		bytes[i + 2] = (byte) (num >> 8);
		bytes[i + 3] = (byte) num;
	}
	public static void wait(int seconds){
		long timeToWait = seconds * 1000;
		long time = System.currentTimeMillis();
		while(System.currentTimeMillis() - time < timeToWait){
			//do nothing
		}
	}
	public static void p(Object o){ //print for lazy people
		System.out.println(o);
	}
}