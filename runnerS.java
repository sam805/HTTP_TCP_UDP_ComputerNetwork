import java.net.InetAddress;

import java.net.InetSocketAddress;


public class runnerS {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Runner Started ...\n");
		RSendUDP sender= new RSendUDP();
		sender.setFilename("C:\\Users\\sara\\Desktop\\NetworkProject2\\test.txt");
		sender.setReceiver( new InetSocketAddress("localhost", 12999));
		sender.setModeParameter(12000);
		sender.setTimeout(2000);
		sender.setMode(1);
		sender.sendFile();

	}

}
