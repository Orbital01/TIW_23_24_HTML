package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.dao.*;

/**
 * Servlet implementation class CheckGroup
 */
@WebServlet("/CheckGroup")
public class CheckGroup extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	
	public CheckGroup() {
		super();
	}

	public void init() throws ServletException {
		try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);

			ServletContext servletContext = getServletContext();

			ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
			templateResolver.setTemplateMode(TemplateMode.HTML);
			this.templateEngine = new TemplateEngine();
			this.templateEngine.setTemplateResolver(templateResolver);
			templateResolver.setSuffix(".html");

		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}

	}

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		// se non sono loggato torno alla pagina di login
		String loginpath = getServletContext().getContextPath() + "/index.html";

		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}
		
		String nome = null;
		String descrizione = null;
		int giorni;
		int minPartecipanti;
		int maxPartecipanti;
		
		String temp = null;
		
		try {
			nome = StringEscapeUtils.escapeJava(request.getParameter("nome"));
			descrizione = StringEscapeUtils.escapeJava(request.getParameter("descrizione"));
			if (nome == null || nome.isEmpty() || descrizione == null || descrizione.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
				return;
			}
			
			temp = StringEscapeUtils.escapeJava(request.getParameter("giorni"));
			if (temp == null || temp.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
				return;
			}
			
			try {
		        giorni = Integer.parseInt(temp);
		    } catch (NumberFormatException e) {
		        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid number format");
		        return;
		    }
			
			temp = StringEscapeUtils.escapeJava(request.getParameter("minPartecipanti"));
			if (temp == null || temp.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
				return;
			}
			
			try {
				minPartecipanti = Integer.parseInt(temp);
		    } catch (NumberFormatException e) {
		        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid number format");
		        return;
		    }
			
			temp = StringEscapeUtils.escapeJava(request.getParameter("maxPartecipanti"));
			if (temp == null || temp.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
				return;
			}
	
			try {
				maxPartecipanti = Integer.parseInt(temp);
		    } catch (NumberFormatException e) {
		        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid number format");
		        return;
		    }
			
		}catch(Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing value");
			return;
		}
		//fine della verifica dei parametri (potrei spostarli in una classe filter)
		
		
		String path;
		
		//controllo che i parametri min e max siano giusti 
		if(!checkMinMax(minPartecipanti, maxPartecipanti)) {
			path = getServletContext().getContextPath() + "/WEB-INF/CreateGroup.html";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsg", "il minimo non può essere maggiore del massimo");
			path = "/WEB-INF/CreateGroup.html";
			templateEngine.process(path, ctx, response.getWriter());
			return;
		}
		//controllo che giorni sia maggiore di 0
		if(giorni==0){
			path = getServletContext().getContextPath() + "/WEB-INF/CreateGroup.html";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsg", "un gruppo non può avere durata 0 giorni");
			path = "/index.html";
			templateEngine.process(path, ctx, response.getWriter());
			return;
		}
		
		//controllo che non esista un gruppo con il nome uguale
		GruppiDAO groupDAO = new GruppiDAO(connection);
		try {
			
			if(groupDAO.alreadyExistingGroup(nome)) {
				path = getServletContext().getContextPath() + "/WEB-INF/CreateGroup.html";
				ServletContext servletContext = getServletContext();
				final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
				ctx.setVariable("errorMsg", "il nome è già in uso");
				path = "/index.html";
				templateEngine.process(path, ctx, response.getWriter());
				return;	
			}
			
		}catch (SQLException e){
			response.sendError(HttpServletResponse.SC_NO_CONTENT, "unable to get groups");
		}
		
		
		
		//se tutto ok vado alla pagina della anagrafica, passando i dati inseriti come parametro
		request.getSession().setAttribute("nome", nome);
		request.getSession().setAttribute("descrizione", descrizione);
		request.getSession().setAttribute("giorni", giorni);
		request.getSession().setAttribute("minPartecipanti", minPartecipanti);
		request.getSession().setAttribute("maxPartecipanti", maxPartecipanti);
		path = getServletContext().getContextPath() + "/GoToAnagrafica";
		response.sendRedirect(path);
	
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException sqle) {
			}
	}
	
	private Boolean checkMinMax(int min, int max) {
		if(min<0 || max<0) {
			return false;
		} else if(min<=max) {
			return true;
		}else {
			return false;
		}
	}

}
