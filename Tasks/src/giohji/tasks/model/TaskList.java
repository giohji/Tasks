package giohji.tasks.model;

import java.util.ArrayList;
import java.util.List;
/**
 * Class used to store Google TaskLists.
 */
public class TaskList {
    /**
     * String values of a Google TaskList.
     */
    private transient String mId, mTitle, mSelfLink;
    /**
     * List containing the TaskList's Tasks.
     */
    private transient List<Task> mTasks = new ArrayList<Task>();

    TaskList(
            final String taskListId,
            final String title,
            final String selfLink) {
        setId(taskListId);
        setTitle(title);
        setSelfLink(selfLink);
    }
    public int getCount() {
        return mTasks.size();
    }

    private void setId(final String taskListId) {
        this.mId = taskListId;
    }

    public String getId() {
        return mId;
    }

    private void setTitle(final String title) {
        this.mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    private void setSelfLink(final String selfLink) {
        this.mSelfLink = selfLink;
    }

    public String getSelfLink() {
        return mSelfLink;
    }

    public void setTasks(final List<Task> tasks) {
        synchronized (mTasks) {
            this.mTasks = tasks;
        }
    }

    public List<Task> getTasks() {
        return mTasks;
    }
}
