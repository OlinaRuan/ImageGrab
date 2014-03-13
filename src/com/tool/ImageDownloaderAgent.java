package com.tool;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.swing.JOptionPane;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageDownloaderAgent implements Runnable {

    private static final Pattern imageFileNamePattern = Pattern.compile("(\\w{1,}\\.(jpg|gif|png))");

    private List<String> imageLinks = null;

    private File imgDir;

    private ImageDownloader imageDownloader;

    public ImageDownloaderAgent(List<String> imageLinks, File imgDir, ImageDownloader imageDownloader) {
        this.imageLinks = imageLinks;
        this.imgDir = imgDir;
        this.imageDownloader = imageDownloader;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used to create a thread, starting the thread
     * causes the object's <code>run</code> method to be called in that separately executing thread. <p> The general
     * contract of the method <code>run</code> is that it may take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (null == imageLinks || imageLinks.isEmpty()) {
            return;
        }

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();

        try {
            final int pendingImageNum = imageLinks.size();
            boolean success = false;
            int retryCount = 0;
            BufferedInputStream bis = null;
            BufferedOutputStream bos =  null;
            for (int i = 0; i < pendingImageNum; i++) {
                String imgLink = imageLinks.get(i);
                HttpGet httpGet = new HttpGet(imgLink);
                success = false;
                retryCount = 0;
                while (!success && retryCount++ <= MainUI.MAX_RETRY) {
                    CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
                    HttpEntity entity = response.getEntity();
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        success = true;

                        if (entity.getContentLength() < MainUI.IMAGE_SIZE_THRESHOLD) {
                            imageDownloader.completeOneMore();
                            response.close();
                            break;
                        }

                        File newFile = new File(imgDir, extractFileNameFromURL(imgLink));
                        try {
                            if (!newFile.exists()) {
                                newFile.createNewFile();
                            } else {
                                new Thread(SoundPlayer.WARNING_SOUND_PLAYER).start();
                                int result = JOptionPane.showConfirmDialog(imageDownloader.getMainUI(), "文件已经存在，是否要覆盖?", "文件覆盖警告", JOptionPane.YES_NO_OPTION);
                                if (result != JOptionPane.YES_OPTION) {
                                    response.close();
                                    break;
                                }

                            }

                            bis = new BufferedInputStream(entity.getContent());
                            bos = new BufferedOutputStream(new FileOutputStream(newFile));
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            while ((len = bis.read(buffer)) > 0) {
                                bos.write(buffer, 0, len);
                            }
                            bos.flush();
                        } catch (IOException e) {
                            //ignore
                        } finally {
                            if (null != bos) {
                                bos.close();
                            }

                            if (null != bis) {
                                bis.close();
                            }
                        }
                        response.close();
                        imageDownloader.completeOneMore();
                    }
                }
            }
            closeableHttpClient.close();
        } catch (IOException e) {
            //ignore for now
        }
    }

    private String extractFileNameFromURL(String url) {
        Matcher matcher = imageFileNamePattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
