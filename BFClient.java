import java.net.*;
import java.lang.*;
import java.sql.Timestamp;
import java.util.*;
import java.io.*;


public class BFClient implements Runnable{
	public static final int INFINITY = 1000000000; //max weight 1,147,483,647
	public String threadType;
	public static long timeout;
	public static int localPort;
	public static InetAddress ip;
	public static Hashtable<Node, Integer> routingTable = new Hashtable<Node, Integer>();
	public static int update;
	public static Hashtable<Node, Long> timeoutTable = new Hashtable<Node, Long>();
	public static Hashtable<Edge, Integer> graph = new Hashtable<Edge, Integer>();
	
	public BFClient(String threadType){
		this.threadType = threadType;
	}

	public static void main(String[] args){
		if(args.length < 4){
			System.err.println("Wrong nuber of arguments try");
			System.err.println("%> ./bfclient localport timeout [ipaddress1 port1 weight1 ...]");
			System.exit(0);
		}
		if(((args.length - 2) % 3) != 0){
			System.err.println("Wrong nuber of arguments try");
			System.err.println("%> ./bfclient localport timeout [ipaddress1 port1 weight1 ...]");
			System.exit(0);
		}
		
		try {
			// ip = InetAddress.getByAddress(InetAddress.getLocalHost().getAddress());
			ip = getRawIP("localhost");
			localPort = Integer.parseInt(args[0]);
			timeout = Long.parseLong(args[1]) * 1000;
			update = 1;
		} catch (Exception e){
			e.printStackTrace();
		}

		// add the nodes to the routingTable
		int index = args.length -1;
		try{
			while(index > 2){
				Node node = new Node(getRawIP(args[index-2]), Integer.parseInt(args[index-1]));
				routingTable.put(node, Integer.parseInt(args[index]));
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

		//handle commands
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		try{
			while((input = stdIn.readLine()) != null ){
				if(input.startsWith("LINKUP")){

				}
				if(input.startsWith("LINKDOWN")){

				}
				if(input.equals("SHOWRT")){

				}
				if(input.equals("CLOSE")){
					System.exit(0);
				}else{

				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public void run(){
		if(threadType.equals("send")){
			while(true){
				if(true){
					//send to everyone
					for (Enumeration<Node> e = routingTable.keys(); e.hasMoreElements();){
						Node to = e.nextElement();
						byte[] bytes = new byte[2400];//suitable for a network ~ 100, increase for a large network
						byte[] ipbytes = ip.getAddress();
						for(int i = 0; i<4; i++){
							bytes[i] = ipbytes[i];
						}
						setInt(bytes, 4, localPort);
						try{
							byte[] tablebytes = serialize(routingTable);
							for(int i = 0; i<tablebytes.length; i++){
		  						 bytes[i+8] = tablebytes[i];
		  					}
							DatagramPacket packet = new DatagramPacket(bytes, bytes.length, to.ip, to.port);
							DatagramSocket datagramSocket = new DatagramSocket();
							datagramSocket.send(packet);
						} catch (Exception errorSend){
							errorSend.printStackTrace();
						}
					}
					update--;
					wait(2);
				}
				//check if update
			}
		}
		if(threadType.equals("listen")){
			try{
				DatagramSocket listenSocket = new DatagramSocket(localPort); 
  				byte[] receiveData = new byte[2408]; //suitable for a network ~ 100, increase for a larger network 
  				while(true){
  					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
  					listenSocket.receive(receivePacket);
  					byte[] bytes = receivePacket.getData();
  					byte[] ipbytes = new byte[4];
  					byte[] portbytes = new byte[4];
  					byte[] tablebytes = new byte[1024];
  					for(int i = 0; i< 4; i++){
  						ipbytes[i] = bytes[i];
  						portbytes[i] = bytes[i+4];
  					}
  					for(int i = 0; i<tablebytes.length; i++){
  						tablebytes[i] = bytes[i+8];
  					}
  					InetAddress ipRec = InetAddress.getByAddress(ipbytes);
  					int portRec = getInt(portbytes, 0);
  					@SuppressWarnings("unchecked")
					Hashtable<Node, Integer> receivedTable = ((Hashtable<Node, Integer>)deserialize(tablebytes));
					//p("this is te receivedTable");
					//p(receivedTable);
					//p("graph before" + graph);
					Node from = new Node(ipRec, portRec);
					for (Enumeration<Node> e = receivedTable.keys(); e.hasMoreElements();){
						Node to = e.nextElement();
						Edge toFrom = new Edge(to, from);

						// p(graph.contains(toFrom));
						// for (Enumeration<Edge> e2 = graph.keys(); e2.hasMoreElements();){
						// 	Edge edge = e2.nextElement();
						// 	p("edge   " + edge.a.ip );
						// 	p("toFrom " + toFrom.a.ip );
						// 	p("edge   " + edge.a.port );
						// 	p("toFrom " + toFrom.a.port );
						// 	p("edge   " + edge.b.ip );
						// 	p("toFrom " + toFrom.b.ip );
						// 	p("edge   " + edge.b.port );
						// 	p("toFrom " + toFrom.b.port );
						// 	p("edge   " + edge.hashCode() );
						// 	p("toFrom " + toFrom.hashCode() );
						// 	p("edge and toFrom " + edge.equals(toFrom) );
						// }

						if(graph.get(toFrom) == null){
							graph.put(toFrom, receivedTable.get(to));
						}
					}
					// p("graph now = " + graph);
  				}
  			} catch (Exception e){
  				e.printStackTrace();
  			}
		}
		if(threadType.equals("algorithm")){
			while(true){
				Hashtable<Edge, Integer> tempGraph = graphDeepCopy(graph);
				p("before" + tempGraph);
				tempGraph = bellmanFord(tempGraph);
				p("after " + tempGraph);
				wait(5);
			}
		}
		if(threadType.equals("timer")){

		}
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
				if(graph.get(aToB) != null){
					if(graph.get(aToB) + graph.get(to) < tempGraph.get(aToC)){
						tempGraph.put(aToC, graph.get(aToB) + graph.get(to));
					}
				}
				if(graph.get(aToC) != null){
					if(graph.get(aToC) + graph.get(to) < tempGraph.get(aToB)){
						tempGraph.put(aToB, graph.get(aToC) + graph.get(to));
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