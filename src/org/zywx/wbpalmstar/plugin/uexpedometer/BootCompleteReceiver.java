package org.zywx.wbpalmstar.plugin.uexpedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * 监听开机完成广播的广播接收器，在开机时自动启动计步器服务
 * 
 * @author waka
 *
 */
public class BootCompleteReceiver extends BroadcastReceiver {

	/**
	 * 开机完成后启动计步器服务
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "boot complete", Toast.LENGTH_SHORT).show();
		Intent intent2 = new Intent(context, StepService.class);
		context.startService(intent2);
	}

}
