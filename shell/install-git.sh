#linux git
#运行例子：sh install-git.sh /usr/local
 
#定义本程序的当前目录
base_path=$(pwd)
ntpdate time.nist.gov

#处理外部参数
git_install_path=$1
if [ ! $git_install_path ]; then
	echo 'error command!!! you must input git install path...'
	echo 'for example: sh install-git.sh /usr/local'
	exit
fi

#建立临时安装目录
echo 'preparing working path...'
install_path='/install'
rm -rf $install_path
mkdir -p $install_path

yum -y install curl curl-devel zlib-devel openssl-devel perl cpio expat-devel gettext-devel gcc libc6-dev gcc-c++ pcre-devel libgd2-xpm libgd2-xpm-dev geoip-database libgeoip-dev make libxslt-dev rsync lrzsz

cd $install_path

if [ ! -d $git_install_path/autoconf ]; then
	autoconf='autoconf-2.69'
	if [ ! -f $base_path/$autoconf.tar.gz ]; then
		wget -O $base_path/$autoconf.tar.gz http://ftp.gnu.org/gnu/autoconf/$autoconf.tar.gz || exit
	fi
	tar zxvf $base_path/$autoconf.tar.gz -C $install_path || exit
	cd $install_path/$autoconf
	./configure --prefix=$git_install_path/autoconf && make && make install || exit
	yes|cp $git_install_path/autoconf/bin/* /usr/bin/
fi

if [ ! -f $base_path/git-master.zip ]; then
	echo 'git.zip is not exists, system will going to download it...'
	wget -O $base_path/git-master.zip https://github.com/git/git/archive/master.zip || exit
	echo 'download git.zip finished...'
fi
unzip $base_path/git-master.zip -d $install_path || exit

cd $install_path/git-master
autoconf && ./configure --prefix=$git_install_path/git && make && make install || exit 
yes|cp -rf $git_install_path/git/bin/* /usr/bin/
git --version
