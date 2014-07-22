package gga.mailServerWithMyBatis;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class User implements Runnable {
	
	public static final int PORT = 6666;
	public static final String ADDRESS = "127.0.0.1";
	
	public static final Object _monitor = new Object();
	
	private BlockingQueue<Message> _recievedMessages = new LinkedBlockingQueue<Message>();
	private BlockingQueue<Message> _sentMessages = new LinkedBlockingQueue<Message>();
		
	private String _name;
	
	public User(String name) {
		_name = name;
	}
	
	public String getName() {
		return _name;
	}
	
	@Override
	public void run() {
		
		Socket socket = connectToServer();
		if (socket == null) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return;
		}
		
		System.out.println("User " + _name + " " + Thread.currentThread().getName() + 
				" connnected to server");
		PrintWriter pw;
		Scanner scanner;
		try {
			pw = new PrintWriter(socket.getOutputStream());
			scanner = new Scanner(socket.getInputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		for (int j = 0; j < 2; j++) {
			
			Message message = createMessage();
			sendMessage(pw, message);	
			_sentMessages.add(message);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("---Sent--");
		printMessages(_sentMessages);
		
		System.out.println("User " + Thread.currentThread().getName() + 
				" sent all his messages");
		
		Queue<String> strings = new LinkedList<>();	
		getMessages(socket, pw, scanner, strings, this.getName());
		parseStrings(strings);
		System.out.println("---Recieved--");
		printMessages(_recievedMessages);
		
		System.out.println("User " + Thread.currentThread().getName() + " end");
	}

	private void printMessages(Queue<Message> q) {
		int i = 1;
		System.out.println("User " + Thread.currentThread().getName() + " messages");
		System.out.println("-------");
		for (Message msg : q) {
			System.out.print(i + ". ");
			System.out.println(msg);
			++i;
		}
		System.out.println("-------");
	}	
	
	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 1; i <= _name.length(); i++) {
			hash += i*_name.charAt(i-1);
		}
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		return  ( (obj instanceof User) 
				&& _name.equals( ((User)obj).getName() ) );
	}

	private  void getMessages(Socket socket, PrintWriter pw, Scanner scanner,
			Queue<String> strings, String name) {
				pw.println("GET");
				pw.flush();
				pw.println(name);
				pw.flush();
				
				while (true) {
					try {
						String command = scanner.nextLine();
						strings.add(command);
						if (command.equals("END")) {
							break;
						}
					}
					catch (Exception e) {
						break;
					}
				}
				
				try {
					scanner.close();
		            pw.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	}

	private void parseStrings(Queue<String> strings) {
		while (!strings.isEmpty()) {
			String s = strings.poll();
			if (s.equals("EMPTY")) {
				break;
			}
			if (s.equals("PUT")) {
				s = strings.poll();
				if (!s.equals("MAIL From:")) {
					break;
				}
				s = strings.poll();
				if (s == null) {
					break;
				}
				String from = new String(s);
				s = strings.poll();
				if (s == null || !s.equals("RCPT To:")) {
					break;
				}
				s = strings.poll();
				if (s == null) {
					break;
				}
				String to = new String(s);
				s = strings.poll();
				if (s == null || !s.equals("DATA")) {
					break;
				}
				s = strings.poll();
				if (s == null) {
					break;
				}
				String text = new String(s);
				
				Message msg = new Message(new User(from), 
						new User(to), text);
				try {
					_recievedMessages.put(msg);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void sendMessage(PrintWriter pw, Message message) {
				
				pw.println("PUT");
				pw.flush();
				pw.println("MAIL From:");
				pw.println(message.getFromText());
				pw.flush();
				pw.println("RCPT To:");
				pw.println(message.getToText());
				pw.flush();
		        pw.println("DATA");
		        pw.flush();
		        pw.println(message.getText());
		        pw.flush();
		        pw.println(".");
		        pw.flush();
		        pw.println("QUIT");
		        pw.flush();
		        
		        try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	}

	private Socket connectToServer() {
		InetAddress inetAddress;
		Socket _socket;

		try {
			inetAddress = InetAddress.getByName(ADDRESS);
			_socket = new Socket(inetAddress, PORT); 
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return null;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return _socket;
	}
	
	private Message createMessage() {
		Collection<User> _users = Initializer.getUsers();
		int index = Initializer.getRandom(_users.size());
		User u = (User) _users.toArray()[index];
		Message message =
				new Message(this, u, "blabla");
		return message;
	}
	
	@Override
	public String toString() {
		return "User " + _name;
	}
	
}
