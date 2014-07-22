package gga.mailServerWithMyBatis;

public class Message {

	private User _from;
	private User _to;
	private String _text;
	
	public Message(User from, User to, String text) {
		_from = from;
		_to = to;
		_text = text;
	}

	public User getFrom() {
		return _from;
	}
	public User getTo() {
		return _to;
	}
	public String getFromText() {
		return _from.getName();
	}
	public String getToText() {
		return _to.getName();
	}
	public String getText() {
		return _text;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Message)) {
			return false;
		}
		Message msg = (Message)obj;
		return ( _from.equals(msg.getFrom()) &&
				_to.equals(msg.getTo()) 
						&& _text.equals(msg.getText()) );
	}
	
	@Override
	public String toString() {
		return "From: " + _from + ", \n To: " +
				_to + ", \n Text: "
						+ _text;
	}
	
}
