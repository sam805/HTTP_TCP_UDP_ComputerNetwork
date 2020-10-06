import java.net.DatagramPacket;
import java.net.InetAddress;
import edu.utulsa.unet.*;
//import java.net.InetSocketAddress;
//import javax.swing.plaf.metal.MetalTreeUI;
import java.io.*;

public class RReceiveUDP implements edu.utulsa.unet.RReceiveUDPI {

	private int selectMode;
	private long modeParam;
	private String fileName;
	private long timeOut;
	private int lPort;
	private String ip;
	private FileOutputStream fileOut;
	private boolean isBlock;
	private int[] sequenceOrder;
	private byte[][] rcvWindow;
	private int[] packetLength;
	private byte[] buffer;
	private DatagramPacket packet;
	private boolean exceptionalEOF;
	long tReceived;

	// Constructor
	public RReceiveUDP() {
		selectMode = 0;
		modeParam = 256;
		lPort = 12987;
		ip = "127.0.0.1";
		timeOut = 1000;
		exceptionalEOF = false;
		tReceived = 0;

	}

	@Override
	public int getMode() {
		return selectMode;
	}

	@Override
	public String getFilename() {
		return fileName;
	}

	@Override
	public int getLocalPort() {
		return lPort;
	}

	@Override
	public long getModeParameter() {
		return modeParam;
	}

	@Override
	public void setFilename(String param1) {
		fileName = param1;
	}

	@Override
	public boolean setLocalPort(int param1) {
		lPort = param1;
		return true;
	}

	@Override
	public boolean setMode(int param1) {
		selectMode = param1;
		return true;
	}

	@Override
	public boolean setModeParameter(long param1) {
		modeParam = param1;
		return true;
	}

	@Override
	public boolean receiveFile() {
		initialize();
		int lastSeqNo = -1;
		if (selectMode == 0) {
			try {
				fileOut = new FileOutputStream(fileName);

			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}

			try {
				UDPSocket socket = new UDPSocket(getLocalPort());
				System.out.println("\nReceived UDP socket is created ---> "
						+ InetAddress.getLocalHost().getHostAddress() + " : " + Integer.toString(getLocalPort()));
				System.out.println("*** ARQ Algorithm's mode is 0: Stop-and-wait ****");
				System.out.println("WAITING FOR SENDER ...");
				buffer = new byte[socket.getSendBufferSize()];
				packet = new DatagramPacket(buffer, buffer.length);
				boolean isClientPrinted = false;
				boolean rcvEOF = false;
				long curTime = System.currentTimeMillis();
				long fileLength = 0;
				while (!rcvEOF) {
					socket.receive(packet);
					InetAddress client = packet.getAddress();
					if (!isClientPrinted) {
						System.out.println("Client is connected from this address ---> " + client.getHostAddress()
								+ " : " + packet.getPort());
						curTime = System.currentTimeMillis();
						isClientPrinted = true;
						//System.out.println("\n*** LIST OF TRANSACTIONS FOR STOP-AND-WAIT ****");
					}
					int pMode = (int) buffer[0];
					int seqNumber = (int) (buffer[1] & 0x000000FF) + (int) (buffer[2] & 0x000000FF) * 256
							+ (int) (buffer[3] & 0x000000FF) * 65536 + (int) (buffer[4] & 0x000000FF) * 16777216;
					int length1 = (int) (buffer[5] & 0x000000FF) + (int) (buffer[6] & 0x000000FF) * 256;
					int isEOF = (int) buffer[7];
					byte[] newBuffer = new byte[length1];
					for (int i = 0; i < length1; i++)
						newBuffer[i] = buffer[i + 8];

					System.out.println("Sequence number of received message ---> " + Integer.toString(seqNumber)
							+ " \nlength of message ---> " + Integer.toString(length1));

					if (seqNumber == lastSeqNo + 1) {
						fileLength += length1;
						byte[] ssequenceNo = new byte[4];
						for (int i = 0; i < 4; i++)
							ssequenceNo[i] = buffer[i + 1];

						socket.send(new DatagramPacket(ssequenceNo, 4,
								InetAddress.getByName(packet.getAddress().getHostAddress()), packet.getPort()));
						System.out.println("Ack message number ---> " + Integer.toString(seqNumber));

						if (isEOF == 1)
							rcvEOF = true;
						else
							rcvEOF = false;

						fileOut.write(newBuffer);
						lastSeqNo = seqNumber;
					} else {
						byte[] ssequenceNo = new byte[4];
						for (int i = 0; i < 4; i++)
							ssequenceNo[i] = buffer[i + 1];

						socket.send(new DatagramPacket(ssequenceNo, 4,
								InetAddress.getByName(packet.getAddress().getHostAddress()), packet.getPort()));
						System.out.println("Ack message number ---> " + Integer.toString(seqNumber));
					}
				}

				fileOut.close();
				System.out.println("File received successfully... length of file in byte ---> " + fileLength
						+ "\nTime passed ---> "
						+ Double.toString((double) (System.currentTimeMillis() - curTime) / 1000));
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
			// ******************************************************
			// ******************SLIDING WINDOW**********************
			// ******************************************************
		} else if (selectMode == 1) {

			try {
				fileOut = new FileOutputStream(fileName);

			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}

			try {

				final UDPSocket socket = new UDPSocket(getLocalPort());
				int MTU = socket.getSendBufferSize();
				rcvWindow = new byte[(int) getModeParameter()][MTU];
				packetLength = new int[(int) getModeParameter()];
				boolean rcvEOF = false;

				System.out.println("Received UDP socket is created ---> "
						+ InetAddress.getLocalHost().getHostAddress() + " : " + Integer.toString(getLocalPort()));

				System.out.println("***ARQ Algorithm's mode is 0: Stop-and-wait ***");

				System.out.println("WAITING FOR SENDER ...");

				Thread auReceiver = new Thread(new Runnable() {
					public void run() {

						try {
							boolean clientWriteFlag = false;
							while (!Thread.currentThread().isInterrupted()) {
								byte[] rcvBuffer = new byte[MTU];
								DatagramPacket ReceivePacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);

								socket.receive(ReceivePacket);
								InetAddress client = ReceivePacket.getAddress();
								if (!clientWriteFlag) {
									System.out.println("Client is connected from this address ---> "
											+ client.getHostAddress() + " : " + ReceivePacket.getPort());
									tReceived = System.currentTimeMillis();
									clientWriteFlag = true;
									//System.out.println("\n**** LIST OF TRANSACTIONS FOR SLIDING WINDOW ****\n");

								}

								int modePacket = (int) rcvBuffer[0];
								int seqNo = (int) (rcvBuffer[1] & 0x000000FF) + (int) (rcvBuffer[2] & 0x000000FF) * 256
										+ (int) (rcvBuffer[3] & 0x000000FF) * 65536
										+ (int) (rcvBuffer[4] & 0x000000FF) * 16777216;
								if ((int) (rcvBuffer[4] & 0x000000FF) > (byte) 0x7F) {
									System.out.println("Sequence overflow!!!\n");
									return;
								}

								int len = (int) (rcvBuffer[5] & 0x000000FF) + (int) (rcvBuffer[6] & 0x000000FF) * 256;
								int isEOF = (int) rcvBuffer[7];

								System.out.println(
										"Sequence number of received message ---> " + Integer.toString(seqNo)
												+ "\nLength of message ---> " + Integer.toString(len));

								boolean notInList = true;
								for (int i = 0; i < getModeParameter(); i++) {
									if (sequenceOrder[i] == seqNo) {
										notInList = false;
										byte[] ssequenceNo = new byte[4];
										for (int j = 0; j < 4; j++)
											ssequenceNo[j] = rcvBuffer[j + 1];
										socket.send(new DatagramPacket(ssequenceNo, 4,
												InetAddress.getByName(ReceivePacket.getAddress().getHostAddress()),
												ReceivePacket.getPort()));
										System.out.println("Ack message number ---> " + Integer.toString(seqNo));
									}
								}
								for (int i = 0; i < getModeParameter(); i++) {
									if (notInList && (sequenceOrder[i] == -1)
											&& ((modePacket == 1) || (modePacket == 0))) {
										if (isEOF != 1) {
											sequenceOrder[i] = seqNo;

										} else {
											sequenceOrder[i] = -seqNo;
											if (sequenceOrder[i] == -1)
												exceptionalEOF = true;
										}

										for (int k = 0; k < len; k++)
											rcvWindow[i][k] = rcvBuffer[k + 8];

										packetLength[i] = len;

										byte[] ssequenceNo = new byte[4];
										for (int j = 0; j < 4; j++)
											ssequenceNo[j] = rcvBuffer[j + 1];
										socket.send(new DatagramPacket(ssequenceNo, 4,
												InetAddress.getByName(ReceivePacket.getAddress().getHostAddress()),
												ReceivePacket.getPort()));
										System.out
												.println("Ack message number ---> " + Integer.toString(seqNo));
										break;
									}
								}

							}
						} catch (Exception ex) {
							ex.printStackTrace();
							Thread.currentThread().interrupt();
							return;
						} finally {
						}
					}
				});

				auReceiver.start();
				int lastSeqRead = 0;
				long fileLen = 0;
				while (!rcvEOF) {
					Thread.sleep(200);
					for (int ind = 0; ind < getModeParameter(); ind++) {
						if (sequenceOrder[ind] != -1 || exceptionalEOF == true) {
							if ((Math.abs(sequenceOrder[ind]) == lastSeqNo + 1)) {
								byte[] writeBuffer = new byte[packetLength[ind]];
								for (int j = 0; j < packetLength[ind]; j++) {
									writeBuffer[j] = rcvWindow[ind][j];
								}

								fileOut.write(writeBuffer);
								lastSeqNo = sequenceOrder[ind];
								System.out.println(
										"\nWritten on file sequence number ---> " + Integer.toString(lastSeqNo));
								lastSeqRead++;
								fileLen += packetLength[ind];
								if ((sequenceOrder[ind] < -1) || exceptionalEOF == true) {
									rcvEOF = true;
									break;
								}

								sequenceOrder[ind] = -1;
							} else if (Math.abs(sequenceOrder[ind]) < lastSeqNo + 1) {
								sequenceOrder[ind] = -1;
								lastSeqRead++;
							} else {
								lastSeqRead++;
							}

						}

					}

				}

				System.out.println("\nFile is received successfully! Length of message in byte ---> " + fileLen
						+ "\nTime passed in seconds ---> "
						+ Double.toString((double) (System.currentTimeMillis() - tReceived) / 1000));
				fileOut.close();
				auReceiver.interrupt();
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}

		} else {
			System.out.println("\nARQ algorithm is not valid!!! Please select 0 or 1");
			return false;
		}

		return true;
	}

	private void initialize() {
		sequenceOrder = new int[(int) getModeParameter()];
		for (int i = 0; i < getModeParameter(); i++)
			sequenceOrder[i] = -1;

	}

}
