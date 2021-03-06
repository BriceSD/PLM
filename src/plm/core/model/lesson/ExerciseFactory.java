package plm.core.model.lesson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import plm.core.PLMCompilerException;
import plm.core.lang.ProgrammingLanguage;
import plm.core.log.LogHandler;
import plm.core.log.Logger;
import plm.core.model.lesson.Exercise.StudentOrCorrection;
import plm.core.model.lesson.tip.AbstractTipFactory;
import plm.core.model.lesson.tip.DefaultTipFactory;
import plm.core.model.session.SourceFile;
import plm.core.model.session.TemplatedSourceFileFactory;
import plm.core.utils.FileUtils;

public class ExerciseFactory {

	private ExerciseRunner exerciseRunner;
	private ProgrammingLanguage[] programmingLanguages;
	private Locale[] humanLanguages;
	private TemplatedSourceFileFactory sourceFileFactory;
	private AbstractTipFactory tipsFactory = new DefaultTipFactory();

	private String rootDirectory = "exercises";

	public ExerciseFactory(Locale locale, ExerciseRunner exerciseRunner, ProgrammingLanguage[] programmingLanguages, Locale[] humanLanguages) {
		this.exerciseRunner = exerciseRunner;
		this.programmingLanguages = programmingLanguages;
		this.humanLanguages = humanLanguages;
		this.sourceFileFactory = new TemplatedSourceFileFactory(locale);
	}

	public static Exercise cloneExercise(Exercise exo) {
		return new BlankExercise(exo);
	}

	public void initializeExercise(Exercise exo, ProgrammingLanguage progLang) {
		computeSupportedProgrammingLanguages(exo);
		computeMissions(exo);
		computeHelps(exo);
		computeAnswer(exo, progLang);
	}

	public void computeSupportedProgrammingLanguages(Exercise exo) {
		for(ProgrammingLanguage progLang: programmingLanguages) {
			String entityName = progLang.nameOfCorrectionEntity(exo).replaceAll("\\.", "/") + "." + progLang.getExt();
			String entityPath = rootDirectory + "/" + entityName;
			if(new File(entityPath).exists()) {
				SourceFile sourceFile = sourceFileFactory.newSourceFromFile(exo.getId(), progLang, entityPath);
				exo.addDefaultSourceFile(progLang, sourceFile);
			}
		}
	}

	public void computeMissions(Exercise exo) {
		String filename = exo.getId().replaceAll("\\.", "/");
		for(Locale humanLanguage: humanLanguages) {
			StringBuffer sb = null;
			try {
				sb = FileUtils.readContentAsText(filename, humanLanguage, "html", true);
				String mission = tipsFactory.computeTips(exo, humanLanguage, sb.toString());
				exo.addMission(humanLanguage.getLanguage(), mission);
			}
			catch (FileNotFoundException e) {
				Logger.log(LogHandler.INFO, "No mission found for: " + exo.getId());
				exo.addMission(humanLanguage.getLanguage(), "");
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void computeHelps(Exercise exo) {
		String baseName = exo.getInitialWorlds().get(0).getClass().getName();
		String filename = baseName.replaceAll("\\.", "/");
		for(Locale humanLanguage: humanLanguages) {
			StringBuffer sb = null;
			try {
				sb = FileUtils.readContentAsText(filename, humanLanguage, "html", true);
				exo.addHelp(humanLanguage.getLanguage(), sb.toString());
			}
			catch (FileNotFoundException e) {
				Logger.log(LogHandler.INFO, "No help found for: " + exo.getId());
				exo.addHelp(humanLanguage.getLanguage(), "");
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void computeAnswer(Exercise exo, ProgrammingLanguage progLang) {
		// TODO: Handle case if progLang is not supported
		if(exo.isProgLangSupported(progLang)) {
			SourceFile sf = exo.getDefaultSourceFile(progLang);
			try {
				exerciseRunner.mutateEntities(exo, sf, progLang, StudentOrCorrection.CORRECTION, null);
			} catch (PLMCompilerException e) {
				e.printStackTrace();
			}
			exerciseRunner.runDemo(exo, progLang);
		}
	}

	public String getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public void setTipFactory(AbstractTipFactory tipsFactory) {
		this.tipsFactory = tipsFactory;
	}
}
