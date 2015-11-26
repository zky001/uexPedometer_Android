package org.zywx.wbpalmstar.plugin.uexpedometer.SQLite;

import java.sql.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import android.widget.Toast;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库操作帮助类，实现了插入，查询，更新功能，为追求客观真实，删除功能未写
 * 
 * @author waka
 *
 */
public class PedometerSQLiteHelper extends SQLiteOpenHelper {
	private static final String TAG = "PedometerSQLiteHelper";// log标记
	private Context mContext;
	public static final String TABLE_NAME = "pedometer";// 步数表名

	/**
	 * 构造方法
	 */
	public PedometerSQLiteHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		mContext = context;
	}

	/**
	 * 创建数据库
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (_date DATE PRIMARY KEY,_step INTEGER)");// 创建步数表
		Toast.makeText(mContext, "Create Success", Toast.LENGTH_SHORT).show();
	}

	/**
	 * 升级数据库
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	/**
	 * 根据日期查询数据库中某一天的步数
	 * 
	 * @param db
	 * @param date
	 * @return 如果有数据，则返回数据；没有数据返回-1
	 */
	public int queryData(SQLiteDatabase db, String date) {
		int step = -1;
		Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where _date = ?", new String[] { date });
		if (cursor.moveToFirst()) {
			step = cursor.getInt(cursor.getColumnIndex("_step"));
		}
		return step;
	}

	/**
	 * 向数据库中插入数据，只能插入今天的数据;因为要开启多个进程保证service不被kill掉的几率，所以改动数据库的操作需加锁
	 * 
	 * @param db
	 * @param step
	 * @return 插入成功返回true，失败返回false
	 */
	public synchronized boolean insertData(SQLiteDatabase db, int step) {
		// 判断数据库中是否有今天日期的数据
		int is_exsit = queryData(db, new Date(System.currentTimeMillis()).toString());
		if (is_exsit == -1) {// 如果没有今天的数据，则可插入，返回true
			ContentValues values = new ContentValues();
			values.put("_date", new Date(System.currentTimeMillis()).toString());
			values.put("_step", step);
			db.insert(TABLE_NAME, null, values);// 插入数据
			return true;
		} else {// 如果有今天的数据，插入失败，返回false
			Log.i(TAG, new Date(System.currentTimeMillis()).toString() + "的数据已存在！不可重复插入");
			return false;
		}
	}

	/**
	 * 更新数据库中的数据，只能更新今天的数据;因为要开启多个进程保证service不被kill掉的几率，所以改动数据库的操作需加锁
	 * 
	 * @param db
	 * @param step
	 * @return 更新成功返回true，失败返回false
	 */
	public synchronized boolean updateData(SQLiteDatabase db, int step) {
		// 判断数据库中是否有今天日期的数据
		int is_exsit = queryData(db, new Date(System.currentTimeMillis()).toString());
		if (is_exsit == -1) {// 如果没有今天的数据，则不可更新，返回false
			Log.i(TAG, new Date(System.currentTimeMillis()).toString() + "的数据不存在！不能更新");
			return false;
		} else {
			db.execSQL("UPDATE " + TABLE_NAME + " SET _step = " + step + " WHERE _date = '"
					+ new Date(System.currentTimeMillis()).toString() + "'");
			return true;
		}
	}

}
