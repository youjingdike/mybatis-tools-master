package mybatis.tools.postgres;

import mybatis.tools.util.PropertiesUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * MyBatis 生成Table对应Bean
 * Created by jzq1999 on 2017/11/14.
 */
public class TableToBean {

    private static final String LINE = "\r\n";
    private static final String TAB = "\t";

    String filePath = PropertiesUtil.getValue(PropertiesUtil.FILE_PATH);

    private static Map<String, String> javaTypeMap;
    private static Map<String, String> importTypeMap;

    static {
        javaTypeMap = new HashMap<String, String>();
        importTypeMap = new HashMap<String, String>();

        //Postgresql
        javaTypeMap.put("varchar", "String");
        javaTypeMap.put("timestamptz", "Date");
        javaTypeMap.put("timestamp", "Date");
        javaTypeMap.put("numeric", "Double");
        javaTypeMap.put("serial", "Integer");
        javaTypeMap.put("int4", "Integer");
        javaTypeMap.put("text", "String");
        javaTypeMap.put("date", "Date");
        javaTypeMap.put("time", "Date");

        importTypeMap.put("date", "java.util.Date");
        importTypeMap.put("time", "java.util.Date");
        importTypeMap.put("timestamp", "java.util.Date");
        importTypeMap.put("timestamptz", "java.util.Date");
    }

    public static String getPojoType(String dataType) {
        System.out.println("dataType --------->> " + dataType);
        StringTokenizer st = new StringTokenizer(dataType);
        return javaTypeMap.get(st.nextToken());
    }

    public static String getImport(String dataType) {
        if (importTypeMap.get(dataType) == null || "".equals(importTypeMap.get(dataType))) {
            return null;
        } else {
            return importTypeMap.get(dataType);
        }
    }

    public void tableToBean(Connection connection, String tableName) throws SQLException {
        String sql = "select * from " + tableName + " limit 1"; //MySql DB
        PreparedStatement ps = null;
        ResultSet rs = null;
        ps = connection.prepareStatement(sql);
        rs = ps.executeQuery();
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();
        StringBuffer sb = new StringBuffer();
        tableName = tableName.substring(0, 1).toUpperCase() + tableName.subSequence(1, tableName.length());
        tableName = this.dealLine(tableName);
        //  sb.append("package " + this.packages + " ;");

        sb.append(LINE);
        importPackage(md, columnCount, sb);
        sb.append(LINE);
        sb.append("public class " + tableName + " {");
        sb.append(LINE);

        defProperty(md, columnCount, sb);

        genSetGet(md, columnCount, sb);
        sb.append("}");
        buildJavaFile(filePath + "/" + tableName + ".java", sb.toString());

        // linux平台, 不需要把"/"转成"\\"
    }

    //属性生成get、 set 方法
    private void genSetGet(ResultSetMetaData md, int columnCount, StringBuffer sb) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            sb.append(TAB);
            String pojoType = getPojoType(md.getColumnTypeName(i));
            String columnName = dealLine(md, i);
            String getName = null;
            String setName = null;
            if (columnName.length() > 1) {
                getName = "public " + pojoType + " get" + columnName.substring(0, 1).toUpperCase()
                        + columnName.substring(1, columnName.length()) + "() {";
                setName = "public void set" + columnName.substring(0, 1).toUpperCase()
                        + columnName.substring(1, columnName.length()) + "(" + pojoType + " " + columnName + ") {";
            } else {
                getName = "public get" + columnName.toUpperCase() + "() {";
                setName = "public set" + columnName.toUpperCase() + "(" + pojoType + " " + columnName + ") {";
            }
            sb.append(LINE).append(TAB).append(getName);
            sb.append(LINE).append(TAB).append(TAB);
            sb.append("return " + columnName + ";");
            sb.append(LINE).append(TAB).append("}");
            sb.append(LINE);
            sb.append(LINE).append(TAB).append(setName);
            sb.append(LINE).append(TAB).append(TAB);
            sb.append("this." + columnName + " = " + columnName + ";");
            sb.append(LINE).append(TAB).append("}");
            sb.append(LINE);
        }
    }

    //导入属性所需包
    private void importPackage(ResultSetMetaData md, int columnCount, StringBuffer sb) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            String im = getImport(md.getColumnTypeName(i));
            if (im != null) {
                sb.append("import " + im + ";");
                sb.append(LINE);
                break;
            }
        }
    }

    //属性定义
    private void defProperty(ResultSetMetaData md, int columnCount, StringBuffer sb) throws SQLException {

        for (int i = 1; i <= columnCount; i++) {
            sb.append(TAB);
            String columnName = dealLine(md, i);
            sb.append("private " + getPojoType(md.getColumnTypeName(i)) + " " + columnName + ";");
            sb.append(LINE);
        }
    }

    private String dealLine(ResultSetMetaData md, int i) throws SQLException {
        String columnName = md.getColumnName(i);
        // 处理下划线情况，把下划线后一位的字母变大写；
        columnName = dealName(columnName);
        return columnName;
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

    //生成java文件
    public void buildJavaFile(String filePath, String fileContent) {
        try {
            File file = new File(filePath);
            FileOutputStream osw = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(osw);
            pw.println(fileContent);
            pw.close();
        } catch (Exception e) {
            System.out.println("生成java文件出错：" + e.getMessage());
        }
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        String jdbcUrl = PropertiesUtil.getValue("postgres.jdbcUrl");
        Class.forName(PropertiesUtil.getValue("db.postgres.driver"));
        Connection con = DriverManager.getConnection(jdbcUrl, PropertiesUtil.getValue("db.postgres.username"),
                PropertiesUtil.getValue("db.postgres.password"));

        DatabaseMetaData databaseMetaData = con.getMetaData();
        String[] tableType = {"TABLE"};
        //ResultSet rs = databaseMetaData.getTables(null, null, "%", tableType);
        ResultSet rs = databaseMetaData.getTables(null, "%",  "%", tableType);

        TableToBean d = new TableToBean();
        while (rs.next()) {
            String tableName = rs.getString(3).toString();
            //if(tableName.equals("organ")) {
                System.out.println("正在生成Bean的表名： ================ "+tableName);
                d.tableToBean(con, tableName);
            //}
        }
    }

}
