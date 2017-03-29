package bib;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class BibController {

	// INSERT query
	final String INSERT = "INSERT INTO bibentries (title, author, year, journal)" +
			" VALUES (?,?,?,?)";
	// DELETE query
	final String DELETE = "DELETE FROM bibentries WHERE title = ? and author = ? " +
			"and year = ? and journal = ?";
	// UPDATE query
	final String UPDATE = "UPDATE bibentries SET title = ?, author = ?, year = ?, " +
			"journal = ? WHERE title = ? and author = ? and year = ? and journal = ?";

	@Autowired
	JdbcTemplate jdbcTemplate;

	@RequestMapping("/biblio")
	public String biblioForm(Model model) {

		// first handle database creation if not already done
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS bibentries (id SERIAL, title VARCHAR(255), " +
				"author VARCHAR(255), year INTEGER, journal VARCHAR(255))");

		// now retrieve everything in the database
		List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT title," +
				" author, year, journal from bibentries");

		ArrayList<BibEntry> bibEntries = new ArrayList<BibEntry>();
		for(Map<String, Object> map : results){
			String author = (String) map.get("author");
			String title = (String) map.get("title");
			int year = Integer.parseInt(map.get("year").toString());
			String journal = (String) map.get("journal");

			// add the entry to the list
			BibEntry entry = new BibEntry();
			entry.setAuthor(author);
			entry.setTitle(title);
			entry.setYear(year);
			entry.setJournal(journal);
			bibEntries.add(entry);
		}

		model.addAttribute("entries", bibEntries);
		return "biblio";
	}

	@GetMapping("/add")
	public String addEntry(Model model){
		model.addAttribute("entry", new BibEntry());
		return "add";
	}

	@PostMapping("/add")
	public String addEntry(@ModelAttribute BibEntry entry, Model model){
		model.addAttribute(entry);

		jdbcTemplate.update(INSERT, entry.getTitle(), entry.getAuthor(),
				entry.getYear(), entry.getJournal());
		return "redirect:/biblio"; // redirect to biblio - dont explicitly return biblio
	}

	@RequestMapping("/remove")
	public String remove(@RequestParam Map<String, String> requestParams){
		String author = requestParams.get("author");
		String title = requestParams.get("title");
		int year = Integer.parseInt(requestParams.get("year"));
		String journal = requestParams.get("journal");

		System.out.println("Requesting to remove: \n\t"+author+"\n\t"+title+
			"\n\t"+year+"\n\t"+journal);
		jdbcTemplate.update(DELETE, title, author, year, journal);

		return "redirect:/biblio"; // redirect to biblio - dont explicitly return biblio
	}

	@GetMapping("/edit")
	public String edit(@RequestParam Map<String, String> requestParams, Model model){
		String author = requestParams.get("author");
		String title = requestParams.get("title");
		int year = Integer.parseInt(requestParams.get("year"));
		String journal = requestParams.get("journal");
		String queryForId = "SELECT id from bibentries WHERE" +
				" author = ? and title = ? and year = ?" +
				" and journal = ?";

		int id = jdbcTemplate.query(queryForId, new Object[]{author, title, year, journal}, new RowMapper<Integer>() {
			@Override
			public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
				return resultSet.getInt("id");
			}
		}).get(0);
		model.addAttribute("id", id);
		model.addAttribute("author", author);
		model.addAttribute("title", title);
		model.addAttribute("year", year);
		model.addAttribute("journal", journal);
		return "edit";
	}

	@PostMapping("/edit")
	public String editSubmit(@RequestParam Map<String, String> requestParams, Model model){
		String author = requestParams.get("author");
		String title = requestParams.get("title");
		int year = Integer.parseInt(requestParams.get("year"));
		String journal = requestParams.get("journal");

		//jdbcTemplate.update(UPDATE, )
		return "redirect:/biblio";
	}
}