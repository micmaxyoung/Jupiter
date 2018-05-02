package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection{
	private Connection conn;

	public MySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}


	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		String sql = "INSERT IGNORE INTO history (user_id, item_id) VALUES (?, ?)";
		try {
			for (String itemId : itemIds) {
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
              e.printStackTrace();
		}

		
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
		try {
			for (String itemId : itemIds) {
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favorite = new HashSet<>();
		if (conn == null) {
			return favorite;
		}
		String sql = "SELECT item_id FROM history WHERE user_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				favorite.add(rs.getString("item_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favorite;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<Item> favorite = new HashSet<>();
		if (conn == null) {
			return favorite;
		}
		String sql = "SELECT * FROM items, (SELECT item_id FROM history WHERE user_id = ?) "
				+ "AS itemids WHERE items.item_id = itemids.item_id";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				ItemBuilder builder = new ItemBuilder();
				builder.setItemId(rs.getString("item_id"));
				builder.setName(rs.getString("name"));
				builder.setRating(rs.getDouble("rating"));
				builder.setAddress(rs.getString("address"));
				builder.setImageUrl(rs.getString("image_url"));
				builder.setUrl(rs.getString("url"));
				builder.setDistance(rs.getDouble("distance"));
				builder.setCategories(getCategories(rs.getString("item_id")));
				Item favoriteItem = builder.build();
				favorite.add(favoriteItem);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favorite;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		if (conn == null) {
			return categories;
		}
		String sql = "SELECT category FROM categories WHERE item_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				categories.add(rs.getString("category"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		List<Item> items = tmAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return;
		}
		try {
			// Insert into items table
			String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, item.getItemId());
			statement.setString(2, item.getName());
			statement.setDouble(3, item.getRating());
			statement.setString(4, item.getAddress());
			statement.setString(5, item.getImageUrl());
			statement.setString(6, item.getUrl());
			statement.setDouble(7, item.getDistance());
			statement.execute();
			
			// Update categories table for each category
			sql = "INSERT IGNORE INTO categories VALUES (?, ?)";
			for (String category : item.getCategories()) {
				statement = conn.prepareStatement(sql);
				statement.setString(1, item.getItemId());
				statement.setString(2, category);
				statement.execute();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getFullname(String userId) {
		String name = "Root";
		if (conn == null) {
			return name;
		}
		String sql = "SELECT firstname, lastname FROM users WHERE user_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				name += rs.getString("firstname") + rs.getString("lastname");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		return false;
	}

}
