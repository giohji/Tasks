package giohji.tasks.controller;

import java.util.List;

import giohji.tasks.R;
import giohji.tasks.auth.TasksLogin;
import giohji.tasks.model.Task;
import giohji.tasks.model.TaskList;
import giohji.tasks.model.TasksAdapter;
import giohji.tasks.model.TasksWidgetProvider;
import giohji.tasks.model.TasksWidgetProvider.UpdateService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
/**
 * This class implements the service that is responsible for executing the user requests
 * (that are fired when the user clicks one of the widget's buttons).
 */
public class ControlService extends Service{
	/**
	 * The TAG for logging.
	 */
	public static final String LOG_TAG = "ControlService";
	/**
	 * The key to retrieve the control action integer from the intent.
	 */
	public static final String CONTROL_ACTION = "giohji.tasks.controller.ControlService.CONTROL_ACTION";
	/**
	 * The key to retrieve the Task from the intent.
	 */
	public static final String TASK = "giohji.tasks.controller.ControlService.TASK";
	/**
	 * The key to send the no refresh flag to the UpdateService.
	 */
	public static final String NO_REFRESH = "giohji.tasks.controller.ControlService.NO_REFRESH";
	/**
	 * The refresh action.
	 */
	public static final int REFRESH = 1;
	/**
	 * The clear completed tasks action.
	 */
	public static final int CLEAR_COMPLETED = 2;
	/**
	 * The update task action.
	 */
	public static final int UPDATE_TASK = 3;
	/**
	 * The next page action.
	 */
	public static final int NEXT_PAGE = 4;
	/**
	 * The previous page action.
	 */
	public static final int PREVIOUS_PAGE = 5;
	/**
	 * The go to non empty page action.
	 */
	public static final int GO_TO_NON_EMPTY_PAGE = 6;
	/**
	 * The intent.
	 */
	private Intent mIntent;
	/**
	 * The Account Manager
	 */
	private AccountManager mAccManager;
	/**
	 * The Google Tasks Account.
	 */
	private Account mAccount = null;

	@Override
	public void onStart(final Intent intent, final int startId) {
		mIntent = intent;
		mAccManager = AccountManager.get(this);
		final Account[] accounts = mAccManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE);
		final ComponentName thisWidget = new ComponentName(this, TasksWidgetProvider.class);
		final AppWidgetManager manager = AppWidgetManager.getInstance(this);
		//Only try to execute the action if there is a Google Tasks account.
		if (accounts.length > 0) {
			// Push refresh layout for this widget to the home screen
			final RemoteViews widgetView = new RemoteViews(this.getPackageName(), R.layout.tasks_widget_refreshing);
			manager.updateAppWidget(thisWidget, widgetView);
			mAccount = accounts[0];
			//Sometimes Android runs GC on the TaskAdapter, so we must verify if its instance is still valid,
			//otherwise we must initialize it again with the token and the token secret.
			if (TasksAdapter.getAdapter() == null) {
				final String token = mAccManager.peekAuthToken(mAccount, TasksLogin.TASKS_TOKEN_KEY);
				final String secret = mAccManager.peekAuthToken(mAccount, TasksLogin.TASKS_SECRET_KEY);
				TasksAdapter.initialize(secret, token);
				Log.d(LOG_TAG, "Adapter initialized!");
			}
			final int controlAction = mIntent.getIntExtra(CONTROL_ACTION, 0);
			//execute the action!
			executeAction(controlAction);
		} else {
			final RemoteViews widgetView = new RemoteViews(this.getPackageName(), R.layout.tasks_widget_noaccount);
			manager.updateAppWidget(thisWidget, widgetView);
		}
	}
	/**
	 * This method executes the desired action given the controlAction.
	 * @param controlAction
	 * The action to be executed. Can be REFRESH, CLEAR_COMPLETED, UPDATE_TASK,
	 * NEXT_PAGE, PREVIOUS_PAGE and GO_TO_NON_EMPTY_PAGE.
	 */
	private void executeAction(final int controlAction) {
		switch (controlAction) {
		//refreshes the widget.
		case REFRESH:
			startService(new Intent(this, UpdateService.class));
			break;
		//clear all completed widget from list.
		case CLEAR_COMPLETED:
			final List<TaskList> taskLists = TasksAdapter.getAdapter().getTaskList();
			if (taskLists != null && !taskLists.isEmpty()) {
				final String taskListId = taskLists.get(0).getId();
				TasksAdapter.getAdapter().clearCompleted(taskListId);
			}
			startService(new Intent(this, UpdateService.class));
			break;
		//update the task passed through the TASK extra.
		case UPDATE_TASK:
			final Task task = mIntent.getParcelableExtra(TASK);
			if ("completed".equals(task.getStatus())) {
				task.setStatus("needsAction");
			} else {
				task.setStatus("completed");
			}
			TasksAdapter.getAdapter().updateTask(task);
			startService(new Intent(this, UpdateService.class));
			break;
		//goes to the next page.
		case NEXT_PAGE:
			final int nextPage = Integer.parseInt(AccountManager.get(this).getUserData(mAccount, TasksLogin.PAGE)) + 1;
			mAccManager.setUserData(mAccount, TasksLogin.PAGE, String.valueOf(nextPage));
			final Intent nextPageIntent = new Intent(this, UpdateService.class);
			nextPageIntent.putExtra(NO_REFRESH, true);
			startService(nextPageIntent);
			break;
		//goes to the previous page.
		case PREVIOUS_PAGE:
			int previousPage = Integer.parseInt(AccountManager.get(this).getUserData(mAccount, TasksLogin.PAGE)) - 1;
			if (previousPage < 0) {
				previousPage = 0;
			}
			AccountManager.get(this).setUserData(mAccount, TasksLogin.PAGE, String.valueOf(previousPage));
			final Intent previousPageIntent = new Intent(this, UpdateService.class);
			previousPageIntent.putExtra(NO_REFRESH, true);
			startService(previousPageIntent);
			break;
		//goes to the last non empty page.
		case GO_TO_NON_EMPTY_PAGE:
			final int nonBlankPage = (TasksAdapter.getAdapter().getTaskList().get(0).getTasks().size() - 1) / 5;
			AccountManager.get(this).setUserData(mAccount, TasksLogin.PAGE, String.valueOf(nonBlankPage));
			final Intent nonBlankPageIntent = new Intent(this, UpdateService.class);
			nonBlankPageIntent.putExtra(NO_REFRESH, true);
			startService(nonBlankPageIntent);
			break;
		default:
			break;
		}
	}
	/**
	 * Ignore this method, we do not use it since this service is only used by
	 * the Tasks.apk itself.
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}
	

}
