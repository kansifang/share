<?php require view::dir().'head.php';?><?php view::load_js('xheditor/xheditor-1.2.1.min');?><?php view::load_js('xheditor/zh-cn');?><?php view::load_js('uploadify/jquery.uploadify.min');?><?php view::load_css('uploadify');?><script>$(function(){$('#xheditor').xheditor({tools:'Cut,Copy,Paste,Fontface,FontSize,Bold,Italic,Underline,Strikethrough,FontColor,Align,Link,Unlink,Img,Flash,Media,Emot',urlType:'rel',width:500,height:500,});$('#file_upload').uploadify({'formData': {'uid': <?php echo $uid;?>,'file': <?php echo $count;?>},'queueID': 'queue','method': 'post','auto': true,'fileSizeLimit': '500KB','fileTypeExts': '*.jpg;*.gif;*.png','multi': false,'swf': '<?php echo url().'skin/'.view::get_skin().'/js/uploadify/';?>uploadify.swf','uploader': '<?php echo $setting['server_url'][$setting['server']]?>uploadfrontcover','onUploadSuccess':function(file,data,response){var data = $.parseJSON(data);var html = '<img src="'+data.url+data.filename+'.'+data.filetype+'" width="120" height="120"/>';$('#file_upload').css('margin-left','130px');$('#queue').css('margin-left','130px');$('#preview').css('margin-bottom','10px');$('#preview').html(html);$('#file').attr('value',data.filename+'.'+data.filetype);}});})</script><div class="main"><div class="inner"><div class="container"><div class="do_box"><dl class="do_top"><dt><h1>发表经验</h1></dt><dd></dd></dl><div class="clear"></div><div class="do_list"><div class="center"><form method="post" action="<?php echo url('content','add')?>"><table width="100%" border="0" cellspacing="0" cellpadding="0" class="do_table"><tr><th>标题：</th><td><input type="text" class="do_input" name="title"/></td></tr><tr><th>类别：</th><td><select class="do_select" name="category"><option selected="selected">请选择</option><?php foreach($category as $c):?><option value="<?php echo $c['_id']?>"><?php echo $c['name']?></option><?php endforeach;?></select></td></tr><tr><th>封面：</th><td><div id="preview" style="float:left"></div><input id="file_upload" name="file_upload" type="file" multiple="false"><div id="queue"></div><input type="hidden" name="file" id="file"/></td></tr><tr><th>内容：</th><td><textarea rows="12" cols="80" class="do_textarea" id="xheditor" name="content"></textarea></td></tr><tr><th>&nbsp;</th><td><button class="btn_login">发表</button></td></tr></table></form><br/><br/><br/></div></div></div></div></div></div><?php require view::dir().'foot.php';?>