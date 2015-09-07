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
					session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR | (frame.getSeq()&0x0F)),(byte)0x01,frame.getAddr(),new byte[0]));
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
			case 0x0B:
				//实时数据   由PC发出  集中器返回数据帧
				
				//发送应答帧  ，将数据帧判断  发送给抄表程序
				Frame data_f = (Frame) message;
				byte slave_seq_ = (byte) (data_f.getSeq() & 0x0F);
				Frame data_ack = new Frame(0, (byte)(Frame.ZERO), 
						Frame.AFN_YES, (byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR | slave_seq_), 
						(byte)0x01, data_f.getAddr(), new byte[0]);
				session.write(data_ack);
				
				//如果是单独帧  或者是最后一帧   判断序列号   session中保存发送到服务器的序列号
				int seq_sign = data_f.getSeq() & 0x60;  //帧位置的标识符
				if(seq_sign == 0x60 || seq_sign == 0x20){
					byte session_seq = (byte) session.getAttribute("slave_seq",0x10);
					if(slave_seq_ != session_seq){
						//单独帧  最后一帧   session中的序列号 与帧的序列号不同     发送给抄表服务器。
						session.setAttribute("slave_seq",slave_seq_);
						send = pc.get(frame.getAddrstr());
						if(send != null && (boolean)send.getAttribute("online")){
							send.write(message);
							ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "0", "3", byteArrayToHexStr(frame.getFrame(),frame.getFrame().length),session.getRemoteAddress().toString()));
						}
					}else{
						//这一帧已经发送给抄表服务器了。
					}
					
				}else{
					//中间帧  首帧
					session.setAttribute("slave_seq",slave_seq_);
					send = pc.get(frame.getAddrstr());
					if(send != null && (boolean)send.getAttribute("online")){
						send.write(message);
						ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "0", "3", byteArrayToHexStr(frame.getFrame(),frame.getFrame().length),session.getRemoteAddress().toString()));
					}
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
					IoSession oldsession = pc.get(frame.getAddrstr());
					if(oldsession != null && (boolean)oldsession.getAttribute("online")){
						session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x02,frame.getAddr(),new byte[0]));
						ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "1", "3", "WAIT",session.getRemoteAddress().toString()));
					}else{
						pc.put(frame.getAddrstr(), session);
						//确认
						if(gprs.containsKey(frame.getAddrstr()) && (boolean)gprs.get(frame.getAddrstr()).getAttribute("online")){
							session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x01,frame.getAddr(),new byte[0]));
							ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "1", "3", "GPRS",session.getRemoteAddress().toString()));
						}else{
							session.write(new Frame(0,(byte)(Frame.ZERO|Frame.PRM_S_LINE),Frame.AFN_YES,(byte)(Frame.ZERO|Frame.SEQ_FIN|Frame.SEQ_FIR),(byte)0x02,frame.getAddr(),new byte[0]));
							ListenerLogDao.insertLog(new ListenerLog(frame.getAddrstr(), "1", "3", "NOGPRS",session.getRemoteAddress().toString()));
						}
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
