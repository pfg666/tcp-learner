package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class RangePicker {
	private final long min, max;
	private final LinkedList<Long> pointsOfInterest;
	private final List<Tuple2<Long, Long>> rangesOfInterest;
	private final Random r;
	
	public RangePicker(Random random, long min, long max, LinkedList<Long> pointsOfInterest2) {
		this.r = random;
		this.min = min;
		this.max = max;
		this.pointsOfInterest = pointsOfInterest2;
		if (pointsOfInterest.isEmpty()) {
			rangesOfInterest = Collections.emptyList();
			return ;
		}
		List<Long> boundaryValues = new ArrayList<>();
		for (long l : this.pointsOfInterest) {
			long low = l-1, high = l+1;
			if (low >= min && low <= max) {
				boundaryValues.add(low);
			}
			if (high >= min && high <= max) {
				boundaryValues.add(high);
			}
		}
		this.pointsOfInterest.addAll(boundaryValues);
		
		Collections.sort(this.pointsOfInterest);
		removeDuplicatesFromSorted(this.pointsOfInterest);
		if (this.min > this.pointsOfInterest.get(0) || this.max < this.pointsOfInterest.get(this.pointsOfInterest.size()-1)) {
			throw new RuntimeException("Range (" + this.pointsOfInterest +
					") should have its values between min (" + this.min + ") and max (" + this.max + ")");
		}
		this.rangesOfInterest = computeRangesFromSorted(this.pointsOfInterest);
	}

	private List<Tuple2<Long, Long>> computeRangesFromSorted(LinkedList<Long> pointsOfInterest) {
		ArrayList<Tuple2<Long, Long>> ranges = new ArrayList<>();
		long rangeStart = this.min;
		ListIterator<Long> it = pointsOfInterest.listIterator();
		while(it.hasNext()) {
			long poi = it.next();
			long rangeEnd = poi - 1;
			if (rangeStart < rangeEnd) {
				ranges.add(new Tuple2<Long, Long>(rangeStart, rangeEnd));
			} else if (rangeStart == rangeEnd) {
				it.previous();
				it.add(rangeStart);
				it.next();
			}
			rangeStart = poi + 1;
		}
		if (rangeStart < max) {
			ranges.add(new Tuple2<Long, Long>(rangeStart, max));
		} else if (rangeStart == max) {
			pointsOfInterest.add(rangeStart);
		}
		return ranges;
	}

	private static void removeDuplicatesFromSorted(LinkedList<Long> longs) {
		if (longs.isEmpty()) {
			return;
		}
		ListIterator<Long> it = longs.listIterator();
		long prev = it.next();
		while (it.hasNext()) {
			long next = it.next();
			if (next == prev) {
				it.remove();
			} else {
				prev = next;
			}
		}
	}
	
	public long getRandom() {
		if (pointsOfInterest.isEmpty()) {
			return Calculator.randWithinRange(min, max);
		} else {
			return getRandom(r.nextInt(this.pointsOfInterest.size() + this.rangesOfInterest.size()));
		}
	}
	
	/**
	 * Get the point of interest or a random number from a range of interest specified
	 * by this index
	 * @param i
	 * @return
	 */
	private  long getRandom(int i) {
		int j;
		if (i < this.pointsOfInterest.size()) {
			return this.pointsOfInterest.get(i);
		} else if ((j = i - this.pointsOfInterest.size()) < this.rangesOfInterest.size()) {
			Tuple2<Long, Long> rangeOfInterest = this.rangesOfInterest.get(j);
			return Rand.nextLong(r, rangeOfInterest.tuple1 - rangeOfInterest.tuple0) + rangeOfInterest.tuple0;
		} else {
			throw new RuntimeException("Index (" + i + ") should be smaller than sum of points and ranges of interest ("
						+ (this.pointsOfInterest.size() + this.rangesOfInterest.size()) + ")");
		}
	}
	
	/**
	 * The number of points/ranges of interest
	 * @return
	 */
	public int getNumberOfInterests() {
		return this.pointsOfInterest.size() + this.rangesOfInterest.size();
	}
}
