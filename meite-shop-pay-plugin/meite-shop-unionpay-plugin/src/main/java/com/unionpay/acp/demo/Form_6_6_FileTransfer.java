package com.unionpay.acp.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.unionpay.acp.sdk.AcpService;
import com.unionpay.acp.sdk.LogUtil;
import com.unionpay.acp.sdk.SDKConfig;

/**
 * 重要：联调测试时请仔细阅读注释！
 * 
 * 产品：跳转网关支付产品<br>
 * 交易：文件传输类接口：后台获取对账文件交易，只有同步应答 <br>
 * 日期： 2015-09<br>
 
 * 版权： 中国银联<br>
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 * 该接口参考文档位置：open.unionpay.com帮助中心 下载  产品接口规范  《网关支付产品接口规范》<br>
 *              《平台接入接口规范-第5部分-附录》（内包含应答码接口规范，全渠道平台银行名称-简码对照表）<br>
 *              《全渠道平台接入接口规范 第3部分 文件接口》（对账文件格式说明）<br>
 * 测试过程中的如果遇到疑问或问题您可以：1）优先在open平台中查找答案：
 * 							        调试过程中的问题或其他问题请在 https://open.unionpay.com/ajweb/help/faq/list 帮助中心 FAQ 搜索解决方案
 *                             测试过程中产生的6位应答码问题疑问请在https://open.unionpay.com/ajweb/help/respCode/respCodeList 输入应答码搜索解决方案
 *                          2） 咨询在线人工支持： open.unionpay.com注册一个用户并登陆在右上角点击“在线客服”，咨询人工QQ测试支持。
 * 交易说明： 对账文件的格式请参考《全渠道平台接入接口规范 第3部分 文件接口》
 *        对账文件示例见目录file下的802310048993424_20150905.zip
 *        解析落地后的对账文件可以参考BaseDemo.java中的parseZMFile();parseZMEFile();方法
 *        
 */
public class Form_6_6_FileTransfer extends HttpServlet {

	@Override
	public void init(ServletConfig config) throws ServletException {
		/**
		 * 请求银联接入地址，获取证书文件，证书路径等相关参数初始化到SDKConfig类中
		 * 在java main 方式运行时必须每次都执行加载
		 * 如果是在web应用开发里,这个方法可使用监听的方式写入缓存,无须在这出现
		 */
		//这里已经将加载属性文件的方法挪到了web/AutoLoadServlet.java中
		//SDKConfig.getConfig().loadPropertiesFromSrc(); //从classpath加载acp_sdk.properties文件
		super.init();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String merId = req.getParameter("merId");
		String settleDate = req.getParameter("settleDate");
		
		Map<String, String> data = new HashMap<String, String>();
		
		/***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
		data.put("version", UnionPayBase.version);               //版本号 全渠道默认值
		data.put("encoding", UnionPayBase.encoding);             //字符集编码 可以使用UTF-8,GBK两种方式
		data.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
		data.put("txnType", "76");                           //交易类型 76-对账文件下载
		data.put("txnSubType", "01");                        //交易子类型 01-对账文件下载
		data.put("bizType", "000000");                       //业务类型，固定
		
		/***商户接入参数***/
		data.put("accessType", "0");                         //接入类型，商户接入填0，不需修改
		data.put("merId", merId);                	         //商户代码，请替换正式商户号测试，如使用的是自助化平台注册的777开头的商户号，该商户号没有权限测文件下载接口的，请使用测试参数里写的文件下载的商户号和日期测。如需777商户号的真实交易的对账文件，请使用自助化平台下载文件。
		data.put("settleDate", settleDate);                  //清算日期，如果使用正式商户号测试则要修改成自己想要获取对账文件的日期， 测试环境如果使用700000000000001商户号则固定填写0119
		data.put("txnTime",UnionPayBase.getCurrentTime());       //订单发送时间，取系统时间，格式为YYYYMMDDhhmmss，必须取当前时间，否则会报txnTime无效
		data.put("fileType", "00");                          //文件类型，一般商户填写00即可
		
		/**请求参数设置完毕，以下对请求参数进行签名并发送http post请求，接收同步应答报文------------->**/
		
		Map<String, String> reqData = AcpService.sign(data,UnionPayBase.encoding);//报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
		String url = SDKConfig.getConfig().getFileTransUrl();//获取请求银联的前台地址：对应属性文件acp_sdk.properties文件中的acpsdk.fileTransUrl
		Map<String, String> rspData =  AcpService.post(reqData,url,UnionPayBase.encoding);

		/**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
		//应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
		String fileContentDispaly = "";
		if(!rspData.isEmpty()){
			if(AcpService.validate(rspData, UnionPayBase.encoding)){
				LogUtil.writeLog("验证签名成功");
				String respCode = rspData.get("respCode");
				if("00".equals(respCode)){
					String outPutDirectory ="d:\\";
					// 交易成功，解析返回报文中的fileContent并落地
					String zipFilePath = AcpService.deCodeFileContent(rspData,outPutDirectory,UnionPayBase.encoding);
					//对落地的zip文件解压缩并解析
					List<String> fileList = UnionPayBase.unzip(zipFilePath, outPutDirectory);
					//解析ZM，ZME文件
					fileContentDispaly ="<br>获取到商户对账文件，并落地到"+outPutDirectory+",并解压缩 <br>";
					for(String file : fileList){
						if(file.indexOf("ZM_")!=-1){
							List<Map> ZmDataList = UnionPayBase.parseZMFile(file);
							fileContentDispaly = fileContentDispaly+UnionPayBase.getFileContentTable(ZmDataList,file);
						}else if(file.indexOf("ZME_")!=-1){
							UnionPayBase.parseZMEFile(file);
						}
					}
					//TODO
				}else{
					//其他应答码为失败请排查原因
					//TODO
				}
			}else{
				LogUtil.writeErrorLog("验证签名失败");
				//TODO 检查验证签名失败的原因
			}
		}else{
			//未返回正确的http状态
			LogUtil.writeErrorLog("未获取到返回报文或返回http状态码非200");
		}
		
		String reqMessage = UnionPayBase.genHtmlResult(reqData);
		String rspMessage = UnionPayBase.genHtmlResult(rspData);
		resp.getWriter().write("</br>请求报文:<br/>"+reqMessage+"<br/>" + "应答报文:</br>"+rspMessage+fileContentDispaly);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

}
