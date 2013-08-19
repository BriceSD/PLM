package lessons.sort.comb;

import jlm.universe.sort.SortingEntity;

class ScalaAlgCombSortEntity extends SortingEntity {

	override def run() {
		combSort();
	}

	/* BEGIN TEMPLATE */
	def combSort() {
		/* BEGIN SOLUTION */
		var gap = getValueCount();
		var swapped=false;
		do {
			if (gap>1) 
				gap = (gap.asInstanceOf[Double] / 1.3).asInstanceOf[Int];
			swapped = false;
			for (i <- 0 to getValueCount()-gap-1)
				if (!isSmaller(i,i+gap)) {
					swap(i,i+gap);
					swapped =true;
				}	
		} while (gap>1 || swapped);
		/* END SOLUTION */
	}
	/* END TEMPLATE */

}

