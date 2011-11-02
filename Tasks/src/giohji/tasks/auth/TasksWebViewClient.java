package giohji.tasks.auth;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * This class is responsible for listening when the user reaches the final url indicating
 * that he granted permission to his google tasks account.
 */
public class TasksWebViewClient extends WebViewClient {

    /** The tasks login activity. */
    private final transient TasksLogin mTasksLogin;
    /** The spinner dialog box. */
    private final transient ProgressDialog mSpinner;

    /**
     * Instantiates a new calendar web view client.
     *
     * @param calendarLogin
     *            the calendar login
     * @param webView
     */
    public TasksWebViewClient(final TasksLogin calendarLogin, final WebView webView) {
        super();
        this.mTasksLogin = calendarLogin;
        mSpinner = new ProgressDialog(webView.getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mSpinner.show();
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        super.onPageFinished(view, url);
        mSpinner.dismiss();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView,
     * java.lang.String)
     */
    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        Log.d("WebViewClient", "URL= " + url);
        // if it is the URL indicating that the user has granted access, then we must notify TasksLogin
        //by calling connectionReady.
        if (url.contains(TasksLogin.CALLBACK_URI.toString())) {
        	mTasksLogin.connectionReady(url);
        	// else, only load an URL if it is part of access granting process.
        } else if (url.contains("OAuthAuthorizeToken")
                || url.contains("https://www.google.com/accounts/Logout?continue")) {
            view.loadUrl(url);
        }
        return true;
    }
}
