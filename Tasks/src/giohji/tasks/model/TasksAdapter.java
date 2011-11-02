package giohji.tasks.model;

import giohji.tasks.auth.TasksLogin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Class used to make API calls to the Google Data API to request for Tasks feeds.
 */
public final class TasksAdapter {

    /** The TAG. */
    private static final String TAG = "TasksAdapter";

    /** The Constant BASE_URL. */
    private static final String BASE_URL = "https://www.googleapis.com/tasks/v1";

    /** The Constant GET_TASK_LISTS. */
    private static final String GET_TASK_LISTS = "/users/@me/lists";

    /** The Constant GET_TASK_PREFIX. */
    private static final String LISTS_PREFIX = "/lists/";

    /** The Constant GET_TASK_SUFIX. */
    private static final String GET_TASK_SUFIX = "/tasks";

    /** The Constant GET_TASK_SUFIX. */
    private static final String MOVE_TASK_SUFIX = "/move";

    /** The Constant CLEAR_COMPLETED_SUFIX. */
    private static final String CLEAR_COMPLETED_SUFIX = "/clear";

    /** The Http Response code OK. */
    private static final int CODE_OK = 200;

    /** The Http Response code NO_CONTENT. */
    private static final int CODE_NO_CONTENT = 204;
    
    /** The location field in the redirect Header. */
    private static final String LOCATION = "location";

    /** The app secret */
    private String mSecret;

    /** The access token */
    private String mToken;

    /** The singleton. */
    private static TasksAdapter singleton;

    public static void initialize(final String secret, final String token) {
        synchronized (TasksAdapter.class) {
        	singleton = new TasksAdapter(secret, token);
        }
    }
    /**
     * Gets the adapter.
     *
     * @return the adapter
     */
    public static TasksAdapter getAdapter() {
        synchronized (TasksAdapter.class) {
            return singleton;
        }
    }

    /**
     * Instantiates a new tasks adapter. Private Method used by Singleton
     */
    private TasksAdapter(final String secret, final String token) {
    	mSecret = secret;
    	mToken = token;
    }

    /**
     * Gets all TasksList from the calendar/tasks user account stored in SimpleSettingsManager.
     * @return
     * an ArrayList containing all user TasksList.
     */
    public List<TaskList> getTaskList() {
        final OAuthConsumer consumer =
                new CommonsHttpOAuthConsumer(TasksLogin.CONSUMER_KEY,
                        TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        List<TaskList> taskLists = null;
        try {
            URL url = new URL(BASE_URL + GET_TASK_LISTS);
            //Set redirect parameter to false so we can
            // handle the redirection and re-sign the request
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            do {
                final HttpGet req = new HttpGet(url.toString());
                req.setParams(params);

                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            final HttpEntity entity = response.getEntity();
            final BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            final InputStream input = bufHttpEntity.getContent();
            final String taskListsStream = convertStreamToString(input);
            input.close();
            //we need to parse the response and store it in the model.
            Log.d("GET_TASK_LISTS", taskListsStream);
            taskLists = parseTaskLists(taskListsStream);
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return taskLists;
    }

    /**
     * Parses the user taskLists.
     *
     * @param userPhotosJSON
     *            the Input Stream with the user photo list.
     */
    private List<TaskList> parseTaskLists(final String taskListsJSON) {
        JSONObject rawObject;
        final ArrayList<TaskList> taskLists = new ArrayList<TaskList>();
        try {
            rawObject = new JSONObject(taskListsJSON);
            final JSONArray entryArray = rawObject.getJSONArray("items");

            for (int i = 0; i < entryArray.length(); i++) {
                final JSONObject taskListObject = entryArray.getJSONObject(i);
                final TaskList taskList = new TaskList(
                        taskListObject.getString("id"),
                        taskListObject.getString("title"),
                        taskListObject.getString("selfLink")
                );
                taskLists.add(taskList);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return taskLists;
    }

    /**
     * Gets all tasks in the TaskList with taskListId.
     * @param taskListId
     * the TaskList id.
     * @return
     * an ArrayList with all the tasks from the TaskList with taskListId
     */
    public List<Task> getTasks(final String taskListId) {
        List<Task> tasks = null;
        final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                TasksLogin.CONSUMER_KEY, TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        try {
            URL url = new URL(BASE_URL + LISTS_PREFIX +  taskListId + GET_TASK_SUFIX);
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            do {
                final HttpGet req = new HttpGet(url.toString());
                //Set redirect parameter to false so we can
                // handle the redirection and re-sign the request
                req.setParams(params);
                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            final HttpEntity entity = response.getEntity();
            final BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            final InputStream input = bufHttpEntity.getContent();
            final String tasksStream = convertStreamToString(input);
            input.close();
            Log.d("GetTasks:", tasksStream);
            tasks = parseTasks(tasksStream);
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return tasks;
    }

    /**
     * Parses the tasks.
     *
     * @param tasksJSON
     *            the Input Stream with all the tasks.
     */
    private List<Task> parseTasks(final String tasksJSON) {
        JSONObject rawObject;
        final ArrayList<Task> tasks = new ArrayList<Task>();
        try {
            rawObject = new JSONObject(tasksJSON);
            if (rawObject.has("items")) {
                final JSONArray entryArray = rawObject.getJSONArray("items");
                for (int i = 0; i < entryArray.length(); i++) {
                    final JSONObject taskObject = entryArray.getJSONObject(i);
                    final Task task = new Task(
                            taskObject.getString("id"),
                            taskObject.getString("title"),
                            DateParsingUtil.parseDate(taskObject.optString("updated", null)),
                            taskObject.getString("selfLink"),
                            taskObject.optString("parent", null),
                            taskObject.optString("position", null),
                            taskObject.optString("notes", null),
                            taskObject.getString("status"),
                            DateParsingUtil.parseDate(taskObject.optString("due", null)),
                            DateParsingUtil.parseDate(taskObject.optString("completed", null)),
                            taskObject.optBoolean("deleted", false),
                            taskObject.optBoolean("hidden", false));
                    tasks.add(task);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return tasks;
    }
    /**
     * Clear all completed tasks from the TaskList with taskListId.
     * @param taskListId
     * the TaskList id.
     * @return
     * true if the TaskList was cleared successfully.
     */
    public Boolean clearCompleted(final String taskListId) {
        Boolean cleared = false;
        final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                TasksLogin.CONSUMER_KEY, TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        try {
            URL url = new URL(BASE_URL + LISTS_PREFIX +  taskListId + CLEAR_COMPLETED_SUFIX);
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            do {
                final HttpPost req = new HttpPost(url.toString());
                //Set redirect parameter to false so we can
                // handle the redirection and re-sign the request
                req.setParams(params);
                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            if (response.getStatusLine().getStatusCode() == CODE_OK
                    || response.getStatusLine().getStatusCode() == CODE_NO_CONTENT) {
                cleared = true;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return cleared;
    }
    /**
     * Delete the task with the taskSelfLink.
     * @param taskSelfLink
     * the task selfLink
     * @return
     * true if the task was deleted successfully
     */
    public Boolean deleteTask(final String taskSelfLink) {
        Boolean deleted = false;
        final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                TasksLogin.CONSUMER_KEY, TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        try {
            URL url = new URL(taskSelfLink);
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            do {
                final HttpDelete req = new HttpDelete(url.toString());
                //Set redirect parameter to false so we can
                // handle the redirection and re-sign the request
                req.setParams(params);
                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            if (response.getStatusLine().getStatusCode() == CODE_OK
                    || response.getStatusLine().getStatusCode() == CODE_NO_CONTENT) {
                deleted = true;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return deleted;
    }
    /**
     * Move the task with selfLink=targetTaskLink to the parent with id=parentTaskLink and
     * and after the task with id=previousTaskLink.
     * @param targetTaskLink
     * The selfLink of the Task to be moved.
     * @param parentTaskId
     * The id of the parent Task, the Task will be moved to the root if parentTaskLink
     * is null.
     * @param previousTaskId
     * The id of the previous Task, the Task will be moved to the first position if
     * previousTaskId is null.
     * @return
     * true is the Task was moved successfully.
     */
    public Boolean moveTask(
            final String targetTaskLink,
            final String parentTaskId,
            final String previousTaskId) {
        Boolean moved = false;
        final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                TasksLogin.CONSUMER_KEY, TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        String parameters = "";
        Boolean firstParam = true;
        if (parentTaskId != null || previousTaskId != null) {
            parameters = "?";
            if (parentTaskId != null) {
                firstParam = false;
                parameters = parameters.concat("parent=" + parentTaskId);
            }
            if (previousTaskId != null) {
                if (firstParam) {
                    parameters = parameters.concat("previous=" + previousTaskId);
                } else {
                    parameters = parameters.concat("&previous=" + previousTaskId);
                }
            }
        }
        try {
            URL url = new URL(targetTaskLink + MOVE_TASK_SUFIX + parameters);
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            do {
                final HttpPost req = new HttpPost(url.toString());
                //Set redirect parameter to false so we can
                // handle the redirection and re-sign the request
                req.setParams(params);
                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            if (response.getStatusLine().getStatusCode() == CODE_OK
                    || response.getStatusLine().getStatusCode() == CODE_NO_CONTENT) {
                moved = true;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return moved;
    }
    /**
     * Updates the modifications in the task.
     * @param task
     * the modified task to be updated.
     * @return
     * true if the update was successful.
     */
    public Boolean updateTask(final Task task) {
        Boolean updated = false;
        final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                TasksLogin.CONSUMER_KEY, TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        try {
            URL url = new URL(task.getSelfLink());
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            //create the JSON request body to update the Task
            final StringEntity stringEntity = new StringEntity(task.getJSONObject().toString(),
                    "UTF-8");
            do {
                final HttpPut req = new HttpPut(url.toString());
                //Set redirect parameter to false so we can
                // handle the redirection and re-sign the request
                req.setParams(params);
                //set the request body
                req.setEntity(stringEntity);
                //set the content type header to JSON format
                req.addHeader("Content-Type", "application/json");
                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            if (response.getStatusLine().getStatusCode() == CODE_OK
                    || response.getStatusLine().getStatusCode() == CODE_NO_CONTENT) {
                updated = true;
            }
            final HttpEntity entity = response.getEntity();
            final BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            final InputStream input = bufHttpEntity.getContent();
            final String taskListsStream = convertStreamToString(input);
            input.close();
            Log.d("UPDATE_TASK", taskListsStream);
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return updated;
    }
    /**
     * Creates a task in the specified taskListId.
     * @param title
     * the title of the new task.
     * @param notes
     * the notes of the new task.
     * @param due
     * the deadline of the new task.
     * @param taskListId
     * the taskListId to add the task.
     * @param parentTaskId
     * the parent of the new task (if null the task will be created in the root).
     * @param previousTaskId
     * the previous task of the new task (if null the task will be the first in its parent).
     * @return
     */
    public Boolean createTask(
            final String title,
            final String notes,
            final Date due,
            final String taskListId,
            final String parentTaskId,
            final String previousTaskId) {
        Boolean created = false;
        final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                TasksLogin.CONSUMER_KEY, TasksLogin.CONSUMER_SECRET);
        setAuthToken(consumer);
        String parameters = "";
        Boolean firstParam = true;
        if (parentTaskId != null || previousTaskId != null) {
            parameters = "?";
            if (parentTaskId != null) {
                firstParam = false;
                parameters = parameters.concat("parent=" + parentTaskId);
            }
            if (previousTaskId != null) {
                if (firstParam) {
                    parameters = parameters.concat("previous=" + previousTaskId);
                } else {
                    parameters = parameters.concat("&previous=" + previousTaskId);
                }
            }
        }
        try {
            URL url = new URL(BASE_URL + LISTS_PREFIX +  taskListId + GET_TASK_SUFIX + parameters);
            Log.d("CREATE_TASK", url.toString());
            final HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, false);
            Header locationHeader;
            HttpResponse response;
            //creating the JSONObject to build the request body.
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final JSONObject taskJSON = new JSONObject();
            taskJSON.put("title", title);
            if (notes != null) {
                taskJSON.put("notes", notes);
            }
            if (due != null) {
                taskJSON.put("due", sdf.format(due));
            }
            //create the JSON request body to update the Task
            final StringEntity stringEntity = new StringEntity(taskJSON.toString(), "UTF-8");
            do {
                final HttpPost req = new HttpPost(url.toString());
                //Set redirect parameter to false so we can
                // handle the redirection and re-sign the request
                req.setParams(params);
                //set the request body
                req.setEntity(stringEntity);
                //set the content type header to JSON format
                req.addHeader("Content-Type", "application/json");
                // Sign the request with the authenticated token
                consumer.sign(req);
                // Send the request
                final HttpClient httpClient = new DefaultHttpClient();
                response = httpClient.execute(req);
                //Google tasks redirects and appends a gsessionid query string
                //so we need to re-sign the request.
                locationHeader = response.getFirstHeader(LOCATION);
                if (locationHeader != null) {
                    url = new URL(locationHeader.getValue());
                }
            } while (locationHeader != null);
            if (response.getStatusLine().getStatusCode() == CODE_OK
                    || response.getStatusLine().getStatusCode() == CODE_NO_CONTENT) {
                created = true;
            }
            final HttpEntity entity = response.getEntity();
            final BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            final InputStream input = bufHttpEntity.getContent();
            final String taskListsStream = convertStreamToString(input);
            input.close();
            Log.d("UPDATE_TASK", taskListsStream);
        } catch (IOException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthCommunicationException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (OAuthMessageSignerException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return created;
    }
    /**
     * Sets the auth token.
     */
    private void setAuthToken(final OAuthConsumer consumer) {
        consumer.setTokenWithSecret(mToken, mSecret);
    }

    /**
     * Convert stream to string.
     *
     * @param inputStream
     *            the Input Stream
     * @return the string
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public String convertStreamToString(final InputStream inputStream) throws IOException {
        String convertedString = "";
        /*
         * To convert the InputStream to String we use the Reader.read(char[] buffer) method. We
         * iterate until the Reader return -1 which means there's no more data to read. We use the
         * StringWriter class to produce the string.
         */
        if (inputStream != null) {
            final Writer writer = new StringWriter();

            final char[] buffer = new char[1024];
            try {
                final Reader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));
                int charNo = reader.read(buffer);
                while (charNo != -1) {
                    writer.write(buffer, 0, charNo);
                    charNo = reader.read(buffer);
                }
            } finally {
                inputStream.close();
            }
            convertedString = writer.toString();
        }
        return convertedString;
    }
}
