package com.share.core.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * 时间类
 */
public final class Time {
	private Time() {
	}

	private static final Calendar calendar = Calendar.getInstance();
	/**
	 * 默认日期格式
	 */
	private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * 获取当前时间
	 */
	public static final int now() {
		return (int) now(false);
	}

	/**
	 * 获取当前时间(毫秒级)
	 * 
	 * @param isMicroTime
	 *            是否返回毫秒
	 */
	public static final long now(boolean isMicroTime) {
		if (isMicroTime) {
			return System.currentTimeMillis();
		}
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * 获取当天凌晨的时间戳
	 * 
	 * @param time
	 *            时间格式
	 * @param isMicrotime
	 *            是否显示毫秒
	 */
	public static final long dayBreak(String time, String format, boolean isMicrotime) {
		return str2time(time, format, isMicrotime);
	}

	/**
	 * 获取当天凌晨的时间戳
	 * 
	 * @param time
	 *            时间格式
	 */
	public static final int dayBreak(String time, String format) {
		return str2time(time2str(time), format);
	}

	/**
	 * 获取系统所在时区
	 */
	public static final String getTimeZone() {
		return calendar.getTimeZone().getID();
	}

	/**
	 * 获取当前时间是星期几(0为星期天)
	 */
	public static final int getDayOfWeek() {
		return calendar.get(Calendar.DAY_OF_WEEK) - 1;
	}

	/**
	 * 获取输入的时间戳是星期几
	 * 
	 * @param timestamp
	 *            时间戳
	 */
	public static final int getDayOfWeek(int timestamp) {
		calendar.setTimeInMillis((long) timestamp * 1000);
		return calendar.get(Calendar.DAY_OF_WEEK) - 1;
	}

	/**
	 * 获取当前时间是今年的第几周
	 */
	public static final int getWeekOfYear() {
		return calendar.get(Calendar.WEEK_OF_YEAR);
	}

	/**
	 * 获取输入的时间戳是当年的第几周
	 * 
	 * @param timestamp
	 *            时间戳
	 */
	public static final int getWeekOfYear(int timestamp) {
		calendar.setTimeInMillis((long) timestamp * 1000);
		return calendar.get(Calendar.WEEK_OF_YEAR);
	}

	/**
	 * 把当前时间戳转字符串
	 * 
	 * @param format
	 *            时间格式
	 */
	public static final String time2str(String format) {
		return time2str(now(true), format);
	}

	/**
	 * 时间戳转字符串
	 * 
	 * @param timestamp
	 *            时间戳
	 * @param format
	 *            时间格式
	 */
	public static final String time2str(long timestamp, String format) {
		return new SimpleDateFormat(format).format(timestamp);
	}

	/**
	 * 字符串转时间戳
	 * 
	 * @param str
	 *            字符串
	 * @param format
	 *            时间格式
	 * @param isMicrotime
	 *            是否输出毫秒
	 */
	public static final long str2time(String str, String format, boolean isMicrotime) {
		if (str == null || "".equals(str)) {
			return -1;
		}
		try {
			if (isMicrotime) {
				return new SimpleDateFormat(format).parse(str).getTime();
			}
			return new SimpleDateFormat(format).parse(str).getTime() / 1000;
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 字符串转时间戳
	 * 
	 * @param str
	 *            字符串
	 * @param format
	 *            时间格式
	 */
	public static final int str2time(String str) {
		return (int) str2time(str, DEFAULT_FORMAT, false);
	}
	
	/**
	 * 字符串转时间戳
	 * 
	 * @param str
	 *            字符串
	 * @param isMicrotime
	 *            是否输出毫秒
	 */
	public static final long str2time(String str, boolean isMicrotime) {
		return str2time(str, DEFAULT_FORMAT, isMicrotime);
	}

	/**
	 * 字符串转时间戳
	 * 
	 * @param str
	 *            字符串
	 * @param format
	 *            时间格式
	 */
	public static final int str2time(String str, String format) {
		return (int) str2time(str, format, false);
	}

	/**
	 * 计算一个时间段相隔多少秒
	 * 
	 * @param hm
	 *            12:00-13:00
	 */
	public static final int diff(String hm) {
		String[] time = hm.split("-");
		String[] minArr1 = time[0].split(":");
		String[] minArr2 = time[1].split(":");
		return (Integer.parseInt(minArr2[0]) * 60 + Integer.parseInt(minArr2[1]) - Integer.parseInt(minArr1[0]) * 60 - Integer.parseInt(minArr1[1])) * 60;
	}

	/**
	 * 计算两个时间段相差多少秒
	 * 
	 * @param time1
	 *            时间1
	 * @param time2
	 *            时间2
	 * @param format
	 *            时间格式
	 */
	public static final int diff(String time1, String time2, String format) {
		try {
			DateFormat df = new SimpleDateFormat(format);
			return java.lang.Math.abs((int) ((df.parse(time1).getTime() - df.parse(time2).getTime()) / 1000));
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 计算两个时间段相差多少分钟
	 * 
	 * @param time1
	 *            时间1
	 * @param time2
	 *            时间2
	 * @param format
	 *            时间格式
	 */
	public static final int diffMinute(String time1, String time2, String format) {
		return diff(time1, time2, format) / 60;
	}

	/**
	 * 计算两个时间段相差多少小时
	 * 
	 * @param time1
	 *            时间1
	 * @param time2
	 *            时间2
	 * @param format
	 *            时间格式
	 */
	public static final int diffHour(String time1, String time2, String format) {
		return diff(time1, time2, format) / 3600;
	}

	/**
	 * 计算两个时间段相差多少天
	 * 
	 * @param time1
	 *            时间1
	 * @param time2
	 *            时间2
	 * @param format
	 *            时间格式
	 */
	public static final int diffDay(String time1, String time2, String format) {
		return diff(time1, time2, format) / 86400;
	}

	/**
	 * 把当前时间格式化成yyyy-MM-dd HH:mm:ss
	 * 
	 * @return String
	 */
	public static final String date() {
		return date(DEFAULT_FORMAT);
	}

	/**
	 * 把当前时间格式化
	 * 
	 * @param format
	 * @return String
	 */
	public static final String date(String format) {
		return date(format, System.currentTimeMillis());
	}

	/**
	 * 把时间戳（秒）格式化成yyyy-MM-dd HH:mm:ss
	 * 
	 * @param timestamp
	 * @return String
	 */
	public static final String date(int timestamp) {
		return date(DEFAULT_FORMAT, timestamp);
	}

	/**
	 * 把时间戳（毫秒）格式化成yyyy-MM-dd HH:mm:ss
	 * 
	 * @param timestamp
	 * @return String
	 */
	public static final String date(long timestamp) {
		return date(DEFAULT_FORMAT, timestamp);
	}

	/**
	 * 把时间戳格式化
	 * 
	 * @param format
	 * @param timestamp
	 *            秒
	 * @return String
	 */
	public static final String date(String format, int timestamp) {
		return date(format, timestamp * 1000L);
	}

	/**
	 * 把时间戳格式化
	 * 
	 * @param format
	 * @param timestamp
	 *            毫秒
	 * @return String
	 */
	public static final String date(String format, long timestamp) {
		return new SimpleDateFormat(format).format(timestamp);
	}

	/**
	 * 寻找最合适的单位来显示时间
	 * 
	 * @author ruan 2013-7-21
	 * @param time
	 * @return
	 */
	public static final String showTime(long time) {
		String str = "";
		if (time > 0 && time <= 1000) {
			str = time + " ns";
		} else if (time > 1000 && time <= 1000000) {
			str = new DecimalFormat("0.00").format(time / 1000.0) + " μs";
		} else if (time > 1000000 && time <= 1000000000) {
			str = new DecimalFormat("0.00").format(time / 1000000.0) + " ms";
		} else {
			str = new DecimalFormat("0.00").format(time / 1000000000.0) + " s";
		}
		return str;
	}
}