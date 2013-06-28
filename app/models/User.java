package models;


import javax.persistence.Entity;
import javax.persistence.Id;
import play.db.ebean.Model;
import java.lang.Long;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;



@Entity
public class User extends Model {

    @Id
    public Long id;

    public String username;
    public String password;
    public String sources;
    public String urls;


    public static Finder<Long,User> find = new Finder<>(
            Long.class, User.class
        );
		
	public static Model.Finder<String,User> findUser = new Finder<>(
			String.class, User.class
		);


    public User(String username, String password, String sources, String urls) {
        this.username = username;
        this.password = password;
        this.sources = sources;
        this.urls = urls;
    }

    public static void create(User user){
        user.save();
    }

    public static void delete(Long id){
        find.ref(id).delete();
    }

    public static List<User> getAllUsers(){
        return find.all();
    }

    public static boolean uniqueUser(String username){
        List<User> users = User.getAllUsers();
        for(User user: users){
            if(user.username.contentEquals(username)){
                return false;
            }
        }
        return true;
    }
	
	public static User authenticate(String username, String password){
		return findUser.where().eq("username", username).eq("password", password).findUnique();
	}

    public static User getUser(String username){
        return findUser.where().eq("username",username).findUnique();
    }


}