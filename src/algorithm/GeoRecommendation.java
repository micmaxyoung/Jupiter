package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;


public class GeoRecommendation {
	public List<Item> recommendItems(String userId, double lat, double lon) {
		List<Item> recommendedItems = new ArrayList<>();

		DBConnection conn = DBConnectionFactory.getConnection();

		// Step 1 Get all favorited itemIds
		Set<String> favoriteItemIds = conn.getFavoriteItemIds(userId);

		// Step 2 Get all categories of favorited items
		Map<String, Integer> allCategories = new HashMap<>();
		for (String favoriteItemId : favoriteItemIds) {
			Set<String> categories = conn.getCategories(favoriteItemId);
			for (String category : categories) {
				if (!allCategories.containsKey(category)) {
					allCategories.put(category, 0);
				}
				allCategories.put(category, allCategories.get(category) + 1);
			}
		} 
		
		List<Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
		Collections.sort(categoryList, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return Integer.compare(o2.getValue(), o1.getValue());
			}
		});


		// Step 3 Do search based on catgory, filer out favorited items, sort by
		// distance
		Set<String> itemIds = new HashSet<>();
		for (Entry<String, Integer> category : categoryList) {
			List<Item> raw = conn.searchItems(lat, lon, category.getKey());
			List<Item> filter = new ArrayList<>();
			for (Item item : raw) {
				if (!itemIds.contains(item.getItemId()) && !favoriteItemIds.contains(item.getItemId())) {
					filter.add(item);
				}
			}
			Collections.sort(filter, new Comparator<Item>() {
				@Override
				public int compare(Item o1, Item o2) {
					return Double.compare(o1.getDistance(), o2.getDistance());
				}
			});
			recommendedItems.addAll(filter);
		}
		conn.close();
		return recommendedItems;
	}

}
