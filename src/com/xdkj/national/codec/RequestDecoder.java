package com.xdkj.national.codec;

import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RequestDecoder extends CumulativeProtocolDecoder {
	private static final Logger logger = LoggerFactory.getLogger(RequestDecoder.class);
	private static final String DECODER_STATE_KEY = RequestDecoder.class.getName() + ".STATE";
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		
		in.order(ByteOrder.LITTLE_ENDIAN);
//		logger.info("decode");
		
//		byte[] b = new byte[in.limit()];
//		in.get(b);   这个地方会将position 设置到limit  导致remaining = 0
//		System.out.println(DataHandler.byteArrayToHexStr(b, b.length));
//		
		DecoderState decoderState = (DecoderState) session.getAttribute(DECODER_STATE_KEY);
		if(decoderState == null){
			decoderState = new DecoderState();
			session.setAttribute(DECODER_STATE_KEY, decoderState);
		}
//		System.out.println(decoderState.start);
		if(!decoderState.start){
//			System.out.println("remaining"+in.remaining());
			if(in.remaining() >= 6){
				for(int i = 0;i < in.remaining() && (in.remaining() - i)>5;i++){
					if(in.get(i) == 0x68 && in.get(i+5) == 0x68){
						int len1 = in.getShort(i+1);
						int len2 = in.getShort(i+3);
						if(len1 == len2){
							decoderState.length = len1>>2;
							decoderState.start = true;
							decoderState.nonlength = i;
							break;
						}
					}
				}
				
				if(!decoderState.start){
					decoderState.length = 0;
					decoderState.start = false;
					decoderState.nonlength = 0;
//					in.rewind();  //remaining  会一直增加
					in.flip();
//					in.clear();  
					return false;
				}
			}
			if(decoderState.start){
				if(in.remaining() >= (decoderState.length+decoderState.nonlength+8)){
					byte[] frame = new byte[decoderState.length+8];
//					in.get(frame,decoderState.nonlength,decoderState.length+8);
					
					for (int i = 0; i < frame.length; i++)
						frame[i] = in.get(i+decoderState.nonlength);
					in.position(decoderState.length+decoderState.nonlength+8);
					
					if(checkFrame(frame)){
						//deal the frame  and  write out
						out.write(new Frame(frame));
						decoderState.start = false;
						decoderState.length = 0;
						decoderState.nonlength = 0;
						return true;
					}else{
						decoderState.start = false;
						decoderState.length = 0;
						decoderState.nonlength = 0;
						in.flip();
						return false;
					}
				}
			}
		}else{
			if(in.remaining() >= (decoderState.length+decoderState.nonlength+8)){
				byte[] frame = new byte[decoderState.length+8];
//				in.get(frame,decoderState.nonlength,frame.length);
				
				for (int i = 0; i < frame.length; i++)
					frame[i] = in.get(i+decoderState.nonlength);
				
				in.position(decoderState.length+decoderState.nonlength+8);
				if(checkFrame(frame)){
					//deal the frame  and  write out
					out.write(new Frame(frame));
					decoderState.start = false;
					decoderState.length = 0;
					decoderState.nonlength = 0;
					return true;
				}else{
					decoderState.start = false;
					decoderState.length = 0;
					decoderState.nonlength = 0;
					in.flip();
					return false;
				}
			}
		}
		return false;
	}

	private static class DecoderState{
		private int length;
		private boolean start = false;
		private int nonlength;
	}
	
	private boolean checkFrame(byte[] frame){
		byte cs = 0;
		for(int i =6;i < (frame.length-2);i++){
			cs += frame[i];
		}
		if(cs == frame[frame.length-2] && frame[frame.length - 1]==0x16){
			return true;
		}
		return false;
	}
	
}
