package com.emtech.service.itax.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ResourceBundle;

public class Configurations {
    Properties prop;

    public Configurations() {

    }

    //public ResourceBundle rb = ResourceBundle.getBundle("conf/itax");
    public Properties getProperties() {
        prop = new Properties();
        try {
            InputStream url = getClass().getClassLoader().getResourceAsStream("application.properties");
            prop.load(url);
        } catch (IOException asd) {
            System.out.println(asd.getMessage());
        }
        return prop;
    }
}
