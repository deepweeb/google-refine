package com.metaweb.gridworks.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;

public class HistoryEntry implements Serializable, Jsonizable {
	private static final long serialVersionUID = 532766467813930262L;
	
	final public long 				id;
	final public long 				projectID;
	final public String				description;
	final public AbstractOperation	operation;
	final public Date				time;
	
	transient protected Change _change;
	
	public HistoryEntry(Project project, String description, AbstractOperation operation, Change change) {
		this.id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
		this.projectID = project.id;
		this.description = description;
		this.operation = operation;
		this.time = new Date();
		
		_change = change;
	}
	
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		
		writer.object();
		writer.key("id"); writer.value(id);
		writer.key("description"); writer.value(description);
		writer.key("time"); writer.value(sdf.format(time));
		writer.endObject();
	}
	
	public void apply(Project project) {
		if (_change == null) {
			loadChange();
		}
		_change.apply(project);
        
		// When a change is applied, it can hang on to old data (in order to be able
		// to revert later). Hence, we need to save the change out.
		
        saveChange();
	}
	
	public void revert(Project project) {
		if (_change == null) {
			loadChange();
		}
		_change.revert(project);
	}
	
	public void delete() {
		File file = getFile();
		if (file.exists()) {
			file.delete();
		}
	}
	
	protected void loadChange() {
		File file = getFile();
		
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(file);
			in = new ObjectInputStream(fis);
			
			_change = (Change) in.readObject();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
	}
	
	protected void saveChange() {
		File file = getFile();
		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(file);
			out = new ObjectOutputStream(fos);
			
			out.writeObject(_change);
			out.flush();
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
				}
			}
		}
	}
	
	protected File getFile() {
		File dir = new File(ProjectManager.singleton.getDataDir(), projectID + ".history");
		dir.mkdirs();
		
		return new File(dir, id + ".entry");
	}
}
