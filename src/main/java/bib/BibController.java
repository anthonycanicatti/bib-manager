package bib;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
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

	/**
	 * Main page mapping
	 *
	 * @param invUpload optional parameter for if upload failed
	 * @param model model to add all data to be displayed on screen
	 * @return main web page
	 */
	@RequestMapping("/biblio")
	public String biblioForm(@RequestParam(value="invalidUpload",required = false) String invUpload, Model model) {

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
		model.addAttribute("invalidUpload", invUpload);
		return "biblio";
	}

	/**
	 * Get Mapping for adding entry
	 *
	 * @param validity optional parameter if adding entry with invalid input
	 * @param model adding emtpy data to model
	 * @return add page
	 */
	@GetMapping("/add")
	public String addEntry(@RequestParam(value="invalid", required = false) String validity, Model model){
		model.addAttribute("entry", new BibEntry());
		model.addAttribute("invalid", validity);
		return "add";
	}

	/**
	 * Post Mapping for adding an entry
	 *
	 * @param entry the populated entry object
	 * @param bindingResult result of binding the data entered to the object
	 * @param model the model to add the added entry
	 * @param redirectAttributes only needed if theres an error in binding result
	 * @return will redirect to biblio
	 */
	@PostMapping("/add")
	public String addEntry(@Valid @ModelAttribute BibEntry entry,
						   BindingResult bindingResult, Model model,
						   RedirectAttributes redirectAttributes){

		if(bindingResult.hasErrors()) {
			redirectAttributes.addAttribute("invalid","true");
			return "redirect:/add";
		}

		model.addAttribute(entry);

		jdbcTemplate.update(INSERT, entry.getTitle(), entry.getAuthor(),
				entry.getYear(), entry.getJournal());
		return "redirect:/biblio"; // redirect to biblio - dont explicitly return biblio
	}

	/**
	 * Mapping for removing an entry
	 *
	 * @param requestParams just an ID param of the entry
	 * @return will redirect to biblio
	 */
	@RequestMapping("/remove")
	public String remove(@RequestParam Map<String, String> requestParams){
		int id = Integer.parseInt(requestParams.get("id"));

		System.out.println("*** REMOVING ENTRY ID "+id+" ***");
		jdbcTemplate.update(DELETE, id);

		return "redirect:/biblio"; // redirect to biblio - dont explicitly return biblio
	}

	/**
	 * Get Mapping for initial edit entry request
	 *
	 * @param requestParams properties of BibEntry requesting to edit
	 * @param model model for inserting edited object back into page
	 * @return edit page
	 */
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

	/**
	 * Post Mapping upon submitting edits
	 *
	 * @param entry the BibEntry object that was edit (attempted)
	 * @param idParm the ID of the BibEntry (for some reason had to pass separately)
	 * @param model model not actually needed or used
	 * @return will redirect to biblio
	 */
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

	/**
	 * Get Mapping for searching the database
	 *
	 * @param query the search query object (really just a class wrapper for a string)
	 * @param model model used for inserting list of results from search back to webpage
	 * @return search page
	 */
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

	/**
	 * Post Mapping for importing files
	 * @param file the file the user selected
	 * @param session
	 * @param redirectAttributes attributes needed to send back to redirect (in case file upload failed)
	 * @return will redirect to biblio
	 */
	@RequestMapping(value="/fileImport",method=RequestMethod.POST)
	public String importFile(@RequestParam MultipartFile file, HttpSession session,
							 RedirectAttributes redirectAttributes){

		String fileName = file.getOriginalFilename();
		if(!fileName.endsWith(".bib")){ // quit right away if its not a bib file
			redirectAttributes.addAttribute("invalidUpload","true");
			return "redirect:/biblio";
		}
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
				if(line.trim().startsWith("year")) {
					String yrLine = "";
					for(char c : line.toCharArray()){
						if(Character.isDigit(c)) yrLine += c;
					}
					year = Integer.parseInt(yrLine);
				}
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

	/**
	 * Mapping for exporting files in the database
	 *
	 * @param requestParams the parameters passed into the URL (entry info)
	 * @param request the HTTPServletRequest - needed for path to create file to export
	 * @param response the HTTPServletResponse - needed to flush file to output (download)
	 */
	@RequestMapping("/export")
	public void export(@RequestParam Map<String, String> requestParams, HttpServletRequest request,
						 HttpServletResponse response){
		String author = requestParams.get("author");
		String title = requestParams.get("title");
		int year = Integer.parseInt(requestParams.get("year"));
		String journal = requestParams.get("journal");

		System.out.println("*** EXPORTING FILE ***");
		System.out.println("Export params:\n\t"+author+"\n\t"+title+"\n\t"+year+"\n\t"+journal);
		String path = request.getServletContext().getRealPath("/")  +
				title.replace(" ", "_") + "_entry.bib";
		System.out.println("file path: "+path);
		BibEntry entry = new BibEntry();
		entry.setAuthor(author);
		entry.setTitle(title);
		entry.setYear(year);
		entry.setJournal(journal);

		createFile(entry, path); // first create the file to download
		File file = new File(path); // now have a reference to the file

		response.setContentType("application/bib"); // the file format
		response.setContentLength((int)file.length());
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", file.getName());
		response.setHeader(headerKey, headerValue);

		try { // do the actual out writing of the file
			InputStream in = new FileInputStream(file);
			org.apache.commons.io.IOUtils.copy(in, response.getOutputStream());
			response.flushBuffer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Upon request to export file, first create file
	 * @param entry the BibEntry information to create file out of
	 * @param filePath the path to create file for
	 */
	private void createFile(BibEntry entry, String filePath){
		try {
			FileWriter out = new FileWriter(filePath);
			out.write("@article{"+entry.getTitle()+"_bibtex,\n");
			out.write(" author\t\t=\t\""+entry.getAuthor()+"\",\n"); // write author line
			out.write(" title\t\t=\t\""+entry.getTitle()+"\",\n"); // write title line
			out.write(" year\t\t=\t"+entry.getYear()+",\n"); // write year line
			out.write(" journal\t=\t\""+entry.getJournal()+"\"\n"); // write journal line
			out.write("}"); // finish

			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}