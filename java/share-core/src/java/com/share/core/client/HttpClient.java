package com.share.core.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.share.core.system.SystemProperty;

/**
 * http客户端
 * @author ruan
 */
public final class HttpClient {
	/**
	 * logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(HttpClient.class);
	/**
	 * 系统基础配置
	 */
	@Autowired
	private SystemProperty systemProperty;
	/**
	 * 默认字符集
	 */
	private Charset charset;
	/**
	 * 默认超时时间(5秒)
	 */
	private int connectTimeout = 5000;
	/**
	 * http连接池
	 */
	private CloseableHttpClient client;
	/**
	 * 连接池管理器
	 */
	private PoolingHttpClientConnectionManager cm;

	/**
	 * 私有构造函数
	 */
	private HttpClient() {
	}

	/**
	 * 初始化
	 * @author ruan
	 */
	public void init() {
		// 支持http和https
		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory> create();
		registryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);
		registryBuilder.register("https", new SSLConnectionSocketFactory(SSLContexts.createSystemDefault(), new BrowserCompatHostnameVerifier()));
		Registry<ConnectionSocketFactory> socketFactoryRegistry = registryBuilder.build();

		// 初始化连接池
		cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		cm.setMaxTotal(2000);
		cm.setDefaultMaxPerRoute(100);

		// 设置字符集
		charset = systemProperty.getSystemCharset();
		ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom();
		connectionConfigBuilder.setCharset(charset);
		cm.setDefaultConnectionConfig(connectionConfigBuilder.build());

		// 设置socket连接选项
		SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
		socketConfigBuilder.setTcpNoDelay(true);
		socketConfigBuilder.setSoKeepAlive(false);
		socketConfigBuilder.setSoTimeout(connectTimeout);
		cm.setDefaultSocketConfig(socketConfigBuilder.build());

		// 请求超时设置
		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setConnectTimeout(connectTimeout);
		requestConfigBuilder.setConnectionRequestTimeout(connectTimeout);
		requestConfigBuilder.setSocketTimeout(connectTimeout);

		client = HttpClients.custom().setConnectionManager(cm).build();
		logger.info("http client inited");
	}

	/**
	 * 关闭方法
	 */
	public void close() {
		try {
			cm.close();
			client.close();
		} catch (IOException e) {
			logger.error("", e);
		}
		logger.info("http client closed");
	}

	/**
	 * 设置超时时间
	 * @author ruan
	 * @param connectTimeout
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 发送get请求，返回string
	 * @author ruan
	 * @param url
	 */
	public String getString(String url) {
		HttpGet get = new HttpGet(url);
		try {
			HttpResponse httpResponse = client.execute(get);
			return EntityUtils.toString(httpResponse.getEntity(), charset);
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			get.releaseConnection();
		}
		return null;
	}

	/**
	 * 发送get请求，返回byte[]
	 * @author ruan
	 * @param url
	 */
	public byte[] getByte(String url) {
		HttpGet get = new HttpGet(url);
		try {
			HttpResponse httpResponse = client.execute(get);
			return EntityUtils.toByteArray(httpResponse.getEntity());
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			get.releaseConnection();
		}
		return null;
	}

	/**
	 * 发送get请求
	 * @author ruan
	 * @param url
	 * @param data
	 * @return
	 */
	public String get(String url, Map<String, Object> data) {
		StringBuilder sb = new StringBuilder(url);
		if (url.lastIndexOf("/") != url.length() - 1) {
			sb.append("/");
		}
		sb.append("?");
		for (Entry<String, Object> e : data.entrySet()) {
			sb.append(e.getKey());
			sb.append("=");
			sb.append(e.getValue());
			sb.append("&");
		}
		int len = sb.length();
		sb.delete(len - 1, len);
		return getString(sb.toString());
	}

	/**
	 * 发送post请求
	 * @author ruan
	 * @param url
	 * @param data
	 * @return 字符串
	 */
	public String post(String url, Map<String, Object> data) {
		HttpPost httppost = new HttpPost(url);
		try {
			List<NameValuePair> valuePairList = new ArrayList<NameValuePair>();
			for (Entry<String, Object> e : data.entrySet()) {
				valuePairList.add(new BasicNameValuePair(e.getKey().trim(), e.getValue().toString().trim()));
			}
			httppost.setEntity(new UrlEncodedFormEntity(valuePairList, charset));
			HttpResponse response = client.execute(httppost);
			return EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			httppost.releaseConnection();
		}
		return null;
	}

	/**
	 * 发送post请求
	 * @author ruan
	 * @param url
	 * @param data
	 * @return 字节流
	 */
	public byte[] post(String url, byte[] data) {
		HttpPost httppost = new HttpPost(url);
		try {
			ByteArrayEntity reqEntity = new ByteArrayEntity(data);
			httppost.setEntity(reqEntity);
			HttpResponse response = client.execute(httppost);
			return EntityUtils.toByteArray(response.getEntity());
		} catch (IOException e) {
			logger.error("", e);
		} finally {
			httppost.releaseConnection();
		}
		return null;
	}
}