package plm.core.lang;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.swing.ImageIcon;

import lessons.lightbot.universe.LightBotEntity;
import plm.core.PLMCompilerException;
import plm.core.model.LogWriter;
import plm.core.model.lesson.ExecutionProgress;
import plm.core.model.lesson.Exercise;
import plm.core.model.lesson.Exercise.StudentOrCorrection;
import plm.universe.Entity;

public abstract class ProgrammingLanguage implements Comparable<ProgrammingLanguage> {
	String lang;
	String ext;
	ImageIcon icon;
	public ProgrammingLanguage(String l, String ext, ImageIcon i) {
		lang = l;
		this.ext = ext;
		this.icon = i;
	}
	public boolean equals(Object o) {
		if (!super.equals(o))
			return false;
		if (getClass() != o.getClass())
			return false;
		return lang.equals(((ProgrammingLanguage)o).lang);
	}
	public String getLang() {
		return lang;
	}
	public String getExt() {
		return ext;
	}
	@Override
	public String toString() {
		return lang;
	}
	@Override
	public int hashCode() {
		return lang.hashCode();
	}
	@Override
	public int compareTo(ProgrammingLanguage o) {
		if (o == null)
			return 1;
		int res = lang.compareTo(o.lang);
		if (res != 0)
			return res;
		return ext.compareTo(o.ext);
	}
	public ImageIcon getIcon() {
		return icon;
	}
	
	protected Map<String, String> runtimePatterns = new TreeMap<String, String>();
	public abstract void compileExo(Exercise exercise, LogWriter out, StudentOrCorrection whatToCompile) throws PLMCompilerException;
	public abstract List<Entity> mutateEntities(Exercise exercise, List<Entity> old, StudentOrCorrection whatToMutate);
	/** Make the entity run, according to the used universe and programming language.
	 * 
	 * This task is not trivial given that it depends on the universe and the programming language:
	 *  * In most universes, the active part is the entity itself. But in the Bat universe, the 
	 *    student-provided method (that is not a real entity but part of the world directly) 
	 *    is run against all testcase, that are not real worlds either.
	 *    
	 *  * Java and Scala entities are launched by just executing their {@link #run()} method that 
	 *    was redefined by the student (possibly with some templating)
	 *  * LightBot entities are launched by executing the {@link LightBotEntity#run()} method, 
	 *    that is NOT defined by the student, but interprets the code of the students.
	 *  * Python (and other scripting language) entities are launched by injecting the 
	 *    student-provided code within a {@link ScriptEngine}. 
	 *    In this later case, the java entity is injected within the scripting world so that it 
	 *    can forward the student commands to the world. 
	 *  * C starts an external program that executes the student logic, along with an handful of threads 
	 *    to deal with the pipes that are connected to the external process
	 * 
	 *  @see #run() that encodes the student logic in Java
	 */
	public abstract void runEntity(Entity ent, ExecutionProgress progress);
	
	public String nameOfCorrectionEntity(Exercise exo) { // This will be redefined by Scala to prepend "Scala" to that string
		return exo.nameOfCorrectionEntity();
	}
}
