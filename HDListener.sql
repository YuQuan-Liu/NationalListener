USE [master]
GO
/****** Object:  Database [HDListener]    Script Date: 11/23/2015 14:12:58 ******/
CREATE DATABASE [HDListener] ON  PRIMARY 
( NAME = N'HDListener', FILENAME = N'F:\服务器数据库\HDListener.mdf' , SIZE = 577536KB , MAXSIZE = UNLIMITED, FILEGROWTH = 1024KB )
 LOG ON 
( NAME = N'HDListener_log', FILENAME = N'F:\服务器数据库\HDListener_log.ldf' , SIZE = 1024KB , MAXSIZE = 2048GB , FILEGROWTH = 10%)
GO
ALTER DATABASE [HDListener] SET COMPATIBILITY_LEVEL = 100
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [HDListener].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [HDListener] SET ANSI_NULL_DEFAULT OFF
GO
ALTER DATABASE [HDListener] SET ANSI_NULLS OFF
GO
ALTER DATABASE [HDListener] SET ANSI_PADDING OFF
GO
ALTER DATABASE [HDListener] SET ANSI_WARNINGS OFF
GO
ALTER DATABASE [HDListener] SET ARITHABORT OFF
GO
ALTER DATABASE [HDListener] SET AUTO_CLOSE OFF
GO
ALTER DATABASE [HDListener] SET AUTO_CREATE_STATISTICS ON
GO
ALTER DATABASE [HDListener] SET AUTO_SHRINK OFF
GO
ALTER DATABASE [HDListener] SET AUTO_UPDATE_STATISTICS ON
GO
ALTER DATABASE [HDListener] SET CURSOR_CLOSE_ON_COMMIT OFF
GO
ALTER DATABASE [HDListener] SET CURSOR_DEFAULT  GLOBAL
GO
ALTER DATABASE [HDListener] SET CONCAT_NULL_YIELDS_NULL OFF
GO
ALTER DATABASE [HDListener] SET NUMERIC_ROUNDABORT OFF
GO
ALTER DATABASE [HDListener] SET QUOTED_IDENTIFIER OFF
GO
ALTER DATABASE [HDListener] SET RECURSIVE_TRIGGERS OFF
GO
ALTER DATABASE [HDListener] SET  DISABLE_BROKER
GO
ALTER DATABASE [HDListener] SET AUTO_UPDATE_STATISTICS_ASYNC OFF
GO
ALTER DATABASE [HDListener] SET DATE_CORRELATION_OPTIMIZATION OFF
GO
ALTER DATABASE [HDListener] SET TRUSTWORTHY OFF
GO
ALTER DATABASE [HDListener] SET ALLOW_SNAPSHOT_ISOLATION OFF
GO
ALTER DATABASE [HDListener] SET PARAMETERIZATION SIMPLE
GO
ALTER DATABASE [HDListener] SET READ_COMMITTED_SNAPSHOT OFF
GO
ALTER DATABASE [HDListener] SET HONOR_BROKER_PRIORITY OFF
GO
ALTER DATABASE [HDListener] SET  READ_WRITE
GO
ALTER DATABASE [HDListener] SET RECOVERY SIMPLE
GO
ALTER DATABASE [HDListener] SET  MULTI_USER
GO
ALTER DATABASE [HDListener] SET PAGE_VERIFY CHECKSUM
GO
ALTER DATABASE [HDListener] SET DB_CHAINING OFF
GO
USE [HDListener]
GO
/****** Object:  Table [dbo].[ListenerLog]    Script Date: 11/23/2015 14:13:00 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[ListenerLog](
	[PID] [int] IDENTITY(1,1) NOT NULL,
	[GPRSTel] [varchar](11) NOT NULL,
	[Src] [char](1) NOT NULL,
	[Type] [char](1) NOT NULL,
	[RemoteAddr] [varchar](30) NOT NULL,
	[Data] [varchar](2048) NULL,
	[Transfered] [char](1) NOT NULL,
	[ActionTime] [smalldatetime] NOT NULL,
 CONSTRAINT [PK_ListenerLog] PRIMARY KEY CLUSTERED 
(
	[PID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING OFF
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'0~gprs   1~pc' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'ListenerLog', @level2type=N'COLUMN',@level2name=N'Src'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'1~上线    2~关闭   3~数据 4~心跳' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'ListenerLog', @level2type=N'COLUMN',@level2name=N'Type'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'传送的数据' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'ListenerLog', @level2type=N'COLUMN',@level2name=N'Data'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'0~未找到目的地    1~找到目的地(默认)' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'ListenerLog', @level2type=N'COLUMN',@level2name=N'Transfered'
GO
/****** Object:  Table [dbo].[GPRS]    Script Date: 11/23/2015 14:13:00 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[GPRS](
	[PID] [int] IDENTITY(1,1) NOT NULL,
	[GPRSTel] [varchar](11) NOT NULL,
	[GPRSAddr] [varchar](200) NULL,
	[GPRSStatus] [char](1) NOT NULL,
	[Reading] [char](1) NOT NULL,
	[NeighborName] [varchar](100) NULL,
	[NeighborID] [int] NULL,
	[NeighborAddr] [varchar](200) NULL,
 CONSTRAINT [PK_GPRS] PRIMARY KEY CLUSTERED 
(
	[PID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING OFF
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'0~不在线   1~在线' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'GPRS', @level2type=N'COLUMN',@level2name=N'GPRSStatus'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'0~未抄表   1~抄表中...' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'GPRS', @level2type=N'COLUMN',@level2name=N'Reading'
GO
/****** Object:  Default [DF_ListenerLog_Transfered]    Script Date: 11/23/2015 14:13:00 ******/
ALTER TABLE [dbo].[ListenerLog] ADD  CONSTRAINT [DF_ListenerLog_Transfered]  DEFAULT ((1)) FOR [Transfered]
GO
/****** Object:  Default [DF_ListenerLog_ActionTime]    Script Date: 11/23/2015 14:13:00 ******/
ALTER TABLE [dbo].[ListenerLog] ADD  CONSTRAINT [DF_ListenerLog_ActionTime]  DEFAULT (getdate()) FOR [ActionTime]
GO
