#linux nsq
#运行例子：sh install-nsq.sh
 
#定义本程序的当前目录
base_path=$(pwd)
ntpdate ntp.api.bz

#建立临时安装目录
echo 'preparing working path...'
install_path='/install'
rm -rf $install_path
mkdir -p $install_path
nsq_install_path='/usr/local' 

# 安装nsq 
nsq_version='nsq-0.3.6.linux-amd64.go1.5.1'
rm -rf /usr/bin/nsq*
if [ ! -f $base_path/nsq.tar.gz ]; then
	wget -O $base_path/nsq.tar.gz https://s3.amazonaws.com/bitly-downloads/nsq/$nsq_version.tar.gz
fi

tar zxvf $base_path/nsq.tar.gz -C $install_path || exit

rm -rf $nsq_install_path
mkdir -p $nsq_install_path/nsq
mkdir -p $nsq_install_path/nsq/data

mv $install_path/$nsq_version/ $install_path/nsq
mv $install_path/nsq/* $nsq_install_path/nsq/
chmod -R 777 $nsq_install_path/nsq/bin/*
yes | cp -rf $nsq_install_path/nsq/bin/* /usr/bin/

#生成启动nsq脚本
rm -rf $nsq_install_path/nsq/start_nsq.sh
echo 'cd '$nsq_install_path'/nsq/' >> $nsq_install_path/nsq/start_nsq.sh
ifconfig > 'tmp'
temp=$(awk '{print;exit}' tmp | cut -f 1 -d ":")
echo "#!/bin/bash" >> $nsq_install_path/nsq/start_nsq.sh
echo "list=\$(ps -ef|grep nsq|grep start_nsq|grep -v grep|awk '{print \$2}')" >> $nsq_install_path/nsq/start_nsq.sh
echo "for proc in \$list" >> $nsq_install_path/nsq/start_nsq.sh
echo "do" >> $nsq_install_path/nsq/start_nsq.sh
echo "	kill -15 \$proc" >> $nsq_install_path/nsq/start_nsq.sh
echo "done" >> $nsq_install_path/nsq/start_nsq.sh
echo 'ip=$(ifconfig '$temp' | grep "netmask" | cut -b 14-26)' >> $nsq_install_path/nsq/start_nsq.sh
echo 'nsqlookupd &' >> $nsq_install_path/nsq/start_nsq.sh
echo 'sleep 2' >> $nsq_install_path/nsq/start_nsq.sh
echo 'nsqd --lookupd-tcp-address=$ip:4160 --broadcast-address=$ip &' >> $nsq_install_path/nsq/start_nsq.sh
echo 'sleep 2' >> $nsq_install_path/nsq/start_nsq.sh
echo 'nsqadmin --lookupd-http-address=$ip:4161 -http-address=$ip:4171 &' >> $nsq_install_path/nsq/start_nsq.sh
echo 'sleep 2' >> $nsq_install_path/nsq/start_nsq.sh
echo 'nsq_to_file --output-dir='$nsq_install_path'/nsq/data --lookupd-http-address=$ip:4161 &' >> $nsq_install_path/nsq/start_nsq.sh
chmod 777 $nsq_install_path/nsq/start_nsq.sh
rm -rf 'tmp'

$nsq_install_path/nsq/start_nsq.sh