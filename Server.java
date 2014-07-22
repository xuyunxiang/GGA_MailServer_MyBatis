package gga.mailServerWithMyBatis;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
	private static class SingletonHolder {
		private static final Server instance = new Server();
	}

	private static Map<String, User> _usersByNames = 
			new HashMap<String, User>();																				
	private BlockingQueue<Queue<String>> _recievedCommands = 
			new LinkedBlockingQueue<Queue<String>>();

	private static final int PORT = 6666;
	private Object _monitorForRecived = new Object();
	private Object _monitorForOutgoing = new Object();
	
	private DBManagerWithMyBatis dbManager = DBManagerWithMyBatis.getInstance();

	/*
	 * This thread forms messages from received commands and put them to maps
	 * database.
	 */
	private class TaskManageMessages implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					Queue<String> q;
					synchronized (_monitorForRecived) {
						q = _recievedCommands.take();
					}
					if (q.isEmpty()) {
						if (Thread.currentThread().isInterrupted()) {
							break;
						}
						Thread.sleep(1000);
						continue;
					}
					String s = q.poll();
					if (!s.equals("MAIL From:")) {
						continue;
					}
					s = q.poll();
					if (s == null) {
						continue;
					}
					String from = new String(s);
					s = q.poll();
					if (s == null || !s.equals("RCPT To:")) {
						continue;
					}
					s = q.poll();
					if (s == null) {
						continue;
					}
					String to = new String(s);
					s = q.poll();
					if (s == null || !s.equals("DATA")) {
						continue;
					}
					s = q.poll();
					if (s == null) {
						continue;
					}
					String text = new String(s);

					if (!_usersByNames.containsKey(from)
							|| !_usersByNames.containsKey(to)) {
						continue;
					}

					User sender = _usersByNames.get(from);
					User reciever = _usersByNames.get(to);
					Message message = new Message(sender, reciever, text);
					synchronized (_monitorForOutgoing) {
						dbManager.saveMessage(message);
					}
				} catch (InterruptedException e) {
					break;
				}

			}
		}

	}

	/*
	 * This thread treat client: get command from channel and start specific
	 * thread to process it
	 */
	private class TaskTreatClient implements Runnable {

		private Socket socket;

		public TaskTreatClient(Socket aSocket) {
			socket = aSocket;
		}

		@Override
		public void run() {

			// Create thread for processing commands
			Thread threadManageMessages = new Thread(new TaskManageMessages());
			threadManageMessages.start();

			Scanner scanner = null;
			try {
				scanner = new Scanner(socket.getInputStream());
			} catch (IOException e1) {
				return;
			}

			while (scanner.hasNext()) {
				try {
					String command = scanner.nextLine();
					System.out.println("Server: command '" + command + "'");
					switch (command) {
					case "PUT":
						Queue<String> q = saveCommandsFromInputStream(scanner);
						_recievedCommands.add(q);
						break;
					case "GET":
						if (scanner.hasNextLine()) {
							try {
								String username = scanner.nextLine();
								sendMessagesToUser(username);
							} catch (NoSuchElementException e) {
								break;
							}
						}
						break;
					default:
						throw new InvalidCommandException();
					}
				} catch (NoSuchElementException | InvalidCommandException e) {
					scanner.close();
					return;
				}
			}
			threadManageMessages.interrupt();
		}

		private void sendMessagesToUser(String username) {
			Queue<Message> queue;
			while (true) {
				synchronized (_monitorForOutgoing) {
					queue = dbManager.getMessages(username);
				}
				PrintWriter pw;
				try {
					pw = new PrintWriter(socket.getOutputStream());
				} catch (IOException e1) {
					e1.printStackTrace();
					break;
				}
				if (queue == null || queue.isEmpty()) {
					try {
						pw = new PrintWriter(socket.getOutputStream());
						pw.println("EMPTY");
						pw.flush();
						pw.println("END");
						pw.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
				for (Message msg : queue) {
					pw.println("PUT");
					pw.flush();
					pw.println("MAIL From:");
					pw.println(msg.getFromText());
					pw.flush();
					pw.println("RCPT To:");
					pw.println(msg.getToText());
					pw.flush();
					pw.println("DATA");
					pw.flush();
					pw.println(msg.getText());
					pw.flush();
					pw.println(".");
					pw.flush();
					pw.println("QUIT");
					pw.flush();
				}
				pw.println("END");
				pw.flush();
				pw.close();
				break;
			}
		}

		private Queue<String> saveCommandsFromInputStream(Scanner scanner) {
			
			Queue<String> strings = new LinkedList<>();
			while (scanner.hasNextLine()) {
				try {
					String command = scanner.nextLine();
					strings.add(command);
					if (command.equals("QUIT")) {
						break;
					}
				} catch (NoSuchElementException e) {
					break;
				}
			}

			return strings;
		}

	}

	private class TaskListenSocket implements Runnable {

		Queue<Thread> q = new LinkedList<>();

		@Override
		public void run() {

			ServerSocket serverSocket = null;

			try {
				serverSocket = new ServerSocket(PORT);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			System.out.println("Server init");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}

			Socket socket = null;
			while (true) {
				try {
					serverSocket.setSoTimeout(10000);
					try {
						socket = serverSocket.accept();
					} catch (SocketTimeoutException ex) {
						for (Thread thread : q) { // wait for all working child threads
							try {
								thread.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						socket.close();
						break;
					}

					// create thread for treating client and save it in queues
					Thread t = new Thread(new TaskTreatClient(socket));
					q.add(t);
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("Server end");
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	public void startServer() {
		if (!dbManager.connectToDB()) {
			System.out.println("Error in connecting to DB");
			return;
		}
		new Thread(new TaskListenSocket()).start();
	}

	public static Server getInstance() {
		return SingletonHolder.instance;
	}

	private Server() {
	}

	public static void setUsersByNames(Map<String, User> _usersByNames) {
		Server._usersByNames = _usersByNames;
	}
}
