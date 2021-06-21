package com.dc.videojc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/21
 */
public class ProcessTest {
    public static void main(String[] args) throws Exception {
        Process process = new ProcessBuilder("ping -t localhost".split(" ")).start();
        try (InputStream inputStream = process.getInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("GBK")));
            String line = null;
            while (null != (line = bufferedReader.readLine())) {
                System.out.println(line);
            }
            
        }
    }
}
