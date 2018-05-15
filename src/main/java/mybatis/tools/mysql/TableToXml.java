package mybatis.tools.mysql;

import mybatis.tools.util.PropertiesUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jzq1999 on 2017/4/12.
 */
public class TableToXml {

    private static final String LINE = "\r\n";
    private static final String TAB = "\t";

    private static Map<String, String> jdbcTypeMap;

    String rootPath = PropertiesUtil.getValue("genXmlFilePath");

    static {
        jdbcTypeMap = new HashMap<String, String>();
        // MySql
        jdbcTypeMap.put("DATETIME", "TIMESTAMP");
        jdbcTypeMap.put("TINYINT", "INTEGER");
        jdbcTypeMap.put("SMALLINT", "INTEGER");
        jdbcTypeMap.put("INT", "INTEGER");

        //Postgresql

    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        String jdbcUrl = PropertiesUtil.getValue("jdbcUrl");
        Class.forName(PropertiesUtil.getValue("db.driver"));
        Connection con = DriverManager.getConnection(jdbcUrl, PropertiesUtil.getValue("db.username"),
                PropertiesUtil.getValue("db.password"));
        DatabaseMetaData metadata = con.getMetaData();
        ResultSet rs = metadata.getTables(null, null, null, null);
        TableToXml d = new TableToXml();
        while (rs.next()) {
            String tableName = rs.getString(3);
            System.out.println("正在生成XML Mapping的表名： ================ "+tableName);
            d.tableToXml(con, tableName);
        }
    }

    private void tableToXml(Connection conn, String tableName) {
        String entityName = convertTableToEntity(tableName);
        String sql = "select * from " + tableName + " limit 1"; //MySql DB
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append(LINE);
        xml.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >");
        xml.append(LINE);
        // mapping.xml中要引用的Mapper接口,com.zainagou.supplier.mapper为xml映射的entity类的包名
        String mapperName = "com.dmall.settle.mapper." +entityName+"Mapper";
        xml.append("<mapper namespace=\""+mapperName+"\">");
        xml.append(LINE);
        xml.append(TAB);
        String fullEntityName = "com.dmall.settle.domain."+entityName;
        xml.append("<resultMap id=\"baseResultMap\" type=\""+fullEntityName+"\" >");

        List<String> columeList = new ArrayList<String>();

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            int columnCount = metaData.getColumnCount();

            for(int i=1;i<=columnCount;i++){
                xml.append(LINE);
                xml.append(TAB);
                xml.append(TAB);
                String columnName = metaData.getColumnName(i);
                columeList.add(columnName);

                String columnTypeName = metaData.getColumnTypeName(i);
                if(columnTypeName.contains(" ")){
                    columnTypeName = columnTypeName.split(" ")[0];
                }
//                System.out.println("columnName:"+columnName+", columnType:"+columnTypeName);

                if(jdbcTypeMap.containsKey(columnTypeName)){
                    columnTypeName = jdbcTypeMap.get(columnTypeName);
                }
                if(columnName.equals("id")){
                    xml.append("<id column=\"id\" property=\"id\" jdbcType=\"");
                    xml.append(columnTypeName);
                    xml.append("\" />");
                }else{
                    xml.append("<result column=\"");
                    xml.append(columnName);
                    xml.append("\" property=\"");
                    String propertyName = dealName(columnName);
                    xml.append(propertyName);
                    xml.append("\" jdbcType=\"");
                    xml.append(columnTypeName);
                    xml.append("\" />");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        xml.append(LINE);
        xml.append(TAB);
        xml.append("</resultMap>");
        xml.append(LINE);
        xml.append(TAB);
        xml.append("<sql id=\"base_column_list\" >");
        xml.append(LINE);
        xml.append(TAB);
        xml.append(TAB);
        for(int i=0,size=columeList.size();i<size;i++){
            xml.append(columeList.get(i));
            if(i!=(size-1)){
                xml.append(",");
            }
        }
        xml.append(LINE);
        xml.append(TAB);
        xml.append("</sql>");
        xml.append(LINE);
        xml.append("</mapper>");
//        String rootPath = this.getClass().getClassLoader().getResource("").getPath();
//        String endPath = rootPath + "\\" + (packages.replace("/", "\\")).replace(".", "\\");
        buildXmlFile(rootPath + "/" + entityName + ".xml", xml.toString());
    }

    //生成xml文件
    public void buildXmlFile(String filePath, String fileContent) {
        try {
            File file = new File(filePath);
            FileOutputStream osw = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(osw);
            pw.println(fileContent);
            pw.close();
        } catch (Exception e) {
            System.out.println("生成xml文件出错：" + e.getMessage());
        }
    }

    //    转换表名为实体名
    public String convertTableToEntity(String tableName){
        tableName = tableName.substring(0, 1).toUpperCase() + tableName.subSequence(1, tableName.length());
        String entityName = dealLine(tableName);

        return entityName;
    }

    private String dealLine(String tableName) {
        // 处理下划线情况，把下划线后一位的字母变大写；
        tableName = dealName(tableName);
        return tableName;
    }

    //下划线后一位字母大写
    private String dealName(String columnName) {
        if (columnName.contains("_")) {
            StringBuffer names = new StringBuffer();
            String arrayName[] = columnName.split("_");
            names.append(arrayName[0]);
            for (int i = 1; i < arrayName.length; i++) {
                String arri = arrayName[i];
                String tmp = arri.substring(0, 1).toUpperCase() + arri.substring(1, arri.length());
                names.append(tmp);
            }
            columnName = names.toString();
        }
        return columnName;
    }

}
