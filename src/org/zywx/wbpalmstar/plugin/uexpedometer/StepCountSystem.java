package org.zywx.wbpalmstar.plugin.uexpedometer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * 使用系统提供的方法实现计步
 * 
 * @author waka
 *
 */
public class StepCountSystem implements SensorEventListener {
	private static final String TAG = "StepCountSystem";
	@SuppressWarnings("unused")
	private Context context;
	private SensorManager mSensorManager;
	private Sensor mStepDetectorSensor;
	private Sensor mStepCountSensor;
	private float mStepDetector;
	private float mStepCount;

	/**
	 * 构造方法，初始化传感器
	 * 
	 * @param context
	 */
	@SuppressLint("InlinedApi")
	public StepCountSystem(Context context) {
		this.context = context;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
		mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
	}

	/**
	 * 注册
	 */
	public void register() {
		mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mStepCountSensor, SensorManager.SENSOR_DELAY_FASTEST);
	}

	/**
	 * 取消注册
	 */
	public void unRegister() {
		mSensorManager.unregisterListener(this);
	}

	/**
	 * 当传感器检测到的数值发生变化时就会调用这个方法
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.i(TAG, "mStepCount--->" + mStepCount);
		Log.i(TAG, "mStepDetector--->" + mStepDetector);
		if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
			if (event.values[0] == 1.0) {
				mStepDetector++;
			}
		}
		if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
			mStepCount = event.values[0];
		}
	}

	/**
	 * 当传感器的精度发生变化时就会调用这个方法
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/**
	 * 得到步数
	 * 
	 * @return
	 */
	public float getmStepCount() {
		return mStepCount;
	}

	/**
	 * 得到Detector，其实我也不懂这个是干嘛的
	 * 
	 * @return
	 */
	public float getmStepDetector() {
		return mStepDetector;
	}

}
