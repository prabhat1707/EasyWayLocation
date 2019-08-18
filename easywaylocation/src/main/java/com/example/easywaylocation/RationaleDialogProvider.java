package com.example.easywaylocation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;

/**
 * Created by prabhat on 2/10/18.
 */

public class RationaleDialogProvider extends FragmentActivity {
    private final WeakReference<Activity> weakActivity;
    private static final String TAG = RationaleDialogProvider.class.getSimpleName();

    private String mMessage;
    private int requestCode;
    private String mTitle;

    /**
     * Constructor.
     * <p>
     * Takes a reference to the calling Activity as a parameter and assigns it to a WeakReference
     * object in order to allow it to be properly garbage-collected if necessary.
     * <p>
     * Any method that relies on the Activity attempts to re-acquire a strong reference to it,
     * checks its state (for example not null, not finishing etc.) and exits gracefully if it
     * is no longer available.
     * <p>
     * The default dialog title and message body are assigned here but can be altered at
     * run-time with a call to setTitle() and/or setMessage() as necessary.
     */
    RationaleDialogProvider(Activity activity, int requestCode) {
        // assign the activity to the weak reference
        this.weakActivity = new WeakReference<>(activity);
        this.requestCode = requestCode;
        // Set the default title and message for the dialog
        mTitle = activity.getString(R.string.deviceLocationUtil_default_rationale_request_title);
        mMessage = activity.getString(R.string.deviceLocationUtil_default_rationale_request_messageBody);
    }


    /**
     * Displays an alert dialog to the user with the title set by mTitle and the message set
     * by mMessage. The default values are assigned in the constructor but can be changed
     * at run-time using the setTitle() and setMessage() methods if necessary.
     *
     * @param callback An interface which must be implemented by the caller in order to listen
     *                 for when the OK button has been pressed and the dialog dismissed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void displayDialog(final RequestCallback.PermissionRequestCallback callback) {
        // Re-acquire a strong reference to the calling activity and verify that it still exists and is active
        Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            // Activity is no longer valid, don't do anything
            return;
        }

        // Build the alert
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(mTitle)
                .setMessage(mMessage)
                .setCancelable(false)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        callback.onRationaleDialogOkPressed(requestCode);
                    }
                });

        // Display the alert
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

}// End RationaleDialogProvider nested class

