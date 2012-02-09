package com.powertac.tourney.listeners;

import java.io.IOException;
import java.io.PrintWriter;

import javax.faces.FacesException;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.powertac.tourney.services.Rest;

import sun.util.logging.resources.logging;

public class RestoreViewPhaseListener implements PhaseListener {

	public void afterPhase(PhaseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void beforePhase(PhaseEvent pe) {
		// System.out.println(pe.getFacesContext().getViewRoot().getViewId());
		HttpServletRequest r = (HttpServletRequest) pe.getFacesContext()
				.getExternalContext().getRequest();
		if (r.getParameterMap().size() != 0) {
			String url = r.getRequestURL().toString();
			if (url.indexOf("index.jsp") > 0) {
				System.out.println("index.jsp");
			} else if (url.indexOf("brokerLogin.jsp") > 0) {
				System.out.println("brokerLogin.jsp");
				HttpServletResponse response = (HttpServletResponse) pe
						.getFacesContext().getExternalContext().getResponse();
				
				response.setContentType("text/plain; charset=UTF-8");
				try {
					PrintWriter pw = response.getWriter();
					pw.print(Rest.parseBrokerLogin(r.getParameterMap()));
				} catch (IOException ex) {
					throw new FacesException(ex);
				}
				pe.getFacesContext().responseComplete();

			} else {
				return;
			}
		}

	}

	public PhaseId getPhaseId() {
		return PhaseId.RESTORE_VIEW;
	}

}
