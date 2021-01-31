package com.coolftc.prompt.source

/**
 * This file contains the various request and reply model classes used for parsing the
 * JSON used in the Web Services calls.
 */

data class WebServiceModels(val filler: Int)

/* Used to get the base domain name for the API. */
data class BaseCamp(val Host: String?, val Path: String?, val Parameter: String?, val Auth: String?)

data class PingResponse(val version: String?)

/* Used to sign up for the service. */
data class RegisterRequest(var uname: String?,
                           var verify: Boolean,
                           var timezone: String?,
                           var dname: String?,
                           var scycle: Int,
                           var cname: String?,
                           var device: String?,
                           var target: String?,
                           var type: Int)

data class RegisterResponse(var id: Long, var ticket: String?)

/* Any post registration updates to the user. */
data class UserRequest(var timezone: String?,
                       var dname: String?,
                       var scycle: Int,
                       var target: String?,
                       var type: Int)

/* User details that friends can see. */
data class UserResponse(var dname: String?,
                        var scycle: Int,
                        var timezone: String?,
                        var cname: String?,
                        var uname: String?,
                        var verified: Boolean,
                        var ads: Boolean,
                        var broadcast: Boolean,
                        var created: String?)

/* After registration, need to verify the user. */
data class VerifyRequest(var code: Long, var provider: String?, var credential: String?)

/* To make friends, both parties have to accept the invites. */
data class InviteRequest(var fname: String?, var fdisplay: String?, var message: String?, var mirror: Boolean)

data class InviteResponse(var fname: String?,
                          var friendId: Long,
                          var fdisplay: String?,
                          var scycle: Int,
                          var timezone: String?,
                          var mirror: Boolean,
                          var complete: Boolean)

data class Invitations(var friends: List<WebServiceModelsOld.InviteResponse?>?,
                       var rsvps: List<WebServiceModelsOld.InviteResponse?>?,
                       var invites: List<WebServiceModelsOld.InviteResponse?>?)

/* Create a prompt. */
data class PromptRequest(var `when`: String?,
                         var timezone: String?,
                         var timename: Int,
                         var timeadj: Int,
                         var scycle: Int,
                         var receiveId: Long,
                         var units: Int,
                         var period: Int,
                         var end: String?,
                         var recurs: Int,
                         var groupId: Int,
                         var message: String?)

data class PromptResponse(var promptId: Long, var promptTime: String?)

/* Set a snooze for a prompt. */
data class SnoozeRequest(var `when`: String?,
                         var timezone: String?,
                         var snoozeId: Long,
                         var senderId: Long,
                         var message: String?)