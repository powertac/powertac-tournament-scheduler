package com.powertac.tourney.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.powertac.tourney.constants.*;

public class Rest{
	public static String parseBrokerLogin(Map params){
		String response = "";
		String responseType = ((String[]) params.get(Constants.REQ_PARAM_TYPE))[0];
		
		// Check if a response type was specified, default is xml
		if(responseType != null){
			if(responseType.equalsIgnoreCase("xml")){
				response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<retry>200</retry>";
			}else if(responseType.equalsIgnoreCase("json")){
				response = "{\n \"retry\":200\n}";				
			}else{
				response = "Error making rest call, please check your parameters:"+responseType;
			}
			
			
		// Default xml
		}else{
			responseType = "xml";
			
			
		}
		
		
		return response;
	}
	
	public void respond() {

		String queryString = ((HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest())
				.getQueryString();

		queryString = "stuff";
		if (queryString != null) {
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext ext = context.getExternalContext();
			HttpServletResponse response = (HttpServletResponse) ext
					.getResponse();
			response.setContentType("text/plain; charset=UTF-8");
			try {
				PrintWriter pw = response.getWriter();
				pw.print(queryString);
			} catch (IOException ex) {
				throw new FacesException(ex);
			}
			context.responseComplete();
		}

	}

}
