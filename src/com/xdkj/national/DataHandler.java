package com.xdkj.national;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xdkj.hdlistener.dao.ListenerLogDao;
import com.xdkj.hdlistener.obj.ListenerLog;
import com.xdkj.national.codec.Frame;


public class DataHandler extends IoHandlerAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(DataHandler.class);
	
	private static final ConcurrentHashMap<String, IoSession> gprs = new ConcurrentHashMap<>(2048);
	private static final ConcurrentHashMap<String, IoSession> pc = new ConcurrentHashMap<>();
	
	@Override
	public void exceptionCaught(IoSession session,Throwable cause){
//		cause.printStackTrace();
		session.close(true);
		logger.error("MINA错误", cause);
	}
	
	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {
		
		if(session.getIdleCount(status) == 1){
			session.close(true);
		}
		
	}
	
	@Override
	public void messageReceived(IoSession session,Object message){
		
		Frame frame = (Frame) message;
//		byte from = frame.getDir();
		if(session.getAttribute("online") == null){
			session.setAttribute("from", frame.getDir());
			session.setAttribute("addr", frame.getAddrstr());
		}
		
		session.setAttribute("online", true);
		
		if(frame.getDir() == 0x01){
			//from gprs
			switch(frame.getAfn()){
			case 0x02:
				//链路接口
				switch(frame.getFn()){
				case 0x01:
					//登录
					gprs.put(frame.getAddrstr(), session);
					//确认
					session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x01,frame.getAddr(),new byte[0]));
					ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "0", "1", "",session.getRemoteAddress().toString()));
					break;
				case 0x02:
					//退出
					session.close(true);
					break;
				case 0x03:
					//心跳
					gprs.put(frame.getAddrstr(), session);
					session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x01,frame.getAddr(),new byte[0]));
					System.out.println(frame.getAddrstr()+"HeartBeat"+Calendar.getInstance().getTime().toString());
					ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "0", "4", "",session.getRemoteAddress().toString()));
					break;
				}
				break;
			case 0x00:
				//确认否认
			case 0x03:
				//设置参数  由PC发出  集中器返回确认否认帧
			case 0x04:
				//控制命令  由PC发出  集中器返回确认否认帧
			case 0x0A:
				//查询参数  由PC发出  集中器返回数据帧
			case 0x0B:
				//实时数据   由PC发出  集中器返回数据帧
			case 0x0C:
				//历史数据   由PC发出  集中器返回数据帧
				//集中器发送过来的数据  发给PC
				IoSession send = null;
				send = pc.get(frame.getAddrstr());
				if(send != null && (boolean)send.getAttribute("online")){
					send.write(message);
					ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "0", "3", byteArrayToHexStr(frame.getFrame(),frame.getFrame().length),session.getRemoteAddress().toString()));
				}
				break;
				
			}
			
		}else{
			//from pc
			switch(frame.getAfn()){
			case 0x02:
				//链路接口
				switch(frame.getFn()){
				case 0x01:
					//登录
					pc.put(frame.getAddrstr(), session);
					//确认
					if(gprs.containsKey(frame.getAddrstr()) && (boolean)gprs.get(frame.getAddrstr()).getAttribute("online")){
						session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x01,frame.getAddr(),new byte[0]));
						ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "1", "3", "GPRS",session.getRemoteAddress().toString()));
					}else{
						session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x02,frame.getAddr(),new byte[0]));
						ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "1", "3", "NOGPRS",session.getRemoteAddress().toString()));
					}
					break;
				case 0x02:
					//退出
					session.close(true);
					break;
				case 0x03:
					//心跳  PC不会有心跳
					break;
				}
				break;
			case 0x00:
				//确认否认
			case 0x03:
				//设置参数  
			case 0x04:
				//控制命令
			case 0x0A:
				//查询参数
			case 0x0B:
				//实时数据
			case 0x0C:
				//历史数据
				IoSession send = null;
				send = gprs.get(frame.getAddrstr());
				if(send != null && (boolean)send.getAttribute("online")){
					send.write(message);
					ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "1", "3", byteArrayToHexStr(frame.getFrame(),frame.getFrame().length),session.getRemoteAddress().toString()));
				}else{
					session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x02,frame.getAddr(),new byte[0]));
				}
				break;
				
			}
		}
	}
	
	@Override
	public void sessionOpened(IoSession session) throws Exception{
		logger.info(session.getRemoteAddress().toString());
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception{
		if(session.getAttribute("online") == null){
			//son of bitch
		}else{
			session.setAttribute("online", false);
		}
		
	}
	
	
	public static String byteArrayToHexStr(byte[] b,int size){
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i < size;i++){
			sb.append(Integer.toHexString(b[i]&0xFF)+" ");
		}
		return sb.toString();
	}
}
