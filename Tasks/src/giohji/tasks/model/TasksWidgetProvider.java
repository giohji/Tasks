package giohji.tasks.model;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import giohji.tasks.R;
import giohji.tasks.auth.TasksLogin;
import giohji.tasks.controller.ControlService;
import giohji.tasks.view.EditTasksActivity;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
/**
 * This is the class responsible for extending AppWidgetProvider, it is responsible for
 * treating the broadcasts directed to the Tasks widget. 
 */
public class TasksWidgetProvider extends AppWidgetProvider {
	/**
	 * The TAG for logging.
	 */
	private static final String LOG_TAG = "TasksWidgetProvider";
	/**
	 * Array with all the checkboxes' ImageButton resource IDs.
	 */
	private static final int[] CHECKBOX = {
		R.id.checkbox1,
		R.id.checkbox2,
		R.id.checkbox3,
		R.id.checkbox4,
		R.id.checkbox5
	};
	/**
	 * Array with all the titles' TextView resource IDs.
	 */
	private static final int[] TITLE = {
		R.id.title1,
		R.id.title2,
		R.id.title3,
		R.id.title4,
		R.id.title5
	};
	/**
	 * Array with all the notes' TextView resource IDs.
	 */
	private static final int[] NOTES = {
		R.id.notes1,
		R.id.notes2,
		R.id.notes3,
		R.id.notes4,
		R.id.notes5		
	};
	/**
	 * Array with all the dates' TextView resource IDs.
	 */
	private static final int[] DATE = {
		R.id.date1,
		R.id.date2,		
		R.id.date3,		
		R.id.date4,		
		R.id.date5,
	};
	/**
	 * Instance of the latest TaskList, used for caching when we do not need to refresh the whole list.
	 */
	private static TaskList mTaskList;
	@Override
	public void onEnabled (final Context context) {
		//initializing the TasksAdapter with the token and the token secret from the Google Tasks account.
    	final AccountManager accManager = AccountManager.get(context);
    	final Account[] accounts = accManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE);
    	if (accounts.length > 0) {
    		final Account acc = accounts[0];
    		final String token = accManager.peekAuthToken(acc, TasksLogin.TASKS_TOKEN_KEY);
    		final String secret = accManager.peekAuthToken(acc, TasksLogin.TASKS_SECRET_KEY);
   			TasksAdapter.initialize(secret, token);
   			Log.d(LOG_TAG, "Adapter initialized!");
    	}
		Log.d(LOG_TAG, "onEnabled called!");
	}
	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d(LOG_TAG, "onUpdate()");
		// Push refresh layout for this widget to the home screen
		final RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.tasks_widget_refreshing);
		final ComponentName thisWidget = new ComponentName(context, TasksWidgetProvider.class);
		final AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(thisWidget, widgetView);
		//We must update the widget, but since BroadcastReceivers are not guaranteed to execute long tasks
		//we must perform the update in a service.
        context.startService(new Intent(context, UpdateService.class));
	}
	/**
	 * This class is responsible for implementing the Service that refreshes the widget and updates its UI.
	 */
	public static class UpdateService extends Service {
		@Override
		public void onStart(final Intent intent, final int startId) {
			Log.d(LOG_TAG, "onStart");
			final AccountManager accManager = AccountManager.get(this);
			final Account[] accounts = accManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE);
			RemoteViews widgetView;
			//Only refresh the widget if we have a Google Tasks account.
			if (accounts.length > 0) {
				final Account acc = accounts[0];
				//We must make sure Android's GC didn't collect our TasksAdapter, if it did
				//we must initialize it once again!
				if (TasksAdapter.getAdapter() == null) {
					final String token = accManager.peekAuthToken(acc, TasksLogin.TASKS_TOKEN_KEY);
					final String secret = accManager.peekAuthToken(acc, TasksLogin.TASKS_SECRET_KEY);
					TasksAdapter.initialize(secret, token);
					Log.d(LOG_TAG, "Adapter initialized!");
				}
				//if there is no need to refresh (according to the NO_REFRESH flag) AND we got the TaskList cached, then just build the
				//remote view using it.
				if (intent.getBooleanExtra(ControlService.NO_REFRESH, false) && mTaskList != null && mTaskList.getTasks().size() > 0 ) {
					final int page = Integer.parseInt(accManager.getUserData(acc, TasksLogin.PAGE));
					widgetView = buildUpdateView(this, mTaskList.getTasks(), page);
				//else we must refresh the whole list.
				} else {
					//Make the API call to get the TaskList
					final List<TaskList> taskLists = TasksAdapter.getAdapter().getTaskList();
					//Verify if the call executed successfully.
					if (taskLists != null && !taskLists.isEmpty()) {
						mTaskList = taskLists.get(0);
						mTaskList.setTasks(TasksAdapter.getAdapter().getTasks(mTaskList.getId()));
						final int page = Integer.parseInt(accManager.getUserData(acc, TasksLogin.PAGE));
						//build the update view with the retrieved list of Tasks.
						widgetView = buildUpdateView(this, mTaskList.getTasks(), page);
					//else we got a connection problem.
					} else {
						widgetView = buildNoConnectionView(this);
					}
				}
			//We have no account, prompt the user to create a new account.
			}  else {
				final Intent loginIntent = new Intent(TasksLogin.TASKS_LOGIN);
				loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				loginIntent.putExtra(TasksLogin.ADD_ACCOUNT, true);
				startActivity(loginIntent);
				widgetView = buildNoAccountView(this);
			}
			// Push update for this widget to the home screen
			final ComponentName thisWidget = new ComponentName(this, TasksWidgetProvider.class);
			final AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(thisWidget, widgetView);
		}
		/**
		 * Used to build the "No Account" view.
		 * @param context
		 * The context used to create the RemoteView
		 * @return
		 * The RemoteView indicating that no account has been added.
		 */
		private RemoteViews buildNoAccountView(final Context context) {
			final RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.tasks_widget_noaccount);
			final Intent intent = new Intent(context, UpdateService.class);
			final PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.noAccountText, pendingIntent);
			return view;
		}
		/**
		 * Used to build the "No Connection" view.
		 * @param context
		 * The context used to create the RemoteView
		 * @return
		 * The RemoteView indicating that there is no Internet connection.
		 */
		private RemoteViews buildNoConnectionView(final Context context) {
			final RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.tasks_widget_noconnection);
			final Intent intent = new Intent(context, UpdateService.class);
			final PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.noConnectionText, pendingIntent);
			return view;
		}
		/**
		 * 
		 * @param context
		 * The context used to create the RemoteView
		 * @param taskList
		 * The taskList used to populate the View's items.
		 * @param page
		 * The page that the widget is showing.
		 * @return
		 * The RemoteView built using the taskList.
		 */
		private RemoteViews buildUpdateView(final Context context, final List<Task> taskList, final int page) {
			final RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.tasks_widget);
			//if we are on the first page then we don't need the previous page button.
			if (page == 0) {
				 view.setViewVisibility(R.id.previousButton, View.INVISIBLE);
			//else we show the previous page button and set its PendingIntent to trigger the
			//ControlService with the PREVIOUS_PAGE controlAction.
			} else {
				view.setViewVisibility(R.id.previousButton, View.VISIBLE);
				final Intent intent = new Intent(context, ControlService.class);
				intent.setAction("avoiding previouspageintent conflicts" + System.currentTimeMillis());
				intent.putExtra(ControlService.CONTROL_ACTION, ControlService.PREVIOUS_PAGE);
				final PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				view.setOnClickPendingIntent(R.id.previousButton, pendingIntent);
			}
			//if we are on the last page (or greater) then we don't need the next page button.
			if ((5 + page * 5) >= taskList.size()) {
				 view.setViewVisibility(R.id.nextButton, View.INVISIBLE);
				 //if we are on a page that is empty, then we must return to a non-empty page.
				 if(page * 5 >= taskList.size()) {
						final Intent intent = new Intent(context, ControlService.class);
						intent.setAction("avoiding nonemptypageintent conflicts" + System.currentTimeMillis());
						intent.putExtra(ControlService.CONTROL_ACTION, ControlService.GO_TO_NON_EMPTY_PAGE);
						startService(intent);
				 }
			//if we are not on the last page, then we need a next page button.
			} else {
				view.setViewVisibility(R.id.nextButton, View.VISIBLE);
				final Intent intent = new Intent(context, ControlService.class);
				intent.setAction("avoiding nextpageintent conflicts" + System.currentTimeMillis());
				intent.putExtra(ControlService.CONTROL_ACTION, ControlService.NEXT_PAGE);
				final PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				view.setOnClickPendingIntent(R.id.nextButton, pendingIntent);
			}
			//Adding the PendingIntent to the clear button.
			final Intent clearIntent = new Intent(context, ControlService.class);
			clearIntent.setAction("avoiding clearintent conflicts" + System.currentTimeMillis());
			clearIntent.putExtra(ControlService.CONTROL_ACTION, ControlService.CLEAR_COMPLETED);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.clearButton, pendingIntent);
			//Adding the PendingIntent to the refresh button.
			final Intent refreshIntent = new Intent(context, ControlService.class);
			refreshIntent.setAction("avoiding refreshintent conflicts" + System.currentTimeMillis());
			refreshIntent.putExtra(ControlService.CONTROL_ACTION, ControlService.REFRESH);
			pendingIntent = PendingIntent.getService(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.refreshButton, pendingIntent);
			//Adding the PendingIntent to the new button.
			final Intent newTaskIntent = new Intent(context, EditTasksActivity.class);
			newTaskIntent.setAction("avoiding newTaskintent conflicts" + System.currentTimeMillis());
			newTaskIntent.putExtra(EditTasksActivity.TASKLISTID,  mTaskList.getId());
			pendingIntent = PendingIntent.getActivity(context, 0, newTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.newButton, pendingIntent);
			//Now it is time to populate all the five items with the corresponding Tasks.
			//note that we calculate which tasks to show based on the current page.
			for (int i = page * 5; i < 5 + page * 5 ; i++) {
				//if the task to load is not greater than the task list then we can load it.
				if (i < taskList.size()) {
					final Task task = taskList.get(i);
					//setting the title
					view.setTextViewText(TITLE[i%5], task.getTitle());
					//setting the notes
					view.setTextViewText(NOTES[i%5], task.getNotes());
					//setting the due date (if it has any).
					if(task.getDue() != null) {
						view.setViewVisibility(DATE[i%5], View.VISIBLE);
						final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy");
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						view.setTextViewText(DATE[i%5], sdf.format(task.getDue()));
					} else {
						view.setViewVisibility(DATE[i%5], View.INVISIBLE);
					}
					//setting the checkbox based on the task's status.
					view.setViewVisibility(CHECKBOX[i%5], View.VISIBLE);
					Log.d("test", task.getTitle() + task.getNotes());
					if ("completed".equals(task.getStatus())) {
						view.setImageViewResource(CHECKBOX[i%5], R.drawable.checkbox_on);
					} else {
						view.setImageViewResource(CHECKBOX[i%5], R.drawable.checkbox_off);
					}
					// Adding the PendingIntent to the checkbox.
					final Intent intent = new Intent(context, ControlService.class);
					intent.setAction("avoiding checkboxintent conflicts" + System.currentTimeMillis());
					intent.putExtra(ControlService.CONTROL_ACTION, ControlService.UPDATE_TASK);
					intent.putExtra(ControlService.TASK, task);
					Log.d(LOG_TAG, task.getTitle());
					pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
					view.setOnClickPendingIntent(CHECKBOX[i%5], pendingIntent);
					//Adding the PendingIntent to the title. (It will launch the Activity to edit the task)
					final Intent editIntent = new Intent(context, EditTasksActivity.class);
					editIntent.setAction("avoiding edittaskintent conflicts" + System.currentTimeMillis());
					editIntent.putExtra(EditTasksActivity.TASK, task);
					final PendingIntent editPendingIntent = PendingIntent.getActivity(context, 0, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					view.setOnClickPendingIntent(TITLE[i%5], editPendingIntent);
				//There is no task with the given index, so we just hide all item's views.
				} else {
					view.setTextViewText(TITLE[i%5], null);
					view.setTextViewText(NOTES[i%5], null);
					view.setTextViewText(DATE[i%5], null);
					view.setViewVisibility(CHECKBOX[i%5], View.INVISIBLE);
				}
			}
			return view;
		}
		@Override
		public IBinder onBind(final Intent arg0) {
			return null;
		}
	}
}
