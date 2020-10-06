
// Imported Classes
import java.net.DatagramPacket;
import java.net.InetAddress;
import edu.utulsa.unet.*;
import java.net.InetSocketAddress;
import java.io.*;

//**************  Send Class ***************

public class RSendUDP implements edu.utulsa.unet.RSendUDPI {

	private String sentFileName;
	private int mode;
	private long modeParam;
	private InetSocketAddress rcvInstance;
	private long timeOut;
	private int localPort;
	private String iP;
	private FileInputStream inStrm;
	private long fLen;
	private long initTime;

	public RSendUDP() {
		sentFileName = "";
		localPort = 12987;
		modeParam = 256;
		timeOut = 1000;
		iP = "127.0.0.1";
		rcvInstance = new InetSocketAddress(iP, localPort);
		mode = 0;
		fLen = 0;
		initTime = System.currentTimeMillis();
	}

	@Override
	public String getFilename() {
		return sentFileName;
	}

	@Override
	public int getLocalPort() {
		return localPort;
	}

	@Override
	public int getMode() {

		return mode;
	}

	@Override
	public long getModeParameter() {
		return modeParam;
	}

	@Override
	public InetSocketAddress getReceiver() {

		return rcvInstance;
	}

	@Override
	public long getTimeout() {

		return timeOut;
	}

	@Override
	public void setFilename(String param1) {

		sentFileName = param1;

	}

	@Override
	public boolean setLocalPort(int param1) {

		localPort = param1;
		return true;
	}

	@Override
	public boolean setMode(int param1) {

		mode = param1;
		return true;
	}

	@Override
	public boolean setModeParameter(long param1) {

		modeParam = param1;
		return true;
	}

	@Override
	public boolean setReceiver(InetSocketAddress param1) {

		try {
			rcvInstance = param1;
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

	}

	@Override
	public boolean setTimeout(long param1) {

		timeOut = param1;
		return true;
	}

	@Override
	public boolean sendFile() {

		UDPSocket udpSocket1 = null;
		try {
			inStrm = new FileInputStream(sentFileName);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

		try {
			udpSocket1 = new UDPSocket(localPort);
			System.out.println("UDP socket create on this address ---> " + InetAddress.getLocalHost().getHostAddress()
					+ " : " + Integer.toString(getLocalPort()));
			System.out.println("Receiver's address and port number ---> " + getReceiver().getAddress().getHostAddress()
					+ " : " + Integer.toString(rcvInstance.getPort()));

			// ****** CREATING PACKET BASED ON MODE: SLIDING WINDOW OR STOP_AND_WAIT********
			int MTU = udpSocket1.getSendBufferSize();
			//System.out.println(MTU);

			// ****** SUM OF PAYLOAD, SEQ NO, MODE, AND isEndOfFile
			int header = 8;
			int payLength = 0;

			int bufferSize = (int) modeParam;
			if (bufferSize < MTU - header)
				bufferSize = MTU - header;
			bufferSize = MTU - header;

			if (bufferSize < 1) {
				System.out.println("******ERROR!!! Please increase size of MTU, too small MTU!****");
				udpSocket1.close();
				return false;
			}

			// ******** STOP-AND-WAIT: MODE = 0 **********//
			// *******************************************//
			if (mode == 0) {
				System.out.println("*** ARQ Algorithm's mode is 0: Stop-and-wait ****");
				//System.out.println("\n**** LIST OF TRANSACTIONS FOR STOP-AND-WAIT ****\n");
				byte[] bufferS = new byte[bufferSize];
				byte isEOF = 0;
				byte modeSAW = 0;
				int seqNo = 0;
				int frameSize = 0;
				byte[] payLenSAW = new byte[2];
				byte[] seqNoSAW = new byte[4];
				AsyncronizedSender reliableSndr = new AsyncronizedSender();

				initTime = System.currentTimeMillis();
				while ((payLength = inStrm.read(bufferS)) != -1) {
					byte[] packetSAW = new byte[payLength + header];
					payLenSAW[0] = (byte) (payLength & 0xFF);
					payLenSAW[1] = (byte) ((payLength >> 8) & 0xFF);

					seqNoSAW[0] = (byte) (seqNo & 0xFF);
					seqNoSAW[1] = (byte) ((seqNo >> 8) & 0xFF);
					seqNoSAW[2] = (byte) ((seqNo >> 16) & 0xFF);
					seqNoSAW[3] = (byte) ((seqNo >> 24) & 0xFF);

					packetSAW[0] = modeSAW;

					for (int i = 0; i < 4; i++) {
						packetSAW[i + 1] = seqNoSAW[i];

					}
					packetSAW[5] = payLenSAW[0];
					packetSAW[6] = payLenSAW[1];
					if (bufferS.length > payLength)
						isEOF = 1;
					else
						isEOF = 0;

					packetSAW[7] = isEOF;

					for (int i = 0; i < payLength; i++) {
						packetSAW[header + i] = bufferS[i];
					}
					frameSize = payLength + header;

					reliableSndr.asynSend(udpSocket1, packetSAW, frameSize, seqNo, getReceiver(), getTimeout());
					fLen += payLength;

					seqNo++;
				}

				System.out.println("Successful... Size of sent file in bytes ---> " + Long.toString(fLen)
						+ "  \nTime of sending in second ---> "
						+ Double.toString((double) (System.currentTimeMillis() - initTime) / 1000));
				inStrm.close();
				return true;

			}
			// ******** SLIDING WINDOW: MODE = 1 **********//
			// ******** ***********************************//
			else if (mode == 1) {
				System.out.println("*** ARQ Algorithm's mode is 1: Sliding Window ****");
				//System.out.println("\n**** LIST OF TRANSACTIONS FOR SLIDING WINDOW ****\n");

				byte[] bufferSW = new byte[bufferSize];
				byte modSW = 1;
				byte isEOF = 0;
				int sequenceNo = 0;
				int packetSize = 0;
				byte[] payLenSW = new byte[2];
				byte[] sequenceNoSW = new byte[4];

				SlidingWindow swin = new SlidingWindow(getModeParameter(), MTU, getTimeout());
				swin.winSender(udpSocket1, getReceiver());
				swin.receiver(udpSocket1);
				initTime = System.currentTimeMillis();
				while ((payLength = inStrm.read(bufferSW)) != -1) {
					byte[] frameSW = new byte[payLength + header];
					payLenSW[0] = (byte) (payLength & 0xFF);
					payLenSW[1] = (byte) ((payLength >> 8) & 0xFF);

					sequenceNoSW[0] = (byte) (sequenceNo & 0xFF);
					sequenceNoSW[1] = (byte) ((sequenceNo >> 8) & 0xFF);
					sequenceNoSW[2] = (byte) ((sequenceNo >> 16) & 0xFF);
					sequenceNoSW[3] = (byte) ((sequenceNo >> 24) & 0xFF);

					frameSW[0] = modSW;

					for (int i = 0; i < 4; i++) {
						frameSW[i + 1] = sequenceNoSW[i];

					}
					frameSW[5] = payLenSW[0];
					frameSW[6] = payLenSW[1];
					if (bufferSW.length > payLength)
						isEOF = (byte) 1;
					else
						isEOF = (byte) 0;

					frameSW[7] = isEOF;

					for (int i = 0; i < payLength; i++) {
						frameSW[header + i] = bufferSW[i];
					}
					packetSize = payLength + header;
					swin.winSenderManager(frameSW, packetSize, sequenceNo);
					fLen += payLength;
					sequenceNo++;
				}

				while (!swin.isAllsent())
					;

				System.out.println("Successful... Size of sent file in bytes ---> " + Long.toString(fLen)
						+ "\nTime of sending in seconds ---> "
						+ Double.toString((double) (System.currentTimeMillis() - initTime) / 1000));

				inStrm.close();
				swin.stop();
				return true;

			} else {
				System.out.println("ERROR!!! ARQ Algorithm Mode is not valid, it should be 0 or 1");
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();

			return false;
		} finally {
			// socket.close();
		}

	}

	public class AsyncronizedSender

	{

		private int ackSeqNumber;
		boolean isExit = false;

		public AsyncronizedSender() {
			ackSeqNumber = -1;
		}

		public boolean asynSend(UDPSocket sockAdrs, byte[] packet, int packetSize, int seqNumber,
				InetSocketAddress rcvAdrs, long timeOut) {
			final UDPSocket connectedSocket2 = sockAdrs;

			ackSeqNumber = -1;
			isExit = true;
			Thread asynReceiver = new Thread(new Runnable() {
				public void run() {
					try {
						isExit = false;
						while (ackSeqNumber < seqNumber) {
							isExit = false;
							byte[] ackBuffer = new byte[4];
							DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
							connectedSocket2.receive(ackPacket);

							ackSeqNumber = (int) (ackBuffer[0] & 0x000000FF) + (int) (ackBuffer[1] & 0x000000FF) * 256
									+ (int) (ackBuffer[2] & 0x000000FF) * 65536
									+ (int) (ackBuffer[3] & 0x000000FF) * 16777216;

							System.out.println(
									"Ack is received, for sequence number: " + Integer.toString(ackSeqNumber));

						}

						isExit = true;

					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}

			});

			try {
				asynReceiver.start();
				long currentTime = System.currentTimeMillis();
				connectedSocket2.send(new DatagramPacket(packet, packetSize, rcvAdrs));

				System.out.println("Message Number ---> " + seqNumber + "\nfile size in byte --->\n"
						+ Integer.toString(packetSize - 8));
				System.out.println();
				while (ackSeqNumber < seqNumber) {
					if (((System.currentTimeMillis() - currentTime) > timeOut) && asynReceiver.isAlive()) {
						System.out.println("Transaction number ---> " + Integer.toString(seqNumber)
								+ "Timed-out!!! Resending the file...\n ");
						connectedSocket2.send(new DatagramPacket(packet, packetSize, rcvAdrs));

						System.out.println("Message Number ---> " + seqNumber + "\nfile size in byte --->\n"
								+ Integer.toString(packetSize - 8));

						currentTime = System.currentTimeMillis();

					}
					if (isExit)
						break;

				}
				asynReceiver.interrupt();
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}

			return true;
		}

	}

	public class SequenceWindow {
		public boolean isAcked;
		public int seqNumber;
		public long t;
		public int packLeng;

	}

	public class SlidingWindow {
		private byte[][] winPack;
		private SequenceWindow[] seqWindow;
		private int lPSndInd; // last packet sent index
		private int lAckRcvInd; // last Ack received index
		private int lastSequence;
		private int sws;
		private long pTimeOut;
		private boolean isblock;
		private boolean stopSender;
		private boolean stopReceiver;

		public SlidingWindow(long winSize, int maxPacketLength, long timedOutValue) {
			// ***** RISKY
			// maxPacketLength = (int)winSize-8;
			// *****

			sws = (int) winSize;
			pTimeOut = timedOutValue;
			lPSndInd = 0;
			lAckRcvInd = 0;
			seqWindow = new SequenceWindow[sws];
			for (int i = 0; i < sws; i++)
				seqWindow[i] = new SequenceWindow();
			winPack = new byte[sws][maxPacketLength];
			isblock = false;
			stopReceiver = false;
			stopSender = false;
		}

		public void stop() {
			stopSender = true;
			stopReceiver = true;
		}

		public boolean isAllsent() {
			boolean allSentFlag = true;
			for (int i = 0; i < lPSndInd; i++) {
				if ((seqWindow[i].isAcked == false) && (seqWindow[i].t != -1)) {
					allSentFlag = false;
				}

			}
			return allSentFlag;
		}

		public void winSenderManager(byte[] packt, int packetSize, int seqNumber) {
			try {

				seqWindow[lPSndInd].isAcked = false;
				seqWindow[lPSndInd].packLeng = packetSize;
				seqWindow[lPSndInd].seqNumber = seqNumber;
				seqWindow[lPSndInd].t = -1;
				for (int i = 0; i < packetSize; i++)
					winPack[lPSndInd][i] = packt[i];
				lastSequence = seqNumber;
				lPSndInd++;
				// until sliding window is not full
				while (sws - lPSndInd < 2) {

					int counter = 0;
					boolean interruptFlag = false;
					for (int i = 0; i < lPSndInd; i++) {
						if (seqWindow[i].isAcked == false)

						{
							interruptFlag = true;
						}

						if (seqWindow[i].isAcked == true && interruptFlag == false) {
							counter++;

						}
					}
					if (counter > 0) {

						isblock = true;
						for (int i = 0; i < lPSndInd; i++) {

							try {
								if ((counter + i) < lPSndInd) {
									seqWindow[i].isAcked = seqWindow[counter + i].isAcked;
									seqWindow[i].t = seqWindow[counter + i].t;
									seqWindow[i].packLeng = seqWindow[counter + i].packLeng;
									seqWindow[i].seqNumber = seqWindow[counter + i].seqNumber;

									for (int j = 0; j < seqWindow[i].packLeng; j++)
										winPack[i][j] = winPack[i + counter][j];
								} else {
									seqWindow[i].isAcked = false;
									seqWindow[i].t = -1;
									seqWindow[i].packLeng = -1;
									seqWindow[i].seqNumber = -1;
								}
							} catch (Exception ex) {
								System.out.println(
										"i ---> " + Integer.toString(i) + "/nCount ---> " + Integer.toString(counter));
							}
						}
						for (int i = lPSndInd - counter; i < lPSndInd; i++) {
							seqWindow[i].t = -1;
							seqWindow[i].isAcked = false;
							seqWindow[i].seqNumber = -1;
							seqWindow[i].packLeng = -1;
						}
						lPSndInd = lPSndInd - counter;
						lAckRcvInd = lAckRcvInd - counter;
						isblock = false;

					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();

			}

		}

		public void winSender(UDPSocket socketAdrs, InetSocketAddress rcvAdrs) {

			final UDPSocket senderSockett = socketAdrs;
			final InetSocketAddress rcvrAddr = rcvAdrs;
			try {
				senderSockett.send(new DatagramPacket(("Hello").getBytes(), 5, rcvrAddr));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Thread rSender = new Thread(new Runnable() {
				public void run() {
					while (!stopSender) {
						for (int i = 0; i < lPSndInd; i++) {

							if ((seqWindow[i].packLeng > 0) && (seqWindow[i].isAcked == false)
									&& !((seqWindow[i].t != -1)
											&& ((System.currentTimeMillis() - seqWindow[i].t) < pTimeOut))) {
								if ((seqWindow[i].t != -1)
										&& ((System.currentTimeMillis() - seqWindow[i].t) > pTimeOut))
									System.out.println("Transaction number ---> " + Integer.toString(seqWindow[i].seqNumber)
													+ "\nTimed-out!!! Resending the file... ");
								byte[] bufr = null;
								try {
									bufr = new byte[seqWindow[i].packLeng];
									for (int j = 0; j < seqWindow[i].packLeng; j++) {
										bufr[j] = winPack[i][j];
									}
								} catch (Exception ex) {
									System.out.println("***** ERROR!!! Tried to send Sequence Window [ "
											+ Integer.toString(i) + "] - Sequence Number = "
											+ Integer.toString(seqWindow[i].seqNumber) + "  Length= "
											+ seqWindow[i].packLeng + "\n\n");
									for (int k = 0; k < lPSndInd; k++) {
										System.out.println("****ERROR Sequence window [" + Integer.toString(k) + "]"
												+ Boolean.toString(seqWindow[k].isAcked) + "  For Sequence Number ---> "
												+ Integer.toString(seqWindow[k].seqNumber));
									}
									System.out.println("\n\n");
									continue;
								}

								try {
									senderSockett.send(new DatagramPacket(bufr, seqWindow[i].packLeng, rcvrAddr));
									System.out.println("Message number ---> " + seqWindow[i].seqNumber
											+ " \nFile size in byte ---> "
											+ Integer.toString(seqWindow[i].packLeng - 8));
									Thread.sleep(60);
									seqWindow[i].t = System.currentTimeMillis();

								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}

					}
				}
			});
			rSender.start();

		}

		public void receiver(UDPSocket newSocket) {
			final UDPSocket rcvrSocket = newSocket;

			Thread rReceiver = new Thread(new Runnable() {
				public void run() {
					try {
						byte[] ackBuffer = new byte[4];
						DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);

						while (!stopReceiver) {
							rcvrSocket.receive(ackPacket);

							int ackSeqNo = (int) (ackBuffer[0] & 0x000000FF) + (int) (ackBuffer[1] & 0x000000FF) * 256
									+ (int) (ackBuffer[2] & 0x000000FF) * 65536
									+ (int) (ackBuffer[3] & 0x000000FF) * 16777216;

							System.out.println(
									"Ack is received, for Sequence number:  " + Integer.toString(ackSeqNo));

							for (int i = 0; i < lPSndInd; i++) {
								if (seqWindow[i].seqNumber == ackSeqNo) {

									seqWindow[i].isAcked = true;
									if (i > lAckRcvInd)
										lAckRcvInd = i;
								}
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}

			});
			rReceiver.start();

		}

	}
}
