package org.zywx.wbpalmstar.plugin.uexpedometer;

import java.sql.Date;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.uexpedometer.SQLite.PedometerSQLiteHelper;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * 后台记步服务，10s判断一次，如果10s内的步数小于10步或大于50步，则无效
 * 
 * @author waka
 *
 */
public class StepService extends Service {
	private static final String TAG = "StepService";
	public static boolean serviceFlag = false;// 服务开关标志
	public static boolean stableWalkStatusFlag = false;// 稳定走路状态标志，为true的话SELECTED_STEP=CURRENT_SETP
	public static int SELECTED_STEP = 0;// 筛选过的步数
	private int stepToday;
	private int stepHistory = -1;
	private static final int MIN_STEP_IN_10SECONDS = 10;// 10s内最小步数，人类的一步在0.2s到2s之间
	private static final int MAX_STEP_IN_10SECONDS = 50;// 10s内最大步数
	// 传感器
	private SensorManager mSensorManager;
	private Sensor mSensor, mStepDetectorSensor, mStepCountSensor;
	private StepDetector mStepDetector;// 信号监听记步类
	// 通知
	private static final int NOTIFICATION_STEP_SERVICE = 1;// 通知标记
	private static final String NOTIFICATION_STEP_SERVICE_TICKER = EUExUtil
			.getString("plugin_pedometer_step_service_ticker");// 通知ticker，在通知刚生成时在手机最上方弹出的一闪而过的提示
	private static final String NOTIFICATION_STEP_SERVICE_TITLE = EUExUtil.getString("plugin_pedometer_step_number");// 通知标题
	private static final String NOTIFICATION_STEP_SERVICE_CONTENT = EUExUtil.getString("plugin_pedometer_step_calorie");// 通知内容
	private static final float SCALE_STEP_CALORIES = 43.22f;
	private Notification.Builder builder;
	private Notification mNotification;
	// 线程
	private Thread selectStepThread;// 此线程用来筛选步数
	private Thread updateStepThread;// 此线程用来更新通知栏的步数
	// 数据库操作类
	private PedometerSQLiteHelper dbHelper;
	private SQLiteDatabase db;

	/**
	 * 绑定服务
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * 创建服务
	 */
	@Override
	public void onCreate() {
		super.onCreate();
	}

	/**
	 * 开始服务
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		serviceFlag = true;// 服务开启标志置为true
		// 初始化数据库
		dbHelper = new PedometerSQLiteHelper(StepService.this, "Pedometer.db", null, 1);
		db = dbHelper.getWritableDatabase();// 得到数据库实例
		int step = dbHelper.queryData(db, new Date(System.currentTimeMillis()).toString());
		if (step == -1) {
			dbHelper.insertData(db, 0);// 如果没有今天的数据，插入
		} else {
			// 获取今天的数据
			StepDetector.CURRENT_SETP = step;
			SELECTED_STEP = step;
		}
		showNotification();// 在通知栏显示
		startStepDetector();// 开始监听步数
		return START_STICKY;// 粘性服务，在被kill后尝试自动开启
	}

	/**
	 * 销毁服务
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceFlag = false;// 服务开启标志置为false,停止筛选线程和更新线程
		stopForeground(true);// 在通知栏停止前台服务
		if (mStepDetector != null) {// 如果记步类对象依然存在
			mSensorManager.unregisterListener(mStepDetector);// 取消监听
			mStepDetector = null;// 将记步类对象置为null
		}
	}

	/**
	 * 显示通知
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void showNotification() {
		builder = new Notification.Builder(this);// 使用Notification.Builder创建Notification
		// 将通知栏设置为自定义View
		builder.setContentTitle(SELECTED_STEP + NOTIFICATION_STEP_SERVICE_TITLE);
		builder.setContentText(String.format("%.1f", (SELECTED_STEP * SCALE_STEP_CALORIES) / 1000)
				+ NOTIFICATION_STEP_SERVICE_CONTENT);
		builder.setTicker(NOTIFICATION_STEP_SERVICE_TICKER);
		builder.setSmallIcon(EUExUtil.getResDrawableID("plugin_pedometer_pedometer"));
		if (Build.VERSION.SDK_INT >= 16) {
			mNotification = builder.build();
		} else {
			mNotification = builder.getNotification();
		}
		mNotification.flags = Notification.FLAG_NO_CLEAR;// 在点击通知后，通知并不会消失
		startForeground(NOTIFICATION_STEP_SERVICE, mNotification);// 在通知栏添加前台服务
	}

	/**
	 * 开始监听步数
	 */
	@SuppressLint("InlinedApi")
	private void startStepDetector() {
		if (mStepDetector == null) {
			mStepDetector = new StepDetector(StepService.this);// 如果不为空，new一个
		}
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 得到传感器管理器SensorManager
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// 得到加速度计传感器
		// 如果SDK版本大于等于19，则试着获取系统自带的步数传感器
		if (Build.VERSION.SDK_INT >= 19) {
			mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);// 得到STEP_DETECTOR传感器
			mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);// 得到STEP_COUNTER传感器
		}
		// 如果存在系统自带的步数传感器，则使用系统的，否则使用加速度传感器
		if (mStepDetectorSensor != null) {
			mSensorManager.registerListener(mStepDetector, mStepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
			// 开启更新通知3线程
			initUpdateStepDetectorThread();
			updateStepThread.start();
		} else if (mStepCountSensor != null) {
			mSensorManager.registerListener(mStepDetector, mStepCountSensor, SensorManager.SENSOR_DELAY_FASTEST);
			// 开启更新通知2线程
			initUpdateStepCountThread();
			updateStepThread.start();
		} else {
			mSensorManager.registerListener(mStepDetector, mSensor, SensorManager.SENSOR_DELAY_FASTEST);// 注册监听
			// 开启筛选步数线程
			initSelectStepThread();
			selectStepThread.start();
			// 开启更新通知线程
			initUpdateStepThread();
			updateStepThread.start();
		}
	}

	/**
	 * 初始化筛选线程
	 */
	private void initSelectStepThread() {
		selectStepThread = new Thread() {
			private int step_before;// 计时之前的步数
			private int step_after;// 计时之后的步数
			private int step_diff;// 差值
			private String date_before = new Date(System.currentTimeMillis()).toString();

			@Override
			public void run() {
				while (serviceFlag) {
					step_before = StepDetector.CURRENT_SETP;
					try {
						sleep(10000);// 十秒之后
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					String date_after = new Date(System.currentTimeMillis()).toString();
					// 如果10s后天数没有更迭
					if (date_after.equals(date_before)) {
						Log.i(TAG, "date_before" + date_before);
						Log.i(TAG, "date_after" + date_after);
						step_after = StepDetector.CURRENT_SETP;
						step_diff = step_after - step_before;
						if (step_diff > MIN_STEP_IN_10SECONDS && step_diff < MAX_STEP_IN_10SECONDS) {
							stableWalkStatusFlag = true;// 进入稳定走路状态
						} else {
							stableWalkStatusFlag = false;// 退出稳定走路状态
							StepDetector.CURRENT_SETP = StepDetector.CURRENT_SETP - step_diff;// 将步数不达标的无用步数减去
						}
					} else {
						StepDetector.CURRENT_SETP = 0;
						date_before = date_after;
					}
				}
			}
		};
	}

	/**
	 * 初始化更新通知线程
	 */
	private void initUpdateStepThread() {
		updateStepThread = new Thread() {
			@SuppressWarnings("deprecation")
			@SuppressLint("NewApi")
			@Override
			public void run() {
				while (serviceFlag) {// 当服务运行时，线程开启
					try {
						sleep(1000);// 每隔一秒更新一次
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (stableWalkStatusFlag) {
						SELECTED_STEP = StepDetector.CURRENT_SETP;
						if (!dbHelper.updateData(db, SELECTED_STEP)) {// 每隔1s更新一次数据库，如果更新失败（此处情况为两天交替时发生）
							dbHelper.insertData(db, 0);// 为第二天添加一个新的数据列
							StepDetector.CURRENT_SETP = 0;
							SELECTED_STEP = 0;
						}
					}
					builder.setContentTitle(SELECTED_STEP + NOTIFICATION_STEP_SERVICE_TITLE);
					builder.setContentText(String.format("%.1f", (SELECTED_STEP * SCALE_STEP_CALORIES) / 1000)
							+ NOTIFICATION_STEP_SERVICE_CONTENT);
					if (Build.VERSION.SDK_INT >= 16) {
						mNotification = builder.build();
					} else {
						mNotification = builder.getNotification();
					}
					mNotification.flags = Notification.FLAG_NO_CLEAR;// 在点击通知后，通知并不会消失
					startForeground(NOTIFICATION_STEP_SERVICE, mNotification);// 在通知栏添加前台服务
					// Log.i(TAG, "" + StepDetector.CURRENT_SETP);
				}
			}
		};
	}

	/**
	 * 初始化更新通知线程2,当系统自带记步方法TYPE_STEP_COUNTER时启用这个线程
	 */
	private void initUpdateStepCountThread() {
		updateStepThread = new Thread() {
			@SuppressWarnings("deprecation")
			@SuppressLint("NewApi")
			@Override
			public void run() {
				while (serviceFlag) {// 当服务运行时，线程开启
					try {
						sleep(1000);// 每隔一秒更新一次
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// 获得今天的步数
					int step = dbHelper.queryData(db, new Date(System.currentTimeMillis()).toString());
					// 如果stepHistory还未初始化（即等于-1），将系统总步数赋给它
					if (stepHistory == -1) {
						stepHistory = StepDetector.STEP_COUNT
								- step;/** 核心算法：这个算法相当的巧妙，即使算出的结果为负数，也是我们需要的 **/
						Log.i(TAG, "stepHistory----->" + stepHistory);
					}
					// 今天走的步数等于总步数减去历史步数
					stepToday = StepDetector.STEP_COUNT
							- stepHistory;/** 核心算法：这个算法相当的巧妙 **/
					if (!dbHelper.updateData(db, stepToday)) {// 每隔1s更新一次数据库，如果更新失败（此处情况为两天交替时发生）
						dbHelper.insertData(db, 0);// 为第二天添加一个新的数据列
						stepHistory = -1;
					}
					builder.setContentTitle(stepToday + NOTIFICATION_STEP_SERVICE_TITLE);
					builder.setContentText(String.format("%.1f", (stepToday * SCALE_STEP_CALORIES) / 1000)
							+ NOTIFICATION_STEP_SERVICE_CONTENT);
					if (Build.VERSION.SDK_INT >= 16) {
						mNotification = builder.build();
					} else {
						mNotification = builder.getNotification();
					}
					mNotification.flags = Notification.FLAG_NO_CLEAR;// 在点击通知后，通知并不会消失
					startForeground(NOTIFICATION_STEP_SERVICE, mNotification);// 在通知栏添加前台服务
					// Log.i(TAG, "" + StepDetector.CURRENT_SETP);
				}
			}
		};
	}

	/**
	 * 初始化更新通知线程3,当系统自带记步方法TYPE_STEP_DETECTOR时启用这个线程
	 */
	private void initUpdateStepDetectorThread() {
		updateStepThread = new Thread() {
			@SuppressWarnings("deprecation")
			@SuppressLint("NewApi")
			@Override
			public void run() {
				while (serviceFlag) {// 当服务运行时，线程开启
					try {
						sleep(1000);// 每隔一秒更新一次
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!dbHelper.updateData(db, StepDetector.STEP_DETECTOR)) {// 每隔1s更新一次数据库，如果更新失败（此处情况为两天交替时发生）
						dbHelper.insertData(db, 0);// 为第二天添加一个新的数据列
						StepDetector.STEP_DETECTOR = 0;
					}
					builder.setContentTitle(StepDetector.STEP_DETECTOR + NOTIFICATION_STEP_SERVICE_TITLE);
					builder.setContentText(
							String.format("%.1f", (StepDetector.STEP_DETECTOR * SCALE_STEP_CALORIES) / 1000)
									+ NOTIFICATION_STEP_SERVICE_CONTENT);
					if (Build.VERSION.SDK_INT >= 16) {
						mNotification = builder.build();
					} else {
						mNotification = builder.getNotification();
					}
					mNotification.flags = Notification.FLAG_NO_CLEAR;// 在点击通知后，通知并不会消失
					startForeground(NOTIFICATION_STEP_SERVICE, mNotification);// 在通知栏添加前台服务
					// Log.i(TAG, "" + StepDetector.CURRENT_SETP);
				}
			}
		};
	}
}
