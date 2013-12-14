import java.net.*;
import java.lang.*;
import java.sql.Timestamp;
import java.util.*;
import java.io.*;


public class BFClient implements Runnable{
	public static final int INFINITY = 2147483647;
	public String type; //type can be listen, send
	public static long timeout;
	public static int localPort;
	public static InetAddress ip;
	public static Hashtable<Node, Integer> routingTable = new Hashtable<Node, Integer>();
	public static boolean update;
	public static Hashtable<Node, Long> timeoutTable = new Hashtable<Node, Long>();
	public static Hashtable<Edge, Integer> graph = new Hashtable<Edge, Integer>();
	
	public BFClient(String type){
		this.type = type;
	}

	public static void main(String[] args){
		if(args.length > 4){
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
			ip = InetAddress.getLocalHost();
			localPort = Integer.parseInt(args[0]);
			timeout = Long.parseLong(args[1]);
		} catch (Exception e){
			e.printStackTrace();
		}

		// add the nodes to the Hashtable
		try{
			int numNodesIndex = ((args.length - 2) / 3) - 1;
			while(numNodesIndex >= 0){
				Node node = new Node(InetAddress.getByName(args[numNodesIndex-2]), Integer.parseInt(args[numNodesIndex-1]));
				routingTable.put(node, Integer.parseInt(args[numNodesIndex]));
				numNodesIndex--;
			}
		} catch (Exception e){
			e.printStackTrace();
		}

		new Thread(new BFClient("listen")).start();
		new Thread(new BFClient("send")).start();
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
		if(type.equals("send")){
			while(true){
				if(update){
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
							for(int i = 8; i<bytes.length; i++){
		  						 bytes[i] = tablebytes[i-8];
		  					}
							DatagramPacket packet = new DatagramPacket(bytes, bytes.length, to.ip, to.port);
							DatagramSocket datagramSocket = new DatagramSocket();
							datagramSocket.send(packet);
						} catch (Exception esend){
							esend.printStackTrace();
						}
					}
					update = false;
				} else{
					//save off temps
					//run the algorithm
				}
				//check if update
			}
		}
		if(type.equals("listen")){
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
  					for(int i = 8; i<bytes.length; i++){
  						tablebytes[i-8] = bytes[i];
  					}
  					InetAddress ipRec = InetAddress.getByAddress(ipbytes);
  					int portRec = getInt(portbytes, 0);
  					@SuppressWarnings("unchecked")
					Hashtable<Node, Integer> receivedTable = ((Hashtable<Node, Integer>)deserialize(tablebytes));
					Node from = new Node(ipRec, portRec);
					for (Enumeration<Node> e = receivedTable.keys(); e.hasMoreElements();){
						Node to = e.nextElement();
						graph.put(new Edge(to, from), receivedTable.get(e));
					}
  				}
  			} catch (Exception e){
  				e.printStackTrace();
  			}
		}
	}

	public static byte[] serialize(Object obj) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	ObjectOutputStream os = new ObjectOutputStream(out);
    	os.writeObject(obj);
    return out.toByteArray();
	}
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
	public static void p(Object o){ //print for lazy people
		System.out.println(o);
	}
}