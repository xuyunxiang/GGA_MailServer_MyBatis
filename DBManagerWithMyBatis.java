package gga.mailServerWithMyBatis;


import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class DBManagerWithMyBatis {


	private static class SingletonHolder {
		private static final DBManagerWithMyBatis instance = new DBManagerWithMyBatis();
	}
	private DBManagerWithMyBatis() {
	}
	public static DBManagerWithMyBatis getInstance() {
		return SingletonHolder.instance;
	}
	
	private static 	SqlSessionFactory sessionFactory;
	
	public boolean connectToDB() {
		try {
			InputStream inputStream = 
					org.apache.ibatis.io.Resources.getResourceAsStream("config.xml");
			SqlSessionFactory factory = 
					new SqlSessionFactoryBuilder().build(inputStream);
			factory.getConfiguration().addMapper(UserMapper.class);
			factory.getConfiguration().addMapper(MessageMapper.class);
			sessionFactory = factory;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public void saveMessage(Message message) {
		SqlSession session = sessionFactory.openSession();
		
		MessageMapper mapper = session.getMapper(MessageMapper.class);
		
		mapper.saveMessage(message.getFromText(), message.getToText(), 
				message.getText());
		session.commit();
		session.close();
	}
	
	public Queue<Message> getMessages(String username) {
		Queue<Message> q = new LinkedList<>();
		
		SqlSession session = sessionFactory.openSession();
		
		MessageMapper mapper = session.getMapper(MessageMapper.class);
		List<MessageBean> messages = mapper.getMessagesFor(username);
		
		session.commit();
		session.close();
		
		for (MessageBean bean : messages) {
			q.add(new Message(new User(bean.getSender()), 
					new User(bean.getReceiver()), 
					bean.getText()));
		}
		
		return q;
	}
	
	public void addUser(String user) {
		SqlSession session = sessionFactory.openSession();
		
		UserMapper mapper = session.getMapper(UserMapper.class);
		mapper.addNewUser(user);
		session.commit();
		
		session.close();
	}

}
