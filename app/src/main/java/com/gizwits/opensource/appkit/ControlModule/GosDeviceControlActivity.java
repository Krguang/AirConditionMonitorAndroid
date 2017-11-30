package com.gizwits.opensource.appkit.ControlModule;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.opensource.appkit.R;
import com.gizwits.opensource.appkit.utils.HexStrUtils;
import com.gizwits.opensource.appkit.view.HexWatcher;

import org.w3c.dom.Text;


public class GosDeviceControlActivity extends GosControlModuleBaseActivity
		implements OnClickListener,View.OnTouchListener {

	java.text.DecimalFormat oneDot=new java.text.DecimalFormat("00.0");

	private float fWenDuZhi;
	private float fShiDuZhi;
	private float fWenDuSet;
	private float fShiDuSet;
	private float fLengShuiFa;
	private float fReShuiFa;
	private float fJiaShiQi;

	String disWenDuZhi;
	String disShiDuZhi;
	String disWenDuSet;
	String disShiDuSet;
	String disLengShuiFa;
	String disReShuiFa;
	String disJiaShiQi;

	/** 设备列表传入的设备变量 */
	private GizWifiDevice mDevice;

	private Button bt_KongTiao;
	private Button bt_ZhiBan;
	private Button bt_FuYa;

	private ImageButton bt_JiZuYunXing;
	private ImageButton bt_ZhiBanYunXing;
	private ImageButton bt_FuYaYunXing;
	private ImageButton bt_JiZuGuZhang;
	private ImageButton bt_GaoXiaoZuSe;
	private TextView tv_data_WenDuZhi;
	private TextView tv_data_ShiDuZhi;
	private EditText tv_data_WenDuSet;
	private EditText tv_data_ShiDuSet;
	private TextView tv_data_LengShuiFa;
	private TextView tv_data_ReShuiFa;
	private TextView tv_data_JiaShuiQi;


	Timer timer1;
	TimerTask task1;

	private enum handler_key {

		/** 更新界面 */
		UPDATE_UI,

		DISCONNECT,
	}

	private Runnable mRunnable = new Runnable() {
		public void run() {
			if (isDeviceCanBeControlled()) {
				progressDialog.cancel();
			} else {
				toastDeviceNoReadyAndExit();
			}
		}

	};

	/** The handler. */
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			handler_key key = handler_key.values()[msg.what];
			switch (key) {
			case UPDATE_UI:
				updateUI();
				break;
			case DISCONNECT:
				toastDeviceDisconnectAndExit();
				break;
			}
		}
	};

	@Override
	protected void onStop() {
		super.onStop();
		if (timer1 != null) {
			timer1.cancel();
			timer1 = null;
		}

		if (task1 != null) {
			task1.cancel();
			task1 = null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gos_device_control);
		initDevice();
		setActionBar(true, true, getDeviceName());
		initView();
		initEvent();
	}



	private void initView() {

		bt_KongTiao= (Button) findViewById(R.id.bt_SW_KongTiao);
		bt_ZhiBan= (Button) findViewById(R.id.bt_SW_ZhiBan);
		bt_FuYa= (Button) findViewById(R.id.bt_SW_FuYa);
		bt_JiZuYunXing= (ImageButton) findViewById(R.id.bt_JiZuYunXing);
		bt_ZhiBanYunXing= (ImageButton) findViewById(R.id.bt_ZhiBanYunXing);
		bt_FuYaYunXing= (ImageButton) findViewById(R.id.bt_FuYaYunXing);
		bt_JiZuGuZhang= (ImageButton) findViewById(R.id.bt_JiZuGuZhang);
		bt_GaoXiaoZuSe= (ImageButton) findViewById(R.id.bt_GaoXiaoZuSe);
		tv_data_WenDuZhi = (TextView) findViewById(R.id.tv_WenDuZhi);
		tv_data_ShiDuZhi = (TextView) findViewById(R.id.tv_ShiDuZhi);
		tv_data_WenDuSet= (EditText) findViewById(R.id.tv_WenDuSet);
		tv_data_ShiDuSet= (EditText) findViewById(R.id.tv_ShiDuSet);
		tv_data_LengShuiFa = (TextView) findViewById(R.id.tv_data_LengShuiFa);
		tv_data_ReShuiFa = (TextView) findViewById(R.id.tv_data_ReShuiFa);
		tv_data_JiaShuiQi = (TextView) findViewById(R.id.tv_data_JiaShiQi);
	}

	private void initEvent() {

		bt_KongTiao.setOnClickListener(this);
		bt_ZhiBan.setOnClickListener(this);
		bt_FuYa.setOnClickListener(this);

		bt_KongTiao.setOnTouchListener(this);
		bt_ZhiBan.setOnTouchListener(this);
		bt_FuYa.setOnTouchListener(this);

		tv_data_WenDuSet.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				data_WenDuSet= (int) (Double.parseDouble(tv_data_WenDuSet.getText().toString())*10.0);
				sendCommand(KEY_WENDUSET, ( data_WenDuSet+ WENDUSET_OFFSET ) * WENDUSET_RATIO + WENDUSET_ADDITION);
				return false;
			}
		});

		tv_data_ShiDuSet.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				data_ShiDuSet= (int) (Double.parseDouble(tv_data_ShiDuSet.getText().toString())*10.0);
				sendCommand(KEY_SHIDUSET, (data_ShiDuSet + SHIDUSET_OFFSET ) * SHIDUSET_RATIO + SHIDUSET_ADDITION);
				return false;
			}
		});
	}

	private void initDevice() {
		Intent intent = getIntent();
		mDevice = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
		mDevice.setListener(gizWifiDeviceListener);
		Log.i("Apptest", mDevice.getDid());
	}

	private String getDeviceName() {
		if (TextUtils.isEmpty(mDevice.getAlias())) {
			return mDevice.getProductName();
		}
		return mDevice.getAlias();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getStatusOfDevice();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacks(mRunnable);
		// 退出页面，取消设备订阅
		mDevice.setSubscribe(false);
		mDevice.setListener(null);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()){
			case R.id.bt_SW_KongTiao:
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					v.setBackgroundResource(R.drawable.bt_kongtiao_down);
					break;
				}else if (event.getAction()==MotionEvent.ACTION_UP){
					v.setBackgroundResource(R.drawable.bt_kongtiao_up);
					break;
				}

			case R.id.bt_SW_ZhiBan:
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					v.setBackgroundResource(R.drawable.bt_zhiban_down);
					break;
				}else if (event.getAction()==MotionEvent.ACTION_UP){
					v.setBackgroundResource(R.drawable.bt_zhiban_up);
					break;
				}

			case R.id.bt_SW_FuYa:
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					v.setBackgroundResource(R.drawable.bt_fuya_down);
					break;
				}else if (event.getAction()==MotionEvent.ACTION_UP){
					v.setBackgroundResource(R.drawable.bt_fuya_up);
					break;
				}

			default:
				break;
		}
		return false;
	}

	@Override
	public void onClick(View v) {


		switch (v.getId()) {
			case R.id.bt_SW_KongTiao:
				if (data_SW_KongTiao){
					data_SW_KongTiao=false;
					sendCommand(KEY_SW_KONGTIAO, data_SW_KongTiao);
					break;
				}else {
					data_SW_KongTiao=true;
					sendCommand(KEY_SW_KONGTIAO, data_SW_KongTiao);
					break;
				}
			case R.id.bt_SW_ZhiBan:
				if (data_SW_ZhiBan){
					data_SW_ZhiBan=false;
					sendCommand(KEY_SW_ZHIBAN, data_SW_ZhiBan);
					break;
				}else {
					data_SW_ZhiBan=true;
					sendCommand(KEY_SW_ZHIBAN, data_SW_ZhiBan);
					break;
				}
			case R.id.bt_SW_FuYa:
				if (data_SW_FuYa){
					data_SW_FuYa=false;
					sendCommand(KEY_SW_FUYA, data_SW_FuYa);
					break;
				}else {
					data_SW_FuYa=true;
					sendCommand(KEY_SW_FUYA, data_SW_FuYa);
					break;
				}

			default:
			break;
		}


	}

/*

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		switch (seekBar.getId()) {
		case R.id.sb_data_WenDuSet:
			sendCommand(KEY_WENDUSET, (seekBar.getProgress() + WENDUSET_OFFSET ) * WENDUSET_RATIO + WENDUSET_ADDITION);
			break;
		case R.id.sb_data_ShiDuSet:
			sendCommand(KEY_SHIDUSET, (seekBar.getProgress() + SHIDUSET_OFFSET ) * SHIDUSET_RATIO + SHIDUSET_ADDITION);
			break;
		case R.id.sb_data_YaChaSet:
			sendCommand(KEY_YACHASET, (seekBar.getProgress() + YACHASET_OFFSET ) * YACHASET_RATIO + YACHASET_ADDITION);
			break;
		default:
			break;
		}
	}
*/
	/*
	 * ========================================================================
	 * 菜单栏
	 * ========================================================================
	 */

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.device_more, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_setDeviceInfo:
			setDeviceInfo();
			break;

		case R.id.action_getHardwareInfo:
			if (mDevice.isLAN()) {
				mDevice.getHardwareInfo();
			} else {
				myToast("只允许在局域网下获取设备硬件信息！");
			}
			break;

		case R.id.action_getStatu:
			mDevice.getDeviceStatus();
			break;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
*/
	/**
	 * Description:根据保存的的数据点的值来更新UI
	 */
	protected void updateUI() {

		fWenDuZhi=data_WenDuZhi;
		fShiDuZhi=data_ShiDuZhi;
		fWenDuSet=data_WenDuSet;
		fShiDuSet=data_ShiDuSet;
		fLengShuiFa=data_LengShuiFa;
		fReShuiFa=data_ReShuiFa;
		fJiaShiQi=data_JiaShuiQi;

		disWenDuZhi=oneDot.format(fWenDuZhi/10);
		disShiDuZhi=oneDot.format(fShiDuZhi/10);
		disWenDuSet=oneDot.format(fWenDuSet/10);
		disShiDuSet=oneDot.format(fShiDuSet/10);
		disLengShuiFa=oneDot.format(fLengShuiFa/10);
		disReShuiFa=oneDot.format(fReShuiFa/10);
		disJiaShiQi=oneDot.format(fJiaShiQi/10);

		if (data_ZS_JiZuYunXing){
			bt_JiZuYunXing.setBackgroundResource(R.drawable.led_green);
		}else {
			bt_JiZuYunXing.setBackgroundResource(R.drawable.led_gray);
		}
		if (data_ZS_ZhiBanYunXing){
			bt_ZhiBanYunXing.setBackgroundResource(R.drawable.led_green);
		}else {
			bt_ZhiBanYunXing.setBackgroundResource(R.drawable.led_gray);
		}
		if (data_ZS_FuYaYunXing){
			bt_FuYaYunXing.setBackgroundResource(R.drawable.led_green);
		}else {
			bt_FuYaYunXing.setBackgroundResource(R.drawable.led_gray);
		}
		if (data_ZS_JiZuGuZhang){
			bt_JiZuGuZhang.setBackgroundResource(R.drawable.led_red);
		}else {
			bt_JiZuGuZhang.setBackgroundResource(R.drawable.led_gray);
		}
		if (data_ZS_GaoXiaoZuSe){
			bt_GaoXiaoZuSe.setBackgroundResource(R.drawable.led_red);
		}else {
			bt_GaoXiaoZuSe.setBackgroundResource(R.drawable.led_gray);
		}

		tv_data_WenDuSet.setText(disWenDuSet);
		tv_data_ShiDuSet.setText(disShiDuSet);
		tv_data_WenDuZhi.setText(disWenDuZhi+"℃");
		tv_data_ShiDuZhi.setText(disShiDuZhi+"%");
		tv_data_LengShuiFa.setText(disLengShuiFa+"%");
		tv_data_ReShuiFa.setText(disReShuiFa+"%");
		tv_data_JiaShuiQi.setText(disJiaShiQi+"%");

	}

	private void setEditText(EditText et, Object value) {
		et.setText(value.toString());
		et.setSelection(value.toString().length());
		et.clearFocus();
	}

	/**
	 * Description:页面加载后弹出等待框，等待设备可被控制状态回调，如果一直不可被控，等待一段时间后自动退出界面
	 */
	private void getStatusOfDevice() {
		// 设备是否可控
		if (isDeviceCanBeControlled()) {
			// 可控则查询当前设备状态
			mDevice.getDeviceStatus();
		} else {
			// 显示等待栏
			progressDialog.show();
			if (mDevice.isLAN()) {
				// 小循环10s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 10000);
			} else {
				// 大循环20s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 20000);
			}
		}
	}

	/**
	 * 发送指令,下发单个数据点的命令可以用这个方法
	 * 
	 * <h3>注意</h3>
	 * <p>
	 * 下发多个数据点命令不能用这个方法多次调用，一次性多次调用这个方法会导致模组无法正确接收消息，参考方法内注释。
	 * </p>
	 * 
	 * @param key
	 *            数据点对应的标识名
	 * @param value
	 *            需要改变的值
	 */
	private void sendCommand(String key, Object value) {
		if (value == null) {
			return;
		}
		int sn = 5;
		ConcurrentHashMap<String, Object> hashMap = new ConcurrentHashMap<String, Object>();
		hashMap.put(key, value);
		// 同时下发多个数据点需要一次性在map中放置全部需要控制的key，value值
		// hashMap.put(key2, value2);
		// hashMap.put(key3, value3);
		mDevice.write(hashMap, sn);
		Log.i("liang", "下发命令：" + hashMap.toString());
	}

	private boolean isDeviceCanBeControlled() {
		return mDevice.getNetStatus() == GizWifiDeviceNetStatus.GizDeviceControlled;
	}

	private void toastDeviceNoReadyAndExit() {
		Toast.makeText(this, "设备无响应，请检查设备是否正常工作", Toast.LENGTH_SHORT).show();
		finish();
	}

	private void toastDeviceDisconnectAndExit() {
		Toast.makeText(GosDeviceControlActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
		finish();
	}

	/**
	 * 展示设备硬件信息
	 * 
	 * @param hardwareInfo
	 */
	private void showHardwareInfo(String hardwareInfo) {
		String hardwareInfoTitle = "设备硬件信息";
		new AlertDialog.Builder(this).setTitle(hardwareInfoTitle).setMessage(hardwareInfo)
				.setPositiveButton(R.string.besure, null).show();
	}

	/**
	 * Description:设置设备别名与备注
	 */
	private void setDeviceInfo() {

		final Dialog mDialog = new AlertDialog.Builder(this).setView(new EditText(this)).create();
		mDialog.show();

		Window window = mDialog.getWindow();
		window.setContentView(R.layout.alert_gos_set_device_info);

		final EditText etAlias;
		final EditText etRemark;
		etAlias = (EditText) window.findViewById(R.id.etAlias);
		etRemark = (EditText) window.findViewById(R.id.etRemark);

		LinearLayout llNo, llSure;
		llNo = (LinearLayout) window.findViewById(R.id.llNo);
		llSure = (LinearLayout) window.findViewById(R.id.llSure);

		if (!TextUtils.isEmpty(mDevice.getAlias())) {
			setEditText(etAlias, mDevice.getAlias());
		}
		if (!TextUtils.isEmpty(mDevice.getRemark())) {
			setEditText(etRemark, mDevice.getRemark());
		}

		llNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDialog.dismiss();
			}
		});

		llSure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(etRemark.getText().toString())
						&& TextUtils.isEmpty(etAlias.getText().toString())) {
					myToast("请输入设备别名或备注！");
					return;
				}
				mDevice.setCustomInfo(etRemark.getText().toString(), etAlias.getText().toString());
				mDialog.dismiss();
				String loadingText = (String) getText(R.string.loadingtext);
				progressDialog.setMessage(loadingText);
				progressDialog.show();
			}
		});

		mDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				hideKeyBoard();
			}
		});
	}
	
	/*
	 * 获取设备硬件信息回调
	 */
	@Override
	protected void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device,
			ConcurrentHashMap<String, String> hardwareInfo) {
		super.didGetHardwareInfo(result, device, hardwareInfo);
		StringBuffer sb = new StringBuffer();
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS != result) {
			myToast("获取设备硬件信息失败：" + result.name());
		} else {
			sb.append("Wifi Hardware Version:" + hardwareInfo.get(WIFI_HARDVER_KEY) + "\r\n");
			sb.append("Wifi Software Version:" + hardwareInfo.get(WIFI_SOFTVER_KEY) + "\r\n");
			sb.append("MCU Hardware Version:" + hardwareInfo.get(MCU_HARDVER_KEY) + "\r\n");
			sb.append("MCU Software Version:" + hardwareInfo.get(MCU_SOFTVER_KEY) + "\r\n");
			sb.append("Wifi Firmware Id:" + hardwareInfo.get(WIFI_FIRMWAREID_KEY) + "\r\n");
			sb.append("Wifi Firmware Version:" + hardwareInfo.get(WIFI_FIRMWAREVER_KEY) + "\r\n");
			sb.append("Product Key:" + "\r\n" + hardwareInfo.get(PRODUCT_KEY) + "\r\n");

			// 设备属性
			sb.append("Device ID:" + "\r\n" + mDevice.getDid() + "\r\n");
			sb.append("Device IP:" + mDevice.getIPAddress() + "\r\n");
			sb.append("Device MAC:" + mDevice.getMacAddress() + "\r\n");
		}
		showHardwareInfo(sb.toString());
	}
	
	/*
	 * 设置设备别名和备注回调
	 */
	@Override
	protected void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
		super.didSetCustomInfo(result, device);
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
			myToast("设置成功");
			progressDialog.cancel();
			finish();
		} else {
			myToast("设置失败：" + result.name());
		}
	}

	/*
	 * 设备状态改变回调，只有设备状态为可控才可以下发控制命令
	 */
	@Override
	protected void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
		super.didUpdateNetStatus(device, netStatus);
		if (netStatus == GizWifiDeviceNetStatus.GizDeviceControlled) {
			mHandler.removeCallbacks(mRunnable);
			progressDialog.cancel();
		} else {
			mHandler.sendEmptyMessage(handler_key.DISCONNECT.ordinal());
		}
	}
	
	/*
	 * 设备上报数据回调，此回调包括设备主动上报数据、下发控制命令成功后设备返回ACK
	 */
	@Override
	protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
			ConcurrentHashMap<String, Object> dataMap, int sn) {
		super.didReceiveData(result, device, dataMap, sn);
		Log.i("liang", "接收到数据");
		if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS && dataMap.get("data") != null) {
			getDataFromReceiveDataMap(dataMap);
			mHandler.sendEmptyMessage(handler_key.UPDATE_UI.ordinal());
		}
	}

}