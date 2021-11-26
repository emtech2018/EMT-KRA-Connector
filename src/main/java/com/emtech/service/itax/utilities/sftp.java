package com.emtech.service.itax.utilities;

//Class File with the code to send a copy of the receipt file to finacle server \\

/**
 *
 * @author omukubwa emukule
 */
import com.jcraft.jsch.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import org.apache.commons.io.IOUtils;

public class sftp {
    //Instance of the Configuration Classes
    Configurations cn = new Configurations();
    //Encryption Key
    private final String key = cn.getProperties().getProperty("enc.key").trim();
    //Encryption Init Vector
    private final String initVector =  cn.getProperties().getProperty("enc.initVector").trim();
    //key location
    private final String keylocation = cn.getProperties().getProperty("itax.sftp.keylocation").trim();

    //SFTP IP
    private final String ip = Encryptor.decrypt(key,initVector,cn.getProperties().getProperty("itax.sftp.ip").trim());

    //SFTP Username
    private final String username = Encryptor.decrypt(key,initVector,cn.getProperties().getProperty("itax.sftp.username").trim());

    //SFTP password
    private final String password = Encryptor.decrypt(key,initVector,cn.getProperties().getProperty("itax.sftp.password").trim());

    //SFTP root folder
    private final String rootfolder = cn.getProperties().getProperty("itax.sftp.rootfolder").trim();
    //SFTP remote directory
    private final String remotedir = cn.getProperties().getProperty("itax.sftp.remotedirectory").trim();
    //Source directory
    private final String sourcedir = cn.getProperties().getProperty("itax.sftp.sourcedir").trim();

    //SFTP root folder - CBK
    private final String rootfoldercbk = cn.getProperties().getProperty("itax.sftp.rootfolder").trim();
    //SFTP remote directory - cbk
    private final String remotedircbk = cn.getProperties().getProperty("itax.sftp.cbk.remotedirectory").trim();
    //Source directory - cbk
    private final String sourcedircbk = cn.getProperties().getProperty("itax.sftp.cbk.sourcedir").trim();

    //SFTP FOR RECEIPTS
    public void uploadReceiptToRemote(String filename) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        //jsch.addIdentity(keylocation);
        //jsch.setKnownHosts("known_hosts");
        //Session session = jsch.getSession("ubuntu", "3.13.214.62");
        Session session = jsch.getSession(username, ip);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        Vector<ChannelSftp.LsEntry> entries = channel.ls(rootfolder);
/*
        System.out.println("Entries in root directory:");
        for (ChannelSftp.LsEntry entry : entries) {
            System.out.println(entry.getFilename());
        }
*/
        channel.cd(remotedir);
        entries = channel.ls(".");
/*
        System.out.println("Entries in /dir-1 directory:");
        for (ChannelSftp.LsEntry entry : entries) {
            System.out.println(entry.getFilename());
        }
*/
        InputStream inputStream = new FileInputStream(new File(sourcedir+filename));
        //InputStream inputStream = channel.get("/home/ubuntu/receipt.pdf");
        byte[] bytes = IOUtils.toByteArray(inputStream);
        //System.out.println("Contents of this receipt file :: " + new String(bytes));

        //channel.put(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), "new.pdf");
        channel.put(new ByteArrayInputStream(bytes), filename);

        channel.disconnect();
        session.disconnect();
    }


    //SFTP FOR CBK SETTLEMENT REPORTS
    public void uploadCBKReportToRemote(String filename) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, ip);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        Vector<ChannelSftp.LsEntry> entries = channel.ls(rootfoldercbk);
        channel.cd(remotedircbk);
        entries = channel.ls(".");

        //InputStream inputStream = channel.get(sourcedircbk+filename);
        InputStream inputStream = new FileInputStream(new File(sourcedircbk+filename));
        byte[] bytes = IOUtils.toByteArray(inputStream);
        channel.put(new ByteArrayInputStream(bytes), filename);

        channel.disconnect();
        session.disconnect();
    }
}
