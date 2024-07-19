package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.beans.*;
import it.polimi.tiw.dao.*;

@WebServlet("/GoToHome")
public class GoToHome extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;
	
	public GoToHome() {
		super();
	}
	
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");

		try {
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		//se non sono loggato torno alla pagina di login 
		String loginpath = getServletContext().getContextPath() + "/index.html";
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}
		
		//aggiungo tutti i gruppi in due liste separate
		User user = (User) session.getAttribute("user");
		GruppiDAO gruppiDAO = new GruppiDAO(connection);
		ArrayList<Gruppi> adminGroups = new ArrayList<Gruppi>();
		
		try {
			adminGroups = gruppiDAO.getActiveGroupsByUser(user.getUsername());
		} catch (SQLException e) {
			// Imposto l'errore
            request.setAttribute("errorMessage", "unable to recover Groups");
            
            // Forward alla servlet GoToError
            RequestDispatcher dispatcher = request.getRequestDispatcher("/GoToError");
            dispatcher.forward(request, response);
			return;
		}
		
		PartecipationDAO partecipationDAO = new PartecipationDAO(connection);
		ArrayList<Gruppi> GroupsWithUser = new ArrayList<Gruppi>();
		try {
			GroupsWithUser = partecipationDAO.getGroupsWithUser(user.getUsername());
		} catch (SQLException e) {
			// Imposto l'errore
            request.setAttribute("errorMessage", "unable to recover Groups");
            
            // Forward alla servlet GoToError
            RequestDispatcher dispatcher = request.getRequestDispatcher("/GoToError");
            dispatcher.forward(request, response);
			return;
		}

		//se sono loggato vado alla home con tutti i gruppi come parametro 
		String path = "/WEB-INF/Home.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("admin_groups", adminGroups); //gruppi dove lo user è amministratore
		ctx.setVariable("groups_with_user", GroupsWithUser); //gruppi dove lo user è stato invitato
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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
	
}
