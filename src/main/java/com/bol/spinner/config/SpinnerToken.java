package com.bol.spinner.config;

import cn.github.driver.connection.MatrixConnection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class SpinnerToken {
    public static MatrixConnection connection = null;

    public static void closeConnection() {
        try {
            connection.close();
        } catch (IOException e) {
            log.error("Error: close matrix connection, {}", e.getLocalizedMessage(), e);
        }
        connection = null;
    }
}
