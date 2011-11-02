package giohji.tasks.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import giohji.tasks.R;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * This class does all authentication required to get the tokens needed
 * for Google Tasks and Google Calendar.
 * After the authentication is done, the tokens are saved under the account
 * with the auth token keys: TASKS_TOKEN_KEY and TASKS_SECRET_KEY.
 */
public class TasksLogin extends Activity {

    /** The Constant CALENDAR_LOGIN_REQUESTCODE. */
    public static final int CALENDAR_LOGIN_REQUESTCODE = 1;

    /** The Constant TASKS_LOGIN. */
    public static final String TASKS_LOGIN =
            "giohji.tasks.auth.LOGIN";

    /** The OAuth Consumer Key. */
    public static final String CONSUMER_KEY = "199205637057.apps.googleusercontent.com";

    /** The OAuth Consumer Secret. */
    public static final String CONSUMER_SECRET = "f36AMv-XLliwtNgK2U6mT3Hx";

    /** The Constant CALENDAR_REQUEST_TOKEN_URL. */
    private static final String REQ_TOKEN_URL =
            "https://www.google.com/accounts/OAuthGetRequestToken?xoauth_displayname=TasksWidget&scope=";

    /** The Constant CALENDAR_ACCESS_TOKEN_URL. */
    private static final String ACCESS_TOKEN_URL =
            "https://www.google.com/accounts/OAuthGetAccessToken";

    /** The Constant CALENDAR_AUTHORIZE_URL. */
    private static final String AUTHORIZE_URL =
            "https://www.google.com/accounts/OAuthAuthorizeToken?hd=default";

    /** The Constant ADD_ACCOUNT. */
    public static final String ADD_ACCOUNT = "tasks.addacount";

    /** Account type for AccountManager. */
    public static final String ACCOUNT_TYPE =
            "giohji.tasks.auth.TASKS_ACCOUNT";

    /** The Request Token key on SimpleSettingsManager. */
    public static final String TASKS_TOKEN_KEY =
            "com.compal.mylifestyle.widget.apps.todo.auth.requestToken";

    /** The Request Token Secret key on SimpleSettingsManager. */
    public static final String TASKS_SECRET_KEY =
            "com.compal.mylifestyle.widget.apps.todo.auth.requestTokenSecret";

    /** The Constant CALLBACK_URI. */
    public static final Uri CALLBACK_URI = Uri.parse("oauth-app://TasksWidget");

    /** The Constant TAG. */
    private static final String TAG = "CalendarLogin";

    /** The Constant PAGE. */
    public static final String PAGE = "page";

    /** The Constant USERNAME. */
    public static final String USERNAME = "username";

    /** The Scope of this authentication. (Which is Google Tasks) */
    private static final String SCOPE =
            "https://www.googleapis.com/auth/tasks";

    /** The intent. */
    private transient Intent mIntent;

    /** The OAuth consumer. */
    private static OAuthConsumer sConsumer = null;

    /** The OAuth provider. */
    private static OAuthProvider sProvider = null;

    /** used to store the temporary token **/
	private String mToken = null;

	/** used to store the temporary token secret**/
	private String mSecret = null;
    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String authUrl = null;
        mIntent = this.getIntent();
        //disabling screen orientation, so our activity is not destroyed during a http request if the user changes the orientation.
        disableScreenOrientation();
        final Bundle option = this.getIntent().getExtras();
        setContentView(R.layout.tasks_login);
        setResult(RESULT_OK, mIntent);
        final AccountManager accManager = AccountManager.get(this);
        //checks if we have internet connection.
        if (isOnline(this)) {
        	//checks if this activity was called to check the tokens
            if (option.get(ADD_ACCOUNT) == null) {
                checkToken();
            //else this activity was called to create a new account. If we have no account, the continue.
            } else if (accManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE).length == 0){
                try {
                	//creating our consumer and provider that we will use during the authentication process.
                    sConsumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
                    sProvider = new CommonsHttpOAuthProvider(REQ_TOKEN_URL
                            + URLEncoder.encode(SCOPE, "utf-8"), ACCESS_TOKEN_URL, AUTHORIZE_URL);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, e.getClass().getName() + e.getMessage());
                }
                //setting the content view to the webview that will show the grant access url.
                setContentView(R.layout.tasks_login);
                setResult(RESULT_OK, mIntent);
                try {
                	//getting the grant access url.
                    authUrl = authenticate();
                } catch (OAuthMessageSignerException e) {
                    Log.e(TAG, e.getClass().getName() + e.getMessage());
                    finish();
                } catch (OAuthNotAuthorizedException e) {
                    Log.e(TAG, e.getClass().getName() + e.getMessage());
                    finish();
                } catch (OAuthExpectationFailedException e) {
                    Log.e(TAG, e.getClass().getName() + e.getMessage());
                    finish();
                } catch (OAuthCommunicationException e) {
                    Log.e(TAG, e.getClass().getName() + e.getMessage());
                    connectionError();
                }
                //if we managed to get it, then load it.
                if (authUrl != null) {
                    final WebView webView = (WebView) findViewById(R.id.tasksLoginWebView);
                    webView.getSettings().setJavaScriptEnabled(true);
                    //setting the webview client that will call connectionReady after the user has granted the access.
                    webView.setWebViewClient(new TasksWebViewClient(this, webView));
                    Log.d(TAG, "authUrl " + authUrl);

                    final Activity activity = this;
                    setProgressBarVisibility(true);
                    webView.setWebChromeClient(new WebChromeClient() {
                        public void onProgressChanged(final WebView view, final int progress) {
                            activity.setProgress(progress * 100);
                        }
                    });

                    webView.loadUrl(authUrl);
                } else {
                    Log.d(TAG, "Connection Problem...");

                }
                // there is an account already! no need to create another.
            } else {
            	accountAlreadyAdded();
            }
            //we got a connection error.
        } else {
            connectionError();
        }
    }

    @Override
    protected void onDestroy() {
        enableScreenOrientation();
        super.onDestroy();
    }

    /**
     * Check if the Calendar/Tasks authentication token exists and return to the Authenticator
     * Service.
     */
    private void checkToken() {
    	final AccountManager accManager = AccountManager.get(this);
    	final Account[] accounts = accManager.getAccountsByType(ACCOUNT_TYPE);
    	Account acc = null;
    	if (accounts.length > 0) {
    		acc = accounts[0];
    	}
    	String mToken = null;
    	String mSecret = null;
    	if (acc != null) {
    		mToken = accManager.peekAuthToken(acc, TASKS_TOKEN_KEY);
    		mSecret = accManager.peekAuthToken(acc, TASKS_SECRET_KEY);
    	}
        if (mToken != null && mSecret != null) {
            final Bundle extras = this.getIntent().getExtras();
            final AccountAuthenticatorResponse response = extras
                    .getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, acc.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, mToken);
            response.onResult(result);
            finish();
        }
    }

    /**
     * Called when access to the Tasks service is granted by user.
     *
     * @param url
     *            the url with the verification code
     */
    public void connectionReady(final String url) {
        if (mIntent != null) {
            try {
                Log.d(TAG, "CalendarLogin::connectionReady");
                onAccessGranted(url);
            } catch (OAuthMessageSignerException e) {
                Log.e(TAG,
                        "OAuthMessageSignerException: " + e.getMessage());
                connectionError();
            } catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURLException: " + e.getMessage());
                connectionError();
            } catch (OAuthNotAuthorizedException e) {
                Log.e(TAG,
                        "OAuthNotAuthorizedException: " + e.getMessage());
                connectionError();
            } catch (OAuthExpectationFailedException e) {
                Log.e(TAG,
                        "OAuthExpectationFailedException: " + e.getMessage());
                connectionError();
            } catch (OAuthCommunicationException e) {
                Log.e(TAG,
                        "OAuthCommunicationException: " + e.getMessage());
                connectionError();
            }
            final Bundle extras = getIntent().getExtras();
            final AccountAuthenticatorResponse response = extras
                    .getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (response != null) {
            	final Bundle result = new Bundle();
            	result.putString(AccountManager.KEY_ACCOUNT_NAME, "Google Calendar");
            	result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
            	response.onResult(result);
            }
            finish();
        }
    }

    /**
     * Called when a connection error occurs.
     */
    private void connectionError() {
        Log.d(TAG, "Connection error");

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.tasks_dialog_conn_error);
        dialog.setTitle("Calendar service unavailable");
        dialog.setCancelable(true);
        final TextView txtView = (TextView) dialog.findViewById(R.id.textMessage);
        txtView.setText(
                "Service unavailable.\nCheck your internet and time settings and try again.");
        final Button button = (Button) dialog.findViewById(R.id.okButton);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.show();

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final AccountAuthenticatorResponse response = extras
                    .getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            final Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Connection error");
            response.onResult(result);
        }
    }
    /**
     * Called when an account has already been added.
     */
    private void accountAlreadyAdded() {
        Log.d(TAG, "Connection error");

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.tasks_dialog_conn_error);
        dialog.setTitle("An account has already been added!");
        dialog.setCancelable(true);
        final TextView txtView = (TextView) dialog.findViewById(R.id.textMessage);
        txtView.setText(
                "Please delete that account first to add another one.");
        final Button button = (Button) dialog.findViewById(R.id.okButton);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.show();

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final AccountAuthenticatorResponse response = extras
                    .getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            final Bundle result = new Bundle();
            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_REQUEST);
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Account already added");
            response.onResult(result);
        }
    }

    /**
     * Checks if is online.
     *
     * @param context
     *            the context
     * @return true, if is online
     */
    private static boolean isOnline(final Context context) {
        boolean isConnected = false;
        try {
            final ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connManager.getActiveNetworkInfo() == null) {
                Log.d(TAG, "getActiveNetworkInfo " + connManager.getActiveNetworkInfo());
            } else {
                if (connManager.getActiveNetworkInfo().isConnectedOrConnecting()) {
                    Log.d(TAG, "isConnectedOrConnecting "
                            + connManager.getActiveNetworkInfo().isConnectedOrConnecting());

                    Log.d(TAG, "conn 1");
                    final URL url = new URL("http://google.com.br");
                    Log.d(TAG, "conn 2");
                    final HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                    Log.d(TAG, "conn 3");
                    urlc.setRequestProperty("User-Agent", "Calendar Test");
                    Log.d(TAG, "conn 4");
                    urlc.setRequestProperty("Connection", "close");
                    Log.d(TAG, "conn 5");
                    urlc.setConnectTimeout(3000); // mTimeout is in seconds
                    Log.d(TAG, "conn 6");
                    urlc.connect();
                    Log.d(TAG, "conn 7");
                    isConnected = true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }

        return isConnected;

    }
    /**
     * Retrieves the Google authentication URL.
     *
     * @return the authentication URL.
     * @throws OAuthMessageSignerException
     *             the o auth message signer exception
     * @throws OAuthNotAuthorizedException
     *             the o auth not authorized exception
     * @throws OAuthExpectationFailedException
     *             the o auth expectation failed exception
     * @throws OAuthCommunicationException
     *             the o auth communication exception
     */
    private String authenticate() throws OAuthMessageSignerException, OAuthNotAuthorizedException,
            OAuthExpectationFailedException, OAuthCommunicationException {

        sProvider.setOAuth10a(true);

        Log.i(TAG, "Fetching request token...");

        String authUrl = null;

        authUrl = sProvider.retrieveRequestToken(sConsumer, CALLBACK_URI.toString());
        mToken = sConsumer.getToken();
        mSecret =  sConsumer.getTokenSecret();

        Log.d(TAG, "Request token: " + sConsumer.getToken());
        Log.d(TAG, "Token secret: " + sConsumer.getTokenSecret());

        return authUrl;
    }

    /**
     * Parses the verification code and retrieve the access token.
     *
     * @param accessresult
     *            the string with the verification code.
     * @param context
     *            the context
     * @throws OAuthMessageSignerException
     *             the oauth message signer exception
     * @throws MalformedURLException
     *             the malformed url exception
     * @throws OAuthNotAuthorizedException
     *             the o auth not authorized exception
     * @throws OAuthExpectationFailedException
     *             the o auth expectation failed exception
     * @throws OAuthCommunicationException
     *             the o auth communication exception
     */
    private void onAccessGranted(final String accessresult)
            throws OAuthMessageSignerException, MalformedURLException, OAuthNotAuthorizedException,
            OAuthExpectationFailedException, OAuthCommunicationException {
        final String mAccessTokenURL = accessresult;
    	final AccountManager accManager = AccountManager.get(this);
        final Account account = new Account("Tasks", TasksLogin.ACCOUNT_TYPE);
        accManager.addAccountExplicitly(account, null, null);
        accManager.setUserData(account, PAGE, "0");
        if (!(mToken == null) || !(mSecret == null)) {
            sConsumer.setTokenWithSecret(mToken, mSecret);
        }
        Log.d(TAG, "Access token 2: " + sConsumer.getToken());
        Log.d(TAG, "Token secret 2: " + sConsumer.getTokenSecret());

        final String mVerifier = Uri.parse(mAccessTokenURL).getQueryParameter(OAuth.OAUTH_VERIFIER);

        sProvider.retrieveAccessToken(sConsumer, mVerifier);

        saveAuthTokens(sConsumer.getToken(), sConsumer.getTokenSecret());

        Log.d(TAG, "Access token: " + sConsumer.getToken());
        Log.d(TAG, "Token secret: " + sConsumer.getTokenSecret());

    }
    /**
     * Save the authentication tokens.
     *
     * @param token
     *            the token
     * @param tokenSecret
     *            the token secret
     */
    private void saveAuthTokens(final String token, final String tokenSecret) {
    	final AccountManager accManager = AccountManager.get(this);
    	final Account[] accounts = accManager.getAccountsByType(ACCOUNT_TYPE);
    	Account acc = null;
    	if (accounts.length > 0) {
    		acc = accounts[0];
    	}
    	if (acc != null) {
    		accManager.setAuthToken(acc, TASKS_TOKEN_KEY, token);
    		accManager.setAuthToken(acc, TASKS_SECRET_KEY, tokenSecret);
    	}
    }

    private void disableScreenOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void enableScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
