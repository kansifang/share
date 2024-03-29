package com.share.core.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.share.core.util.StringUtil;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;
import redis.clients.util.SafeEncoder;

/**
 * redis类
 */
public class Redis {
	/**
	 * logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Redis.class);
	/**
	 * 每次管道最大数量
	 */
	private final static int maxPipelineLen = 100;
	/**
	 * jedisPool
	 */
	private JedisPool jedisPool = null;
	/** 
	 * 操作Key的方法 
	 */
	public Keys KEYS = new Keys();
	/** 
	 * 对存储结构为String类型的操作 
	 */
	public Strings STRINGS = new Strings();
	/** 
	 * 对存储结构为List类型的操作
	 */
	public Lists LISTS = new Lists();
	/** 
	 * 对存储结构为Set类型的操作
	 */
	public Sets SETS = new Sets();
	/** 
	 * 对存储结构为HashMap类型的操作 
	 */
	public Hash HASH = new Hash();
	/** 
	 * 对存储结构为Set(排序的)类型的操作 
	 */
	public SortSet SORTSET = new SortSet();
	/** 
	 * 发布/订阅
	 */
	public PubSub PUBSUB = new PubSub();
	/** 
	 * 事务 
	 */
	public Transactions TRANSACTIONS = new Transactions();
	/** 
	 * 脚本
	 */
	public Scripts SCRIPTS = new Scripts();
	/** 
	 * 连接
	 */
	public Connection CONNECTION = new Connection();
	/** 
	 * 服务 
	 */
	public Server SERVER = new Server();
	/**
	 * 最小空闲连接数
	 */
	@Value("${redis.minIdle}")
	private int minIdle;
	/**
	 * 最大空闲连接数
	 */
	@Value("${redis.maxIdle}")
	private int maxIdle;
	/**
	 * redis地址
	 */
	@Value("${redis.host}")
	private String host;
	/**
	 * redis端口
	 */
	@Value("${redis.port}")
	private int port;
	/**
	 * redis密码
	 */
	@Value("${redis.password}")
	private String password;
	/**
	 * redis连接超时时间
	 */
	@Value("${redis.timeout}")
	private int timeout;
	/**
	 * 获取一个实例的最大等待时间(毫秒)
	 */
	@Value("${redis.maxWait}")
	private int maxWait;
	/**
	 * 可分配最大实例数
	 */
	@Value("${redis.maxTotal}")
	private int maxTotal;

	/**
	 * 构造函数
	 */
	private Redis() {
	}

	/**
	 * 初始化
	 */
	public void init() {
		GenericObjectPoolConfig jedisPoolConfig = new GenericObjectPoolConfig();
		jedisPoolConfig.setMinIdle(minIdle);
		jedisPoolConfig.setMaxIdle(maxIdle);
		jedisPoolConfig.setTestOnBorrow(true);// 检查连接是否可用
		jedisPoolConfig.setTestOnCreate(false);
		jedisPoolConfig.setTestWhileIdle(false);
		jedisPoolConfig.setTestOnReturn(true);
		jedisPoolConfig.setMaxWaitMillis(maxWait);
		jedisPoolConfig.setMaxTotal(maxTotal);

		if (password.isEmpty()) {
			jedisPool = new JedisPool(jedisPoolConfig, host, port);
		} else {
			jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password);
		}
		logger.warn("redis init " + host + ":" + port);

		// 检查连接
		try {
			KEYS.del("test");
		} catch (Exception e) {
			logger.error("", e);
			System.exit(0);
		}
	}

	/**
	 * 关闭方法
	 */
	public void close() {
		jedisPool.close();
		logger.warn("redis closed");
	}

	public class Keys {
		private Keys() {
		}

		/**
		 * 设置过期时间
		 * @param key 键
		 * @param seconds 多少秒后过期
		 */
		public long expire(String key, int seconds) {
			if (seconds <= 0) {
				return 0;
			}
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.expire(key, seconds);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 重命名key
		 * @param oldkey 老key
		 * @param newkey 新key
		 */
		public String rename(String oldkey, String newkey) {
			return rename(SafeEncoder.encode(oldkey), SafeEncoder.encode(newkey));
		}

		/**
		 * 重命名一个key,新的key必须是不存在的key
		 * @param oldkey 老key
		 * @param newkey 新key
		 * */
		public long renamenx(String oldkey, String newkey) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.renamenx(oldkey, newkey);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 重命名key
		 * @param oldkey 老key
		 * @param newkey 新key
		 */
		public String rename(byte[] oldkey, byte[] newkey) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rename(oldkey, newkey);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 设置一个UNIX时间戳的过期时间
		 * @param key 键
		 * @param timestamp 时间戳(秒)
		 * @return 影响的记录数
		 */
		public long expireAt(String key, long timestamp) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.expireAt(key, timestamp);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获取key的有效时间
		 * @param key 键
		 * @return 以秒为单位的时间表示
		 */
		public long ttl(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.ttl(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获取key的有效时间
		 * @param key 键
		 * @return 以毫秒为单位的时间表示
		 */
		public long pttl(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.pttl(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 移动一个key到另一个数据库
		 * @param key 键
		 * @param dbIndex 数据库索引号
		 * @return
		 */
		public long move(String key, int dbIndex) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.move(key, dbIndex);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获取指定key的数据压缩方式
		 * @param key 键
		 */
		public String objectEncoding(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.objectEncoding(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回指定key被引用的次数
		 * @param key 键
		 */
		public long objectRefcount(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.objectRefcount(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回指定key自被存储之后空闲的时间
		 * @param key 键
		 * @return 以10秒为单位的秒级别时间
		 */
		public long objectIdletime(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.objectIdletime(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 反序列化给定的序列化值，并将它和给定的key关联
		 * @param key 键
		 * @param ttl 毫秒为单位为key设置生存时间
		 * @param serializedValue 序列号之后的值
		 * @return
		 */
		public String restore(String key, int ttl, byte[] serializedValue) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.restore(key, ttl, serializedValue);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 反序列化给定的序列化值，并将它和给定的key关联
		 * @param key 键
		 * @param ttl 毫秒为单位为key设置生存时间
		 * @param serializedValue 序列号之后的值
		 * @return
		 */
		public String restore(byte[] key, int ttl, byte[] serializedValue) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.restore(key, ttl, serializedValue);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 将key从redis的一个实例移到另一个实例
		 * @param host 目标地址
		 * @param port 目标地址端口
		 * @param key 键
		 * @param destinationDb 目标数据库
		 * @param timeout 超时时间
		 * @return
		 */
		public String migrate(String host, int port, String key, int destinationDb, int timeout) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.migrate(host, port, key, destinationDb, timeout);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 随机返回一个key
		 */
		public String randomkey() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.randomKey();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 移除key的过期时间
		 * @param key 键
		 * @return 影响的记录数
		 * */
		public long persist(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.persist(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 删除keys对应的记录,可以是多个key
		 * @param keys 键
		 * @return 删除的记录数
		 * */
		public long del(String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.del(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 删除keys对应的记录,可以是多个key
		 * @param keys 键
		 * @return 删除的记录数
		 * */
		public long del(byte[]... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.del(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 判断key是否存在
		 * @param key 键
		 * @return boolean
		 * */
		public boolean exists(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.exists(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return false;
		}

		/**
		 * 对队列、集合、有序集合排序
		 * @param key 键
		 * @return List<String> 集合的全部记录
		 */
		public List<String> sort(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sort(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 对队列、集合、有序集合排序
		 * @param key 键
		 * @param param 排序参数
		 * @return List<String> 集合的全部记录
		 */
		public List<String> sort(String key, SortingParams param) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sort(key, param);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获取key的存储类型
		 * @param key 键
		 */
		public String type(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.type(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 查找所有匹配给定的模式的键
		 * @param pattern
		 */
		public Set<String> keys(String pattern) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.keys(pattern);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 导出key的值
		 * @param key 键
		 */
		public byte[] dump(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.dump(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 增量迭代key
		 * @param cursor 游标
		 */
		public ScanResult<String> scan(String cursor) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scan(cursor);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 增量迭代key
		 * @param cursor 游标
		 * @param params 迭代参数
		 */
		public ScanResult<String> scan(String cursor, ScanParams params) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scan(cursor, params);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class Sets {
		private Sets() {
		}

		/**
		 * 添加一个或者多个元素到集合(set)里
		 * @param key 键
		 * @param member
		 */
		public long sadd(String key, String... member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sadd(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 添加一个或者多个元素到集合(set)里
		 * @param key 键
		 * @param member
		 */
		public long sadd(byte[] key, byte[]... member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sadd(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获取集合里面的元素数量
		 * @param key 键
		 */
		public long scard(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scard(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回从第一组和所有的给定集合之间的差异的成员
		 * 
		 * @param  keys
		 * @return 差异的成员集合
		 * */
		public Set<String> sdiff(String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sdiff(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 这个命令等于sdiff,但返回的不是结果集,而是将结果集存储在新的集合中，如果目标已存在，则覆盖。
		 * @param newkey 新结果集的key
		 * @param keys 比较的集合
		 * @return 新集合中的记录数
		 */
		public long sdiffstore(String newkey, String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sdiffstore(newkey, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获得两个集合的交集
		 * 
		 * @param keys
		 * @return 交集成员的集合
		 * **/
		public Set<String> sinter(String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sinter(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 这个命令等于sinter,但返回的不是结果集,而是将结果集存储在新的集合中，如果目标已存在，则覆盖。
		 * @param newkey 新结果集的key
		 * @param keys 比较的集合
		 * @return 新集合中的记录数
		 * **/
		public long sinterstore(String newkey, String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sinterstore(newkey, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回成员 member 是否是存储的集合 key的成员
		 * 
		 * @param key
		 * @param member 要判断的值
		 */
		public boolean sismember(String key, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sismember(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return false;
		}

		/**
		 * 获取集合里面的所有key
		 * @param key
		 * @return 成员集合
		 */
		public Set<String> smembers(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.smembers(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获取集合里面的所有key
		 * @param key
		 * @return 成员集合
		 */
		public Set<byte[]> smembers(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.smembers(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 移动集合里面的一个key到另一个集合
		 * @param srckey 源集合
		 * @param dstkey 目标集合
		 * @param member 源集合中的成员
		 * @return 状态码，1成功，0失败
		 * */
		public long smove(String srckey, String dstkey, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.smove(srckey, dstkey, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 移除并返回被删除的元素
		 * @param key
		 * */
		public String spop(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.spop(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 从集合里删除一个或多个member
		 * @param key
		 * @param member 要删除的成员
		 * @return 状态码，成功返回1，成员不存在返回0
		 * */
		public long srem(String key, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.srem(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从集合里删除一个或多个member
		 * @param key
		 * @param member 要删除的成员
		 * @return 状态码，成功返回1，成员不存在返回0
		 * */
		public long srem(byte[] key, byte[] member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.srem(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 合并多个集合并返回合并后的结果，合并后的结果集合并不保存
		 * @param  keys
		 * @return 合并后的结果集合
		 */
		public Set<String> sunion(String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sunion(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 合并多个集合并返回合并后的结果，合并后的结果集合并不保存
		 * @param  keys
		 * @return 合并后的结果集合
		 */
		public Set<byte[]> sunion(byte[]... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sunion(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 合并多个集合并将合并后的结果集保存在指定的新集合中，如果新集合已经存在则覆盖
		 * @param newkey 新集合的key
		 * @param keys 要合并的集合
		 */
		public long sunionstore(String newkey, String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.sunionstore(newkey, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}
	}

	public class SortSet {
		private SortSet() {
		}

		/**
		 * 向集合中增加一条记录,如果这个值已存在，这个值对应的权重将被置为新的权重
		 * @param key
		 * @param score 权重
		 * @param member 要加入的值，
		 * @return 状态码 1成功，0已存在member的值
		 * */
		public long zadd(String key, double score, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zadd(key, score, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 向集合中增加一堆记录,如果这个值已存在，这个值对应的权重将被置为新的权重
		 * @param key
		 * @param scoreMembers member => 权重
		 * @return 状态码 1成功，0已存在member的值，-1为出现异常
		 * */
		public long zadd(String key, Map<String, Double> scoreMembers) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();

				// 如果传进来的map大小大于100，拆成每批100来保存
				int size = scoreMembers.size();
				if (size > maxPipelineLen) {
					int i = 1;
					Map<String, Double> tmpMap = new HashMap<String, Double>(maxPipelineLen);
					Iterator<Entry<String, Double>> it = scoreMembers.entrySet().iterator();
					while (it.hasNext()) {
						Entry<String, Double> e = it.next();
						tmpMap.put(e.getKey(), e.getValue());
						if (++i % maxPipelineLen == 0) {
							// 添加途中可能出错，但忽略
							// 一般来说，出错的情况是redis无法访问，这个几率很低
							// member重复的情况会经常出现，不过只是更新score值而已，不会造成重大影响
							jedis.zadd(key, tmpMap);
							tmpMap.clear();
						}

						// 用迭代器来做，这样每处理一个就移除一个
						it.remove();
					}

					// 可能还会有多余的，要判断一次
					if (!tmpMap.isEmpty()) {
						jedis.zadd(key, tmpMap);
					}
					return 1;
				} else {
					// 如果不超100就不拆
					return jedis.zadd(key, scoreMembers);
				}
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return -1;
		}

		/**
		 * 构造keyScoreMembers
		 * @author ruan 
		 * @param keyScoreMembers map
		 * @param key 键
		 * @param score 权重
		 * @param member 元素
		 */
		public void genKeyScoreMembers(Map<String, Map<String, Double>> keyScoreMembers, String key, double score, String member) {
			Map<String, Double> scoreMembers = keyScoreMembers.get(key);
			if (scoreMembers == null) {
				scoreMembers = new HashMap<>();
				keyScoreMembers.put(key, scoreMembers);
			}
			scoreMembers.put(member, score);
		}

		/**
		 * 向集合中增加一堆记录,如果这个值已存在，这个值对应的权重将被置为新的权重
		 * @param keyScoreMembers key => (member => 权重)
		 * */
		public void zadd(Map<String, Map<String, Double>> keyScoreMembers) {
			if (keyScoreMembers == null || keyScoreMembers.isEmpty()) {
				logger.warn("keyScoreMembers is empty");
				return;
			}
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				Pipeline pipeline = jedis.pipelined();

				// 如果传进来的map大小大于100，拆成每批100来保存
				int size = keyScoreMembers.size();
				if (size > maxPipelineLen) {
					int i = 1;
					for (Entry<String, Map<String, Double>> e : keyScoreMembers.entrySet()) {
						pipeline.zadd(e.getKey(), e.getValue());
						if (++i % maxPipelineLen == 0) {
							pipeline.sync();
						}
					}

					// 可能还会有多余的，要多请求一次
					pipeline.sync();
				} else {
					// 如果不超100就一次过搞掂
					for (Entry<String, Map<String, Double>> e : keyScoreMembers.entrySet()) {
						pipeline.zadd(e.getKey(), e.getValue());
					}
					pipeline.sync();
				}
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
		}

		/**
		 * 获取集合中的成员数量
		 * @param key
		 * @return 如果返回0则集合不存在
		 * */
		public long zcard(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return StringUtil.getLong(jedis.zcard(key));
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回有序集key中，score值在min和max之间(默认包括score值等于min或max)的成员数量
		 * @param key
		 * @param min 最小排序位置
		 * @param max 最大排序位置
		 * */
		public long zcount(String key, double min, double max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zcount(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 权重增加给定值，如果给定的member已存在
		 * 
		 * @param key
		 * @param score 要增的权重
		 * @param member 要插入的值
		 * @return 增后的权重
		 * */
		public double zincrby(String key, double score, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zincrby(key, score, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回指定位置的集合元素,0为第一个元素，-1为最后一个元素
		 * @param key
		 * @param start 开始位置(包含)
		 * @param end 结束位置(包含)
		 * @return Set<String>
		 * */
		public Set<String> zrange(String key, int start, int end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrange(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回key的有序集合中的分数在min和max之间的所有元素（包括分数等于max或者min的元素）
		 * @param key
		 * @param min 上限权重
		 * @param max 下限权重
		 * @return Set<String>
		 * */
		public Set<String> zrangeByScore(String key, double min, double max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrangeByScore(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 根据指定权重值从大到小返回元素
		 * @param key
		 * @param max 上限权重
		 * @param min 下限权重
		 * @param limit 返回条数限制
		 */
		public Set<String> zrevrangeByScore(String key, double max, double min, int limit) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrevrangeByScore(key, max, min, 0, limit);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;

		}

		/**
		 * 返回有序集key中成员member的排名(从小到大)
		 * @param key
		 * @param member
		 * @return long 位置
		 * */
		public long zrank(String key, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrank(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回有序集key中成员member的排名(从大到小)
		 * @param key
		 * @param member
		 * @return long 位置
		 * */
		public long zrevrank(String key, String member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrevrank(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从集合中删除成员
		 * @param key
		 * @param member
		 * @return 返回1成功
		 * */
		public long zrem(String key, String... member) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrem(key, member);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 移除有序集key中，指定排名(rank)区间内的所有成员
		 * @param key
		 * @param start 开始区间，从0开始(包含)
		 * @param end 结束区间,-1为最后一个元素(包含)
		 * @return 删除的数量
		 * */
		public long zremrangeByRank(String key, int start, int end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zremrangeByRank(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 移除有序集key中，所有score值介于min和max之间(包括等于min或max)的成员
		 * @param key
		 * @param min 下限权重(包含)
		 * @param max 上限权重(包含)
		 * @return 删除的数量
		 * */
		public long zremrangeByScore(String key, double min, double max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zremrangeByScore(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回有序集key中，指定区间内的成员，其中成员的位置按score值递减(从大到小)来排列
		 * @param key
		 * @param start
		 * @param end
		 * @return Set<String>
		 * */
		public Set<String> zrevrange(String key, int start, int end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrevrange(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回有序集key中，成员member的score值
		 * @param key
		 * @param memeber
		 * @return double 权重
		 * */
		public double zscore(String key, String memebr) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return StringUtil.getDouble(jedis.zscore(key, memebr));
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 批量返回有序集key中，成员member的score值
		 * @author ruan 
		 * @param keyMemebrMap
		 */
		public Map<String, Double> zscore(LinkedHashMap<String, String> keyMemebrMap) {
			if (keyMemebrMap == null || keyMemebrMap.isEmpty()) {
				return new HashMap<>(0);
			}
			int size = keyMemebrMap.size();
			Map<String, Double> map = new LinkedHashMap<>(size);

			int i = 0;
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				Pipeline pipeline = jedis.pipelined();
				List<String> keyList = new ArrayList<>(maxPipelineLen);

				for (Entry<String, String> e : keyMemebrMap.entrySet()) {
					pipeline.zscore(e.getKey(), e.getValue());
					keyList.add(e.getKey());

					// 由于管道一次性不能传输太多，所以要分开来
					if (++i % maxPipelineLen == 0) {
						List<Object> list = pipeline.syncAndReturnAll();
						if (list != null && !list.isEmpty()) {
							for (int j = 0; j < maxPipelineLen; j++) {
								Object obj = list.get(j);
								if (obj != null) {
									map.put(StringUtil.getString(keyList.get(j)), StringUtil.getDouble(obj));
								}
							}
						}
						keyList.clear();
					}
				}
				List<Object> list = pipeline.syncAndReturnAll();
				if (list != null && !list.isEmpty()) {
					size = keyList.size();
					for (int j = 0; j < size; j++) {
						Object obj = list.get(j);
						if (obj != null) {
							map.put(StringUtil.getString(keyList.get(j)), StringUtil.getDouble(obj));
						}
					}
				}
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return map;
		}

		/**
		 * 计算给定的有序集合的交集，并且把结果放到destination中
		 * @param dstkey
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zinterstore(String dstkey, String... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zinterstore(dstkey, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的交集，并且把结果放到destination中
		 * @param dstkey
		 * @param params
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zinterstore(String dstkey, ZParams params, String... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zinterstore(dstkey, params, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的交集，并且把结果放到destination中
		 * @param dstkey
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zinterstore(byte[] dstkey, byte[]... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zinterstore(dstkey, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的交集，并且把结果放到destination中
		 * @param dstkey
		 * @param params
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zinterstore(dstkey, params, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回指定区间的成员个数
		 * @param key
		 * @param min 最小值(包含)
		 * @param max 最小值(包含)
		 * @return
		 */
		public long zlexcount(String key, String min, String max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zlexcount(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回指定区间的成员个数
		 * @param key
		 * @param min 最小值(包含)
		 * @param max 最小值(包含)
		 * @return
		 */
		public long zlexcount(byte[] key, byte[] min, byte[] max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zlexcount(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的并集，并且把结果放到destination中
		 * @param dstkey
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zunionstore(String dstkey, String... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zunionstore(dstkey, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的并集，并且把结果放到destination中
		 * @param dstkey
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zunionstore(byte[] dstkey, byte[]... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zunionstore(dstkey, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的并集，并且把结果放到destination中
		 * @param dstkey
		 * @param params
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zunionstore(dstkey, params, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 计算给定的有序集合的并集，并且把结果放到destination中
		 * @param dstkey
		 * @param params
		 * @param sets
		 * @return 结果有序集合destination中元素个数
		 */
		public long zunionstore(String dstkey, ZParams params, String... sets) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zunionstore(dstkey, params, sets);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 如果所有成员的分数一样，按照二进制排序返回(可以用"["或者"(")
		 * @param key
		 * @param min
		 * @param max
		 * @return 指定范围内的成员
		 */
		public Set<String> zrangeByLex(String key, String min, String max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrangeByLex(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 如果所有成员的分数一样，按照二进制排序返回(可以用"["或者"(")
		 * @param key
		 * @param min
		 * @param max
		 * @return 指定范围内的成员
		 */
		public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrangeByLex(key, min, max);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 如果所有成员的分数一样，按照二进制排序返回(可以用"["或者"(")
		 * @param key
		 * @param min
		 * @param max
		 * @param offset 偏移量
		 * @param count 整数为倒序，负数为正序
		 * @return 指定范围内的成员
		 */
		public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrangeByLex(key, min, max, offset, count);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 如果所有成员的分数一样，按照二进制排序返回(可以用"["或者"(")
		 * @param key
		 * @param min
		 * @param max
		 * @param offset 偏移量
		 * @param count 整数为倒序，负数为正序
		 * @return 指定范围内的成员
		 */
		public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.zrangeByLex(key, min, max, offset, count);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class Hash {
		private Hash() {
		}

		/**
		 * 从hash中删除指定的field
		 * @param key
		 * @param field 存储的名字
		 * @return 状态码，1成功，0失败
		 * */
		public long hdel(String key, String field) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hdel(key, field);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 测试hash中指定的field是否存在
		 * @param key
		 * @param field 存储的名字
		 * */
		public boolean hexists(String key, String field) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hexists(key, field);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return false;
		}

		/**
		 * 返回hash中指定field的值
		 * @param key
		 * @param field 存储的名字
		 * @return 存储对应的值
		 * */
		public String hget(String key, String field) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hget(key, field);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回hash中指定field的值
		 * @param key
		 * @param field 存储的名字
		 * @return 存储对应的值
		 * */
		public byte[] hget(byte[] key, byte[] field) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hget(key, field);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 以Map的形式返回hash中的存储和值
		 * @param key
		 * @return Map<Strinig,String>
		 * */
		public Map<String, String> hgetAll(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hgetAll(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 以Map的形式返回hash中的存储和值
		 * @param key
		 * @return Map<Strinig,String>
		 * */
		public Map<byte[], byte[]> hgetAll(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hgetAll(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 对一个hash表的某个field set值
		 * @param key
		 * @param field
		 * @param value
		 * @return 状态码 1成功，0失败，fieid已存在将更新，也返回0
		 * **/
		public long hset(String key, String field, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hset(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 对一个hash表的某个field set值
		 * @param key
		 * @param field
		 * @param value
		 * @return 状态码 1成功，0失败，fieid已存在将更新，也返回0
		 * **/
		public long hset(byte[] key, byte[] field, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hset(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 对一个hash表的某个field set值，只有在fieid不存在时才执行
		 * @param key
		 * @param field
		 * @param value
		 * @return 状态码 1成功，0失败fieid已存
		 * **/
		public long hsetnx(String key, String field, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hsetnx(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 对一个hash表的某个field set值，只有在fieid不存在时才执行
		 * @param key
		 * @param field
		 * @param value
		 * @return 状态码 1成功，0失败fieid已存
		 * **/
		public long hsetnx(byte[] key, byte[] field, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hsetnx(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获取hash中value的集合
		 * @param key
		 * @return List<String>
		 * */
		public List<String> hvals(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hvals(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获取hash中value的集合
		 * @param key
		 * @return List<byte[]>
		 * */
		public List<byte[]> hvals(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hvals(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 给hash某个field自增
		 * @param key
		 * @param field 存储位置
		 * @param value 要增加的值,可以是负数
		 * @return 增加指定数字后，存储位置的值
		 * */
		public long hincrby(String key, String field, long value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hincrBy(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 给hash某个field自增
		 * @param key
		 * @param field 存储位置
		 * @param value 要增加的值,可以是负数
		 * @return 增加指定数字后，存储位置的值
		 * */
		public long hincrby(byte[] key, byte[] field, long value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hincrBy(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 给hash某个field自增
		 * @param key
		 * @param field 存储位置
		 * @param value 要增加的值,可以是负数
		 * @return 增加指定数字后，存储位置的值
		 * */
		public double hincrByFloat(String key, String field, double value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hincrByFloat(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 给hash某个field自增
		 * @param key
		 * @param field 存储位置
		 * @param value 要增加的值,可以是负数
		 * @return 增加指定数字后，存储位置的值
		 * */
		public double hincrByFloat(byte[] key, byte[] field, double value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hincrByFloat(key, field, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回指定hash中的所有field
		 * @param key
		 * @return Set<String> 存储名称的集合
		 * */
		public Set<String> hkeys(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hkeys(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回指定hash中的所有field
		 * @param key
		 * @return Set<String> 存储名称的集合
		 * */
		public Set<byte[]> hkeys(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hkeys(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获取hash中存储的个数
		 * @param key
		 * @return long 存储的个数
		 * */
		public long hlen(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hlen(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 获取hash中存储的个数
		 * @param key
		 * @return long 存储的个数
		 * */
		public long hlen(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hlen(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 根据多个key，获取对应的value，返回List,如果指定的key不存在,List对应位置为null
		 * @param key
		 * @param fields 多个field
		 * @return List<String>
		 * */
		public List<String> hmget(String key, String... fields) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hmget(key, fields);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 根据多个key，获取对应的value，返回List,如果指定的key不存在,List对应位置为null
		 * @param key
		 * @param fields 多个field
		 * @return List<String>
		 * */
		public List<byte[]> hmget(byte[] key, byte[]... fieids) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hmget(key, fieids);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 添加对应关系，如果对应关系已存在，则覆盖
		 * @param key
		 * @param map
		 * @return 状态，成功返回OK
		 * */
		public String hmset(String key, Map<String, String> map) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hmset(key, map);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 添加对应关系，如果对应关系已存在，则覆盖
		 * @param key
		 * @param map
		 * @return 状态，成功返回OK
		 * */
		public String hmset(byte[] key, Map<byte[], byte[]> map) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.hmset(key, map);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

	}

	public class Strings {
		private Strings() {
		}

		/**
		 * 根据key获取记录
		 * @param key
		 * */
		public String get(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.get(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return "";
		}

		/**
		 * 根据key获取记录
		 * @param key
		 * @return 值
		 * */
		public byte[] get(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.get(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 添加有过期时间的记录
		 * @param key
		 * @param seconds 过期时间，以秒为单位
		 * @param value
		 * */
		public String setex(String key, int seconds, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.setex(key, seconds, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 添加有过期时间的记录
		 * @param key
		 * @param seconds 过期时间，以秒为单位
		 * @param value
		 * */
		public String setex(byte[] key, int seconds, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.setex(key, seconds, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 添加一条记录，仅当给定的key不存在时才插入
		 * @param key
		 * @param value
		 * @return long 状态码，1插入成功且key不存在，0未插入，key存在
		 * */
		public long setnx(String key, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.setnx(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 添加记录,如果记录已存在将覆盖原有的value
		 * @param key
		 * @param value
		 * @return 状态码
		 * */
		public String set(String key, String value) {
			return set(SafeEncoder.encode(key), SafeEncoder.encode(value));
		}

		/**
		 * 添加记录,如果记录已存在将覆盖原有的value
		 * @param key
		 * @param value
		 * @return 状态码
		 * */
		public String set(String key, byte[] value) {
			return set(SafeEncoder.encode(key), value);
		}

		/**
		 * 添加记录,如果记录已存在将覆盖原有的value
		 * @param key
		 * @param value
		 * @return 状态码
		 * */
		public String set(byte[] key, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.set(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 从指定位置开始插入数据，插入的数据会覆盖指定位置以后的数据
		 * @param key
		 * @param offset 位置
		 * @param value 值
		 * @return long value的长度
		 * */
		public long setrange(String key, long offset, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.setrange(key, offset, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从指定位置开始插入数据，插入的数据会覆盖指定位置以后的数据
		 * @param key
		 * @param offset 位置
		 * @param value 值
		 * @return long value的长度
		 * */
		public long setrange(byte[] key, long offset, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.setrange(key, offset, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 在指定的key中追加value
		 * @param key
		 * @param value
		 * @return long 追加后value的长度
		 * **/
		public long append(String key, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.append(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 在指定的key中追加value
		 * @param key
		 * @param value
		 * @return long 追加后value的长度
		 * **/
		public long append(byte[] key, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.append(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 将key对应的value减去指定的值，只有value可以转为数字时该方法才可用
		 * @param key
		 * @param long number 要减去的值
		 * @return long 减去指定值后的值
		 * */
		public long decrBy(String key, long number) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.decrBy(key, number);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 将key对应的value加上指定的值，只有value可以转为数字时该方法才可用
		 * @param key
		 * @param long number 要相加的值
		 * @return long 相加定值后的值
		 * */
		public long incrBy(String key, long number) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.incrBy(key, number);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 将key对应的value加上指定的值，只有value可以转为数字时该方法才可用
		 * @param key
		 * @param long number 要减去的值
		 * @return long 相加后的值
		 * */
		public double incrByFloat(String key, double number) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.incrByFloat(key, number);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 对指定key对应的value进行截取(相当于substring)
		 * @param key
		 * @param long startOffset 开始位置(包含)
		 * @param long endOffset 结束位置(包含)
		 * @return String 截取的值
		 * */
		public String getrange(String key, long startOffset, long endOffset) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.getrange(key, startOffset, endOffset);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获取key原来的value并写入新的value
		 * @param key
		 * @param value
		 * @return 原来的value
		 * */
		public String getset(String key, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.getSet(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 批量获取记录,如果指定的key不存在返回List的对应位置将是null
		 * @param keys
		 * @return List<String> 值得集合
		 * */
		public List<String> mget(String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.mget(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 批量获取记录,如果指定的key不存在返回List的对应位置将是null
		 * @param keys
		 * @return List<byte[]> 值得集合
		 * */
		public List<byte[]> mget(byte[]... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.mget(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 批量获取记录,如果指定的key不存在返回List的对应位置将是null
		 * @param keys
		 * @return List<byte[]> 值得集合
		 * */
		public List<byte[]> mget(Set<String> keys) {
			int size = keys.size();
			byte[][] keyByteArr = new byte[size][];
			int i = 0;
			for (String key : keys) {
				keyByteArr[i] = key.getBytes();
				i += 1;
			}
			return mget(keyByteArr);
		}

		/**
		 * 批量获取记录,如果指定的key不存在返回List的对应位置将是null
		 * @param keys
		 * @return List<byte[]> 值得集合
		 * */
		public List<byte[]> mget(List<String> keys) {
			int size = keys.size();
			byte[][] keyByteArr = new byte[size][];
			int i = 0;
			for (String key : keys) {
				keyByteArr[i] = key.getBytes();
				i += 1;
			}
			return mget(keyByteArr);
		}

		/**
		 * 批量存储记录
		 * @param keysvalues 例:keysvalues="key1","value1","key2","value2";
		 * @return String 状态码
		 * */
		public String mset(String... keysvalues) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.mset(keysvalues);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 批量存储记录
		 * @param keysvalues
		 * @return
		 */
		public String mset(Map<String, String> keysvalues) {
			if (keysvalues == null || keysvalues.isEmpty()) {
				logger.warn("keysvalues is empty");
				return null;
			}
			String[] keysvaluesArray = new String[keysvalues.size() * 2];
			int i = 0;
			for (Entry<String, String> e : keysvalues.entrySet()) {
				keysvaluesArray[i] = e.getKey();
				i++;
				keysvaluesArray[i] = e.getValue();
				i++;
			}
			return mset(keysvaluesArray);
		}

		/**
		 * 批量存储记录，并设置过期时间
		 * @param keysvalues
		 * @param seconds
		 */
		public void msetex(Map<byte[], byte[]> keysvalues, int seconds) {
			if (keysvalues == null || keysvalues.isEmpty()) {
				logger.warn("keysvalues is empty");
				return;
			}

			int i = 0;
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				Pipeline pipeline = jedis.pipelined();
				for (Entry<byte[], byte[]> e : keysvalues.entrySet()) {
					pipeline.setex(e.getKey(), seconds, e.getValue());
					// 由于管道一次性不能传输太多，所以要分开来
					if (++i % maxPipelineLen == 0) {
						pipeline.sync();
					}
				}
				pipeline.sync();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
		}

		/**
		 * 批量存储记录
		 * @param keysvalues
		 */
		public void mset(HashMap<byte[], byte[]> keysvalues) {
			if (keysvalues == null || keysvalues.isEmpty()) {
				logger.warn("keysvalues is empty");
				return;
			}

			int i = 0;
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				Pipeline pipeline = jedis.pipelined();
				for (Entry<byte[], byte[]> e : keysvalues.entrySet()) {
					pipeline.set(e.getKey(), e.getValue());
					// 由于管道一次性不能传输太多，所以要分开来
					if (++i % maxPipelineLen == 0) {
						pipeline.sync();
					}
				}
				pipeline.sync();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
		}

		/**
		 * 获取key对应的值的长度
		 * @param key
		 * @return value值得长度
		 * */
		public long strlen(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.strlen(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 对应给定的keys到set values上，只要有一个key已经存在，MSETNX一个操作都不会执行
		 * @param keysvalues
		 */
		public long msetnx(String... keysvalues) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.msetnx(keysvalues);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 对应给定的keys到set values上，只要有一个key已经存在，MSETNX一个操作都不会执行
		 * @param keysvalues
		 */
		public long msetnx(Map<String, String> keysvalues) {
			if (keysvalues == null || keysvalues.isEmpty()) {
				return 0;
			}
			String[] keysvaluesArray = new String[keysvalues.size() * 2];
			int i = 0;
			for (Entry<String, String> e : keysvalues.entrySet()) {
				keysvaluesArray[i] = e.getKey();
				i++;
				keysvaluesArray[i] = e.getValue();
				i++;
			}
			return msetnx(keysvaluesArray);
		}

		/**
		 * 添加有过期时间的记录
		 * @param key
		 * @param milliseconds 毫秒
		 * @param value
		 * @return
		 */
		public String psetex(String key, long milliseconds, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.psetex(key, milliseconds, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 添加有过期时间的记录
		 * @param key
		 * @param milliseconds 毫秒
		 * @param value
		 * @return
		 */
		public String psetex(byte[] key, long milliseconds, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.psetex(key, milliseconds, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class Lists {
		private Lists() {
		}

		/**
		 * List长度
		 * @param key
		 * @return 长度
		 * */
		public long llen(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.llen(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * List长度
		 * @param key
		 * @return 长度
		 * */
		public long llen(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.llen(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 设置 index 位置的list元素的值为 value
		 * @param key
		 * @param index 位置
		 * @param value 值
		 * @return 状态码
		 * */
		public String lset(byte[] key, int index, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lset(key, index, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 设置 index 位置的list元素的值为 value
		 * @param key
		 * @param index 位置
		 * @param value 值
		 * @return 状态码
		 * */
		public String lset(String key, int index, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lset(key, index, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 在value的相对位置插入记录
		 * @param key
		 * @param where 前面插入或后面插入
		 * @param pivot 相对位置的内容
		 * @param value 插入的内容
		 * @return 记录总数
		 * */
		public long linsert(String key, LIST_POSITION where, String pivot, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.linsert(key, where, pivot, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 在value的相对位置插入记录
		 * @param key
		 * @param where 前面插入或后面插入
		 * @param pivot 相对位置的内容
		 * @param value 插入的内容
		 * @return 记录总数
		 * */
		public long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.linsert(key, where, pivot, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 返回列表里的元素的索引 index 存储在 key 里面，下标是从0开始索引的，所以 0 是表示第一个元素， 1 表示第二个元素，并以此类推
		 * @param key
		 * @param index 位置
		 * @return 值
		 * **/
		public String lindex(String key, int index) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lindex(key, index);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回列表里的元素的索引 index 存储在 key 里面，下标是从0开始索引的，所以 0 是表示第一个元素， 1 表示第二个元素，并以此类推
		 * @param key
		 * @param index 位置
		 * @return 值
		 * **/
		public byte[] lindex(byte[] key, int index) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lindex(key, index);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 移除并且返回 key 对应的 list 从头开始的第一个元素
		 * @param key
		 * @return 移出的记录
		 * */
		public String lpop(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lpop(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 移除并且返回 key 对应的 list 从头开始的第一个元素
		 * @param key
		 * @return 移出的记录
		 * */
		public byte[] lpop(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lpop(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 移除并返回存于 key 的 list 从尾开始的最后一个元素。
		 * @param key
		 * @return 移出的记录
		 * */
		public String rpop(String key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpop(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 移除并返回存于 key 的 list 从尾开始的最后一个元素。
		 * @param key
		 * @return 移出的记录
		 * */
		public byte[] rpop(byte[] key) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpop(key);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 从队列的左边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long lpush(String key, String... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lpush(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从队列的左边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long lpush(byte[] key, byte[]... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lpush(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 当队列存在时，从队列的左边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long lpushx(String key, String... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lpushx(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 当队列存在时，从队列的左边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long lpushx(byte[] key, byte[]... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lpushx(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从队列的右边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long rpush(String key, String... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpush(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从队列的右边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long rpush(byte[] key, byte[]... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpush(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 当队列存在时，从队列的右边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long rpushx(String key, String... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpushx(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 当队列存在时，从队列的右边超入一个或多个元素
		 * @param key
		 * @param value
		 * @return 记录总数
		 * */
		public long rpushx(byte[] key, byte[]... value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpushx(key, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从列表中获取指定返回的元素
		 * @param key
		 * @param start
		 * @param end
		 * @return List
		 * */
		public List<String> lrange(String key, long start, long end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lrange(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 从列表中获取指定返回的元素
		 * @param key
		 * @param start
		 * @param end
		 * @return List
		 * */
		public List<byte[]> lrange(byte[] key, int start, int end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lrange(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 从存于 key 的列表里移除前 count 次出现的值为 value 的元素
		 * @param key
		 * @param count 要删除的数量，如果为负数则从List的尾部检查并删除符合的记录
		 * @param value 要匹配的值<br><ul><li>count > 0: 从头往尾移除值为 value 的元素。</li><li>count < 0: 从尾往头移除值为 value 的元素。</li><li>count = 0: 移除所有值为 value 的元素。</li></ul>
		 * @return 删除后的List中的记录数
		 * */
		public long lrem(byte[] key, long count, byte[] value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lrem(key, count, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 从存于 key 的列表里移除前 count 次出现的值为 value 的元素
		 * @param key
		 * @param count 要删除的数量，如果为负数则从List的尾部检查并删除符合的记录
		 * @param value 要匹配的值<br><ul><li>count > 0: 从头往尾移除值为 value 的元素。</li><li>count < 0: 从尾往头移除值为 value 的元素。</li><li>count = 0: 移除所有值为 value 的元素。</li></ul>
		 * @return 删除后的List中的记录数
		 * */
		public long lrem(String key, int count, String value) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.lrem(key, count, value);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}

		/**
		 * 修剪一个已存在的 list，这样 list 就会只包含指定范围的指定元素
		 * @param key
		 * @param start 记录的开始位置(0表示第一条记录)
		 * @param end 记录的结束位置（如果为-1则表示最后一个，-2，-3以此类推）
		 * @return 执行状态码
		 * */
		public String ltrim(byte[] key, int start, int end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.ltrim(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 修剪一个已存在的 list，这样 list 就会只包含指定范围的指定元素
		 * @param key
		 * @param start 记录的开始位置(0表示第一条记录)
		 * @param end 记录的结束位置（如果为-1则表示最后一个，-2，-3以此类推）
		 * @return 执行状态码
		 * */
		public String ltrim(String key, int start, int end) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.ltrim(key, start, end);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * LPOP的阻塞版本，当给定列表内没有任何元素可供弹出的时候， 连接将被阻塞；当给定多个 key 参数时，按参数 key 的先后顺序依次检查各个列表，弹出第一个非空列表的头元素
		 * @author ruan
		 * @param timeout 超时时间(秒)
		 * @param keys
		 * @return
		 */
		public List<String> blpop(int timeout, String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.blpop(timeout, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * LPOP的阻塞版本，当给定列表内没有任何元素可供弹出的时候， 连接将被阻塞；当给定多个 key 参数时，按参数 key 的先后顺序依次检查各个列表，弹出第一个非空列表的头元素
		 * @author ruan
		 * @param timeout 超时时间(秒)
		 * @param keys
		 * @return
		 */
		public List<byte[]> blpop(int timeout, byte[]... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.blpop(timeout, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * RPOP的阻塞版本，当给定列表内没有任何元素可供弹出的时候， 连接将被阻塞；当给定多个 key 参数时，按参数 key 的先后顺序依次检查各个列表，弹出第一个非空列表的头元素
		 * @author ruan
		 * @param timeout 超时时间(秒)
		 * @param keys
		 * @return
		 */
		public List<String> brpop(int timeout, String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.brpop(timeout, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * RPOP的阻塞版本，当给定列表内没有任何元素可供弹出的时候， 连接将被阻塞；当给定多个 key 参数时，按参数 key 的先后顺序依次检查各个列表，弹出第一个非空列表的头元素
		 * @author ruan
		 * @param timeout 超时时间(秒)
		 * @param keys
		 * @return
		 */
		public List<byte[]> brpop(int timeout, byte[]... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.brpop(timeout, keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 删除列表中的最后一个元素，将其追加到另一个列表
		 * @author ruan
		 * @param srckey 原list
		 * @param dstkey 目标list
		 * @return
		 */
		public String rpoplpush(String srckey, String dstkey) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpoplpush(srckey, dstkey);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 删除列表中的最后一个元素，将其追加到另一个列表
		 * @author ruan
		 * @param srckey 原list
		 * @param dstkey 目标list
		 * @return
		 */
		public byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.rpoplpush(srckey, dstkey);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * rpoplpush的阻塞版本，当 source 是空的时候，Redis将会阻塞这个连接，直到另一个客户端 push 元素进入或者达到 timeout 时限， timeout 为 0 能用于无限期阻塞客户端
		 * @author ruan
		 * @param source 原list
		 * @param destination 目标list
		 * @param timeout 超时时间(秒)
		 * @return
		 */
		public String brpoplpush(String source, String destination, int timeout) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.brpoplpush(source, destination, timeout);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * rpoplpush的阻塞版本，当 source 是空的时候，Redis将会阻塞这个连接，直到另一个客户端 push 元素进入或者达到 timeout 时限， timeout 为 0 能用于无限期阻塞客户端
		 * @author ruan
		 * @param source 原list
		 * @param destination 目标list
		 * @param timeout 超时时间(秒)
		 * @return
		 */
		public byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.brpoplpush(source, destination, timeout);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class PubSub {
		private PubSub() {
		}

		/**
		 * 指定模式订阅频道
		 * @param jedisPubSub 发布订阅监听器
		 * @param patterns 模式
		 */
		public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
			jedisPool.getResource().psubscribe(jedisPubSub, patterns);
		}

		/**
		 * 订阅频道
		 * @param jedisPubSub 发布订阅监听器
		 * @param channels 频道名
		 */
		public void subscribe(JedisPubSub jedisPubSub, String... channels) {
			jedisPool.getResource().subscribe(jedisPubSub, channels);
		}

		/**
		 * 发布一条消息
		 * @param channel 频道名
		 * @param message 消息内容
		 * @return 收到消息的客户端数量
		 */
		public long publish(String channel, String message) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.publish(channel, message);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0L;
		}

		/**
		 * 列出指定模式的活跃频道
		 * @param pattern 模式
		 * @return
		 */
		public List<String> pubsubChannels(String pattern) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.pubsubChannels(pattern);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 列出所有活跃频道
		 */
		public List<String> pubsubChannels() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.pubsubChannels("");
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回给定频道的订阅者数量，订阅模式的客户端不计算在内。
		 * @param channels
		 * @return
		 */
		public Map<String, String> pubsubNumSub(String... channels) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.pubsubNumSub(channels);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回客户端订阅的所有模式的数量总和
		 */
		public long pubsubNumPat() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.pubsubNumPat();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0;
		}
	}

	public class Transactions {
		private Transactions() {
		}

		/**
		 * 监视一个(或多个) key ，如果在事务执行之前这个(或这些) key 被其他命令所改动，那么事务将被打断。
		 * @param keys
		 * @return
		 */
		public String watch(String... keys) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.watch(keys);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 取消对所有 key 的监视
		 */
		public String unwatch() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.unwatch();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 标记一个事务块的开始<br>
		 * 事务块内的多条命令会按照先后顺序被放进一个队列当中，最后由 EXEC 命令原子性(atomic)地执行
		 */
		public Transaction multi() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.multi();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class Scripts {
		private Scripts() {
		}

		/**
		 * 将脚本 script 添加到脚本缓存中，但并不立即执行这个脚本
		 * @param script 脚本
		 * @return
		 */
		public String load(String script) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scriptLoad(script);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 校验所指定的脚本是否已经被保存在缓存当中
		 * @param sha1  脚本的 SHA1 校验和
		 * @return
		 */
		public boolean exists(String sha1) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scriptExists(sha1);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return false;
		}

		/**
		 * 校验所指定的脚本是否已经被保存在缓存当中
		 * @param sha1  脚本的 SHA1 校验和
		 * @return
		 */
		public List<Boolean> exists(String... sha1) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scriptExists(sha1);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 清除所有 Lua 脚本缓存
		 */
		public String flush() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scriptFlush();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 杀死当前正在运行的 Lua 脚本，当且仅当这个脚本没有执行过任何写操作时，这个命令才生效
		 */
		public String kill() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.scriptKill();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 根据给定的 sha1 校验码，对缓存在服务器中的脚本进行求值
		 * @param sha1 sha1 校验码
		 * @return
		 */
		public Object evalsha(String sha1) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.evalsha(sha1);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 根据给定的 sha1 校验码，对缓存在服务器中的脚本进行求值
		 * @param sha1 sha1 校验码
		 * @param keys 键列表
		 * @param args 参数列表
		 * @return
		 */
		public Object evalsha(String sha1, List<String> keys, List<String> args) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.evalsha(sha1, keys, args);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 根据给定的 sha1 校验码，对缓存在服务器中的脚本进行求值
		 * @param sha1 sha1 校验码
		 * @param keyCount 键的数量
		 * @param params 参数
		 * @return
		 */
		public Object evalsha(String sha1, int keyCount, String... params) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.evalsha(sha1, keyCount, params);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 执行一个lua脚本
		 * @param script 脚本内容
		 * @return
		 */
		public Object eval(String script) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.eval(script);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 执行一个lua脚本
		 * @param sha1 sha1 校验码
		 * @param keyCount 键的数量
		 * @param params 参数
		 * @return
		 */
		public Object eval(String sha1, int keyCount, String... params) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.eval(sha1, keyCount, params);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 执行一个lua脚本
		 * @param script 脚本内容
		 * @param keys 键列表
		 * @param args 参数列表
		 * @return
		 */
		public Object eval(String script, List<String> keys, List<String> args) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.eval(script, keys, args);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class Connection {
		private Connection() {
		}

		/**
		 * 验证密码
		 * @author ruan
		 * @param password
		 * @return
		 */
		public String auth(String password) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.auth(password);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * ping服务器
		 * @author ruan
		 * @return
		 */
		public String ping() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.ping();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 输出一个字符串
		 * @author ruan
		 * @param string
		 * @return
		 */
		public String echo(String string) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.echo(string);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 选择一个数据库
		 * @author ruan
		 * @param index
		 * @return
		 */
		public String select(int index) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.select(index);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 请求退出服务器
		 * @author ruan
		 * @return
		 */
		public String quit() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.quit();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}

	public class Server {
		private Server() {
		}

		/**
		 * 删除所有数据库的所有 key
		 */
		public String flushAll() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.flushAll();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 清空当前数据库中的所有 key
		 */
		public String flushDB() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.flushDB();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回当前数据库的 key 的数量
		 */
		public long dbSize() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.dbSize();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return 0L;
		}

		/**
		 * 返回服务器信息
		 */
		public Map<String, String> info() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				String[] infoArray = jedis.info().trim().split("\\r\\n");
				Map<String, String> infoMap = new HashMap<String, String>();
				for (String info : infoArray) {
					String[] arr = info.split(":");
					if (arr.length != 2) {
						continue;
					}
					infoMap.put(arr[0].trim(), arr[1].trim());
				}
				return infoMap;
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 返回当前服务器时间
		 * @author ruan
		 * @return
		 * 返回内容包含两个元素<br>
		 * <li>UNIX时间戳（单位：秒）</li>
		 * <li>微秒</li>
		 */
		public List<String> time() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.time();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 关闭客户端连接
		 * @author ruan
		 * @param client ip:port
		 * @return
		 */
		public String clientKill(String client) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.clientKill(client);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获得客户端连接列表
		 * @author ruan
		 * @return
		 */
		public String clientList() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.clientList();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获得当前连接名称
		 * @author ruan
		 * @return
		 */
		public String clientGetname() {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.clientGetname();
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 设置当前连接名称
		 * @author ruan
		 * @param name
		 * @return
		 */
		public String clientSetname(String name) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.clientSetname(name);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}

		/**
		 * 获取配置参数的值
		 * @author ruan
		 * @param pattern
		 * @return
		 */
		public List<String> configGet(String pattern) {
			Jedis jedis = null;
			try {
				jedis = jedisPool.getResource();
				return jedis.configGet(pattern);
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				jedis.close();
			}
			return null;
		}
	}
}