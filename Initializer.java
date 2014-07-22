package gga.mailServerWithMyBatis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Initializer {
	private static Random _random = new Random();

	private static Map<String, User> _usersByNames = 
			new HashMap<String, User>();
	private static char[] ALPHABET = new char[26];
	static {
		for (int i = 0; i < 26; i++) {
			ALPHABET[i] = (char)('a'+i);
		}
	}
	private static int[] NUMBERS = new int[10];
	static {
		for (int i = 0; i < 10; i++) {
			NUMBERS[i] = i;
		}
	}
	private static Random _randomCretorOfUsers = new Random();
	
	public static void init(String[] args) {

		if (args.length != 1) {
			System.out.println("Error: invalid number of parameters!");
		}
		
		Integer numOfUsers = null;
		try {
			numOfUsers = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException e) {
			System.out.println("Error: Invalid format of number of users!");
			return;
		}
		
		DBManagerWithMyBatis.getInstance().connectToDB();
		
		createUsers(numOfUsers);
		for (String name : _usersByNames.keySet()) {			
			DBManagerWithMyBatis.getInstance().addUser(name);
		}
		Server.setUsersByNames(_usersByNames);
		
		Server.getInstance().startServer();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (User user : _usersByNames.values()) {
			new Thread(user).start();
		}
	}

	private static void createUsers(Integer numOfUsers) {
		for (int i = 0; i < numOfUsers; i++) {
			StringBuilder name = new StringBuilder();
			int letter = 0;
			for (int j = 0; j < 5; j++) {
				letter = _randomCretorOfUsers.nextInt(ALPHABET.length);
				name.append(ALPHABET[letter]);
			}
			name.append("@");
			for (int j = 0; j < 3; j++) {
				letter = _randomCretorOfUsers.nextInt(ALPHABET.length);
				name.append(ALPHABET[letter]);
			}
			name.append(".");
			for (int j = 0; j < 2; j++) {
				letter = _randomCretorOfUsers.nextInt(ALPHABET.length);
				name.append(ALPHABET[letter]);
			}
			_usersByNames.put(name.toString(), new User(name.toString()));
		}
	}
	
	public static Collection<User> getUsers() {
		return _usersByNames.values();
	}
	
	public static synchronized int getRandom(int i) {
		return _random.nextInt(i);
	}
	
}
