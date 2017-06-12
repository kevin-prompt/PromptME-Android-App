package com.coolftc.prompt;

import java.util.List;

/**
 * This file contains the various request and reply model classes used for parsing the
 * JSON used in the Web Services.
 */
public interface  WebServiceModels {

    public class PingResponse
    {
        public String version;
    }

    public class RegisterRequest
    {
        public String uname;
        public boolean verify;
        public String timezone;
        public String dname;
        public int scycle;
        public String cname;
        public String device;
        public String target;
        public int type;
    }

    public class RegisterResponse
    {
        public long id;
        public String ticket;
        public int response;   // This is added locally to aid in processing
    }

    public class UserRequest
    {
        public String timezone;
        public String dname;
        public int scycle;
        public String cname;
        public String target;
        public int type;
    }

    public class UserResponse
    {
        public String dname;
        public int scycle;
        public String timezone;
        public String cname;
        public String uname;
        public boolean verified;
        public boolean ads;
        public boolean broadcast;
        public String created;
        public int response;   // This is added locally to aid in processing
    }

    public class VerifyRequest
    {
        public long code;
        public String provider;
        public String credential;
    }

    public class InviteResponse
    {
        public String fname;
        public long friendId;
        public String fdisplay;
        public int scycle;
        public String timezone;
        public boolean mirror;
        public boolean complete;
        public int response;   // This is added locally to aid in processing
    }

    public class Invitations
    {
        public List<InviteResponse> friends;
        public List<InviteResponse> rsvps;
        public List<InviteResponse> invites;
        public int response;   // This is added locally to aid in processing
    }

    public class PromptRequest
    {
        public String when;
        public String timezone;
        public int timename;
        public int timeadj;
        public int scycle;
        public long receiveId;
        public int units;
        public int period;
        public String end;
        public int recurs;
        public String message;
    }

    public class PromptResponse
    {
        public long noteId;
        public String noteTime;
        public int response;   // This is added locally to aid in processing
    }

}



