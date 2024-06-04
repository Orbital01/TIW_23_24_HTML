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
	
	//(chiunque)
	//ritorna un gruppo dato l'id del gruppo
	public Gruppi getGroupByIdAndUser(int id, String username) throws SQLException {
		String query = "SELECT *, DATEDIFF(durata, CURDATE()) AS diff FROM gruppi INNER JOIN partecipation ON gruppi.ID = partecipation.ID_gruppo WHERE gruppi.ID = ? AND (TIW.gruppi.admin = ? OR partecipation.user = ?)";
		Gruppi gruppo = null;
		
		ResultSet result = null;
		PreparedStatement pstatement = null;
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setInt(1, id);
			pstatement.setString(2, username);
			pstatement.setString(3, username);
			result = pstatement.executeQuery();
			
			while (result.next()) {
				gruppo = new Gruppi();
				gruppo.setID(result.getInt("ID"));
				gruppo.setNome(result.getString("nome"));
				gruppo.setDescrizione(result.getString("descrizione"));
				gruppo.setDurata(result.getInt("diff"));
				gruppo.setAdmin(result.getString("admin"));
				gruppo.setMaxPartecipanti(result.getInt("max_partecipanti"));
				gruppo.setMinPartecipanti(result.getInt("min_partecipanti"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (result != null)
					result.close();
			} catch (SQLException e1) {
				throw e1;
			}
			try {
				if (pstatement != null)
					pstatement.close();
			} catch (SQLException e1) {
				throw e1;
			}
		}
		return gruppo;
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
		//agggiungo ai partecipanti l'admin stesso 
		partecipanti.add(admin);
		//chiamo il partecipation dao e aggiungo tutti i partecipanti 
		PartecipationDAO pdao = new PartecipationDAO(this.connection);
		pdao.addPartecipation(partecipanti, temp);
		
	}
	
}