package org.zywx.wbpalmstar.plugin.uexpedometer;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexpedometer.SQLite.PedometerSQLiteHelper;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * 计步器入口类
 * 
 * @author waka
 *
 */
public class EUExPedometer extends EUExBase {
	private static final String TAG = "EUExPedometer";
	private static final String FUNC_START_STEP_SERVICE_CALLBACK = "uexPedometer.cbStartStepService";
	private static final String FUNC_STOP_STEP_SERVICE_CALLBACK = "uexPedometer.cbStopStepService";
	private static final String FUNC_QUERY_STEP_TODAY_CALLBACK = "uexPedometer.cbQueryStepToday";
	private static final String FUNC_QUERY_STEP_HISTORY_CALLBACK = "uexPedometer.cbQueryStepHistory";

	/**
	 * 构造方法
	 * 
	 * @param arg0
	 * @param arg1
	 */
	public EUExPedometer(Context context, EBrowserView browserView) {
		super(context, browserView);
	}

	/**
	 * 开始后台记步服务
	 * 
	 * @param parm
	 */
	public void startStepService(String[] parm) {
		Intent intent = new Intent(mContext, StepService.class);
		mContext.startService(intent);
		jsCallback(FUNC_START_STEP_SERVICE_CALLBACK, 0, EUExCallback.F_C_TEXT, "StepService已被开启");
	}

	/**
	 * 关闭后台记步服务
	 * 
	 * @param parm
	 */
	public void stopStepService(String[] parm) {
		Intent intent = new Intent(mContext, StepService.class);
		mContext.stopService(intent);
		jsCallback(FUNC_STOP_STEP_SERVICE_CALLBACK, 0, EUExCallback.F_C_TEXT, "StepService已被关闭");
	}

	/**
	 * 查询今天步数
	 * 
	 * @param parm
	 */
	public void queryStepToday(String[] parm) {
		String date = new java.sql.Date(System.currentTimeMillis()).toString();
		Log.i(TAG, "Today" + date);
		String jsonResult = "";
		PedometerSQLiteHelper dbHelper = new PedometerSQLiteHelper(mContext, "Pedometer.db", null, 1);
		SQLiteDatabase db = dbHelper.getWritableDatabase();// 得到数据库实例
		int step = dbHelper.queryData(db, date);
		if (step == -1) {
			try {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("status", "fail");
				jsonObject.put("message", "查询失败，该日期不存在");
				jsonResult = jsonObject.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			jsCallback(FUNC_QUERY_STEP_TODAY_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
		} else {
			try {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("status", "success");
				jsonObject.put("message", "" + step);
				jsonResult = jsonObject.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			jsCallback(FUNC_QUERY_STEP_TODAY_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
		}

	}

	/**
	 * 查询历史步数
	 * 
	 * @param parm
	 */
	public void queryStepHistory(String[] parm) {
		String date = parm[0];
		String jsonResult = "";
		if (date.length() != 10) {
			try {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("status", "fail");
				jsonObject.put("message", "查询失败，请检查日期的格式");
				jsonResult = jsonObject.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			jsCallback(FUNC_QUERY_STEP_HISTORY_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
		} else {
			PedometerSQLiteHelper dbHelper = new PedometerSQLiteHelper(mContext, "Pedometer.db", null, 1);
			SQLiteDatabase db = dbHelper.getWritableDatabase();// 得到数据库实例
			int step = dbHelper.queryData(db, date);
			if (step == -1) {
				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("status", "fail");
					jsonObject.put("message", "查询失败，该日期不存在");
					jsonResult = jsonObject.toString();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				jsCallback(FUNC_QUERY_STEP_HISTORY_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
			} else {
				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("status", "success");
					jsonObject.put("message", "" + step);
					jsonResult = jsonObject.toString();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				jsCallback(FUNC_QUERY_STEP_HISTORY_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
			}
		}
	}

	/**
	 * clean
	 */
	@Override
	protected boolean clean() {
		return false;
	}

}
