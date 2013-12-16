import java.net.*;
import java.io.*;

public class Node implements Serializable{
	public InetAddress ip;
	public int port;
	public Node link;

	public Node(InetAddress ip, int port){
		this.ip = ip;
		this.port = port;
	}

	public Node(Node a, Node link){
		this.ip = a.ip;
		this.port = a.port;
		this.link = link;
	}

	public String toString(){
		return this.link == null ? this.port + "" : "" + this.port + "--" + this.link.port;
	}

	@Override
	public int hashCode(){
		int hash = 1;
		hash = hash * 13 + port;
		hash = hash * 17 + ip.hashCode();
		return hash;
	}

	@Override
       public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (obj == this){
            return true;
        }
        if (!(obj instanceof Node)){
            return false;
        }

        Node rhs = (Node) obj;
        if(rhs.ip.equals(this.ip) && rhs.port == this.port){
        	return true;
        }
        return false;
    }

	public static void main(String[] args){
	
	}
}