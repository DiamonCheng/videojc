package com.dc.videojc.base;

import java.io.Serializable;

/**
 * Data transfer object for ajax response.
 * Mostly used in a rest controller to return a response
 *
 * @author DC
 */
public class AjaxResult implements Serializable {
	private static final long serialVersionUID = -6770538785772264261L;
	private boolean success = true;
	private String message;
	private Object data;
	
	public boolean isSuccess() {
		return success;
	}
	
	public AjaxResult setSuccess(boolean success) {
		this.success = success;
		return this;
	}
	
	public String getMessage() {
		return message;
	}
	
	public AjaxResult setMessage(String message) {
		this.message = message;
		return this;
	}
	
	public Object getData() {
		return data;
	}
	
	public AjaxResult setData(Object data) {
		this.data = data;
		return this;
	}
	
	/**
	 * toString
	 */
	@Override
	public String toString() {
		return "{" +
					   "\"success\":" + success +
					   ", \"message\":\"" + message + '\"' +
					   ", \"data\":\"" + data + "\"" +
					   '}';
	}
}
