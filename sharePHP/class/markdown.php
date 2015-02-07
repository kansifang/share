<?php
/**
 * md文件解析类
 */
class markdown{
	/**
	 * 输出的html代码(js部分与依赖jquery，请自行引入)
	 */
	private $html = '<style>*{color:#000}table th,table td{padding:10px}table th{background:#ccc}table{margin-left:45px;width:100%;border-top:1px solid #ccc;border-left:1px solid #ccc}table th,table td{border-bottom:1px solid #ccc;border-right:1px solid #ccc;}h1,h2,h3,h4,h5,h6{margin:20px;font-weight:bold}h1{font-size:32px}h2{font-size:28px}h3{font-size:24px}h4{font-size:20px}h5{font-size:16px}h6{font-size:16px}p{margin-left:45px}.anchor{display:none;margin-top:5px;margin-right:5px;float:left;width:18px;height:10px;background:url(\'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAAKCAIAAAA2KZn2AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAAuklEQVQokZ2RLRKDMBSElypukNwAHJKjgEMiOUZwHKG9QXCVSCQSHLYO3MNtRRvolHb6sybzzbx9b2fjkcTvOtzfZajLNNTeJh2mZT3M83BK9YqL85GktCZ+szY2rUhTqAckSZDsqghAnNtRuElGm8cAoqrbI5wrs9NtvDMKQH4WUppi85GTzRwe9rl8DcD3V74s+5kXIb/J/FyJKhr50JCrhCSltyYJFACVHPtpWtFJBYmxvbvv/ffdVycYutMLAImQAAAAAElFTkSuQmCC\') no-repeat}</style>';
	/**
	 * 文件句柄
	 */
	private $handle = null;
	/**
	 * 解析表格用到的参数变量集合
	 */
	private $table = array();
	
	/**
	 * 构造函数
	 * @param $file 文件路径
	 */
	public function __construct($file){
		if(!is_file($file)){
			echo_('file '.$file.' not exists...', true);
		}
		$this->handle = fopen($file, 'r');
		if(!$this->handle) {
			echo_('can not read file '.$file, true);
		}
	}
	
	/**
	 * 解析
	 */
	public function parse(){
		while (($buffer = fgets($this->handle)) !== false) {
			$buffer = trim($buffer);
			
			// 解析标题
			$this->parse_title($buffer);
			
			// 解析表格
			$this->parse_table($buffer);
			
			// 解析文本
			$this->parse_content($buffer);
			
			// 解析分割线
			$this->parse_hr($buffer);
		}
		fclose($this->handle);
		return $this->html;
	}
	
	/**
	 * 是否为标题
	 * @param $buffer 文件流
	 */
	private function is_title($buffer){
		$h = intval(substr_count($buffer, '#'));
		return $h > 0 && $h <= 6;
	}
	
	/**
	 * 是否为分割线
	 * @param $buffer 文件流
	 */
	private function is_hr($buffer){
		$bool = false;
		$bool = $bool || is_int(strpos($buffer, '---'));
		$bool = $bool || is_int(strpos($buffer, '***'));
		return $bool;
	}
	
	/**
	 * 解析标题
	 * @param $buffer 文件流
	 */
	private function parse_title($buffer){
		if(!$this->is_title($buffer)) {
			return;
		}
		$h = intval(substr_count($buffer, '#'));
		$buffer = trim(str_replace('#', '', $buffer));
		$id = md5($buffer.uniqid());
		$this->html .= '<h'.$h.' id="'.$id.'" onMouseOver="$(\'#'.$id.' .anchor\').css(\'display\',\'block\')" onMouseOut="$(\'#'.$id.' .anchor\').css(\'display\',\'none\')"><a href="#'.$id.'"><div class="anchor"></div></a><div style="margin-left:25px">'.$this->parse_link($buffer).'</div></h'.$h.'>';
	}
	
	/**
	 * 解析分割线
	 * @param $buffer 文件流
	 */
	private function parse_hr($buffer){
		if(!$this->is_hr($buffer) || $this->table['open']){
			return;
		}
		$this->html .= '<hr>';
	}
	
	/**
	 * 解析文本
	 * @param $buffer 文件流
	 */
	private function parse_content($buffer){
		$bool = true;
		$bool = $bool && !$this->is_title($buffer);
		$bool = $bool && !$this->is_hr($buffer);
		$bool = $bool && !$this->table['open'];
		if(!$bool){
			return;
		}
		$this->html .= '<p>'.$this->parse_link($buffer).'</p>';
	}
	
	/**
	 * 解析超链接
	 * @param $buffer 文件流
	 */
	private function parse_link($buffer){
		return trim(preg_replace('/\[(.*)\]\((.*)\)/', '<a href="$2">$1</a>', $buffer));
	}
	
	/**
	 * 解析表格
	 * @param $buffer 文件流
	 */
	private function parse_table($buffer){
		$td_num = intval(substr_count($buffer, '|'));
		if($td_num <= 0){
			return;
		}
		$td_array = explode('|', $buffer);
		if(empty($td_array) || !is_array($td_array)){
			return;
		}
		if($this->table['open'] && feof($this->handle)){
			$this->table['close'] = true;
		}
		$size = count($td_array);
		if(!$this->table['open']){
			$this->table['open'] = true;
			$this->html .= '<table>';
		}
		if($this->table['open'] && !$this->table['head']){
			$this->table['head'] = true;
			$this->html .= '<thead><tr>';
			for($i = 0; $i < $size; $i++){
				$this->html .= '<th>'.trim($td_array[$i]).'</th>';
			}
			$this->html .= '</tr></thead>';
		}
		if($this->table['open'] && $this->is_hr($buffer)){
			$this->table['body'] = true;
			$this->html .= '<tbody>';
		}
		if($this->table['body']){
			if($this->is_hr($buffer)){
				return;
			}
			$this->html .= '<tr>';
			for($i = 0; $i < $size; $i++){
				$this->html .= '<td>'.trim($td_array[$i]).'</td>';
			}
			$this->html .= '</tr>';
		}
		if($this->table['close']){
			$this->html .= '</tbody></table>';
		}
	}
}
?>