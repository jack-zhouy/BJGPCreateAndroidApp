package com.gc.jd.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.ColorRes;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.LinearLayout;

import com.dk.bleNfc.BleManager.BleManager;
import com.dk.bleNfc.BleManager.Scanner;
import com.dk.bleNfc.BleManager.ScannerCallback;
import com.dk.bleNfc.BleNfcDeviceService;
import com.dk.bleNfc.DeviceManager.BleNfcDevice;
import com.dk.bleNfc.DeviceManager.ComByteManager;
import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.DeviceManager.DeviceManagerCallback;
import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;
import com.dk.bleNfc.Tool.StringTool;
import com.dk.bleNfc.card.Mifare;
import com.dk.bleNfc.card.Ntag21x;
import com.gc.jd.R;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import android.widget.DatePicker;
import java.util.Calendar;
import com.gc.jd.common.NetRequestConstant;
import com.gc.jd.common.NetUrlConstant;
import java.util.HashMap;
import java.util.Map;
import com.gc.jd.interfaces.Netcallback;
import com.gc.jd.utils.SharedPreferencesHelper;
import android.util.DisplayMetrics;
import org.json.JSONArray;
import org.apache.http.util.EntityUtils;
import android.widget.ArrayAdapter;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.LinearLayout.LayoutParams;
import android.view.Gravity;
import android.view.WindowManager;


public class GPCreateActivity extends BaseActivity implements OnClickListener, DatePicker.OnDateChangedListener  {
	BleNfcDeviceService mBleNfcDeviceService;//蓝牙设备服务
	private BleNfcDevice bleNfcDevice;//蓝牙设备
	private Scanner mScanner;
	private ImageView m_imageViewSearchBlue = null;
	private EditText msgText = null;
	private ProgressDialog readWriteDialog = null;

	private StringBuffer msgBuffer;
	private BluetoothDevice mNearestBle = null;
	private Lock mNearestBleLock = new ReentrantLock();// 锁对象
	private int lastRssi = -100;
	//=========================
	private Button m_buttonNext;//下一步

	//	private LinearLayout m_llDate_scrq;//生产日期
//	private LinearLayout m_llDate_scgjrq;//上次钢检日期
//	private LinearLayout m_llDate_xcgjrq;//下次钢检日期
//	private LinearLayout m_llDate_bfrq;//报废日期
	private String m_userId;//当前用户ID

	private ImageView m_imageViewScrq;//生产日期
	private ImageView m_imageViewScgjrq;//上次钢检日期

	private EditText m_editText_scrq;//生产日期
	private EditText m_editText_scgj;//上次钢检日期
	private EditText m_editText_xcgj;//下次钢检日期
	private EditText m_editText_bfrq;//报废日期

	private EditText m_editText_gpbm;//钢瓶号
	private EditText m_editText_ccbm;//出厂编码
	private EditText m_editText_readbq;//读取标签



	private Spinner m_spinnerGPtype; //钢瓶规格

	private Spinner m_spinnerFactory; //钢瓶厂家
	private Spinner m_spinnerGPHead; //钢瓶编码头




	private String m_gp_spec_code;//钢瓶规格code
	private String m_gp_code_head;//钢瓶编码头



	private JSONArray m_factorysArrayJson;//所有生产厂家

	private String m_gp_factory_code;//选择的生产厂家code
	private StringBuffer m_date_scrq, m_date_scgj, m_date_xcgj, m_date_bfrq;
	private int year, month, day, hour, minute;//日期控件临时变量

	private Toast toast = null;
	TextView tv;//toast--view

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {

		setContentView(R.layout.activity_gp_create);

		//控件初始化
		m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
		m_imageViewScrq  = (ImageView) findViewById(R.id.imageView_scrq);//生产日期
		m_imageViewScgjrq  = (ImageView) findViewById(R.id.imageView_scgj);//上次钢检日期

		m_editText_scrq  = (EditText) findViewById(R.id.editText_ccrq);//生产日期
		m_editText_scgj  = (EditText) findViewById(R.id.editText_scgj);//上次钢检日期
		m_editText_xcgj  = (EditText) findViewById(R.id.editText_xcgj);//下次钢检日期
		m_editText_bfrq  = (EditText) findViewById(R.id.editText_bfrq);//报废日期
		m_editText_gpbm = (EditText) findViewById(R.id.editText_gpbm);//钢瓶号
		m_editText_ccbm = (EditText) findViewById(R.id.editText_ccbm);//出厂编码
		m_editText_readbq = (EditText) findViewById(R.id.editText_readbq);//读取标签
		m_spinnerGPtype = (Spinner) findViewById(R.id.spinner_payType);//钢瓶号头
		m_spinnerFactory = (Spinner) findViewById(R.id.spinner_factory);//生产厂家

		m_spinnerGPHead = (Spinner) findViewById(R.id.spinner_gp_head);//钢瓶编码头





		m_buttonNext.setOnClickListener(this);
		m_imageViewScrq.setOnClickListener(this);
		m_imageViewScgjrq.setOnClickListener(this);

		//获取当前用户ID
		m_userId = (String)SharedPreferencesHelper.get("username", "default");


		//日期edit变化事件
		m_editText_scrq.addTextChangedListener(new TextWatcher(){

			public void afterTextChanged(Editable s) {
				String scrqStr = m_editText_scrq.getText().toString();
				if(scrqStr.length()==8){
					String bfrqStr = DateAddByYear(scrqStr, 15);
					if(bfrqStr==null){
						Toast.makeText(GPCreateActivity.this, "非法日期格式", Toast.LENGTH_LONG).show();
						return;
					}
					m_editText_bfrq.setText(bfrqStr);
				}

			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

		});

		//日期edit变化事件
		m_editText_scgj.addTextChangedListener(new TextWatcher(){

			public void afterTextChanged(Editable s) {
				String scgjStr = m_editText_scgj.getText().toString();
				if(scgjStr.length()==8) {
					String xcgjStr = DateAddByYear(scgjStr, 4);
					if(xcgjStr==null){
						Toast.makeText(GPCreateActivity.this, "非法日期格式", Toast.LENGTH_LONG).show();
						return;
					}
					m_editText_xcgj.setText(xcgjStr);
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

		});



		m_date_scrq = new StringBuffer();
		m_date_scgj = new StringBuffer();
		m_date_xcgj = new StringBuffer();
		m_date_bfrq = new StringBuffer();
		initDateTime();

		//获取生产厂家
		getGasCynFactorys();

		//蓝牙设备初始化
		blueDeviceInitial();

		//钢瓶规格选择控件初始化
		m_spinnerGPtype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) {
				switch(pos){
					case 0://5公斤
						m_gp_spec_code = "0001";
						break;
					case 1://15公斤
						m_gp_spec_code = "0002";
						break;
					case 2://50公斤
						m_gp_spec_code = "0003";
						break;
					default:
						break;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}
		});

		//生产厂家选择控件初始化
		m_spinnerFactory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) {
				try {
					JSONObject factoryJson = m_factorysArrayJson.getJSONObject(pos);
					m_gp_factory_code = factoryJson.get("code").toString();
				}catch (JSONException e){

				}

			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}
		});

		m_spinnerGPHead.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) {

				switch(pos){
					case 0:
						m_gp_code_head = "KMA2B";
						break;
					case 1:
						m_gp_code_head = "KMA";
						break;
					default:
						break;
				}


			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}
		});
		final  Handler mHandler = new Handler();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				//do something
				sysUserKeepAlive();//心跳信息-1分钟1次
				mHandler.postDelayed(this, 60000);
			}
		};
		mHandler.postDelayed(r, 100);//延时100毫秒

	}



	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				Message message = new Message();
				message.what = 0x99;
				handlerRestApi.sendMessage(message);
				break;
			case R.id.imageView_scrq:
				createDatePicker(m_editText_scrq, v.getId());
				break;
			case R.id.imageView_scgj:
				createDatePicker(m_editText_scgj, v.getId());
				break;
			default:
				break;
		}

	}



	private String getResponseMessage(HttpResponse response) {
		try {

			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String responseBody = sb.toString();

			if (responseBody.equals("")) {
				responseBody = "{\"message\":\"no value\"}";
			}

			JSONObject errorDetailJson = new JSONObject(responseBody);
			String errorDetail = errorDetailJson.get("message").toString();
			return errorDetail;
		} catch (IOException e) {
			Toast.makeText(GPCreateActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(GPCreateActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		}
	}
	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			BleNfcDeviceService mBleNfcDeviceService = ((BleNfcDeviceService.LocalBinder) service).getService();
			bleNfcDevice = mBleNfcDeviceService.bleNfcDevice;
			mScanner = mBleNfcDeviceService.scanner;
			mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
			mBleNfcDeviceService.setScannerCallback(scannerCallback);
			//开始搜索设备
			searchNearestBleDevice();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBleNfcDeviceService = null;
		}
	};
	private void blueDeviceInitial(){
		msgText = (EditText)findViewById(R.id.msgText);
		m_imageViewSearchBlue= (ImageView) findViewById(R.id.imageView_search);
		m_imageViewSearchBlue.setOnClickListener(new StartSearchButtonListener());
		msgBuffer = new StringBuffer();
		//ble_nfc服务初始化
		Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}


	@Override
	protected void onResume() {
		super.onResume();
		if (mBleNfcDeviceService != null) {
			mBleNfcDeviceService.setScannerCallback(scannerCallback);
			mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
		}
	}
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (readWriteDialog != null) {
			readWriteDialog.dismiss();
		}

		unbindService(mServiceConnection);
	}
	//Scanner 回调
	private ScannerCallback scannerCallback = new ScannerCallback() {
		@Override
		public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
			super.onReceiveScanDevice(device, rssi, scanRecord);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //StringTool.byteHexToSting(scanRecord.getBytes())
				System.out.println("Activity搜到设备：" + device.getName()
						+ " 信号强度：" + rssi
						+ " scanRecord：" + StringTool.byteHexToSting(scanRecord));
			}

			//搜索蓝牙设备并记录信号强度最强的设备
			if ( (scanRecord != null) && (StringTool.byteHexToSting(scanRecord).contains("017f5450"))) {  //从广播数据中过滤掉其它蓝牙设备
				if (rssi < -55) {
					return;
				}
				//msgBuffer.append("搜到设备：").append(device.getName()).append(" 信号强度：").append(rssi).append("\r\n");
				handler.sendEmptyMessage(0);
				if (mNearestBle != null) {
					if (rssi > lastRssi) {
						mNearestBleLock.lock();
						try {
							mNearestBle = device;
						}finally {
							mNearestBleLock.unlock();
						}
					}
				}
				else {
					mNearestBleLock.lock();
					try {
						mNearestBle = device;
					}finally {
						mNearestBleLock.unlock();
					}
					lastRssi = rssi;
				}
			}
		}

		@Override
		public void onScanDeviceStopped() {
			super.onScanDeviceStopped();
		}
	};

	//设备操作类回调
	private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback() {
		@Override
		public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
			super.onReceiveConnectBtDevice(blnIsConnectSuc);
			if (blnIsConnectSuc) {
				System.out.println("Activity设备连接成功");
				msgBuffer.delete(0, msgBuffer.length());
				msgBuffer.append("设备连接成功!");
				if (mNearestBle != null) {
					//msgBuffer.append("设备名称：").append(bleNfcDevice.getDeviceName()).append("\r\n");
				}
				//msgBuffer.append("信号强度：").append(lastRssi).append("dB\r\n");
				//msgBuffer.append("SDK版本：" + BleNfcDevice.SDK_VERSIONS + "\r\n");

				//连接上后延时500ms后再开始发指令
				try {
					Thread.sleep(500L);
					handler.sendEmptyMessage(3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
			super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
			System.out.println("Activity设备断开链接");
			msgBuffer.delete(0, msgBuffer.length());
			msgBuffer.append("设备断开链接!");
			handler.sendEmptyMessage(0);
		}

		@Override
		public void onReceiveConnectionStatus(boolean blnIsConnection) {
			super.onReceiveConnectionStatus(blnIsConnection);
			System.out.println("Activity设备链接状态回调");
		}

		@Override
		public void onReceiveInitCiphy(boolean blnIsInitSuc) {
			super.onReceiveInitCiphy(blnIsInitSuc);
		}

		@Override
		public void onReceiveDeviceAuth(byte[] authData) {
			super.onReceiveDeviceAuth(authData);
		}

		@Override
		//寻到卡片回调
		public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
			super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
			if (!blnIsSus || cardType == BleNfcDevice.CARD_TYPE_NO_DEFINE) {
				return;
			}
			System.out.println("Activity接收到激活卡片回调：UID->" + StringTool.byteHexToSting(bytCardSn) + " ATS->" + StringTool.byteHexToSting(bytCarATS));

			final int cardTypeTemp = cardType;
			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean isReadWriteCardSuc;
					try {
						if (bleNfcDevice.isAutoSearchCard()) {
							//如果是自动寻卡的，寻到卡后，先关闭自动寻卡
							bleNfcDevice.stoptAutoSearchCard();
							isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

							//读卡结束，重新打开自动寻卡
							startAutoSearchCard();
						}
						else {
							isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

							//如果不是自动寻卡，读卡结束,关闭天线
							bleNfcDevice.closeRf();
						}

						//打开蜂鸣器提示读卡完成
						if (isReadWriteCardSuc) {
							bleNfcDevice.openBeep(50, 50, 3);  //读写卡成功快响3声
						}
						else {
							bleNfcDevice.openBeep(100, 100, 2); //读写卡失败慢响2声
						}
					} catch (DeviceNoResponseException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}

		@Override
		public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
			super.onReceiveRfmSentApduCmd(bytApduRtnData);
			System.out.println("Activity接收到APDU回调：" + StringTool.byteHexToSting(bytApduRtnData));
		}

		@Override
		public void onReceiveRfmClose(boolean blnIsCloseSuc) {
			super.onReceiveRfmClose(blnIsCloseSuc);
		}
		@Override
		//按键返回回调
		public void onReceiveButtonEnter(byte keyValue) {}
	};
	//读写卡Demo
	private boolean readWriteCardDemo(int cardType) {
		switch (cardType) {
			case DeviceManager.CARD_TYPE_ULTRALIGHT: //寻到Ultralight卡
				final Ntag21x ntag21x = (Ntag21x) bleNfcDevice.getCard();
				if (ntag21x != null) {
					try {

						String gmpStr = m_editText_gpbm.getText().toString();
						String writebq = m_gp_code_head+m_editText_gpbm.getText().toString();
						if(gmpStr.length()==0){
							handler.sendEmptyMessage(0x58);
							return false;
						}
						if(ntag21x.NdefTextWrite(writebq)){
							if(ntag21x.NdefTextLockFirstBlock()){//锁死标签
							}else{
								handler.sendEmptyMessage(0x59);//锁死标签失败
							}

							String readbq = ntag21x.NdefTextRead();
							Message msg = new Message();
							msg.obj = readbq;
							msg.what = 0x55;//标签读取更新控件
							handler.sendMessage(msg);
						}else{
							handler.sendEmptyMessage(0x56);

						}


						//ntag21x.NdefTextWrite("KMA123456789");

//						Message msg = new Message();
//						msg.obj = readbq;
//						msg.what = 0x88;//标签读取事件
//						handler.sendMessage(msg);
					} catch (CardNoResponseException e) {
						e.printStackTrace();
						return false;
					}
				}
				break;
			case DeviceManager.CARD_TYPE_MIFARE:   //寻到Mifare卡
				final Mifare mifare = (Mifare) bleNfcDevice.getCard();
				if (mifare != null) {
//					msgBuffer.delete(0, msgBuffer.length());
//					msgBuffer.append("寻到Mifare卡->UID:").append(mifare.uidToString()).append("\r\n");
//					msgBuffer.append("开始验证第3块密码\r\n");
					handler.sendEmptyMessage(0);
					byte[] key = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
					try {
						boolean anth = mifare.authenticate((byte) 0x10, Mifare.MIFARE_KEY_TYPE_B, key);
						if (anth) {
							byte[] readDataBytes = mifare.read((byte) 0x10);
							msgBuffer.append("块16数据:").append(StringTool.byteHexToSting(readDataBytes)).append("\r\n");
							handler.sendEmptyMessage(0);

							readDataBytes = mifare.read((byte) 0x11);
							msgBuffer.append("块17数据:").append(StringTool.byteHexToSting(readDataBytes)).append("\r\n");
							handler.sendEmptyMessage(0);

//                            msgBuffer.append("验证密码成功\r\n");
//                            msgBuffer.append("写FFFFFFFFFFFF7F078869FFFFFFFFFFFF到块1\r\n");
//                            handler.sendEmptyMessage(0);
////                            boolean isSuc = mifare.write((byte)7, new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
////                                    (byte)0x7F, (byte)0x07, (byte)0x88, (byte) 0x69, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
//                            boolean isSuc = mifare.write((byte)3, new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
//                                    (byte)0xFF, (byte)0x07, (byte)0x80, (byte) 0x69, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
//
//                            if (isSuc) {
//                                msgBuffer.append("写成功！\r\n");
//                                msgBuffer.append("读块3数据\r\n");
//                                handler.sendEmptyMessage(0);
//                                byte[] readDataBytes = mifare.read((byte) 3);
//                                msgBuffer.append("块1数据:").append(StringTool.byteHexToSting(readDataBytes)).append("\r\n");
//                                handler.sendEmptyMessage(0);
//                            } else {
//                                msgBuffer.append("写失败！\r\n");
//                                handler.sendEmptyMessage(0);
//                                return false;
//                            }
						}
						else {
							msgBuffer.append("验证密码失败\r\n");
							handler.sendEmptyMessage(0);
							return false;
						}
					} catch (CardNoResponseException e) {
						e.printStackTrace();
						return false;
					}
				}
				break;
			default:
				break;
		}
		return true;
	}

	//搜索最近的设备并连接
	private void searchNearestBleDevice() {
		msgBuffer.delete(0, msgBuffer.length());
		msgBuffer.append("正在搜索设备...");
		handler.sendEmptyMessage(0);
		if (!mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized (this) {
						mScanner.startScan(0);
						mNearestBleLock.lock();
						try {
							mNearestBle = null;
						}finally {
							mNearestBleLock.unlock();
						}
						lastRssi = -100;

						int searchCnt = 0;
						while ((mNearestBle == null)
								&& (searchCnt < 20000)
								&& (mScanner.isScanning())
								&& (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
							searchCnt++;
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						if (mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							mScanner.stopScan();
							mNearestBleLock.lock();
							try {
								if (mNearestBle != null) {
									mScanner.stopScan();
									msgBuffer.delete(0, msgBuffer.length());
									msgBuffer.append("正在连接设备...");
									handler.sendEmptyMessage(0);
									bleNfcDevice.requestConnectBleDevice(mNearestBle.getAddress());
								} else {
									msgBuffer.delete(0, msgBuffer.length());
									msgBuffer.append("未找到设备！");
									handler.sendEmptyMessage(0);
								}
							}finally {
								mNearestBleLock.unlock();
							}
						} else {
							mScanner.stopScan();
						}
					}
				}
			}).start();
		}
	}

	//发送读写进度条显示Handler
	private void showReadWriteDialog(String msg, int rate) {
		Message message = new Message();
		message.what = 4;
		message.arg1 = rate;
		message.obj = msg;
		handler.sendMessage(message);
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			msgText.setText(msgBuffer);

			if ((bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) || ((bleNfcDevice.isConnection() == BleManager.STATE_CONNECTING))) {
				//searchButton.setText("断开连接");
			} else {
				//searchButton.setText("搜索设备");
			}

			switch (msg.what) {
				case 1:
					break;
				case 2:
					break;
				case 3:
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								byte versions = bleNfcDevice.getDeviceVersions();
								//msgBuffer.append("设备版本:").append(String.format("%02x", versions)).append("\r\n");
								handler.sendEmptyMessage(0);
								double voltage = bleNfcDevice.getDeviceBatteryVoltage();
								//msgBuffer.append("设备电池电压:").append(String.format("%.2f", voltage)).append("\r\n");
								if (voltage < 3.61) {
									msgBuffer.append("(电量低)");
								} else {
									msgBuffer.append("(电量充足)");
								}

								handler.sendEmptyMessage(0);
								boolean isSuc = bleNfcDevice.androidFastParams(true);
								if (isSuc) {
									//msgBuffer.append("\r\n蓝牙快速传输参数设置成功!");
								} else {
									//msgBuffer.append("\n不支持快速传输参数设置!");
								}
								handler.sendEmptyMessage(0);

								//msgBuffer.append("\n开启自动寻卡...\r\n");
								handler.sendEmptyMessage(0);
								//开始自动寻卡
								startAutoSearchCard();
							} catch (DeviceNoResponseException e) {
								e.printStackTrace();
							}
						}
					}).start();
					break;
				case 0x88:
					//String bottleCode = msg.obj.toString();
					break;
				case 0x55:
					showToast("标签写入成功!");
					//写入成功将钢瓶码控件置空
					m_editText_gpbm.setText("");
					m_editText_ccbm.setText("");
					m_editText_readbq.setText(msg.obj.toString());
					m_buttonNext.setEnabled(true);
					m_buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
					break;
				case 0x56:
					showToast("标签写入失败,请再次写入");

					break;
				case 0x58:
					showToast("请输入钢瓶码!");
					break;
				case 0x59:
					showToast("标签锁定失败!");
					break;
			}

		}
	};
	private Handler handlerRestApi = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0x99://创建钢瓶的handler
					createGP();
					break;
			}
		}
	};
	//开始自动寻卡
	private boolean startAutoSearchCard() throws DeviceNoResponseException {
		//打开自动寻卡，200ms间隔，寻M1/UL卡
		boolean isSuc = false;
		int falseCnt = 0;
		do {
			isSuc = bleNfcDevice.startAutoSearchCard((byte) 20, ComByteManager.ISO14443_P4);
		}while (!isSuc && (falseCnt++ < 10));
		if (!isSuc){
			//msgBuffer.delete(0, msgBuffer.length());
			msgBuffer.append("不支持自动寻卡！\r\n");
			handler.sendEmptyMessage(0);
		}
		return isSuc;
	}
	//搜索按键监听
	private class StartSearchButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) ) {
				bleNfcDevice.requestDisConnectDevice();
				return;
			}
			searchNearestBleDevice();
		}
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 &&event.getAction() == KeyEvent.ACTION_DOWN)        {
			new AlertDialog.Builder(GPCreateActivity.this).setTitle("提示")
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
					.setNegativeButton("退出登录",
							new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog,
													int which)
								{
									SharedPreferencesHelper.put("username", "1");
									SharedPreferencesHelper.put("password", "1");
									Toast.makeText(GPCreateActivity.this, "退出登录成功！", Toast.LENGTH_LONG).show();
									Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
									startActivity(intent);
									finish();
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

	/**
	 * 日期改变的监听事件
	 *
	 * @param view
	 * @param year
	 * @param monthOfYear
	 * @param dayOfMonth
	 */
	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		this.year = year;
		this.month = monthOfYear;
		this.day = dayOfMonth;
	}

	private void createDatePicker(final EditText editText, final int rId){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				StringBuffer tempStringBuffer = new StringBuffer();
				String monthStr = String.format("%02d", month+1);
				String dayStr = String.format("%02d", day);
				editText.setText(tempStringBuffer.append(String.valueOf(year)).append(monthStr).append(dayStr));
				switch (rId) {
					case R.id.imageView_scrq://生产日期
						//报废日期+１５年
						tempStringBuffer.delete(0,tempStringBuffer.length());
						m_editText_bfrq.setText(tempStringBuffer.append(String.valueOf(year+15)).append(monthStr).append(dayStr));
						break;
					case R.id.imageView_scgj://上次钢检
						//下次钢检日期+4年
						tempStringBuffer.delete(0,tempStringBuffer.length());
						m_editText_xcgj.setText(tempStringBuffer.append(String.valueOf(year+4)).append(monthStr).append(dayStr));
						break;
					default:
						break;
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		final AlertDialog dialog = builder.create();
		View dialogView = View.inflate(this, R.layout.dialog_date, null);
		final DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.datePicker);

		dialog.setTitle("设置日期");
		dialog.setView(dialogView);
		dialog.show();
		//初始化日期监听事件
		datePicker.init(year, month - 1, day, this);
	}
	/**
	 * 获取当前的日期和时间
	 */
	private void initDateTime() {
		Calendar calendar = Calendar.getInstance();
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH) + 1;
		day = calendar.get(Calendar.DAY_OF_MONTH);
		hour = calendar.get(Calendar.HOUR);
		minute = calendar.get(Calendar.MINUTE);

	}

	//计费订单创建
	private void createGP() {
		String ccbm = m_editText_ccbm.getText().toString();//出厂编码
		if(ccbm==null||ccbm.trim().length()==0){
			Toast.makeText(GPCreateActivity.this, "请输入出厂编码",
					Toast.LENGTH_LONG).show();
			return;
		}

		String scrqStr = m_editText_scrq.getText().toString();
		String scgjStr = m_editText_scgj.getText().toString();
		String xcgjStr = m_editText_xcgj.getText().toString();
		String bfrqStr = m_editText_bfrq.getText().toString();

		if((!isValidDate(scrqStr)) || (!isValidDate(scgjStr)) || (!isValidDate(xcgjStr)) || (!isValidDate(bfrqStr))){
			Toast.makeText(GPCreateActivity.this, "日期格式错误",
					Toast.LENGTH_LONG).show();
			return;
		}
		m_date_scrq.delete(0,m_date_scrq.length());
		m_date_scgj.delete(0,m_date_scgj.length());
		m_date_xcgj.delete(0,m_date_xcgj.length());
		m_date_bfrq.delete(0,m_date_bfrq.length());
		m_date_scrq = m_date_scrq.append(changeStrTodate(scrqStr));
		m_date_scgj = m_date_scgj.append(changeStrTodate(scgjStr));
		m_date_xcgj = m_date_xcgj.append(changeStrTodate(xcgjStr));
		m_date_bfrq = m_date_bfrq.append(changeStrTodate(bfrqStr));



		String gpbm = m_editText_gpbm.getText().toString();
		if(m_gp_spec_code==null||m_gp_spec_code.trim().length()==0
				||m_date_scrq.length()==0||m_date_scgj.length()==0
				||m_date_xcgj.length()==0||m_date_bfrq.length()==0
				||gpbm==null||gpbm.trim().length()==0){
			Toast.makeText(GPCreateActivity.this, "参数输入不全",
					Toast.LENGTH_LONG).show();
			return;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.POST);
		try {
			nrc.requestUrl = NetUrlConstant.GASCYLINDERURL;
			nrc.context = this;
			Map<String, Object> params = new HashMap<String, Object>();
			Map<String, Object> body = new HashMap<String, Object>();
			body.put("number", m_gp_code_head+gpbm);//钢瓶编码
			body.put("publicNumber", ccbm);//出厂编码
			JSONObject tempJsonObject = new JSONObject();
			tempJsonObject.put("code", m_gp_spec_code);
			body.put("spec", tempJsonObject);//钢瓶规格
			JSONObject tempJsonObject_factory = new JSONObject();
			tempJsonObject_factory.put("code", m_gp_factory_code);
			body.put("factory", tempJsonObject_factory);//钢瓶厂家

			body.put("productionDate", m_date_scrq);//生产日期
			body.put("verifyDate", m_date_scgj);//上次钢检日期
			body.put("nextVerifyDate", m_date_xcgj);//下次钢检日期
			body.put("scrapDate", m_date_bfrq);//报废日期
			body.put("lifeStatus", 1);//1 启用
			body.put("serviceStatus", 1);//0 待使用

			nrc.setBody(body);
			nrc.setParams(params);
			getServer(new Netcallback() {
				public void preccess(Object res, boolean flag) {

					if(flag){
						HttpResponse response=(HttpResponse)res;
						if(response!=null){
							if(response.getStatusLine().getStatusCode()==201) {
								Toast.makeText(GPCreateActivity.this, "钢瓶建档成功,请扫描标签！",
										Toast.LENGTH_LONG).show();
								m_buttonNext.setEnabled(false);
								m_buttonNext.setBackgroundColor(getResources().getColor(R.color.transparentgray));
							}
							else{
								Toast.makeText(GPCreateActivity.this, "未知错误，异常！"+getResponseMessage(response),
										Toast.LENGTH_LONG).show();
							}
						}else {
							Toast.makeText(GPCreateActivity.this, "未知错误，异常！",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(GPCreateActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}

				}
			}, nrc);

		}catch (JSONException e){

		}
	}

	//6.5.	钢瓶厂家查询
	private void getGasCynFactorys() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.GASCYNFACTORYURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				try {
					if(flag){
						HttpResponse response=(HttpResponse)res;
						if(response!=null){
							if(response.getStatusLine().getStatusCode()==200) {
								JSONObject factorysJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray factorysArray = factorysJson.getJSONArray("items");
								m_factorysArrayJson = factorysArray;

								String factoryStr[] = new String[m_factorysArrayJson.length()];
								for (int i = 0; i < m_factorysArrayJson.length(); i++) {
									JSONObject factoryJson = m_factorysArrayJson.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
									factoryStr[i] = factoryJson.get("name").toString();
								}
								ArrayAdapter<String> adapter = new ArrayAdapter<String>(GPCreateActivity.this, android.R.layout.simple_spinner_item, factoryStr);  //创建一个数组适配器
								adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
								m_spinnerFactory.setAdapter(adapter);
							}
							else{
								Toast.makeText(GPCreateActivity.this, "钢瓶生产厂家查询失败！"+getResponseMessage(response),
										Toast.LENGTH_LONG).show();
							}
						}else {
							Toast.makeText(GPCreateActivity.this, "钢瓶生产厂家查询失败，未知错误，异常！",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(GPCreateActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}

				}catch (JSONException e){

				}catch (IOException e){

				}

			}
		}, nrc);


	}

	private String DateAddByYear(String strDate, int offset){
		String date = "";
		if(strDate.length()!=8){
			return null;
		}
		String year = strDate.substring(0,4);
		String month = strDate.substring(4,6);
		String day = strDate.substring(6,8);
		int iYear = Integer.parseInt(year)+offset;
		int iMonth = Integer.parseInt(month);
		int iDay = Integer.parseInt(day);
		if((iMonth<1) || (iMonth>12) || (iDay<1)|| (iDay>31)){

			return null;
		}
		date = Integer.toString(iYear)+month+day;
		return date;
	}

	private String changeStrTodate(String strDate){
		String date = "";
		if(strDate.length()!=8){
			return null;
		}
		String year = strDate.substring(0,4);
		String month = strDate.substring(4,6);
		String day = strDate.substring(6,8);
		int iYear = Integer.parseInt(year);
		int iMonth = Integer.parseInt(month);
		int iDay = Integer.parseInt(day);
		if((iMonth<1) || (iMonth>12) || (iDay<1)|| (iDay>31)){

			return null;
		}
		date = year+"-"+month+"-"+iDay+" "+"00:00:00";
		return date;
	}

	private boolean isValidDate(String strDate){
		if(strDate.length()!=8){
			return false;
		}
		String year = strDate.substring(0,4);
		String month = strDate.substring(4,6);
		String day = strDate.substring(6,8);
		int iYear = Integer.parseInt(year);
		int iMonth = Integer.parseInt(month);
		int iDay = Integer.parseInt(day);
		if((iMonth<1) || (iMonth>12) || (iDay<1)|| (iDay>31)){
			return false;
		}

		return true;
	}

	private void showToast(String info){
		if(toast ==null){
			toast = Toast.makeText(GPCreateActivity.this, null, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			LinearLayout toastView = (LinearLayout)toast.getView();
			WindowManager wm = (WindowManager)this.getSystemService(this.WINDOW_SERVICE);
			DisplayMetrics outMetrics = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(outMetrics);
			tv=new TextView(this);
			toastView.getBackground().setAlpha(0);//0~255透明度值
			//toastView.setBackgroundResource(R.drawable.ic_menu_deal_on);
			tv.setTextSize(40);
			tv.setTextColor(getResources().getColor(R.color.colorAccent));
			toastView.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 0, 0, 180);
			tv.setLayoutParams(params);
			toast.setView(toastView);
			toastView.addView(tv);
		}
		tv.setText(info);
		toast.show();
	}

	//心跳
	private void sysUserKeepAlive() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.SYSUSERKEEPALIVEURL+"/"+m_userId;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;

				} else {
					Toast.makeText(GPCreateActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);


	}

}
