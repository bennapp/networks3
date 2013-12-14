import java.net.*;
import java.io.*;

public class Edge{
	public Node a;
	public Node b;

	public Edge(Node a, Node b){
		this.a = a;
		this.b = b;
	}

	@Override
	public int hashCode(){
		int hash = 1;
		hash = hash * 13 + a.port + b.port;
		hash = hash * 17 + a.ip.hashCode() + b.ip.hashCode();
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
        if (!(obj instanceof Edge)){
            return false;
        }

        Edge rhs = (Edge) obj;
        if((rhs.a.equals(this.a) && rhs.b.equals(this.b)) || (rhs.a.equals(this.b) && rhs.b.equals(this.a))){
        	return true;
        }
        return false;
    }

	public static void main(String[] args){
	}
}