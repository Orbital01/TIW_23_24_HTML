package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import it.polimi.tiw.beans.User;
import it.polimi.tiw.dao.*;
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

/**
 * Servlet implementation class CreateGroup
 */
@WebServlet("/CreateGroup")
public class CreateGroup extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CreateGroup() {
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

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		doPost(request, response);
		
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// se non sono loggato torno alla pagina di login
		String loginpath = getServletContext().getContextPath() + "/index.html";

		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}

		// prendo tutti i parametri dalla servlet precedente
		String nome = (String) session.getAttribute("nome");
		String descrizione = (String) session.getAttribute("descrizione");
		int giorni = (int) session.getAttribute("giorni");
		int minPartecipanti = (int) session.getAttribute("minPartecipanti");
		int maxPartecipanti = (int) session.getAttribute("maxPartecipanti");

		// prendo lo username di chi sta effettuando l'operazione
		User user = (User) session.getAttribute("user");
		String admin = user.getUsername();

		// prendo gli user selezionati
		String[] utentiSelezionati = request.getParameterValues("selectedUsers");

		ArrayList<String> utenti = new ArrayList<>();
		for (String userId : utentiSelezionati) {
			utenti.add(userId);
		}
		
		//controllo che gli utenti selezionati esistano nella base di dati
		UserDAO uDAO = new UserDAO(connection);
		ArrayList<String> allUsernames = new ArrayList<>();
		
		try {
			ArrayList<User> allUsers = uDAO.getAllUser();
			
			 for (User u : allUsers) {
				 	allUsernames.add(u.getUsername());
	            }
			 
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		for(String userID : utenti) {
			if(!allUsernames.contains(userID)) {
				// Imposto l'errore
	            request.setAttribute("errorMessage", "user added does not exist");
	            
	            // Forward alla servlet GoToError
	            RequestDispatcher dispatcher = request.getRequestDispatcher("/GoToError");
	            dispatcher.forward(request, response);
				return;
			}
		}
		

		// controllo che il numero di elementi sia corretto
		int isOk = checkNumPart(utentiSelezionati, minPartecipanti, maxPartecipanti);

		// carico il numero di tentativi o lo setto a 1 (prima visita)
		Integer tentativi = (Integer) session.getAttribute("tentativi");
		if (tentativi == null) {
			tentativi = 1;
			request.getSession().setAttribute("tentativi", tentativi);
		}

		// controllo quante volte l'utente ha fatto l'accesso a questa pagina
		if (checkTries(tentativi)) {
			// se è a 3 lo porto alla pagina CANCELLAZIONE
			String path = getServletContext().getContextPath() + "/GoToCancellazione";

			// prima del redirect resetto il contatore per delle prossime creazioni
			tentativi = null;
			request.getSession().setAttribute("tentativi", tentativi);
			response.sendRedirect(path);
			return;
		}

		// se è ok procedo con la creazione del gruppo e resetto il contatore
		if (isOk == 0) {
			GruppiDAO groupDao = new GruppiDAO(connection);

			try {
				groupDao.addGroup(nome, descrizione, giorni, admin, maxPartecipanti, minPartecipanti, utenti);
				tentativi = null;
				request.getSession().setAttribute("tentativi", tentativi);

				// vado alla home
				String homepath = getServletContext().getContextPath() + "/GoToHome";
				response.sendRedirect(homepath);
				return;

			} catch (SQLException e) {
				// Imposto l'errore
	            request.setAttribute("errorMessage", "unable to add group");
	            
	            // Forward alla servlet GoToError
	            RequestDispatcher dispatcher = request.getRequestDispatcher("/GoToError");
	            dispatcher.forward(request, response);
				return;
			}

		} else {

			// altrimenti aumento il contatore
			tentativi++;
			request.getSession().setAttribute("tentativi", tentativi);

			// poi ritorno su questa pagina
			ArrayList<User> tuttiUtenti = null;
			UserDAO userDao = new UserDAO(connection);
			try {
				tuttiUtenti = userDao.getAllUser();
			} catch (SQLException e) {
				// Imposto l'errore
	            request.setAttribute("errorMessage", "unable to recover Users");
	            
	            // Forward alla servlet GoToError
	            RequestDispatcher dispatcher = request.getRequestDispatcher("/GoToError");
	            dispatcher.forward(request, response);
				return;
			}

			String path = "/WEB-INF/Anagrafica.html";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

			// in base al valore del check sugli utenti ritorno un messaggio diverso
			if (isOk == 1) {
				int value = minPartecipanti - utenti.size();
				ctx.setVariable("errorMsg", "troppi pochi utenti selezionati, aggiungerne almeno " + value); // messaggio
																												// di
																												// errore
			} else if (isOk == 2) {
				int value = utenti.size() - maxPartecipanti;
				ctx.setVariable("errorMsg", "troppi utenti selezionati eliminarne almeno " + value); // messaggio di
																										// errore
			}

			ctx.setVariable("selectedUsers", utentiSelezionati);
			ctx.setVariable("users", tuttiUtenti);

			templateEngine.process(path, ctx, response.getWriter());

			return;
		}
	}

	
	
	// ritorna true se il gruppo è ok altrimenti false
	private int checkNumPart(String[] utenti, int minPartecipanti, int maxPartecipanti) {
		
		if(utenti.length < minPartecipanti) {
			//ho troppi pochi utenti selezionati 
			return 1;
		} else if(utenti.length > maxPartecipanti) {
			//ho più utenti selezionati del dovuto
			return 2;
		}else {
			return 0;
		}
	}

	// controlla che l'utente non abbia superato i tre tentativi
	private Boolean checkTries(int tentativi) {
		if (tentativi > 3) {
			return true;
		} else {
			return false;
		}
	}

}
