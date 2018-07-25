package com.gc.jd.app;




import com.gc.jd.domain.User;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;



public class AppContext extends Application {
	private User user;
	private String groupCode;
	private String groupName;
	private int screenWidth;
	private int screenHeight;
	private SharedPreferences preferences;

	public String getGroupCode() {
		return groupCode;
	}

	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public void setPreferences(SharedPreferences preferences) {
		this.preferences = preferences;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}



	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
		Editor editor = preferences.edit();
		editor.putString("username", user.getUsername());
		editor.putString("password", user.getPassword());
		editor.commit();
	}

}
