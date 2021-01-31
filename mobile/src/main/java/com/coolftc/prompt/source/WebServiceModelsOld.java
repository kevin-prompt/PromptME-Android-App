package com.coolftc.prompt.source;

import java.util.List;

/**
 * This file contains the various request and reply model classes used for parsing the
 * JSON used in the Web Services.
 */
public interface WebServiceModelsOld {

    class PingResponse {
        public String version;
        public int response;   // This is added locally to aid in processing
    }

    class RegisterRequest
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

    class RegisterResponse
    {
        public long id;
        public String ticket;
        public int response;   // This is added locally to aid in processing
    }

    class UserRequest
    {
        public String timezone;
        public String dname;
        public int scycle;
        public String target;
        public int type;
    }

    class UserResponse
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

    class VerifyRequest
    {
        public long code;
        public String provider;
        public String credential;
    }

    class InviteRequest
    {
        public String fname;
        public String fdisplay;
        public String message;
        public boolean mirror;
        public int response;   // This is added locally to aid in processing
    }

    class InviteResponse
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

    class Invitations
    {
        public List<InviteResponse> friends;
        public List<InviteResponse> rsvps;
        public List<InviteResponse> invites;
        public int response;   // This is added locally to aid in processing
    }

    class PromptRequest
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
        public int groupId;
        public String message;
    }

    class PromptResponse
    {
        public long noteId;
        public String noteTime;
        public int response;   // This is added locally to aid in processing
    }

    class SnoozeRequest
    {
        public String when;
        public String timezone;
        public long snoozeId;
        public long senderId;
        public String message;
    }

}
