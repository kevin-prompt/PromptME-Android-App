package com.coolftc.prompt;

/*
 *  By using a standard interface in all fragments, all primary activities can
 *  be set up for Fragment -> Activity communication.  Just set up methods
 *  to use to communicate with.  If the activity does not have use of the
 *  methods, just implement with a throw UnsupportedOperationException().
 *  Of course, depending on how many methods and activities are involved
 *  it might be more understandable to have different interfaces performing
 *  this functionality.
 *
 *  While the Activity implements these methods, the fragment needs to
 *  call them.  It is suggested that an implementation of the interface
 *  be acquired in the onAttach() method of the fragment with the following:
 *      mActivity = (FragmentTalkBack) context;
 *  With appropriate error handling, of course.  The mActivity can then
 *  but used throughout the fragment to call the interface methods.
 *
 *  Note: The alternative to this is to just cast getActivty() to the type of
 *  the activity and then call some public method.  That does not work well
 *  if the fragment might be used in different activities.  This helps define
 *  what the fragment might want to use.
 */
public interface FragmentTalkBack {
    void setDate(String date);
    void setTime(String time);
    void newSort();
    void newInvite(String [] addresses, String display, boolean mirror);
}
