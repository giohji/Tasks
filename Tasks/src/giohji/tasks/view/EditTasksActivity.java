package giohji.tasks.view;

import giohji.tasks.R;
import giohji.tasks.auth.TasksLogin;
import giohji.tasks.model.Task;
import giohji.tasks.model.TasksAdapter;
import giohji.tasks.model.TasksWidgetProvider.UpdateService;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * This is the Activity that is launched to edit an existing Task or to create a new Task.
 */
public class EditTasksActivity extends Activity {
	public static final String LOG_TAG = "EditTasksActivity";
    /**
     * The key to retrieve the Task that will be showed at the edit Task Activity.
     */
    public static final String TASK =
            "giohji.tasks.view.EditTasksActivity.TASK";
    /**
     * The key to retrieve the TaskList ID to create a new task in its corresponding TaskList.
     */
    public static final String TASKLISTID =
            "giohji.tasks.view.EditTasksActivity.TASKLISTID";
    /**
     * The dim amount.
     */
    private static final float DIM_AMOUNT = 0.75f;
    /**
     * The edit Activity's Task.
     */
    private transient Task mTask;
    /**
     * The edit Activity's TaskListId.
     */
    private transient String mTaskListId;
    /**
     * The activity's context.
     */
    private transient Context mContext;


    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        final Intent intent = getIntent();
        mTask = (Task) intent.getExtras().get(EditTasksActivity.TASK);
        mTaskListId = intent.getExtras().getString(EditTasksActivity.TASKLISTID);
        getWindow().getAttributes().dimAmount = DIM_AMOUNT;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        setContentView(R.layout.tasks_edit);
        final Button closeButton = (Button) findViewById(R.id.tasks_edit_button_close);
        closeButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                finish();
            }
        });
        //if mTask is not null, then we are editing an existing task.
        if (mTask != null) {
            updateUI(mTask);
        //else we are creating a new task.
        } else {
            newTaskUI();
        }
    }
    /**
     * Shows the UI with blank forms for creating a new task and sets the save button to create the new Task.
     */
    private void newTaskUI() {
        final EditText titleTextBox = (EditText) findViewById(R.id.titleTextBox);
        final EditText noteTextBox = (EditText) findViewById(R.id.noteTextBox);
        final CheckBox statusCheckBox = (CheckBox) findViewById(R.id.statusCheckBox);
        final ImageButton dateButton = (ImageButton) findViewById(R.id.dateButton);
        final DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        final Button deleteButton = (Button) findViewById(R.id.deleteButton);
        dateButton.setImageResource(android.R.drawable.button_onoff_indicator_off);
        datePicker.setEnabled(false);
        dateButton.setOnClickListener(new OnClickListener() {
        	public void onClick(final View view) {
        		if (datePicker.isEnabled()) {
        			dateButton.setImageResource(android.R.drawable.button_onoff_indicator_off);
        			datePicker.setEnabled(false);
        		} else {
        			dateButton.setImageResource(android.R.drawable.button_onoff_indicator_on);
        			datePicker.setEnabled(true);
        		}
        	}
        });
        saveButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
            	AccountManager accManager = AccountManager.get(mContext);
            	Account[] accounts = accManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE);
            	Account acc = null;
            	if (accounts.length > 0) {
            		acc = accounts[0];
            		if (TasksAdapter.getAdapter() == null) {
            			String token = null;
            			String secret = null;
            			if (acc != null) {
            				token = accManager.peekAuthToken(acc, TasksLogin.TASKS_TOKEN_KEY);
            				secret = accManager.peekAuthToken(acc, TasksLogin.TASKS_SECRET_KEY);
            			}
            			if (secret != null && token!= null) {
            				TasksAdapter.initialize(secret, token);
            				Log.d(LOG_TAG, "Adapter initialized!");
            			}
            		}
            		String title, notes;
            		Date due = null;
            		if (datePicker.isEnabled()) {
            			final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            			calendar.clear();
            			calendar.set(datePicker.getYear(), datePicker.getMonth(),
            					datePicker.getDayOfMonth());
            			due = calendar.getTime();
            		}
            		title = titleTextBox.getText().toString();
            		notes = noteTextBox.getText().toString();
            		if (TasksAdapter.getAdapter().createTask(title, notes, due, mTaskListId, null, null)) {
                		startService(new Intent(mContext, UpdateService.class));
            		} else {
            			Toast toast = Toast.makeText(mContext, "Task creation failed! Check internet and time settings", 5);
            			toast.show();
            		}
            	} else {
            		Toast toast = Toast.makeText(mContext, "Task creation failed! No account!", 5);
            		toast.show();
            	}
                finish();
            }
        });
        statusCheckBox.setEnabled(false);
        deleteButton.setVisibility(View.INVISIBLE);
    }
    /**
     * Updates the UI with the task's info and sets the save button to update the Task.
     * @param task
     * The task to get all the info from.
     */
    private void updateUI(final Task task) {
        final EditText titleTextBox = (EditText) findViewById(R.id.titleTextBox);
        final EditText noteTextBox = (EditText) findViewById(R.id.noteTextBox);
        final CheckBox statusCheckBox = (CheckBox) findViewById(R.id.statusCheckBox);
        final ImageButton dateButton = (ImageButton) findViewById(R.id.dateButton);
        final DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        final Button deleteButton = (Button) findViewById(R.id.deleteButton);
        titleTextBox.setText(task.getTitle());
        noteTextBox.setText(task.getNotes());
        Boolean checked;
        if ("completed".equals(task.getStatus())) {
            checked = true;
        } else {
        	checked = false;
        }
        statusCheckBox.setChecked(checked);
        final Date dueDate = task.getDue();
        if (dueDate != null) {
            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            calendar.setTime(task.getDue());
            datePicker.updateDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
        } else {
            dateButton.setImageResource(android.R.drawable.button_onoff_indicator_off);
            datePicker.setEnabled(false);
        }
        dateButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                if (datePicker.isEnabled()) {
                    dateButton.setImageResource(android.R.drawable.button_onoff_indicator_off);
                    datePicker.setEnabled(false);
                } else {
                    dateButton.setImageResource(android.R.drawable.button_onoff_indicator_on);
                    datePicker.setEnabled(true);
                }
            }
        });
        saveButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
            	AccountManager accManager = AccountManager.get(mContext);
            	Account[] accounts = accManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE);
            	Account acc = null;
            	if (accounts.length > 0) {
            		acc = accounts[0];
            		if (TasksAdapter.getAdapter() == null) {
            			String token = null;
            			String secret = null;
            			if (acc != null) {
            				token = accManager.peekAuthToken(acc, TasksLogin.TASKS_TOKEN_KEY);
            				secret = accManager.peekAuthToken(acc, TasksLogin.TASKS_SECRET_KEY);
            			}
            			if (secret != null && token!= null) {
            				TasksAdapter.initialize(secret, token);
            				Log.d(LOG_TAG, "Adapter initialized!");
            			}
            		}
            		if (datePicker.isEnabled()) {
            			final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            			calendar.clear();
            			calendar.set(datePicker.getYear(),
            					datePicker.getMonth(),
            					datePicker.getDayOfMonth());
            			mTask.setDue(calendar.getTime());
            		} else {
            			mTask.setDue(null);
            		}
            		mTask.setTitle(titleTextBox.getText().toString());
            		mTask.setNotes(noteTextBox.getText().toString());
            		if (statusCheckBox.isChecked()) {
            			mTask.setStatus("completed");
            		} else {
            			mTask.setStatus("needsAction");
            		}
            		if (TasksAdapter.getAdapter().updateTask(mTask)) {
                		startService(new Intent(mContext, UpdateService.class));
            		} else {
            			Toast toast = Toast.makeText(mContext, "Task update failed! Check internet and time settings", 5);
            			toast.show();
            		}
            	} else {
            		Toast toast = Toast.makeText(mContext, "Task update failed! No account!", 5);
            		toast.show();
            	}
                finish();
            }
        });
        deleteButton.setOnClickListener(new OnClickListener() {
        	public void onClick(final View view) {
        		AccountManager accManager = AccountManager.get(mContext);
        		Account[] accounts = accManager.getAccountsByType(TasksLogin.ACCOUNT_TYPE);
        		Account acc = null;
        		if (accounts.length > 0) {
        			acc = accounts[0];
        			if (TasksAdapter.getAdapter() == null) {
        				String token = null;
        				String secret = null;
        				if (acc != null) {
        					token = accManager.peekAuthToken(acc, TasksLogin.TASKS_TOKEN_KEY);
        					secret = accManager.peekAuthToken(acc, TasksLogin.TASKS_SECRET_KEY);
        				}
        				if (secret != null && token!= null) {
        					TasksAdapter.initialize(secret, token);
        					Log.d(LOG_TAG, "Adapter initialized!");
        				}
        			}
        			if (TasksAdapter.getAdapter().deleteTask(mTask.getSelfLink())) {
                		startService(new Intent(mContext, UpdateService.class));
        			} else {
        				Toast toast = Toast.makeText(mContext, "Task deletion failed! Check internet and time settings", 5);
            			toast.show();
        			}
        		} else {
        			Toast toast = Toast.makeText(mContext, "Task deletion failed! No account!", 5);
            		toast.show();
        		}
        		finish();
            }
        });
    }
    @Override
    public void onBackPressed() {
        finish();
    }
}
