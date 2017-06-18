package com.coolftc.prompt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import static com.coolftc.prompt.Constants.*;
import static com.coolftc.prompt.WebServiceModels.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The WebServices Class holds methods that can be used to communicate with the
 * Reminder API, and other Web Services.
 */
public class WebServices {

    // Suggest anyone using the web services to first check that this returns TRUE.
    public boolean IsNetwork(Context context){
        ConnectivityManager net = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo priNet = net.getActiveNetworkInfo();
        return (priNet!=null && priNet.isConnected());
    }

    /*
        The "ping" path of the status resource returns the build version of the service.
        This can be used to check the status of the service, e.g. is it working. If the
        ticket is passed in, it will also check the authentication.
        Note: The Web Service version is a different concept than the build version.
     */
    public String GetVersion(String ticket){
        HttpURLConnection webC = null;
        try {
            ticket = ticket.length() > 0 ? FTI_Ticket + ticket : ticket;
            URL web = new URL(FTI_BaseURL + FTI_Ping + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/json");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            int status = webC.getResponseCode();

            if(status >= 200 && status < 300 ) {
                BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                PingResponse ping = new Gson().fromJson(sb.toString(), PingResponse.class);
                return ping.version;
            }
            else
            {
                return "";
            }
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".GetVersion"); return "";}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".GetVersion"); }
            }
        }

    }

    /*
        This method uses the the "ping" path of the status resource to verify the user is
        authenticated.  Although this will return false for other reasons, too.  E.g. if
        the server is down.
     */
    public boolean CheckRegistration(String ticket) {
        HttpURLConnection webC = null;
        try {
            URL web = new URL(FTI_BaseURL + FTI_Ping + FTI_Ticket + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/json");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            int status = webC.getResponseCode();
            return (status != HttpURLConnection.HTTP_UNAUTHORIZED);
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".CheckRegistration"); return true;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".CheckRegistration"); }
            }
        }
    }

    /*
        Create a new account. Resource = /v1/user (POST)
     */
    public RegisterResponse Registration(RegisterRequest user){
        HttpURLConnection webC = null;
        Gson gson = new Gson();
        RegisterResponse rtn = new RegisterResponse();

        try {
            URL web = new URL(FTI_BaseURL + FTI_Register);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("POST");
            webC.setRequestProperty("Accept", "application/json");
            webC.setRequestProperty("Content-type", "application/json");
            webC.setDoOutput(true);
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            // Create the payload
            final OutputStream body = new BufferedOutputStream(webC.getOutputStream());
            final JsonWriter jwrite = new JsonWriter(new OutputStreamWriter(body, "UTF-8"));
            gson.toJson(user, RegisterRequest.class, jwrite);
            jwrite.flush();
            jwrite.close();
            // Processed the response
            int status = webC.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                rtn = new Gson().fromJson(sb.toString(), RegisterResponse.class);
            }
            rtn.response = status; // manually add status
            return rtn;
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".Registration"); rtn.response = 0; return rtn;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".Registration"); }
            }
        }
    }

    /*
        Update an account. Resource = /v1/user/<id>?ticket=<ticket> (POST)
     */
    public UserResponse ChgUser(String ticket, String id, UserRequest user){
        HttpURLConnection webC = null;
        Gson gson = new Gson();
        UserResponse rtn = new UserResponse();

        try {
            String realPath = FTI_RegisterExtra.replace(SUB_ZZZ, id);
            URL web = new URL(FTI_BaseURL + realPath + FTI_Ticket + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("POST");
            webC.setRequestProperty("Accept", "application/json");
            webC.setRequestProperty("Content-type", "application/json");
            webC.setDoOutput(true);
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            // Create the payload
            final OutputStream body = new BufferedOutputStream(webC.getOutputStream());
            final JsonWriter jwrite = new JsonWriter(new OutputStreamWriter(body, "UTF-8"));
            gson.toJson(user, UserRequest.class, jwrite);
            jwrite.flush();
            jwrite.close();
            // Processed the response
            int status = webC.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                rtn = new Gson().fromJson(sb.toString(), UserResponse.class);
            }
            rtn.response = status; // manually add status
            return rtn;
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".ChgUser"); rtn.response = 0; return rtn;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".ChgUser"); }
            }
        }
    }

    /*
        Retrieve an account. Resource = /v1/user/<id>?ticket=<ticket> (GET)
     */
    public UserResponse GetUser(String ticket, String id){
        HttpURLConnection webC = null;
        UserResponse rtn = new UserResponse();

        try {
            String realPath = FTI_RegisterExtra.replace(SUB_ZZZ, id);
            URL web = new URL(FTI_BaseURL + realPath + FTI_Ticket + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/json");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();

            int status = webC.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                rtn = new Gson().fromJson(sb.toString(), UserResponse.class);
            }
            rtn.response = status;  // manually add status
            return rtn;
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".GetUser"); rtn.response = 0; return rtn;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".GetUser"); }
            }
        }
    }

    /*
        Verify a new account. Resource = /v1/user/<id>?ticket=<ticket> (PUT)
     */
    public int Verify(String ticket, String id, VerifyRequest confirm){
        HttpURLConnection webC = null;
        Gson gson = new Gson();

        try {
            String realPath = FTI_RegisterExtra.replace(SUB_ZZZ, id);
            URL web = new URL(FTI_BaseURL + realPath + FTI_Ticket + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("PUT");
            webC.setRequestProperty("Accept", "application/json");
            webC.setRequestProperty("Content-type", "application/json");
            webC.setDoOutput(true);
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            // Create the payload
            final OutputStream body = new BufferedOutputStream(webC.getOutputStream());
            final JsonWriter jwrite = new JsonWriter(new OutputStreamWriter(body, "UTF-8"));
            gson.toJson(confirm, VerifyRequest.class, jwrite);
            jwrite.flush();
            jwrite.close();
            // Processed the response
            return webC.getResponseCode();
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".Verfiy"); return 0;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".Verfiy"); }
            }
        }
    }

    /*
        Retrieve connections by status. Resource = /v1/user/<id>/friend?ticket=<ticket> (GET)
     */
    public Invitations GetFriends(String ticket, String id){
        HttpURLConnection webC = null;
        Invitations rtn = new Invitations();

        try {
            String realPath = FTI_Friends.replace(SUB_ZZZ, id);
            URL web = new URL(FTI_BaseURL + realPath + FTI_Ticket + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/json");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();

            int status = webC.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                rtn = new Gson().fromJson(sb.toString(), Invitations.class);
            }
            rtn.response = status;  // manually add status
            return rtn;
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".GetFriends"); rtn.response = 0; return rtn;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".GetFriends"); }
            }
        }
    }

    /*
        Create a Prompt message. Resource = /v1/user/<id>/note?ticket=<ticket> (POST)
     */
    public PromptResponse NewPrompt(String ticket, String id, PromptRequest prompt){
        HttpURLConnection webC = null;
        Gson gson = new Gson();
        PromptResponse rtn = new PromptResponse();

        try {
            String realPath = FTI_Message.replace(SUB_ZZZ, id);
            URL web = new URL(FTI_BaseURL + realPath + FTI_Ticket + ticket);
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("POST");
            webC.setRequestProperty("Accept", "application/json");
            webC.setRequestProperty("Content-type", "application/json");
            webC.setDoOutput(true);
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            // Create the payload
            final OutputStream body = new BufferedOutputStream(webC.getOutputStream());
            final JsonWriter jwrite = new JsonWriter(new OutputStreamWriter(body, "UTF-8"));
            gson.toJson(prompt, PromptRequest.class, jwrite);
            jwrite.flush();
            jwrite.close();
            // Processed the response
            int status = webC.getResponseCode();
            if (status >= 200 && status < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                rtn = new Gson().fromJson(sb.toString(), PromptResponse.class);
            }
            rtn.response = status;  // manually add status
            return rtn;
        }
        catch (IOException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".NewReminder"); rtn.response = 0; return rtn;}
        finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                }
                catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".NewReminder"); }
            }
        }

    }


}

