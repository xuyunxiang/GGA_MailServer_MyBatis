package gga.mailServerWithMyBatis;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MessageMapper {

	@Insert("insert into message values(#{from}, #{to}, #{text})")
	void saveMessage(@Param("from") String from, @Param("to") String to,
			@Param("text") String text);
	
	@Select("select sender, receiver, text from "
			+ "message where ( receiver=#{nameOfReceiver} )")
	List<MessageBean> getMessagesFor(@Param("nameOfReceiver") String name);
}
