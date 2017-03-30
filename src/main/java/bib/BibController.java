package bib;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class BibController {

	// INSERT query
	final String INSERT = "INSERT INTO bibentries (title, author, year, journal)" +
			" VALUES (?,?,?,?)";
	// DELETE query
	final String DELETE = "DELETE FROM bibentries WHERE id = ?";
	// UPDATE query
	final String UPDATE = "UPDATE bibentries SET title = ?, author = ?, year = ?, " +
			"journal = ? WHERE id = ?";

	@Autowired
	JdbcTemplate jdbcTemplate;

	@RequestMapping("/biblio")
	public String biblioForm(Model model) {

		// first handle database creation if not already done
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS bibentries (id SERIAL, title VARCHAR(255), " +
				"author VARCHAR(255), year INTEGER, journal VARCHAR(255))");

		// now retrieve everything in the database
		List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * from bibentries");

		ArrayList<BibEntry> bibEntries = new ArrayList<BibEntry>();
		for(Map<String, Object> map : results){
			String author = (String) map.get("author");
			String title = (String) map.get("title");
			int year = Integer.parseInt(map.get("year").toString());
			String journal = (String) map.get("journal");
			int id = Integer.parseInt(map.get("id").toString());

			// add the entry to the list
			BibEntry entry = new BibEntry();
			entry.setAuthor(author);
			entry.setTitle(title);
			entry.setYear(year);
			entry.setJournal(journal);
			entry.setId(id);
			bibEntries.add(entry);
		}

		model.addAttribute("entries", bibEntries);
		model.addAttribute("query", new SearchQuery());
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
		int id = Integer.parseInt(requestParams.get("id"));

		System.out.println("*** REMOVING ENTRY ID "+id+" ***");
		jdbcTemplate.update(DELETE, id);

		return "redirect:/biblio"; // redirect to biblio - dont explicitly return biblio
	}

	@RequestMapping(value="/edit", method=RequestMethod.GET)
	public String edit(@RequestParam Map<String, String> requestParams, Model model){
		String author = requestParams.get("author");
		String title = requestParams.get("title");
		int year = Integer.parseInt(requestParams.get("year"));
		String journal = requestParams.get("journal");
		int id = Integer.parseInt(requestParams.get("id"));

		System.out.println("*** EDITING ENTRY ID "+id+" ***");
		System.out.println("Params:\n\t"+author+"\n\t"+title+"\n\t"+year+"\n\t"+journal);
		BibEntry entry = new BibEntry();
		entry.setId(id);
		entry.setAuthor(author);
		entry.setTitle(title);
		entry.setYear(year);
		entry.setJournal(journal);

		model.addAttribute("entry", entry);
		model.addAttribute("id", id);

		return "edit";
	}

	@RequestMapping(value="/editSubmit", method=RequestMethod.POST)
	public String editSubmit(@ModelAttribute("entry") BibEntry entry,
							 @RequestParam("id") String idParm, Model model){

		String author = entry.getAuthor();
		String title = entry.getTitle();
		int year = entry.getYear();
		String journal = entry.getJournal();
		int id = Integer.parseInt(idParm);

		System.out.println("New params:\n\t"+author+"\n\t"+title+"\n\t"+year+"\n\t"+journal);
		int numRows = jdbcTemplate.update(UPDATE, title, author, year, journal, id);
		System.out.println("*** EDITED "+numRows+" ROWS ***");

		return "redirect:/biblio";
	}

	@RequestMapping(value="/search", method=RequestMethod.GET)
	public String search(@ModelAttribute("query") SearchQuery query, Model model){

		String term = query.getQ();
		System.out.println("*** SEARCH QUERY: "+term);

		ArrayList<BibEntry> entries = new ArrayList<BibEntry>();
		List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * FROM bibentries");
		for(Map<String, Object> row : results){
			String author = (String)row.get("author");
			String title = (String)row.get("title");
			int year = Integer.parseInt(row.get("year").toString());
			String journal = (String)row.get("journal");
			int id = Integer.parseInt(row.get("id").toString());

			BibEntry entry = new BibEntry();
			entry.setAuthor(author);
			entry.setTitle(title);
			entry.setYear(year);
			entry.setJournal(journal);
			entry.setId(id);

			if(author.toLowerCase().contains(term.toLowerCase()) || title.toLowerCase().contains(term.toLowerCase())
					|| journal.toLowerCase().contains(term.toLowerCase()))
				entries.add(entry);
			try {
				if(year == Integer.parseInt(term))
					entries.add(entry);
			} catch(NumberFormatException nfe){
				// term was not a number, can safely ignore
			}

		}
		model.addAttribute("entries", entries);

		return "search";
	}

	@RequestMapping(value="/fileImport",method=RequestMethod.POST)
	public String importFile(@RequestParam MultipartFile file, HttpSession session){
		String fileName = file.getOriginalFilename();
		System.out.println("*** FILE UPLOAD: "+fileName+" ***");

		try {
			byte buffer[] = file.getBytes();
			BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
			String line, author = "", title = "", journal = "";
			int year = 0;
			while((line = reader.readLine()) != null){
				if(line.trim().startsWith("author")){
					author = line.split("=")[1].replace("{","")
							.replace("}","").replace("\"", "")
							.replace(",","");
				}
				if(line.trim().startsWith("title")){
					title = line.split("=")[1].replace("{", "")
							.replace("}","").replace("\"","")
							.replace(",","");
				}
				if(line.trim().startsWith("year"))
					year = Integer.parseInt(line.replace(",","")
							.replace("\"", "")
							.replace(" ", "").split("=")[1]);
				if(line.trim().startsWith("journal")) {
					journal = line.split("=")[1].replace("{", "")
							.replace("}", "").replace("\"", "")
							.replace(",", "");
				}
			}
			BibEntry entry = new BibEntry();
			entry.setTitle(title);
			entry.setAuthor(author);
			entry.setYear(year);
			entry.setJournal(journal);

			System.out.println("*** ADDING ENTRY FROM FILE ***");
			jdbcTemplate.update(INSERT, entry.getTitle(), entry.getAuthor(),
					entry.getYear(), entry.getJournal());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "redirect:/biblio";
	}
}