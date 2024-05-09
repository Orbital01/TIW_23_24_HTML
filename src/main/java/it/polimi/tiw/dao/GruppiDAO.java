package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import it.polimi.tiw.beans.Gruppi;
import java.util.ArrayList;

public class GruppiDAO {

	private Connection connection;

	public GruppiDAO(Connection connection) {
		this.connection = connection;
	}

	// (admin)
	// ritorna i gruppi creati dallo user ancora attivi
	public ArrayList<Gruppi> getActiveGroupsByUser(String username) throws SQLException {
		String query = "SELECT *, DATEDIFF(durata, CURDATE()) AS diff FROM gruppi WHERE durata >= CURDATE() AND admin = ?";
		ArrayList<Gruppi> listaGruppi = new ArrayList<Gruppi>();
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, username);
			try (ResultSet result = pstatement.executeQuery();) {
				while(result.next()) {
					Gruppi temp = new Gruppi();
					temp.setID(result.getInt("ID"));
					temp.setNome(result.getString("nome"));
					temp.setDescrizione(result.getString("descrizione"));
					temp.setDurata(result.getInt("diff")); //non so se funzionerà come sperato
					temp.setAdmin(result.getString("admin"));
					temp.setMaxPartecipanti(result.getInt("max_partecipanti"));
					temp.setMinPartecipanti(result.getInt("min_partecipanti"));
					
					listaGruppi.add(temp);
					
				}
				return listaGruppi;
			}

		}

	}
	
	//(admin)
	//crea un gruppo 
	//chiama il PartecipationDAO per aggiungere gli inviti
	public void addGroup(String nome, String descrizione, int durata, 
			String admin, int maxPartecipanti, int minPartecipanti, ArrayList<String> partecipanti) throws SQLException{
		
		String query = "INSERT INTO gruppi (nome, descrizione, durata, admin, min_partecipanti, max_partecipanti) VALUES (?, ?, DATE_ADD(CURDATE(), INTERVAL ? DAY, ?, ?, ?)";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, nome);
			pstatement.setString(2, descrizione);
			pstatement.setInt(3, durata); //non so se funziona, da testare!!
			pstatement.setString(4, admin);
			pstatement.setInt(5, minPartecipanti);
			pstatement.setInt(6, maxPartecipanti);
			pstatement.executeUpdate();
		}
		
		int temp;
		
		String query2 = "SELECT ID FROM gruppi WHERE nome = ?"; //tanto il nome è unique
		try (PreparedStatement pstatement = connection.prepareStatement(query2);) {
			pstatement.setString(1, nome);
			try(ResultSet result = pstatement.executeQuery();){
				temp = result.getInt("ID");
			}
		}
		
		PartecipationDAO pdao = new PartecipationDAO(this.connection);
		pdao.addPartecipation(partecipanti, temp);
		
	}
	
}