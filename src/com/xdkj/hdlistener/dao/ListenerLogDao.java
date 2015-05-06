package com.xdkj.hdlistener.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xdkj.hdlistener.db.DBPool;
import com.xdkj.hdlistener.obj.ListenerLog;

public class ListenerLogDao {
	private static final Logger logger = LoggerFactory.getLogger(ListenerLogDao.class);
	
	public static void insertLog(ListenerLog log){
		String SQL = "insert into ListenerLog " +
				"(GPRSTel,Src,Type,Data,RemoteAddr) " +
				"values(?,?,?,?,?)";
		Connection con = null;
		try {
			con = DBPool.getConnection();
			PreparedStatement pstmt = con.prepareStatement(SQL);
			pstmt.setString(1, log.getGPRSTel());
			pstmt.setString(2, log.getSrc());
			pstmt.setString(3, log.getType());
			pstmt.setString(4, log.getData());
			pstmt.setString(5, log.getRemoteAddr());
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error("插入记录异常",e);
		} finally{
			if(con != null){
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void insertNonTransferedLog(ListenerLog log){
		String SQL = "insert into ListenerLog " +
				"(GPRSTel,Src,Type,Data,RemoteAddr,Transfered) " +
				"values(?,?,?,?,?,0)";
		Connection con = null;
		try {
			con = DBPool.getConnection();
			PreparedStatement pstmt = con.prepareStatement(SQL);
			pstmt.setString(1, log.getGPRSTel());
			pstmt.setString(2, log.getSrc());
			pstmt.setString(3, log.getType());
			pstmt.setString(4, log.getData());
			pstmt.setString(5, log.getRemoteAddr());
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error("插入未找到目的地数据记录异常",e);
		} finally{
			if(con != null){
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
