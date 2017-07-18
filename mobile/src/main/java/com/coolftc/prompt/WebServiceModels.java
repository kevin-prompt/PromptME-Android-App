package com.coolftc.prompt;

import java.util.List;

/**
 * This file contains the various request and reply model classes used for parsing the
 * JSON used in the Web Services.
 */
public interface  WebServiceModels {

    class PingResponse
    {
        String version;
        int response;   // This is added locally to aid in processing
    }

    class RegisterRequest
    {
        String uname;
        boolean verify;
        String timezone;
        String dname;
        int scycle;
        String cname;
        String device;
        String target;
        int type;
    }

    class RegisterResponse
    {
        long id;
        String ticket;
        int response;   // This is added locally to aid in processing
    }

    class UserRequest
    {
        String timezone;
        String dname;
        int scycle;
        String target;
        int type;
    }

    class UserResponse
    {
        String dname;
        int scycle;
        String timezone;
        String cname;
        String uname;
        boolean verified;
        boolean ads;
        boolean broadcast;
        String created;
        int response;   // This is added locally to aid in processing
    }

    class VerifyRequest
    {
        long code;
        String provider;
        String credential;
    }

    class InviteResponse
    {
        String fname;
        long friendId;
        String fdisplay;
        int scycle;
        String timezone;
        boolean mirror;
        boolean complete;
        int response;   // This is added locally to aid in processing
    }

    class Invitations
    {
        List<InviteResponse> friends;
        List<InviteResponse> rsvps;
        List<InviteResponse> invites;
        int response;   // This is added locally to aid in processing
    }

    class PromptRequest
    {
        String when;
        String timezone;
        int timename;
        int timeadj;
        int scycle;
        long receiveId;
        int units;
        int period;
        String end;
        int recurs;
        String message;
    }

    class PromptResponse
    {
        long noteId;
        String noteTime;
        int response;   // This is added locally to aid in processing
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
