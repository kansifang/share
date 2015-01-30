#linux mongodb自动安装程序 
#运行例子：sh install-redis.sh 2.8.17 /usr/local

#定义本程序的当前目录
base_path=$(pwd)
ntpdate time.nist.gov

#处理外部参数
redis_version=$1
redis_install_path=$2
if [ ! $redis_version ] || [ ! $redis_install_path ] ; then
	echo 'error command!!! you must input redis version and install path...'
	echo 'for example: sh install-redis.sh 2.8.17 /usr/local'
	exit
fi

#建立临时安装目录
echo 'preparing working path...'
install_path='/install'
rm -rf $install_path
mkdir -p $install_path

if [ ! -d $redis_install_path/redis ]; then
	if [ ! -f $base_path/redis-$redis_version.tar.gz ]; then
		echo 'redis-'$redis_version'.tar.gz is not exists, system will going to download it...'
		wget -O $base_path/redis-$redis_version.tar.gz http://download.redis.io/releases/redis-$redis_version.tar.gz || exit
		echo 'download redis-'$redis_version'.tar.gz finished...'
	fi
	tar zxvf $base_path/redis-$redis_version.tar.gz -C $redis_install_path || exit
	mv $redis_install_path/redis-$redis_version $redis_install_path/redis
	cd $redis_install_path/redis
	make
	echo "daemonize yes 	
pidfile "$redis_install_path"/redis/redis.pid 
port 6379 				
timeout 5	 				
databases 16 						
maxclients 1000 					
dir "$redis_install_path"/redis/ 		
syslog-enabled no 					
slowlog-log-slower-than -1 	
appendonly no
auto-aof-rewrite-percentage 0
#requirepass admin" > $redis_install_path/redis/redis.conf
fi

#开放防火墙端口
/sbin/iptables -I INPUT -p tcp --dport 6379 -j ACCEPT && /etc/rc.d/init.d/iptables save && service iptables restart

#开机启动redis
yes|cp -rf $redis_install_path'/redis/src/redis-server' /usr/bin/
yes|cp -rf $redis_install_path'/redis/src/redis-cli' /usr/bin/
echo 'redis-server '$redis_install_path'/redis/redis.conf' >> /etc/rc.local
source /etc/rc.local