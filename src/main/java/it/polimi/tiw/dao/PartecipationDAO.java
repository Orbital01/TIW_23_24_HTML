package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import it.polimi.tiw.beans.Gruppi;
import java.util.ArrayList;

public class PartecipationDAO {

	private Connection connection;

	public PartecipationDAO(Connection connection) {
		this.connection = connection;
	}

	// (user)
	// recupera tutti i gruppi dove l'user è stato invitato
	public ArrayList<Gruppi> getGroupsWithUser(String username) throws SQLException {
		
		String query = "SELECT ID, nome, descrizione, DATEDIFF(durata, CURDATE()) AS diff, admin, min_partecipanti, max_partecipanti FROM partecipation JOIN gruppi ON ID_gruppo=ID  WHERE user = ? and durata >= CURDATE()";
		ArrayList<Gruppi> listaGruppi = new ArrayList<Gruppi>();
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, username);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Gruppi temp = new Gruppi();

					temp.setID(result.getInt("ID"));
					temp.setNome(result.getString("nome"));
					temp.setDescrizione(result.getString("descrizione"));
					temp.setDurata(result.getInt("diff"));
					temp.setAdmin(result.getString("admin"));
					temp.setMaxPartecipanti(result.getInt("max_partecipanti"));
					temp.setMinPartecipanti(result.getInt("min_partecipanti"));

					listaGruppi.add(temp);
				}

				return listaGruppi;
			}
		}
	}
	
	//(user)
	//ritorna i dettagli dei partecipanti di un gruppo (escluso l'admin)
	public ArrayList<String> getPartecipants(int id) throws SQLException{
		ArrayList<String> nomi = new ArrayList<String>();
		String query = "select distinct nome, cognome FROM partecipation NATURAL JOIN users WHERE ID_gruppo = ? ORDER BY cognome asc ";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, id);
			try (ResultSet result = pstatement.executeQuery();) {
				while(result.next()) {
					String temp;
					temp = result.getString("nome") + "" + result.getString("cognome");
					nomi.add(temp);
				}
				return nomi;
			}
		}
	}
	
	

	// (admin)
	// aggiunge gli inviti al gruppo appena creato
	public void addPartecipation(ArrayList<String> usernames, int id) throws SQLException {
		String query = "INSERT INTO partecipation VALUES (?, ?)";
		for(String username : usernames) {
			try (PreparedStatement pstatement = connection.prepareStatement(query);){
				pstatement.setInt(1, id);
				pstatement.setString(2, username);
				pstatement.executeUpdate();
			}
		}
	}
	

}