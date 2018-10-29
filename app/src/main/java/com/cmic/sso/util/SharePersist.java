package com.cmic.sso.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @Title:
 * @Description:
 * @Copyright: Copyright (c) 2013
 * @Company:深圳彩讯科技有限公司
 * @author licq 2014-3-13
 * @version 1.0
 */
public class SharePersist {
	private static SharePersist sharePersist;
	public static final String FILE_NAME = "UMC_DEMO";

	public static SharePersist getInstance() {
		if (sharePersist == null) {
			sharePersist = new SharePersist();
		}
		return sharePersist;
	}

	public boolean put(Context context, String key, String value) {
		SharedPreferences settings = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		return editor.putString(key, value).commit();
	}

	public boolean putLong(Context context, String key, long value) {
		SharedPreferences settings = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		return editor.putLong(key, value).commit();
	}

	public long getLong(Context context, String key) {
		SharedPreferences settings = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		return settings.getLong(key, 0);
	}

	public String get(Context context, String key) {
		SharedPreferences settings = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		return settings.getString(key, "");
	}
}
