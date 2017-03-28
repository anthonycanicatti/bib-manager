package bib;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class BibController {

	// INSERT query
	String INSERT = "INSERT INTO bibentries (title, author, year, journal)" +
			" VALUES (?,?,?,?)";

	@Autowired
	JdbcTemplate jdbcTemplate;

	@GetMapping("/biblio")
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

		model.addAttribute("entry", new BibEntry());
		model.addAttribute("entries", bibEntries);
		return "biblio";
	}

	@PostMapping("/biblio")
	public String biblioSubmit(@ModelAttribute BibEntry entry, Model model){
		model.addAttribute("entry", entry);
		//bibEntries.add(entry);
		String author = entry.getAuthor();
		String title = entry.getTitle();
		int year = entry.getYear();
		String journal = entry.getJournal();

		jdbcTemplate.update(INSERT, title, author, year, journal);
		return "addEntry";
	}

	@RequestMapping("/remove")
	public String remove(@RequestParam Map<String, String> requestParams){
		String author = requestParams.get("author");
		String title = requestParams.get("title");
		int year = Integer.parseInt(requestParams.get("year"));
		String journal = requestParams.get("journal");

		System.out.println("Requesting to remove: \n\t"+author+"\n\t"+title+
			"\n\t"+year+"\n\t"+journal);

		return "biblio";
	}

}