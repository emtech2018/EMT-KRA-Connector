package com.emtech.service.itax.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//All Database Methods for handling C.R.U.D Operations
public class DatabaseMethods {
    //Logging
    //private static final Logger logger= LoggerFactory.getLogger(PaymentService.class);

    //Database Method
    public static int DB(String sql, int params, String args) {
        System.out.println("DATABASE METHODS :: INSERT RECORDS");
        //logger.info("DATABASE METHODS :: INSERT RECORDS");
        DatabaseConnection dbconn = new DatabaseConnection();
        try {
            Connection conn = dbconn.dbConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (params > 0) {
                System.out.println("DATABASE METHODS :: Input values :: " + args);
                //logger.info("DATABASE METHODS :: Input values :: " + args);
                String[] vals = args.split("\\s*,\\s*");
                int l = vals.length;
                System.out.println("DATABASE METHODS :: No of Input Elements :: " + l);
                //logger.info("DATABASE METHODS :: No of Input Elements :: " + l);
                int g = 0;
                while (g < params) {
                    ps.setString(g + 1, vals[g]);
                    g++;
                }
            }
            int n = ps.executeUpdate();
            if (n > 0) {
                conn.setAutoCommit(false);
                conn.commit();
                conn.setAutoCommit(true);
                return 1;
            }
            DatabaseConnection.closeConn(conn);
        } catch (SQLException ex) {
            System.out.println(ex.getLocalizedMessage());
            //logger.info("DATABASE METHODS :: ERROR :: " + ex.getLocalizedMessage());
        }
        return 0;
    }

    //Find Duplicate Values
    public static boolean findDuplicates(String sql, int params, String args) {
        System.out.println("DATABASE METHODS :: FIND DUPLICATES :: Getting Database Connection");
        //logger.info("DATABASE METHODS :: FIND DUPLICATES :: Getting Database Connection");
        DatabaseConnection dbconn = new DatabaseConnection();
        try {
            Connection conn = dbconn.dbConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (params > 0) {
                String[] vals = args.split("\\s*,\\s*");
                int g = 0;
                while (g < params) {
                    ps.setString(g + 1, vals[g]);
                    g++;
                }
            }
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                return true;
            }
            DatabaseConnection.closeConn(conn);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    //Get Values From the Database
    public static String selectValues(String sql, int w, int params, String args) {
        String k = "";
        System.out.println("DATABASE METHODS :: SELECT VALUES :: Getting Database Connection");
        DatabaseConnection dbconn = new DatabaseConnection();
        try {
            Connection conn = dbconn.dbConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (params > 0) {
                String[] values = args.split("\\s*,\\s*");
                int g = 0;
                while (g < params) {
                    ps.setString(g + 1, values[g]);
                    g++;
                }
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (w == 1) {
                    k = rs.getString(1);
                } else {
                    String b = rs.getString(1);
                    for (int q = 2; q <= w;) {
                        b = b + "," + rs.getString(q);
                        q++;
                    }
                    k = b;
                }
            }
            DatabaseConnection.closeConn(conn);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return "";
        }
        return k;
    }
}
