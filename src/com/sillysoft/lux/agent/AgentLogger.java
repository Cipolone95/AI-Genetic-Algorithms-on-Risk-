package com.sillysoft.lux.agent;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Owner
 */
public class AgentLogger {
    
    Logger logger = Logger.getLogger("MyLog");
    FileHandler fh;
    
    
    public AgentLogger (String addToFile) {
        try {
             
            // This block configure the logger with handler and formatter
            File file = new File ("C:/TEMP/" + addToFile + ".txt");
            file.createNewFile();
            fh = new FileHandler(file.toString());
            logger.addHandler(fh);
            //logger.setLevel(Level.ALL);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
             
            // the following statement is used to log any messages
            logger.info("My first log");
             
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void log (String message) {
        logger.info(message);
    }
}
