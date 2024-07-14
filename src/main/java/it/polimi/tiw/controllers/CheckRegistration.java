package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.polimi.tiw.dao.UserDAO;

@WebServlet("/CheckRegistration")
public class CheckRegistration extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	public CheckRegistration() {
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

	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
				throws ServletException, IOException{
			
			String usrn = null;
			String email  = null;
			String pwd = null;
			String cfpwd = null;
			String nome = null;
			String cognome = null;
			
			try {
				usrn = StringEscapeUtils.escapeJava(request.getParameter("Username"));
				email = StringEscapeUtils.escapeJava(request.getParameter("Email"));
				pwd = StringEscapeUtils.escapeJava(request.getParameter("Password"));
				cfpwd = StringEscapeUtils.escapeJava(request.getParameter("cfPassword"));
				nome = StringEscapeUtils.escapeJava(request.getParameter("nome"));
				cognome = StringEscapeUtils.escapeJava(request.getParameter("cognome"));
				
				//controllo che nessuno dei campi sia vuoto 
				if (usrn == null || usrn.isEmpty() || pwd == null || pwd.isEmpty() ||
						email == null || email.isEmpty() || cfpwd==null || cfpwd.isEmpty()) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
					return;
				}
				
			}catch(Exception e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing value");
				return;
			}
			
			String path;
			
			//controllo che la mail sia effettivamente una mail, altrimenti stampo un messaggio 
			if(!testMail(email)) {
				path = getServletContext().getContextPath() + "/registrazione.html";
				ServletContext servletContext = getServletContext();
				final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
				ctx.setVariable("errorMsg", "indirizzo email non valido");
				path = "/registrazione.html";
				templateEngine.process(path, ctx, response.getWriter());
				return;
			}
			
			//controllo che pwd e cfpwd siano UGUALI
			if(!pwd.equals(cfpwd)) {
				path = getServletContext().getContextPath() + "/registrazione.html";
				ServletContext servletContext = getServletContext();
				final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
				ctx.setVariable("errorMsg", "password e conferma paswword non combaciano");
				path = "/registrazione.html";
				templateEngine.process(path, ctx, response.getWriter());
				return;
			}
			
			//provo l'inserimento nel database
			UserDAO usr = new UserDAO(connection);
			
			try {
				Boolean result = usr.addUser(usrn, pwd, email, nome, cognome);
				
				if (!result) {
					path = getServletContext().getContextPath() + "/registrazione.html";
					ServletContext servletContext = getServletContext();
					final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
					ctx.setVariable("errorMsg", "esiste già un utente con lo stesso username");
					path = "/registrazione.html";
					templateEngine.process(path, ctx, response.getWriter());
					return;
					
				} else {
					ServletContext servletContext = getServletContext();
					path = "/index.html";
					final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
					ctx.setVariable("userRegistred", "Registrazione effettuata, accedi");
					templateEngine.process(path, ctx, response.getWriter());
				}
				
			}catch(SQLException e) {
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in DB while inserting new user");
				return;
			}
			
		}

	// ritorna true se la mail corrisponde alla regex
	private boolean testMail(String mail) {
		String emailPattern = "^[\\w\\.-]+@[a-zA-Z\\d\\.-]+\\.[a-zA-Z]{2,}$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(mail);
		return matcher.matches();
	}

}
