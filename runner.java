import java.net.InetAddress;
import java.net.InetSocketAddress;


public class runner {

	public static void main(String[] args) {	
		System.out.println("Runner Started ...\n");
		RReceiveUDP reciverUDP = new RReceiveUDP();
		reciverUDP.setLocalPort(12999);
		reciverUDP.setFilename("C:\\Users\\sara\\Desktop\\NetworkProject2\\testOUT.txt");
		reciverUDP.setModeParameter(12000);
		reciverUDP.setMode(1);
		reciverUDP.receiveFile();
	
	}

}
