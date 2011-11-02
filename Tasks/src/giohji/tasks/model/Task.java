package giohji.tasks.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
/**
 * The class to store the task retrieved from Google Tasks, the only method that should
 * use its complicated constructor is the TaskAdapter.parseTasks(tasksJSON).
 * It is possible to modify the title, notes, due, completed and status. After the modification,
 * the method TaskAdapter.updateTask(task) must be called to persist it in Google Tasks.
 */
public class Task implements Parcelable{
    /**
     * String values of a Google Task.
     */
    private transient String mId, mTitle, mSelfLink, mParent, mPosition, mNotes, mStatus;
    /**
     * Date values of a Google Task.
     */
    private transient Date mUpdated, mDue, mCompleted;
    /**
     * Boolean values of a Google Task.
     */
    private transient Boolean mDeleted, mHidden;

    Task(
            final String taskId,
            final String title,
            final Date updated,
            final String selfLink,
            final String parent,
            final String position,
            final String notes,
            final String status,
            final Date due,
            final Date completed,
            final Boolean deleted,
            final Boolean hidden) {
                setId(taskId);
                mTitle = title;
                setUpdated(updated);
                setSelfLink(selfLink);
                setParent(parent);
                setPosition(position);
                mNotes = notes;
                mStatus = status;
                mDue = due;
                mCompleted = completed;
                setDeleted(deleted);
                setHidden(hidden);
    }
    /**
     * Gets the JSONobject, used to build the entity for the task update.
     * @return
     * the JSONObject
     * @throws JSONException
     */
    public JSONObject getJSONObject() throws JSONException {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        final JSONObject taskJSON = new JSONObject();
        taskJSON.put("id", mId);
        taskJSON.put("title", mTitle);
        taskJSON.put("notes", mNotes);
        if (mDue != null) {
            taskJSON.put("due", sdf.format(mDue));
        }
        taskJSON.put("status", mStatus);
        return taskJSON;
    }

    private void setId(final String taskId) {
        this.mId = taskId;
    }
    public String getId() {
        return mId;
    }
    public void setTitle(final String title) {
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
    private void setParent(final String parent) {
        this.mParent = parent;
    }
    public String getParent() {
        return mParent;
    }
    private void setPosition(final String position) {
        this.mPosition = position;
    }
    public String getPosition() {
        return mPosition;
    }
    public void setNotes(final String notes) {
        this.mNotes = notes;
    }
    public String getNotes() {
        return mNotes;
    }
    public void setStatus(final String status) {
        this.mStatus = status;
    }
    public String getStatus() {
        return mStatus;
    }
    private void setUpdated(final Date updated) {
        this.mUpdated = updated;
    }
    public Date getUpdated() {
        Date updated = null;
        if (mUpdated != null) {
            updated = (Date) mUpdated.clone();
        }
        return updated;
    }
    public void setDue(final Date due) {
        if (due != null) {
            mDue = (Date) due.clone();
        } else {
            mDue = null;
        }
    }
    public Date getDue() {
        Date due = null;
        if (mDue != null) {
            due = (Date) mDue.clone();
        }
        return due;
    }
    public void setCompleted(final Date completed) {
        if (completed != null) {
            mCompleted = (Date) completed.clone();
        } else {
            mCompleted = null;
        }
    }
    public Date getCompleted() {
        Date completed = null;
        if (mCompleted != null) {
            completed = (Date) mCompleted.clone();
        }
        return completed;
    }
    private void setDeleted(final Boolean deleted) {
        this.mDeleted = deleted;
    }
    public Boolean getDeleted() {
        return mDeleted;
    }
    private void setHidden(final Boolean hidden) {
        this.mHidden = hidden;
    }
    public Boolean getHidden() {
        return mHidden;
    }
	public int describeContents() {
		return 0;
	}
	public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mId);
        dest.writeString(mTitle);
        if (mUpdated == null) {
            dest.writeLong(-1);
        } else {
            dest.writeLong(mUpdated.getTime());
        }
        dest.writeString(mSelfLink);
        dest.writeString(mParent);
        dest.writeString(mPosition);
        dest.writeString(mNotes);
        dest.writeString(mStatus);
        if (mDue == null) {
            dest.writeLong(-1);
        } else {
            dest.writeLong(mDue.getTime());
        }
        if (mCompleted == null) {
            dest.writeLong(-1);
        } else {
            dest.writeLong(mCompleted.getTime());
        }
        dest.writeValue(mDeleted);
        dest.writeValue(mHidden);
	}
	public static final Parcelable.Creator<Task> CREATOR
	= new Parcelable.Creator<Task>() {
		public Task createFromParcel(final Parcel inParcel) {
			return new Task(inParcel);
		}

		public Task[] newArray(final int size) {
			return new Task[size];
		}
	};

	private Task(final Parcel inParcel) {
		long aux;
        mId = inParcel.readString();
        mTitle = inParcel.readString();
        aux = inParcel.readLong();
        if (aux != -1) {
            mUpdated = new Date(aux);
        }
        mSelfLink = inParcel.readString();
        mParent = inParcel.readString();
        mPosition = inParcel.readString();
        mNotes = inParcel.readString();
        mStatus = inParcel.readString();
        aux = inParcel.readLong();
        if (aux != -1) {
        	mDue = new Date(aux);
        }
        aux = inParcel.readLong();
        if (aux != -1) {
        	mCompleted = new Date(aux);
        }
        mDeleted = (Boolean) inParcel.readValue(null);
        mHidden = (Boolean) inParcel.readValue(null);
	}

}
