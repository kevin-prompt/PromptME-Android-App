package com.coolftc.prompt.utility;

import com.coolftc.prompt.utility.ExpClass;

public class ExpParseToCalendar extends ExpClass {

    private static final long serialVersionUID = 1564387184956823057L;
    public static final int KTIME_ERR_CODE = 18001;
    public static final String ExpParseToCalendar_NAME = "ParseToCalendarErr";

    public ExpParseToCalendar(String desc, Throwable source) {
        super(KTIME_ERR_CODE, ExpParseToCalendar_NAME, desc, source);
    }
}