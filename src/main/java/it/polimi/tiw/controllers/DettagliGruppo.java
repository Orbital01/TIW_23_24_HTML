package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

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

/**
 * Servlet implementation class DettagliGruppo
 */
@WebServlet("/DettagliGruppo")
public class DettagliGruppo extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;
       
    public DettagliGruppo() {
        super();
        // TODO Auto-generated constructor stub
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

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//se non sono loggato torno alla pagina di login 
		String loginpath = getServletContext().getContextPath() + "/index.html";
		
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}
		
		//	codice per farmi dare la descrizione del singolo gruppo con id = id
		String id_param = request.getParameter("groupId");
		Integer id = -1;
		
		//se è nullo lancio una eccezione
		if (id_param == null) response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing group ID");
		
		//se non è un numero lancio una eccezione
		try {
			id = Integer.parseInt(id_param);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid group ID");
			return;
		}
		
		
		Gruppi group = null;
		try {
			group = new GruppiDAO(connection).getGroupById(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (group == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Group not found");
			return;
		}
			
		//	codice per farmi dare l'elenco dei partecipanti del singolo gruppo con id = id
		ArrayList<String> partecipanti = new ArrayList<>();
		
		try {
			partecipanti = new PartecipationDAO(connection).getPartecipants(id);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// verifico che l'utente appartenga a quel gruppo
		// in caso negativo non può visualizzare quel gruppo
		User utente = (User)session.getAttribute("user");
		
		if (!group.getadmin().equals(utente.getUsername()) && !partecipanti.contains(utente.getUsername())) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Group not found");
			return;
		}
				
		
		
		String path = "/WEB-INF/details.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("group", group);
		ctx.setVariable("partecipants", partecipanti);
		templateEngine.process(path, ctx, response.getWriter());
	}
	
	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
	}
	
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
