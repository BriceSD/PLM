package plm.core.model.session;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;

import plm.core.lang.ProgrammingLanguage;
import plm.core.model.Game;
import plm.core.model.User;
import plm.core.model.UserAbortException;
import plm.core.model.lesson.Exercise;
import plm.core.model.lesson.Lecture;
import plm.core.model.lesson.Lesson;
import plm.core.model.tracking.GitUtils;

public class GitSessionKit implements ISessionKit {

	private Game game;
	private String reponame;
	private User cachedUser = null;

	public GitSessionKit(Game game) {
		this.game = game;
	}

	/**
	 * Store the user source code for all the opened lessons. It doesn't need to
	 * save the lesson summaries : it's compute on start
	 *
	 * @param path
	 * @throws UserAbortException
	 */
	@Override
	public void storeAll(File path) throws UserAbortException {
		reponame = String.valueOf(game.getUsers().getCurrentUser().getUserUUID());

		Collection<Lesson> lessons = this.game.getLessons();
		for (Lesson lesson : lessons) {
			File repoDir = new File(path.getAbsolutePath() + System.getProperty("file.separator") + reponame);

			// First save the bodies
			storeLesson(repoDir, lesson);
			
			// Recompute the summaries
			File summary = new File(repoDir, lesson.getId() + ".summary");
			try {
				FileWriter fwSummary = new FileWriter(summary.getAbsoluteFile());
				BufferedWriter bwSummary = new BufferedWriter(fwSummary);
				bwSummary.write(Game.getInstance().studentWork.lessonSummary(lesson.getId()));
				bwSummary.close();
			} catch (IOException ex) {
				System.out.println("Failed to write the lesson summary on disk: "+ex.getLocalizedMessage());
			}
		}
	}

	/**
	 * Load the user source code of the lessons' exercises. Also get the per
	 * lesson summaries
	 *
	 * @param path
	 */
	@Override
	public void loadAll(final File path) {
		reponame = String.valueOf(game.getUsers().getCurrentUser().getUserUUID());

		if (!Game.getInstance().getUsers().getCurrentUser().equals(cachedUser)) {
			if (Game.getInstance().isDebugEnabled())
				System.out.println("The user changed! switch to the right branch");
			cachedUser = Game.getInstance().getUsers().getCurrentUser();
			
			File gitDir = new File(Game.getSavingLocation() + System.getProperty("file.separator") + cachedUser.getUserUUID().toString());
			if (! gitDir.exists()) {
				String repoUrl = Game.getProperty("plm.git.server.url");
				String userBranch = "PLM"+GitUtils.sha1(reponame); // For some reason, github don't like when the branch name consists of 40 hexadecimal, so we add "PLM" in front of it

				
				Git git;
				try {
					git = Git.cloneRepository().setURI(repoUrl).setDirectory(gitDir).setBranchesToClone(Arrays.asList(userBranch)).call();

					// If no branch can be found remotely, create a new one.
					if (git == null) { 
						git = Git.init().setDirectory(gitDir).call();
						StoredConfig cfg = git.getRepository().getConfig();
						cfg.setString("remote", "origin", "url", repoUrl);
						cfg.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
						cfg.save();
						git.commit().setMessage("Empty initial commit").setAuthor(new PersonIdent("John Doe", "john.doe@plm.net")).setCommitter(new PersonIdent("John Doe", "john.doe@plm.net")).call();
						System.out.println(Game.i18n.tr("Creating a new session locally, as no corresponding session could be retrieved from the servers.",userBranch));
					} else {
						System.out.println(Game.i18n.tr("Your session {0} was automatically retrieved from the servers.",userBranch));
					}
				} catch (GitAPIException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}

		// Load bodies
		for (Lesson lesson : this.game.getLessons()) {
			loadLesson(path, lesson);
		}
		
		// Load summary from the lastly saved files, 
		// but don't trust the game.getLessons that is empty at startup, so search for existing files on disk
		String pattern = "*.summary";
		FileSystem fs = FileSystems.getDefault();
		final PathMatcher matcher = fs.getPathMatcher("glob:" + pattern); // to match file names ending with string "summary"

		FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
				if (! matcher.matches(file.getFileName()))  // Not a summary file. Who cares?
					return FileVisitResult.CONTINUE;			
				
				String lessonId = file.getFileName().toString();
				lessonId = lessonId.substring(0,lessonId.length() - ".summary".length());

				//  1. Read file content in String
				StringBuilder sb = new StringBuilder();
				FileReader fr;
				BufferedReader br = null;
				try {
					fr = new FileReader(file.toFile().getAbsoluteFile());
					br = new BufferedReader(fr);
					String line;
					while ((line = br.readLine()) != null) 
						sb.append(line);
				} catch (FileNotFoundException e) {
					// If there is no summary file, then don't do nothing for that lesson
				} catch (IOException e) {
					System.err.println("Error while reading the summary of lesson "+lessonId+": "+e.getLocalizedMessage());
				} finally {
					if (br != null)
						try {
							br.close();
						} catch (IOException e) {
							// I don't care about not being able to close a file that I *read*
						}
				}
				
				// 2. Pass that string to the sessionDB
				Game.getInstance().studentWork.lessonSummaryParse(lessonId, sb.toString());
				
				return FileVisitResult.CONTINUE;			
			}
		};
				
		try {
			Files.walkFileTree(Paths.get(path.getAbsolutePath() + System.getProperty("file.separator") + reponame), matcherVisitor);
		} catch (IOException ex) {
			ex.printStackTrace();
		}


	}
	
	/**
	 * Store the user source code for a specified lesson
	 *
	 * @param path where to save
	 * @param lesson the lesson to save
	 * @throws UserAbortException
	 */
	@Override
	public void storeLesson(File path, Lesson lesson) throws UserAbortException {
		/* Everything's done by spy */
	}

	/**
	 * Load the lesson's exercises user source code
	 *
	 * @param path
	 * @param lesson
	 */
	@Override
	public void loadLesson(File path, Lesson lesson) {
		for (Lecture lecture : lesson.exercises()) {
			if (lecture instanceof Exercise) {
				Exercise exercise = (Exercise) lecture;
				for (ProgrammingLanguage lang : exercise.getProgLanguages()) {
					// check if exercise already done correctly
					String doneFile = path.getAbsolutePath() + System.getProperty("file.separator") + reponame + System.getProperty("file.separator")
							+ exercise.getId() + "." + lang.getExt() + ".DONE";
					if (new File(doneFile).exists()) { // if the file exists, the exercise was correct
						Game.getInstance().studentWork.setPassed(exercise, lang, true);
					} else {
						Game.getInstance().studentWork.setPassed(exercise, lang, false);
					}
					// load source code 
					SourceFile srcFile = exercise.getSourceFile(lang, 0);
					String fileName = path.getAbsolutePath() + System.getProperty("file.separator") + reponame + System.getProperty("file.separator")
							+ exercise.getId() + "." + lang.getExt() + ".code";
					//System.out.println(fileName);
					String line;
					StringBuilder b = new StringBuilder();
					try {
						FileReader fileReader = new FileReader(fileName);
						try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
							while ((line = bufferedReader.readLine()) != null) {
								b.append(line);
								b.append("\n");
							}
						}
						srcFile.setBody(b.toString());
					} catch (FileNotFoundException fnf) {
						/* that's fine, we never did that exercise */
					} catch (IOException ex) {
						ex.printStackTrace();
					}

				}
			}
		}
	}

	@Override
	public void cleanAll(File path) {
		System.out.println("Clean all lessons. Your session is now lost.");
		for (Lesson lesson : this.game.getLessons()) {
			cleanLesson(new File(path.getAbsolutePath() + System.getProperty("file.separator") + reponame), lesson);
		}
	}

	@Override
	public void cleanLesson(File path, Lesson l) {

		String pattern = l.getId() + "*";
		FileSystem fs = FileSystems.getDefault();
		final PathMatcher matcher = fs.getPathMatcher("glob:" + pattern);
		FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
				Path name = file.getFileName();
				if (matcher.matches(name)) {
					new File(name + "").delete(); // delete files related to the selected Lesson
				}
				return FileVisitResult.CONTINUE;
			}
		};
		try {
			Files.walkFileTree(Paths.get(path.getPath()), matcherVisitor);
		} catch (IOException ex) {

		}
	}

}
