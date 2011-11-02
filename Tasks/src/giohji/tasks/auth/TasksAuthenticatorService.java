package giohji.tasks.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * This class implements the interface needed for implementation of AccountManager for Google Tasks.
 */
public class TasksAuthenticatorService extends Service {
    /**
     * The account authenticator singleton.
     */
    private static AccountAuthenticatorImpl sAccountAuthenticator = null;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(final Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(
                android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = getAuthenticator().getIBinder();
        }
        return ret;
    }
    /**
     * Gets the AccountAuthenticatorImpl singleton.
     * @return
     * the account authenticator needed for AccountManager.
     */
    private AccountAuthenticatorImpl getAuthenticator() {
        if (sAccountAuthenticator == null) {
            sAccountAuthenticator = new AccountAuthenticatorImpl(this);
        }

        return sAccountAuthenticator;

    }
    /**
     * Implements AbstractAccountAuthenticator which is needed for AccountManager's
     * AccountAuthenticators.
     */
    private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {

        public AccountAuthenticatorImpl(final Context context) {
            super(context);
        }

        @Override
        public Bundle addAccount(final AccountAuthenticatorResponse response,
                final String accountType, final String authTokenType,
                final String[] requiredFeatures, final Bundle options)
                throws NetworkErrorException {
            final Bundle reply = new Bundle();
            final Intent loginIntent = new Intent(TasksLogin.TASKS_LOGIN);
            loginIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            loginIntent.putExtra(TasksLogin.ADD_ACCOUNT, true);
            reply.putParcelable(AccountManager.KEY_INTENT, loginIntent);
            return reply;
        }

        @Override
        public Bundle confirmCredentials(final AccountAuthenticatorResponse response,
                final Account account, final Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle editProperties(final AccountAuthenticatorResponse response,
                final String accountType) {
            return null;
        }

        @Override
        public Bundle getAuthToken(final AccountAuthenticatorResponse response,
                final Account account, final String authTokenType, final Bundle options)
                throws NetworkErrorException {
            final Bundle reply = new Bundle();
            final Intent loginIntent = new Intent(TasksLogin.TASKS_LOGIN);
            loginIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            loginIntent.putExtra(TasksLogin.USERNAME, account.name);
            reply.putParcelable(AccountManager.KEY_INTENT, loginIntent);
            return reply;
        }

        @Override
        public String getAuthTokenLabel(final String authTokenType) {
            return null;
        }

        @Override
        public Bundle hasFeatures(final AccountAuthenticatorResponse response,
                final Account account, final String[] features) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle updateCredentials(final AccountAuthenticatorResponse response,
                final Account account, final String authTokenType, final Bundle options)
                throws NetworkErrorException {
            return null;
        }

    }

}
