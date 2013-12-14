import java.net.*;
import java.lang.*;
import java.sql.Timestamp;
import java.util.*;
import java.io.*;

public class Test{
	
	public static void main(String[] args){
		//test that hastable works with Node[]

		//test how long a ip adress in bytes is
		//so it always 4
		// try{
		// 	InetAddress ip = InetAddress.getByName("255.255.255.255");

		// 	byte[] bytes = ip.getAddress();
		// 	p(bytes.length);
		// } catch(Exception e){
		// 	e.printStackTrace();
		// }

		//test how long an int is in a byte[] should be 4
		//byte[] bytes = new byte[100];
		//setInt(bytes, 3, 0);
		//p(getInt(bytes, 3));

		//test is you can parse a hashtable with underflow, yes
		//test is I can hash nodes and edges, yes, and yes
		try{
			byte[] tablebytes = new byte[1024]; 
			Hashtable<Edge, Integer> testHash = new Hashtable<Edge, Integer>();
			Node testNode = new Node(InetAddress.getByName("255.255.255.255"), 2000);
			Node testNode2 = new Node(InetAddress.getByName("255.255.1.255"), 2002);
			Edge edge = new Edge(testNode, testNode2);
			testHash.put(edge, 5);
			
			//tablebytes = serialize(testHash);
			//@SuppressWarnings("unchecked")
			//Hashtable<Edge, Integer> newHash = ((Hashtable<Edge, Integer>)deserialize(tablebytes));

			p(testHash);
			Edge edge2 = new Edge(testNode2, testNode);

			p(edge.equals(edge2));
			p(edge.hashCode());
			p(edge2.hashCode());

			testHash.put(edge2, 10);

			p(testHash.get(edge2));

		} catch (Exception e){
			e.printStackTrace();
		}


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
	public static void p(Object o){ //print for lazy people
		System.out.println(o);
	}
}