package com.bol.spinner.util;

import matrix.db.Context;
import matrix.db.MQLCommand;
import org.apache.commons.lang3.StringUtils;

public class MqlUtil {

    public static String runMql(Context context, String command) throws Exception {
        MQLCommand mqlCommand = new MQLCommand();
        boolean success = mqlCommand.executeCommand(context, command, true, true, false);
        if (success) {
            String result = mqlCommand.getResult();
            try {
                if (StringUtils.isEmpty(result)) {
                    return result;
                }
                if(result.endsWith("\n")){
                    result = result.substring(0, result.length() - 1);
                }
            } catch (Exception e) {
                throw new RuntimeException("GET RESULT ERROR:" + command + "\n" + e.getMessage());
            }
            return result;
        } else {
            throw new RuntimeException("Run MQL ERROR:" + command + "\n" + mqlCommand.getError());
        }
    }
}
