package gga.mailServerWithMyBatis;

import org.apache.ibatis.annotations.Insert;

public interface UserMapper {

	@Insert("insert into usr(name) values(#{param1})")
	void addNewUser(String name);
	
}
