package com.praiseview.util;

import java.io.InputStream;
import java.util.Properties;

public class VersionUtil {

    public static String getVersion() {
        try (InputStream is = VersionUtil.class.getClassLoader()
                .getResourceAsStream("META-INF/maven/com.praiseview/praiseview/pom.properties")) {

            if (is == null) {
                return "Unknown";
            }

            Properties props = new Properties();
            props.load(is);
            return props.getProperty("version");
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
