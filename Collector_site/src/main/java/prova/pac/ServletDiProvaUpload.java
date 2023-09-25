/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package prova.pac;

import collector_site.framework.result.HTMLResult;
import collector_site.framework.utils.ServletHelpers;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.sql.DataSource;

/**
 *
 * @author mauri
 */
public class ServletDiProvaUpload extends HttpServlet{
    
    private static final String ADD_FILE_QUERY = "";
    
    @Resource(name = "jdbc/collector_site")
    private DataSource ds;
    
    private String bytesToHexString(byte[] byteArray){
        StringBuilder hexStringBuffer = new StringBuilder();
        for(int i = 0; i < byteArray.length; i++){
            char[] hexDigits = new char[2];
            hexDigits[0] = Character.forDigit((byteArray[i] >> 4) & 0xF, 16);
            hexDigits[1] = Character.forDigit((byteArray[i] & 0xF), 16);          
            hexStringBuffer.append(new String(hexDigits));
        }
        return hexStringBuffer.toString();
    }
    
    private void action_upload(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, NamingException, NoSuchAlgorithmException, ServletException {

        int fileID = 0;
        Part file_to_upload = request.getPart("filetoupload");
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                File uploaded_file = File.createTempFile("upload_", "", new File(getServletContext().getInitParameter("uploads.directory")));
        try (InputStream is = file_to_upload.getInputStream();
                OutputStream os = new FileOutputStream(uploaded_file)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) > 0) {
                //durante la copia, aggreghiamo i byte del file nel digest sha-1
                //while copying, we aggregate the file bytes in the sha-1 digest
                md.update(buffer, 0, read);
                os.write(buffer, 0, read);
            }
        }
        String sdigest = bytesToHexString(md.digest());
        
        try (Connection c = ds.getConnection();
                //indichiamo al driver la colonna in cui comparirà la chiave auto-generata dall'inserimento
                PreparedStatement s = c.prepareStatement(ADD_FILE_QUERY, new String[]{"ID"})) {

            s.setString(1, file_to_upload.getSubmittedFileName());
            s.setString(2, file_to_upload.getContentType());
            s.setLong(3, file_to_upload.getSize());
            s.setString(4, uploaded_file.getName());
            s.setString(5, sdigest);
            if (s.executeUpdate() == 1) {
                try ( //get the added record ID
                        ResultSet keys = s.getGeneratedKeys()) {
                    keys.first();
                    fileID = keys.getInt(1);
                }
                HTMLResult result = new HTMLResult(getServletContext());
                result.setTitle("Upload complete");
                result.appendToBody("<h1>Successful upload</h1>");
                result.appendToBody("<p>The file " + file_to_upload.getSubmittedFileName() + " (" + file_to_upload.getContentType() + ", " + file_to_upload.getSize() + "bytes, digest " + sdigest + ") has been correctly uploaded as " + uploaded_file.getAbsolutePath() + ". The file ID is " + fileID + "</p>");
                result.activate(request, response);
            } else {
                ServletHelpers.handleError("Upload error", request, response, getServletContext());
            }
        }

    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            if (request.getPart("filetoupload") != null) {
                action_upload(request, response);
            } else {
                ServletHelpers.handleError("Nothing to upload!", request, response, getServletContext());
            }
        } catch (NamingException | SQLException | IOException | NoSuchAlgorithmException ex) {
            ServletHelpers.handleError(ex, request, response, getServletContext());
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
    

