package com.ailk.jt.mannul;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.commons.fileupload.util.Streams;
import org.apache.log4j.Logger;

import com.ailk.uap.dbconn.ConnectionManager;
import com.ailk.uap.entity.DrUploadFileInfo;
import com.ailk.uap.util.CommonUtil;
import com.ailk.uap.util.DatetimeServices;

public class MainAcctLoginFile
{
  private static final Logger log = Logger.getLogger(MainAcctLoginFile.class);
  private static String uap_file_uapload;
  private static String uap_version;
  private static String uap_file_uapload_temp;
  private static final String type = "SMMAL";
  private static String prov_code;
  private static final String intval = "01HR";
  private static String createTime;
  private static String beginTime;
  private static String endTime;
  private static String uploadFileName;
  private static String fileSeq;
  private static String reloadFlag = "0";
  private static long sum;
  private static Connection conn;
  private static OutputStreamWriter output;
  private static FileOutputStream fos;
  private static BufferedWriter bw;
  private static File uapLoadTempFile;
  private static String m_logout_op_type_id;
  private static String op_type_id;
  private static String tab_month;

  public static void readConfig()
  {
    uap_file_uapload = com.ailk.uap.config.PropertiesUtil.getValue("uap_file_uapload");
    uap_version = com.ailk.uap.config.PropertiesUtil.getValue("uap_version");

    prov_code = com.ailk.uap.config.PropertiesUtil.getValue("prov_code").trim();
    uap_file_uapload_temp = 
      com.ailk.uap.config.PropertiesUtil.getValue("uap_file_uapload_temp").trim();
    op_type_id = com.ailk.uap.config.PropertiesUtil.getValue("op_type_id").trim();
    m_logout_op_type_id = com.ailk.uap.config.PropertiesUtil.getValue("m_logout_op_type_id")
      .trim();
  }

  public static void main(String[] args) {
    readConfig();
    log.info("MainAcctLoginFile start to run......");
    log.info("uap_file_uapload==" + uap_file_uapload);
    log.info("uap_version==" + uap_version);
    long statisticRunStartTime = System.currentTimeMillis();
    Calendar calendar = Calendar.getInstance();
    calendar.set(12, 0);
    calendar.set(13, 0);
    calendar.set(14, 0);
    Timestamp a = new Timestamp(
      calendar.getTimeInMillis());

    String end = a.toString().replaceAll("\\.0", "");

    calendar.set(11, 
      calendar.get(11) - 1);
    a = new Timestamp(calendar.getTimeInMillis());

    String begin = a.toString().replaceAll("\\.0", "");

    tab_month = begin.substring(0, 7).replace("-", "");
    try
    {
      conn = ConnectionManager.getUapAuditConnection();
      createTime = 
        com.ailk.jt.util.PropertiesUtil.getValue("create_time");
      beginTime = com.ailk.jt.util.PropertiesUtil.getValue("begin_time");
      endTime = com.ailk.jt.util.PropertiesUtil.getValue("end_time");

      fileSeq = com.ailk.jt.util.PropertiesUtil.getValue("fileSeq");
      uploadFileName = "SMMAL_" + prov_code + "_" + "01HR" + "_" + 
        begin.substring(0, 10).replaceAll("-", "") + "_" + 
        fileSeq + "_" + "000.xml";
      if (args.length != 0)
      {
        uploadFileName = args[0];

        beginTime = args[1];

        endTime = args[2];

        tab_month = beginTime.substring(0, 7).replace("-", "");

        int fileSeqIndex = uploadFileName.lastIndexOf("_");
        fileSeq = uploadFileName.substring(fileSeqIndex - 3, 
          fileSeqIndex);

        int reloadFlagIndex = uploadFileName.lastIndexOf(".");

        if (!uploadFileName.substring(reloadFlagIndex - 3, 
          reloadFlagIndex).equals("000")) {
          reloadFlag = "1";
        }
      }

      log.info("generateMainAcctFullFile  ******Start***************");

      generateMainAcctAuthorFullFile();

      log.info("generateMainAcctFullFile  ******End ***************");

      log.info("DR_UPLOAD_FILE_INFO**********insert ********Start*********");

      DrUploadFileInfo fileInfo = new DrUploadFileInfo();
      fileInfo.setFileName(uploadFileName);
      fileInfo.setFileSeq(fileSeq);
      fileInfo.setReloadFlag(reloadFlag);
      fileInfo.setIntval("01HR");
      fileInfo.setProv(prov_code);
      fileInfo.setTotal(Long.valueOf(sum));
      fileInfo.setType("SMMAL");
      fileInfo.setUploadStatus("0");

      ConnectionManager.closeConnection(conn);
      conn = ConnectionManager.getUapAcctConnection();
      CommonUtil.insertUploadFileInfo(fileInfo, conn, beginTime, endTime);
      long statisticRunEndTime = System.currentTimeMillis();
      log.info("GENERATE MAINACCT FULL FILE  TOTALTIME======" + 
        (statisticRunEndTime - statisticRunStartTime) / 1000L + 
        "s");
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
    }
    finally {
      ConnectionManager.closeConnection(conn);
      try
      {
        if (bw != null) {
          bw.close();
        }
        if (output != null) {
          output.close();
        }
        if (fos != null) {
          fos.close();
        }
        if (uapLoadTempFile.exists())
          uapLoadTempFile.delete();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      log.info("-------------MainAcctLoginFile end-----------------");
    }
  }

  private static void generateMainAcctAuthorFullFile()
    throws Exception, IOException, FileNotFoundException, UnsupportedEncodingException
  {
    StringBuffer mainAcctFileBuffer = new StringBuffer();
    mainAcctFileBuffer.append("<?xml version='1.0' encoding='UTF-8'?>\r\n");

    mainAcctFileBuffer.append("<bomc>\r\n");
    mainAcctFileBuffer.append("<type>SMMAL</type>\r\n");
    mainAcctFileBuffer.append("<province>" + prov_code + "</province>" + 
      "\r\n");
    mainAcctFileBuffer.append("<createtime>" + 
      createTime.replaceAll(" ", "T") + "</createtime>" + "\r\n");

    String mainAcctFullFileSql = "";
    String mainAcctFullFileCountSql = "";
    mainAcctFullFileCountSql = "SELECT count(1) from  (select  MAIN_ACCT_ID as mainacctid, MAIN_ACCT_NAME as loginname, OPERATE_TIME as logintime, CLIENT_IP as ip, OPERATE_RESULT as result,OPERATE_TYPE_ID as logintype  from  iap_app_log t where (t.OPERATE_TYPE_ID='" + 
      op_type_id + 
      "'" + 
      " or t.OPERATE_TYPE_ID='" + 
      m_logout_op_type_id + 
      "') AND t.MAIN_ACCT_NAME is not null and  t.operate_time <  to_date('" + 
      endTime + 
      "', 'yyyy-MM-dd HH24:mi:ss') and t.operate_time >= to_date('" + 
      beginTime + 
      "', 'yyyy-MM-dd HH24:mi:ss')" + 
      " and t.MAIN_ACCT_ID is not null    and t.MAIN_ACCT_NAME is not null   and t.MAIN_ACCT_NAME<>'null'   and t.OPERATE_TIME is not null   and t.client_ip is not null      and t.CLIENT_IP <>'null'    and t.OPERATE_RESULT is not null   and t.OPERATE_TYPE_ID is not null)";

    mainAcctFullFileSql = "select  MAIN_ACCT_ID as mainacctid, MAIN_ACCT_NAME as loginname, OPERATE_TIME as logintime, CLIENT_IP as ip, OPERATE_RESULT as result,OPERATE_TYPE_ID as logintype  from  iap_app_log t where (t.OPERATE_TYPE_ID='" + 
      op_type_id + 
      "'" + 
      " or t.OPERATE_TYPE_ID='" + 
      m_logout_op_type_id + 
      "') AND t.MAIN_ACCT_NAME is not null and  t.operate_time <  to_date('" + 
      endTime + 
      "', 'yyyy-MM-dd HH24:mi:ss') and t.operate_time >= to_date('" + 
      beginTime + 
      " ', 'yyyy-MM-dd HH24:mi:ss') " + 
      " and t.MAIN_ACCT_ID is not null " + 
      "  and t.MAIN_ACCT_NAME is not null " + 
      "  and t.MAIN_ACCT_NAME<>'null' " + 
      " and t.OPERATE_TIME is not null " + 
      "  and t.client_ip is not null    " + 
      "  and t.CLIENT_IP <>'null'  " + 
      "  and t.OPERATE_RESULT is not null " + 
      "  and t.OPERATE_TYPE_ID is not null ";

    log.info("mainAcctFullFileSql===" + mainAcctFullFileSql);

    sum = getMainAcctFullCount(mainAcctFullFileCountSql).longValue();
    log.info("total sum ===" + sum);
    mainAcctFileBuffer.append("<sum>" + sum + "</sum>" + "\r\n");
    mainAcctFileBuffer.append("<begintime>" + beginTime.replace(" ", "T") + 
      "</begintime>" + "\r\n");
    mainAcctFileBuffer.append("<endtime>" + endTime.replace(" ", "T") + 
      "</endtime>" + "\r\n");
    mainAcctFileBuffer.append("<data>");

    log.info("getMainAcctFullInfo*******************Start******");
    getMainAcctFullInfo(mainAcctFullFileSql, mainAcctFileBuffer);
    log.info("getMainAcctFullInfo*******************End*********");

    mainAcctFileBuffer.append("</data>\r\n");
    mainAcctFileBuffer.append("</bomc>");

    log.info("mainAcctFileBuffer====" + mainAcctFileBuffer.toString());

    uapLoadTempFile = new File(uap_file_uapload_temp + "/" + uploadFileName);

    writeMainAcctFileBufferToTempFile(mainAcctFileBuffer);

    BufferedInputStream in = new BufferedInputStream(
      new FileInputStream(uapLoadTempFile));
    File uapLoadFile = new File(uap_file_uapload + "/" + uploadFileName);
    if (!uapLoadFile.exists()) {
      uapLoadFile.createNewFile();
    }
    BufferedOutputStream out = new BufferedOutputStream(
      new FileOutputStream(uapLoadFile));
    Streams.copy(in, out, true);
    in.close();
    out.close();
  }

  private static void writeMainAcctFileBufferToTempFile(StringBuffer mainAcctFileBuffer)
    throws IOException
  {
    bw.write(mainAcctFileBuffer.toString());
    bw.flush();
    output.flush();
    fos.flush();
  }

  private static Long getMainAcctFullCount(String sql) throws Exception
  {
    PreparedStatement prepStmt = conn.prepareStatement(sql);
    ResultSet rs = prepStmt.executeQuery();
    Long count = null;
    while (rs.next()) {
      count = Long.valueOf(rs.getLong(1));
    }
    ConnectionManager.closeResultSet(rs);
    ConnectionManager.closePrepStmt(prepStmt);

    return count;
  }

  private static void getMainAcctFullInfo(String mainAcctFullFileSql, StringBuffer mainAcctFileBuffer) throws Exception
  {
    PreparedStatement prepStmt = conn.prepareStatement(mainAcctFullFileSql);
    ResultSet rs = prepStmt.executeQuery();
    int i = 0;
    File uapLoadTempFile = new File(uap_file_uapload_temp + "/" + 
      uploadFileName);
    if (uapLoadTempFile.exists()) {
      uapLoadTempFile.delete();
      uapLoadTempFile.createNewFile();
    }
    fos = new FileOutputStream(uapLoadTempFile, true);
    output = new OutputStreamWriter(fos, "UTF-8");
    bw = new BufferedWriter(output);

    while (rs.next())
    {
      i++;
      if (i % 1000 == 0) {
        log.info("writeMainAcctFileBufferToTempFile******start***");

        writeMainAcctFileBufferToTempFile(mainAcctFileBuffer);
        log.info("writeMainAcctFileBufferToTempFile******end***");
        mainAcctFileBuffer.setLength(0);
      }

      mainAcctFileBuffer.append("<rcd>\r\n");
      mainAcctFileBuffer.append("<seq>" + String.valueOf(i) + "</seq>" + 
        "\r\n");
      mainAcctFileBuffer.append("<mainacctid>" + 
        String.valueOf(rs.getLong("mainacctid")) + 
        "</mainacctid>" + "\r\n");
      mainAcctFileBuffer.append("<loginname>" + (
        rs.getString("loginname") == null ? "" : rs.getString(
        "loginname").trim()) + "</loginname>" + "\r\n");

      Timestamp updateTimeStamp = rs.getTimestamp("logintime");
      String updateTime = "";
      if (updateTimeStamp != null) {
        updateTime = 
          DatetimeServices.converterToDateTime(updateTimeStamp);
      }

      mainAcctFileBuffer.append("<logintime>" + 
        updateTime.replace(" ", "T") + "</logintime>" + "\r\n");

      mainAcctFileBuffer.append("<ip>" + (
        rs.getString("ip") == null ? "" : rs.getString("ip")
        .trim()) + "</ip>" + "\r\n");
      mainAcctFileBuffer.append("<result>" + (
        rs.getString("result") == null ? "" : rs.getString(
        "result").trim()) + "</result>" + "\r\n");
      mainAcctFileBuffer
        .append("<logintype>" + (
        rs.getString("logintype").equals(op_type_id) ? "1" : 
        "0") + "</logintype>" + "\r\n");
      mainAcctFileBuffer.append("</rcd>\r\n");
    }

    ConnectionManager.closeResultSet(rs);
    ConnectionManager.closePrepStmt(prepStmt);
  }
}