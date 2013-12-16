import java.net.*;
import java.io.*;

public class Edge{
	public Node a;
	public Node b;
	public Node link;

	public Edge(Node a, Node b){
		this.a = a;
		this.b = b;
		this.link = null;
	}

	public Edge(Node a, Node b, Node link){
		this.a = a;
		this.b = b;
		this.link = link;
	}

	public boolean hasNode(Node a){
		return (this.a.equals(a) || this.b.equals(a)) ? true : false;
	}

	public Node otherNode(Node a){
		if(a.equals(this.a)){
			return this.b;
		}
		if(a.equals(this.b)){
			return this.a;
		}
		return null;
	}

	public boolean hasLink(){
		return this.link == null ? false : true;
	}

	public String toString(){
		if(this.link == null){
			return a.port + ":" + b.port;
		}
		return a.port + ":" + b.port + "--" + link.port;
	}

	@Override
	public int hashCode(){
		int hash = 1;
		// hash = hash * 13 + (a.port + b.port + (link == null ? 0 : link.port));
		// hash = hash * 17 + (a.ip.hashCode() + b.ip.hashCode() + (link == null ? 0 : link.ip.hashCode()));
		hash = hash * 13 + (a.port + b.port);
		hash = hash * 17 + (a.ip.hashCode() + b.ip.hashCode());
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