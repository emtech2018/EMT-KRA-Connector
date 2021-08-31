package com.emtech.service.itax.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//Handles all Connections to the Database
//MySQL and Oracle.
public class DatabaseConnection {

    //Variables
    //Database URL
    //@Value("${db.url}")
    //private String url = env.getProperty("db.url");
    private String url = "jdbc:mysql://";
    //Database Driver
    //@Value("${db.driver}")
    private  String classname ="com.mysql.cj.jdbc.Driver";
    //Database Username
    //@Value("${db.username}")
    private String username = "jesse";
    //Database Host IP
    //@Value("${db.port}")
    private String host_ip = "192.168.154.1";
    //Database Port
    //@Value("${db.port}")
    private String port = "3306";
    //Database Password
    //@Value("${db.password}")
    private String password = "Joe@19051998";
    //Database Username
    //@Value("${db.database}")
    private String database_name = "VcbItax";

    //LOGGING
    //private static final Logger logger= LoggerFactory.getLogger(DatabaseConnection.class);

    //Connecting to MySQL Database
    public Connection dbConnection() {
        Connection conn = null;
        /*
        try {
            //logger.info("DATABASE CONNECTION :: Connection variables  :: "+host_ip+" :: "+username+" :: "+port+":"+database_name);
            System.out.println("DATABASE CONNECTION :: Connection variables  :: "+host_ip+" :: "+username+" :: "+port+":"+database_name);
            Class.forName(classname);
            conn = DriverManager.getConnection(url + host_ip + ":" + port + "/" + database_name+"?autoReconnect=true&useSSL=false", username, password);
        } catch (ClassNotFoundException | SQLException asd) {
            System.err.println(asd.getLocalizedMessage());
            //logger.info("DATABASE CONNECTION :: Connection failed :: ERROR :: "+asd.getLocalizedMessage());
            System.out.println("DATABASE CONNECTION :: Connection failed :: ERROR :: "+asd.getLocalizedMessage());
        }
         */
        //connect to database
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String serverName = "3.21.220.181";
            String portNumber = "1521";
            String sid = "sitdb";
            String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
            String username = "system";
            String password = "manager";
            conn = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        if (conn != null) {
            return conn;

        } else {
        }
        return null;
    }

    //Closing the Connection to te database
    public static void closeConn(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
