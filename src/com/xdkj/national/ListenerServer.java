package com.xdkj.national;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xdkj.national.codec.FrameCodecFactory;
import com.xdkj.util.Property;

public class ListenerServer {
	
	private static final Logger logger = LoggerFactory.getLogger(ListenerServer.class);
	
	public static void main(String[] arg){
		logger.info("start");
		
		IoAcceptor acceptor = new NioSocketAcceptor();
		
		
		acceptor.getSessionConfig().setReadBufferSize(2048);
		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 300);
//		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 6);
		
		acceptor.getFilterChain().addLast("protocol", new ProtocolCodecFilter(new FrameCodecFactory()));
		
		acceptor.setHandler(new DataHandler());
		try {
			String ip = Property.getValue("ip").trim();
			String port = Property.getValue("port").trim();
			logger.info("ip:"+ip);
			logger.info("port:"+port);
			acceptor.bind(new InetSocketAddress(ip,Integer.parseInt(port)));
		} catch (Exception e) {
			logger.error("监听启动时异常！");
			e.printStackTrace();
		}
		
		//可以监听多个端口
//		IoAcceptor acceptor2 = new NioSocketAcceptor();
//		
//		
//		acceptor2.setHandler(new DataHandler());
//		
//		acceptor2.getSessionConfig().setReadBufferSize(1024);
//		acceptor2.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 600);
//		
//		try {
//			acceptor2.bind(new InetSocketAddress(Property.getValue("ip").trim(),4444));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

}
