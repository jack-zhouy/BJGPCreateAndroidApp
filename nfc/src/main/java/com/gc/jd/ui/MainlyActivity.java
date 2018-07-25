package com.gc.jd.ui;

import com.gc.jd.R;
import com.gc.jd.app.AppContext;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;
import android.os.StrictMode;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import android.view.KeyEvent;

public class MainlyActivity extends TabActivity implements OnClickListener {

	private TabHost host;
	private final static String GPCREATESTRING = "GPCREATE_STRING";//钢瓶建档

	private ImageView img_gp_create;
	private TextView  text_gp_create;
	private LinearLayout linearlayout_gp_create;


	// 用来计算返回键的点击间隔时间
	private long exitTime = 0;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		setContentView(R.layout.activity_mainly);
		this.getScreenDisplay();

		this.initView();
		host = getTabHost();
		host.setup();
		setValidOrdersTab();
		host.setCurrentTabByTag(GPCREATESTRING);//默认我的界面
	}

	public void initView(){
		img_gp_create=(ImageView) findViewById(R.id.img_vaildorders);
		img_gp_create.setOnClickListener(this);
		text_gp_create=(TextView) findViewById(R.id.text_vaildorders);
		linearlayout_gp_create=(LinearLayout) findViewById(R.id.linearlayout_vaildorders);
		linearlayout_gp_create.setOnClickListener(this);

	}

	private void setValidOrdersTab() {
		TabSpec tabSpec = host.newTabSpec(GPCREATESTRING);
		tabSpec.setIndicator(GPCREATESTRING);
		Intent intent = new Intent(MainlyActivity.this,GPCreateActivity.class);
		tabSpec.setContent(intent);
		host.addTab(tabSpec);
	}


	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.linearlayout_vaildorders:
			case R.id.img_vaildorders:
				host.setCurrentTabByTag(GPCREATESTRING);
				img_gp_create.setBackgroundResource(R.drawable.ic_menu_deal_on);
				text_gp_create.setTextColor(getResources().getColor(R.color.green));
				break;
			default:
				break;
		}
	}

	private void getScreenDisplay(){
		Display display=this.getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight=display.getHeight();

		AppContext appContext=(AppContext) getApplicationContext();
		appContext.setScreenWidth(screenWidth);
		appContext.setScreenHeight(screenHeight);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// RESULT_OK，判断另外一个activity已经结束数据输入功能，Standard activity result:
		// operation succeeded. 默认值是-1
		if (requestCode == 0) {
		}
	}



	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 &&event.getAction() == KeyEvent.ACTION_DOWN)        {
			new AlertDialog.Builder(MainlyActivity.this).setTitle("提示")
					.setMessage("确认退出吗？")
					.setIcon(R.drawable.icon_logo)
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog,
													int which)
								{
									android.os.Process.killProcess(android.os.Process.myPid()); // 结束进程
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog,
													int which)
								{

								}
							})
					.show();
			return false;
		}
		else
		{
			return super.dispatchKeyEvent(event);
		}
	}

	public void exit() {
		if ((System.currentTimeMillis() - exitTime) > 2000) {
			Toast.makeText(getApplicationContext(), "再按一次退出程序",
					Toast.LENGTH_SHORT).show();
			exitTime = System.currentTimeMillis();
		} else {
			finish();
			System.exit(0);
		}
	}

}
