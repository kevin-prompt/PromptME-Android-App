package com.coolftc.prompt;

/*
 *  By using a standard interface in all fragments, all primary activities can
 *  be set up for Fragment -> Activity communication.  Just set up methods
 *  to use to communicate with.
 *
 *  Note: The alternative to this is to just cast getActivty() to the type of
 *  the activity and then call some public method.  That does not work well
 *  if the fragment might be used in different activities.  This helps define
 *  what the fragment might want to use.
 */
public interface FragmentTalkBack {
    void setDate(String date);
    void setTime(String time);
}
