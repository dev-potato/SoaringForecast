package org.soaringforecast.rasp.messages;

import android.support.design.widget.Snackbar;

public final class SnackbarMessage {

    private final String message;
    // Snackbar.LENGTH_SHORT,...
    private final int duration;

    public SnackbarMessage(String message, int duration){
            this.message = message;
        this.duration = duration;
    }

    public SnackbarMessage(String message){
        this.message = message;
        this.duration = Snackbar.LENGTH_SHORT;
    }

    public String getMessage() {
        return message;
    }

    public int getDuration() {
        return duration;
    }
}
