package lessons.welcome.methods.picture;

import java.awt.Color;

import jlm.core.model.lesson.ExerciseTemplated;
import jlm.core.model.lesson.Lesson;
import jlm.universe.Direction;
import jlm.universe.bugglequest.Buggle;
import jlm.universe.bugglequest.BuggleWorld;

public class PictureMono3 extends ExerciseTemplated {

	public PictureMono3(Lesson lesson) {
		super(lesson);
		BuggleWorld myWorld =  new BuggleWorld("World",45,45);
		myWorld.setDelay(5);
		new Buggle(myWorld, "Picasso", 0, 44, Direction.EAST, Color.black, Color.lightGray);
				
		setup(myWorld);
	}
}