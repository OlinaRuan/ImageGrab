/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tool;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author macbookpro
 */
public class ImageDownloader {
    
   private MainUI mainUI;

    private NumberFormat numberFormat = NumberFormat.getPercentInstance();

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
    


    private double completePercent;

   public ImageDownloader(MainUI mainUI) {
       this.mainUI = mainUI;
       numberFormat.setMaximumFractionDigits(2);
   }

   private int totalPending;

   private int completed;

    public MainUI getMainUI() {
        return mainUI;
    }

   public List<String> parseURL(String url) throws IOException {
       List<String> ret = new ArrayList<String>();
       Document document = Jsoup.connect(url).get();
       Elements elements = document.getElementsByTag("img");

       for (Element element : elements) {
           String link = element.attr("src").toLowerCase();
           if (link.contains(".png") || link.contains(".jpg") || link.contains(".gif")) {
              ret.add(element.attr("src"));
           }
       }

       return ret;
   }

    public synchronized void completeOneMore() {
        completed++;
        completePercent = (completed + 0.0)/totalPending;
        mainUI.getProgressBar().setValue((int)(100*completePercent));
        mainUI.getProgressBar().setString(numberFormat.format(completePercent));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainUI.getProgressBar().updateUI();
            }
        });
        
        mainUI.repaint();
        
        if (completed == totalPending) {
            new Thread(SoundPlayer.COMPLETE_SOUND_PLAYER).start();
        }
    }
   
   public void download(String url, File imgDir) {
       try {
           List<String> imageLinks = parseURL(url);
           if (imageLinks.isEmpty()) {
               return;
           }
           totalPending = imageLinks.size();

           File subDir = new File(imgDir, DEFAULT_DATE_FORMAT.format(new Date()));
           if (!subDir.exists()) {
               subDir.mkdirs();
           }

           ImageDownloaderAgent agent = new ImageDownloaderAgent(imageLinks, subDir, this);
           new Thread(agent).start();
       } catch (IOException e) {
           JOptionPane.showMessageDialog(mainUI, e.getMessage(), "网络错误", JOptionPane.ERROR_MESSAGE);
       }
   }
}
