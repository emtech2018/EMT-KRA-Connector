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
    //key location
    private String keylocation = cn.getProperties().getProperty("itax.sftp.keylocation").trim();
    //SFTP IP
    private String ip = cn.getProperties().getProperty("itax.sftp.ip").trim();
    //SFTP Username
    private String username = cn.getProperties().getProperty("itax.sftp.username").trim();
    //SFTP password
    private String password = cn.getProperties().getProperty("itax.sftp.password").trim();
    //SFTP root folder
    private String rootfolder = cn.getProperties().getProperty("itax.sftp.rootfolder").trim();
    //SFTP remote directory
    private String remotedir = cn.getProperties().getProperty("itax.sftp.remotedirectory").trim();
    //Source directory
    private String sourcedir = cn.getProperties().getProperty("itax.sftp.sourcedir").trim();


    //SFTP
    public void uploadToRemote(String filename) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        jsch.addIdentity(keylocation);
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
}
